package com.wixpress.app.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * This class implements the signed instance parse algorithm
 * for more details you can go to: http://dev.wix.com/docs/display/DRAF/Using+the+Signed+App+Instance
 */
public class AuthenticationResolver {
    // Constants for app key and app secret key:
    // should be changed once you register your application with Wix Dev center (http://dev.wix.com/app/create-app)

    //App keys for the first app in Wix's dev dashboard
    /**
     * For security reasons, secret key has been omitted. Should be changed in production environment.
     * public class AppKeys {
     * public final static String APPLICATION_KEY = "YOUR_APP_KEY";
     * public final static String APPLICATION_SECRET_KEY = "YOUR_SECRET_KEY";
     * }
     */
    private final static String APPLICATION_KEY = AppKeys.APPLICATION_KEY;
    private final static String APPLICATION_SECRET_KEY = AppKeys.APPLICATION_SECRET_KEY;
    private static final String SIGN_ALGORITHM = "HMACSHA256";
    private final Base64 base64;
    private final Mac mac;
    private final ObjectMapper objectMapper;

    /**
     * AuthenticationResolver Constructor
     * Initialize the parse algorithm
     */
    public AuthenticationResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        try {
            // initialization
            base64 = new Base64(256, new byte[0], true);
            SecretKeySpec secretKeySpec = new SecretKeySpec(APPLICATION_SECRET_KEY.getBytes(), SIGN_ALGORITHM);

            // The Mac class provides the functionality of a "Message Authentication Code" (MAC) algorithm. (http://docs.oracle.com/javase/7/docs/api/javax/crypto/Mac.html)
            mac = Mac.getInstance(SIGN_ALGORITHM);
            mac.init(secretKeySpec);

        } catch (IllegalArgumentException e) {
            throw new AuthenticationResolverMarshallerException("failed creating Base64 codec: [" + e.getMessage() + "]");
        } catch (NoSuchAlgorithmException e) {
            throw new AuthenticationResolverMarshallerException("failed creating mac algorithm : [" + e.getMessage() + "]");
        } catch (InvalidKeyException e) {
            throw new AuthenticationResolverMarshallerException("failed init mac algorithm : [" + e.getMessage() + "]");
        }
    }

    /**
     * Parse the signed instance
     *
     * @param signedInstance - The instance parameter that was created bty The Wix Platform
     * @return AppInstance that represent the parse of the instance parameter
     */
    public AppInstance unsignInstance(String signedInstance) {
        return unmarshal(signedInstance, AppInstance.class);
    }

    private <T> T unmarshal(String value, Class<T> valueClass) {
        // Split the signed-instance
        int idx = value.indexOf(".");
        byte[] sig = base64.decode(value.substring(0, idx).getBytes());

        String rawPayload = value.substring(idx + 1);

        String payload = new String(base64.decode(rawPayload));

        try {
            byte[] mySig = mac.doFinal(rawPayload.getBytes());

            if (!Arrays.equals(mySig, sig)) {
                throw new InvalidSignatureException("Request signed-instance signature invalid. Are you using the right secret?");
            } else {
                return objectMapper.readValue(payload, valueClass);
            }
        } catch (IOException e) {
            throw new AuthenticationResolverMarshallerException("failed writing payload [" + payload + "] as json : [" + e.getMessage() + "]");
        }
    }
}
