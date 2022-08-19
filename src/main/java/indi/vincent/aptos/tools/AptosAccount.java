package indi.vincent.aptos.tools;

import cn.hutool.core.util.HexUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AptosAccount {

    private String privateKey;
    private String publicKey;
    private String address;
    private String name;

    public AptosAccount(byte[] privateKeyBytes, byte[] publicKeyBytes, byte[] addressBytes, String name) {
        this(
                HexUtil.encodeHexStr(privateKeyBytes),
                HexUtil.encodeHexStr(publicKeyBytes),
                HexUtil.encodeHexStr(addressBytes),
                name
        );
    }
}
