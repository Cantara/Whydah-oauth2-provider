package net.whydah.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.spec.KeySpec;

public class ClientIdUtil {

    private static final String padding = "11111111111111111111111111111111111111111111111111111111111111111111111111111111111111";
    private static final String keyPassword = "myKeyPassword";
    private static Key key = generateNewKey(keyPassword);
    private static final Logger log = LoggerFactory.getLogger(ClientIdUtil.class);

    public static String getApplicationId(String clientId) {

        String applicationId = xorHex(decrypt(clientId, key), padding);
        return applicationId;
    }

    public static String getClientID(String applicationId) {
        log.info("Resolving applicationId:" + applicationId);
        if (applicationId == null || applicationId.length() < 4) {
            return "null";
        }
        String xorString = xorHex(applicationId, padding);

        String clientID = encrypt(xorString, key);
        log.info("Resolved clientId:" + clientID);
        return clientID;
    }

    public static String xorHex(String a, String b) {
        // TODO: Validation
        char[] chars = new char[a.length()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = toHex(fromHex(a.charAt(i)) ^ fromHex(b.charAt(i)));
        }
        return new String(chars);
    }

    private static int fromHex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        throw new IllegalArgumentException();
    }

    private static char toHex(int nybble) {
        if (nybble < 0 || nybble > 15) {
            throw new IllegalArgumentException();
        }
        return "0123456789ABCDEF".charAt(nybble);
    }

    private static final String ALGORITHM = "AES";

    public static String encrypt(final String valueEnc, final Key key) {

        String encryptedVal = null;

        try {
            final Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, key);
            final byte[] encValue = c.doFinal(valueEnc.getBytes());
            encryptedVal = new BASE64Encoder().encode(encValue);
        } catch (Exception ex) {
            System.out.println("The Exception is=" + ex);
        }

        return encryptedVal;
    }

    public static String decrypt(final String encryptedValue, final Key key) {

        String decryptedValue = null;

        try {

            final Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, key);
            final byte[] decorVal = new BASE64Decoder().decodeBuffer(encryptedValue);
            final byte[] decValue = c.doFinal(decorVal);
            decryptedValue = new String(decValue);
        } catch (Exception ex) {
            System.out.println("The Exception is=" + ex);
        }

        return decryptedValue;
    }

    private static Key generateNewKey(String keypassword) {
        try {
            char[] password = keypassword.toCharArray();
            byte[] salt = "jkjk".getBytes();
            /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
            return secret;
        } catch (Exception e) {
            // TODO
            return null;
        }
    }

}
