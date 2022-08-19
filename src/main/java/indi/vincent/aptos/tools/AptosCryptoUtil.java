package indi.vincent.aptos.tools;

import cn.hutool.core.util.RandomUtil;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.encoders.Hex;

public class AptosCryptoUtil {

    /**
     * 生成 ED_25519 签名信息
     *
     * @param privateKeyHex 私钥
     * @param message       消息
     * @return 签名
     */
    public static String ed25519Signature(String privateKeyHex, String message) {
        try {
            byte[] privateKeyBytes = Hex.decode(privateKeyHex);
            byte[] messageBytes = Hex.decode(message);
            Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(privateKeyBytes);
            Signer signer = new Ed25519Signer();
            signer.init(true, privateKey);
            signer.update(messageBytes, 0, messageBytes.length);
            return Hex.toHexString(signer.generateSignature());
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 验证 ED_25519 签名信息
     *
     * @param publicKeyHex 公钥
     * @param signatureHex 签名
     * @param message      消息
     * @return True or False
     */
    public static Boolean ed25519Validate(String publicKeyHex, String signatureHex, String message) {
        byte[] publicKeyBytes = Hex.decode(publicKeyHex);
        byte[] signatureBytes = Hex.decode(signatureHex);
        byte[] messageBytes = Hex.decode(message);
        Ed25519PublicKeyParameters publicKey = new Ed25519PublicKeyParameters(publicKeyBytes);
        Signer verifier = new Ed25519Signer();
        verifier.init(false, publicKey);
        verifier.update(messageBytes, 0, messageBytes.length);
        return verifier.verifySignature(signatureBytes);
    }

    /**
     * 从公钥创建地址
     *
     * @param publicKeyBytes 公钥字节数组
     * @return 地址的字节数组
     */
    public static byte[] createAddress(byte[] publicKeyBytes) {
        return aptosSHA256(publicKeyBytes);
    }

    /**
     * 从私钥创建公钥
     *
     * @param privateKeyBytes 私钥字节数组
     * @return 公钥的字节数组
     */
    public static byte[] createPublicKey(byte[] privateKeyBytes) {
        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(privateKeyBytes, 0);
        return privateKey.generatePublicKey().getEncoded();
    }

    /**
     * 随机生成一个私钥
     *
     * @return 私钥的字节数组
     */
    public static byte[] randomPrivateKey() {
        return RandomUtil.randomBytes(32);
    }

    /**
     * SHA3 256 算法，Aptos 需要在原始消息字节数组后面补一个值为 0 的 Byte
     *
     * @param bytes 原始消息
     * @return 摘要字节数组
     */
    public static byte[] aptosSHA256(byte[] bytes) {
        SHA3.DigestSHA3 digestSHA3 = new SHA3.Digest256();
        byte[] newBytes = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
        newBytes[newBytes.length - 1] = 0;
        digestSHA3.update(newBytes);
        return digestSHA3.digest();
    }

}