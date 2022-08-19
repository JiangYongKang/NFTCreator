package indi.vincent.aptos.tools;

import cn.hutool.json.JSONUtil;
import okhttp3.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Aptos TestCoin 水龙头
 */
public class AptosFaucetClient {

    private final String host;
    private final OkHttpClient client;
    private final MediaType mediaType;

    public AptosFaucetClient(String host) {
        this.host = host;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .callTimeout(1, TimeUnit.MINUTES)
                .build();
        this.mediaType = MediaType.parse("application/json");
    }

    /**
     * 领取 TestCoin
     *
     * @param receiverAddress 接收方地址
     * @param amount          领取金额
     * @return 交易哈希
     */
    public List<String> invokeMint(String receiverAddress, BigInteger amount) {
        try {
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(this.host + "/mint")).newBuilder();
            urlBuilder.addQueryParameter("address", receiverAddress);
            urlBuilder.addQueryParameter("amount", amount.toString());
            RequestBody requestBody = RequestBody.create("", this.mediaType);
            Request request = new Request.Builder().url(urlBuilder.build().toString()).post(requestBody).build();
            Response response = this.client.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            return JSONUtil.toList(responseBody, String.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}