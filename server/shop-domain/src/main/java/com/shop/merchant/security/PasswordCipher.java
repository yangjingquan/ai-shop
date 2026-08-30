package com.shop.merchant.security;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

/**
 * 用于后台密码字段的应用层加密。私钥只保存在服务端内存中，公钥通过认证接口提供给前端。
 */
@Component
public class PasswordCipher {

    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private final PrivateKey privateKey;
    private final String publicKey;

    public PasswordCipher() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            this.privateKey = keyPair.getPrivate();
            this.publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("初始化密码加密密钥失败", e);
        }
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String decrypt(String encryptedPassword) {
        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedPassword);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey, new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new BusinessException(ErrorCode.ENCRYPTED_PASSWORD_INVALID);
        }
    }
}
