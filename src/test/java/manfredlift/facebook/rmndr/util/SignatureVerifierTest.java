package manfredlift.facebook.rmndr.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SignatureVerifierTest {
    @Test
    public void testVerify_success() {
        boolean isValid = SignatureVerifier.isValid("test_app_secret",
            "sha1=004e377c0db93160cf7b58f3fa2023e74202b8f7", "{\"äö\":\"[[saok$@*@#*\"}");

        assertThat(isValid, is(true));
    }

    @Test
    public void testVerify_fail() {
        boolean isValid = SignatureVerifier.isValid("test_app_secret",
            "sha1=004e377c0db93160cf7b58f3fa2023e74202b8f7", "dummy request body");

        assertThat(isValid, is(false));
    }
}
