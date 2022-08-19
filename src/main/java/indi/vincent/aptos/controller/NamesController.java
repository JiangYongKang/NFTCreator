package indi.vincent.aptos.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.json.JSONUtil;
import indi.vincent.aptos.tools.AptosAccount;
import indi.vincent.aptos.tools.AptosClient;
import indi.vincent.aptos.tools.AptosCryptoUtil;
import indi.vincent.aptos.tools.AptosFaucetClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/names")
public class NamesController {

    @Value("${aptos.function}")
    private String aptosFunction;
    @Value("${aptos.keystore}")
    private String aptosKeystore;

    private final AptosClient aptosClient = new AptosClient("https://fullnode.devnet.aptoslabs.com");
    private final AptosFaucetClient aptosFaucetClient = new AptosFaucetClient("https://faucet.devnet.aptoslabs.com");

    @PostMapping
    public ResponseEntity<?> create(@RequestBody List<String> names) {

        Set<String> unregisteredNames = names.stream().filter(name -> {
            String address = aptosClient.invokeGetAddress(name);
            if (address != null) {
                log.info("地址 {} -> 已经注册了 {}，跳过注册", address.toUpperCase(), name);
            }
            return address == null;
        }).collect(Collectors.toSet());

        List<String> transactionHash = unregisteredNames.stream().map(name -> {
            byte[] privateKeyBytes = AptosCryptoUtil.randomPrivateKey();
            byte[] publicKeyBytes = AptosCryptoUtil.createPublicKey(privateKeyBytes);
            byte[] addressBytes = AptosCryptoUtil.createAddress(publicKeyBytes);
            AptosAccount account = new AptosAccount(privateKeyBytes, publicKeyBytes, addressBytes, name);
            String filePath = aptosKeystore + account.getAddress().toUpperCase() + ".JSON";
            FileUtil.writeString(JSONUtil.toJsonStr(account), filePath, StandardCharsets.UTF_8);
            log.info("生成私钥文件：{} -> {}", filePath, name);

            aptosFaucetClient.invokeMint(account.getAddress(), BigInteger.valueOf(10000L));
            AptosClient.TransactionPayload transactionPayload = new AptosClient.TransactionPayload();
            transactionPayload.setType("script_function_payload");
            transactionPayload.setFunction(aptosFunction);
            transactionPayload.setArguments(
                    Collections.singletonList(
                            HexUtil.encodeHexStr(name.getBytes(StandardCharsets.UTF_8))
                    )
            );
            String hash = aptosClient.executeTransactionWithPayload(account, transactionPayload);
            log.info("发送交易 {}，成功注册 {}", hash, name);
            return hash;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(transactionHash);
    }

}
