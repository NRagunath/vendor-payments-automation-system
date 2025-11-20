package com.shanthigear.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class WebhookSignatureVerifier {
    private static final String HMAC_SHA256 = "HmacSHA256";

    public static boolean verifySignature(String payload, String signature, String secret) {
        try {
            String computedSignature = computeHmac256(payload, secret);
            return computedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private static String computeHmac256(String data, String key) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256Hmac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        sha256Hmac.init(secretKey);
        byte[] signedBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signedBytes);
    }
}
