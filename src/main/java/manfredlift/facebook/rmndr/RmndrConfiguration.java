package manfredlift.facebook.rmndr;

import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import lombok.Data;

@Data
public class RmndrConfiguration extends Configuration {
    private String verifyToken;

    private String pageAccessToken;

    private String appSecret;

    private String witToken;

    private boolean testing = false;

    private JerseyClientConfiguration jerseyClient;

    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return jerseyClient;
    }
}
