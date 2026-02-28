package com.lubesoft.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256 CBC encryption/decryption utility.
 * The secret key is derived from a salt string using SHA-256.
 * The IV is derived from the first 16 bytes of the MD5 of the salt.
 */
public class AESUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * Derives a 256-bit AES key from the given salt string.
     */
    private static SecretKey deriveKey(String salt) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(salt.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Derives a 128-bit IV from the salt using MD5.
     */
    private static IvParameterSpec deriveIv(String salt) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] ivBytes = md5.digest(salt.getBytes(StandardCharsets.UTF_8));
        return new IvParameterSpec(ivBytes);
    }

    /**
     * Encrypts plaintext using AES-256-CBC and returns a Base64-encoded ciphertext.
     */
    public static String encrypt(String plaintext, String salt) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(salt), deriveIv(salt));
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypts a Base64-encoded ciphertext encrypted with AES-256-CBC.
     */
    public static String decrypt(String ciphertext, String salt) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(salt), deriveIv(salt));
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
