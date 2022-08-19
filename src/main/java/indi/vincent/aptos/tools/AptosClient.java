package indi.vincent.aptos.tools;

import cn.hutool.core.annotation.Alias;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import lombok.*;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AptosClient {

    private final String host;
    private final OkHttpClient client;
    private final MediaType mediaType;

    public AptosClient(String host) {
        this.host = host;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .callTimeout(1, TimeUnit.MINUTES)
                .build();
        this.mediaType = MediaType.parse("application/json");
    }

    /**
     * 提交包含 Payload 数据的交易
     *
     * @param account            Aptos 账户
     * @param transactionPayload 交易的 Payload
     * @return 交易哈希
     */
    public String executeTransactionWithPayload(AptosAccount account, TransactionPayload transactionPayload) {

        SignatureMessageParam signatureMessageParam = this.doGenerateTransaction(account.getAddress(), transactionPayload);
        SignatureMessageResponse signatureMessageResponse = this.invokeCreateTransactionSigningMessage(signatureMessageParam);
        TransactionSignature transactionSignature = this.doSignatureTransaction(account, signatureMessageResponse);

        SubmitTransactionParam submitTransactionParam = new SubmitTransactionParam();
        BeanUtil.copyProperties(signatureMessageParam, submitTransactionParam);
        submitTransactionParam.setSignature(transactionSignature);

        SubmitTransactionResponse submitTransactionResponse = this.invokeSubmitTransaction(submitTransactionParam);
        return submitTransactionResponse.getHash();
    }


    /**
     * 查询账户
     *
     * @param address 账户地址
     * @return 账户数据
     */
    public AccountResponse invokeGetAccount(String address) {
        try {
            Request request = new Request.Builder().url(this.host + "/accounts/" + address).get().build();
            Response response = this.client.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            return JSONUtil.toBean(responseBody, AccountResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询 Name 对应的地址
     *
     * @param name Name Service
     * @return 地址
     */
    public String invokeGetAddress(String name) {
        try {
            Request request = new Request.Builder().url("https://www.aptosnames.com/api/v1/address/" + name).get().build();
            Response response = this.client.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            Object address = JSONUtil.getByPath(JSONUtil.parseObj(responseBody), "$.address");
            return address == null || address.toString().equalsIgnoreCase("NULL") ? null : address.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取发送放的 Sequence Number，组装 SignatureMessageParam
     *
     * @param sender             发送方地址
     * @param transactionPayload 交易的 Payload
     * @return SignatureMessageParam 对象
     */
    public SignatureMessageParam doGenerateTransaction(String sender, TransactionPayload transactionPayload) {
        AccountResponse response = this.invokeGetAccount(sender);
        SignatureMessageParam signatureMessageParam = new SignatureMessageParam();
        signatureMessageParam.setSequenceNumber(response.getSequenceNumber());
        signatureMessageParam.setSender(sender);
        signatureMessageParam.setMaxGasAmount("2000");
        signatureMessageParam.setGasUnitPrice("1");
        signatureMessageParam.setExpirationTimestampSecs(String.valueOf(System.currentTimeMillis() / 1000 + 600));
        signatureMessageParam.setPayload(transactionPayload);
        return signatureMessageParam;
    }

    /**
     * 使用 ED_25519 进行签名
     *
     * @param account                  Aptos 账户
     * @param signatureMessageResponse 待签名信息
     * @return 交易签名
     */
    public TransactionSignature doSignatureTransaction(AptosAccount account, SignatureMessageResponse signatureMessageResponse) {
        String signatureHex = AptosCryptoUtil.ed25519Signature(account.getPrivateKey(), signatureMessageResponse.getMessage().substring(2));
        TransactionSignature transactionSignature = new TransactionSignature();
        transactionSignature.setType("ed25519_signature");
        transactionSignature.setPublicKey(account.getPublicKey());
        transactionSignature.setSignature("0x" + signatureHex);
        return transactionSignature;
    }

    /**
     * Create transaction signing message
     * This API creates transaction signing message for client to create transaction signature.
     * The success response contains hex-encoded signing message bytes.
     * <p>
     * API Doc: https://aptos.dev/rest-api#tag/transactions/operation/create_signing_message
     *
     * @param signatureMessageParam 包含 Payload 的请求数据
     * @return 需要签名的消息
     */
    public SignatureMessageResponse invokeCreateTransactionSigningMessage(SignatureMessageParam signatureMessageParam) {
        try {
            RequestBody requestBody = RequestBody.create(JSONUtil.toJsonStr(signatureMessageParam), this.mediaType);
            Request request = new Request.Builder().url(this.host + "/transactions/signing_message").post(requestBody).build();
            Response response = this.client.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            return JSONUtil.toBean(responseBody, SignatureMessageResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Submit Transaction
     * Submit transaction using JSON without additional tools
     * Send POST /transactions/signing_message to create transaction signing message.
     * Sign the transaction signing message and create transaction signature.
     * Submit the user transaction request with the transaction signature. The request header "Content-Type" must set to "application/json".
     * <p>
     * API Doc: https://aptos.dev/rest-api#tag/transactions/operation/submit_transaction
     *
     * @param submitTransactionParam 带有交易发送方签名的请求数据
     * @return 带有交易哈希的响应
     */
    public SubmitTransactionResponse invokeSubmitTransaction(SubmitTransactionParam submitTransactionParam) {
        try {
            RequestBody requestBody = RequestBody.create(JSONUtil.toJsonStr(submitTransactionParam), this.mediaType);
            Request request = new Request.Builder().url(this.host + "/transactions").post(requestBody).build();
            Response response = this.client.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            return JSONUtil.toBean(responseBody, SubmitTransactionResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    // ----------------------------------- Request Params and Response Body --------------------------------------------

    @Data
    public static class BaseResponse {
        private String code;
        private String message;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AccountResponse extends BaseResponse {
        @Alias("sequence_number")
        private String sequenceNumber;
        @Alias("authentication_key")
        private String authenticationKey;
    }

    @Data
    public static class SignatureMessageParam {
        @Alias("sender")
        private String sender;
        @Alias("sequence_number")
        private String sequenceNumber;
        @Alias("max_gas_amount")
        private String maxGasAmount;
        @Alias("gas_unit_price")
        private String gasUnitPrice;
        @Alias("gas_currency_code")
        private String gasCurrencyCode;
        @Alias("expiration_timestamp_secs")
        private String expirationTimestampSecs;
        @Alias("payload")
        private TransactionPayload payload;
        @Alias("secondary_signers")
        private List<String> secondarySigners;
    }


    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SubmitTransactionParam extends SignatureMessageParam {
        @Alias("signature")
        private TransactionSignature signature;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SubmitTransactionResponse extends SubmitTransactionParam {
        @Alias("hash")
        private String hash;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionPayload {
        @Alias("type")
        private String type;
        @Alias("function")
        private String function;
        @Alias("type_arguments")
        private List<String> typeArguments = new ArrayList<>();
        @Alias("arguments")
        private List<?> arguments = new ArrayList<>();
    }

    @Data
    public static class TransactionSignature {
        @Alias("type")
        private String type;
        @Alias("public_key")
        private String publicKey;
        @Alias("signature")
        private String signature;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SignatureMessageResponse extends BaseResponse {
        private String message;
    }

}