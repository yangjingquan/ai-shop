package com.shop.merchant.service;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** Encrypts merchant payment secrets before they are persisted. */
@Component
public class PaymentCredentialCipher {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String encodedKey;

    public PaymentCredentialCipher(@Value("${shop.payment-credentials.encryption-key:}") String encodedKey) {
        this.encodedKey = encodedKey;
    }

    public String encrypt(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key(), "AES"),
                    new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.WX_PAY_CONFIG_INCOMPLETE.getCode(), "支付凭据加密失败");
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (!value.startsWith(PREFIX)) {
            throw new BusinessException(ErrorCode.WX_PAY_CONFIG_INCOMPLETE.getCode(), "支付凭据未按安全格式保存，请在运营后台重新配置");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            if (payload.length <= IV_LENGTH) {
                throw new IllegalArgumentException("invalid encrypted credential");
            }
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] ciphertext = java.util.Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key(), "AES"),
                    new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.WX_PAY_CONFIG_INCOMPLETE.getCode(), "支付凭据无法解密，请检查服务端主密钥");
        }
    }

    private byte[] key() {
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey == null ? "" : encodedKey.trim());
            if (key.length != 32) {
                throw new IllegalArgumentException("expected 32-byte key");
            }
            return key;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.WX_PAY_CONFIG_INCOMPLETE.getCode(),
                    "未配置 SHOP_PAYMENT_CREDENTIAL_ENCRYPTION_KEY（需为 Base64 编码的 32 字节密钥）");
        }
    }
}
