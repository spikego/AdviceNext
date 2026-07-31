package cn.advicenext.cloudmusic;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class WeApiCrypto {
    private static final String PRESET_KEY = "0CoJUm6Qyw8W8jud";
    private static final String IV = "0102030405060708";
    private static final String RSA_PUB_KEY = "010001";
    private static final String RSA_MODULUS =
        "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725" +
        "152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312" +
        "ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424" +
        "d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static String generateSecretKey() {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String aesEncrypt(String text, String key) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] ivBytes = IV.getBytes(StandardCharsets.UTF_8);
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

            int pad = 16 - textBytes.length % 16;
            byte[] padded = new byte[textBytes.length + pad];
            System.arraycopy(textBytes, 0, padded, 0, textBytes.length);
            for (int i = textBytes.length; i < padded.length; i++) {
                padded[i] = (byte) pad;
            }

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new IvParameterSpec(ivBytes));
            byte[] encrypted = cipher.doFinal(padded);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String rsaEncrypt(String text) {
        String reversed = new StringBuilder(text).reverse().toString();
        byte[] textBytes = reversed.getBytes(StandardCharsets.UTF_8);
        StringBuilder hex = new StringBuilder();
        for (byte b : textBytes) {
            hex.append(String.format("%02x", b & 0xff));
        }
        BigInteger textBigInt = new BigInteger(hex.toString(), 16);

        BigInteger pubKey = new BigInteger(RSA_PUB_KEY, 16);
        BigInteger modulus = new BigInteger(RSA_MODULUS, 16);
        BigInteger result = textBigInt.modPow(pubKey, modulus);
        String hexResult = result.toString(16);
        while (hexResult.length() < 256) {
            hexResult = "0" + hexResult;
        }
        return hexResult;
    }

    public static Map<String, String> encode(Map<String, String> params) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) json.append(",");
            first = false;
            json.append("\"").append(e.getKey()).append("\":");
            if (e.getValue() == null) {
                json.append("null");
            } else if (e.getValue().equals("true") || e.getValue().equals("false")) {
                json.append(e.getValue());
            } else {
                try {
                    Integer.parseInt(e.getValue());
                    json.append(e.getValue());
                } catch (NumberFormatException ex) {
                    json.append("\"").append(escapeJson(e.getValue())).append("\"");
                }
            }
        }
        json.append("}");
        String text = json.toString();

        String secKey = generateSecretKey();
        String encText = aesEncrypt(aesEncrypt(text, PRESET_KEY), secKey);
        String encSecKey = rsaEncrypt(secKey);

        Map<String, String> result = new HashMap<>();
        result.put("params", encText);
        result.put("encSecKey", encSecKey);
        return result;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}