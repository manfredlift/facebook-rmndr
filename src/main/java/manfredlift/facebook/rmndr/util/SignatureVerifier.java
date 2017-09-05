package manfredlift.facebook.rmndr.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

public class SignatureVerifier {
    public static boolean isValid(String appSecret, String signature, String requestBody) {
        byte[] sha1 = HmacUtils.hmacSha1(appSecret.getBytes(StandardCharsets.UTF_8),
            requestBody.getBytes(StandardCharsets.UTF_8));
        return StringUtils.equals("sha1=" + Hex.encodeHexString(sha1), signature);
    }
}
