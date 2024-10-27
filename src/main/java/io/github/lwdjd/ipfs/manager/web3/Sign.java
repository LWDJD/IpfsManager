package io.github.lwdjd.ipfs.manager.web3;

import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;

public class Sign {
    public static String sign(ECKeyPair keyPair){
        // 获取账户地址
        String address = Keys.getAddress(keyPair);

        // 对账户地址进行签名
        byte[] message = Numeric.hexStringToByteArray(address);
        ECDSASignature signatureBytes = keyPair.sign(message);

        // 将签名转换为十六进制字符串
//        String signature = Numeric.toHexStringWithPrefix();
        return "";
    }
}
