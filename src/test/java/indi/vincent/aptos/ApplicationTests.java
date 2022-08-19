package indi.vincent.aptos;


import cn.hutool.core.util.HexUtil;
import indi.vincent.aptos.tools.AptosAccount;
import indi.vincent.aptos.tools.AptosClient;
import indi.vincent.aptos.tools.AptosCryptoUtil;
import indi.vincent.aptos.tools.AptosFaucetClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Unit test for simple App.
 */
public class ApplicationTests {
    private final AptosClient aptosClient = new AptosClient("https://fullnode.devnet.aptoslabs.com");
    private final AptosFaucetClient aptosFaucetClient = new AptosFaucetClient("https://faucet.devnet.aptoslabs.com");

    @Test
    public void invokeAccountsRequestTest() {
        String address = "0x8A9F294ADFF2290A285970B68EDA71153DEB8D67664AF5A1DFD140C7E813F879";
        AptosClient.AccountResponse accountResp = aptosClient.invokeGetAccount(address);
        Assertions.assertNotNull(accountResp);
        Assertions.assertNotNull(accountResp.getSequenceNumber());
        Assertions.assertEquals(accountResp.getAuthenticationKey(), address.toLowerCase());
    }

    @Test
    public void invokeCreateTransactionSigningMessageTest() {

        byte[] privateKeyBytes = AptosCryptoUtil.randomPrivateKey();
        byte[] publicKeyBytes = AptosCryptoUtil.createPublicKey(privateKeyBytes);
        byte[] addressBytes = AptosCryptoUtil.createAddress(publicKeyBytes);

        AptosAccount account = new AptosAccount();
        account.setPrivateKey(HexUtil.encodeHexStr(privateKeyBytes));
        account.setPublicKey(HexUtil.encodeHexStr(publicKeyBytes));
        account.setAddress(HexUtil.encodeHexStr(addressBytes));

        aptosFaucetClient.invokeMint(account.getAddress(), BigInteger.valueOf(1000000L));


        AptosClient.TransactionPayload transactionPayload = new AptosClient.TransactionPayload();
        transactionPayload.setType("script_function_payload");
        transactionPayload.setFunction("0xf4eb1f3e838411ab992f81cabb25f29ea4eb2406cd167261273da587c3615792::service::claim_name");
        transactionPayload.setArguments(
                Collections.singletonList(
                        HexUtil.encodeHexStr("xxasdasx".getBytes(StandardCharsets.UTF_8))
                )
        );

        aptosClient.executeTransactionWithPayload(account, transactionPayload);
    }
}
