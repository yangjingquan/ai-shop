package com.shop.merchant.service;

import org.junit.jupiter.api.Test;
import com.shop.common.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentCredentialCipherTest {

    private static final String KEY = "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";

    @Test
    void encryptsAndDecryptsPaymentCredential() {
        PaymentCredentialCipher cipher = new PaymentCredentialCipher(KEY);
        String plaintext = "api-v3-secret";

        String encrypted = cipher.encrypt(plaintext);

        assertFalse(encrypted.contains(plaintext));
        assertEquals(plaintext, cipher.decrypt(encrypted));
    }

    @Test
    void rejectsPlaintextCredential() {
        PaymentCredentialCipher cipher = new PaymentCredentialCipher(KEY);
        assertThrows(BusinessException.class, () -> cipher.decrypt("plaintext"));
    }
}
