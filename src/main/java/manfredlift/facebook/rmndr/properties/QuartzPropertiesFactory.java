package manfredlift.facebook.rmndr.properties;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class QuartzPropertiesFactory {
    public static Properties create() {
        Properties properties = new Properties();
        try {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = classloader.getResourceAsStream("quartz.properties");
            properties.load(inputStream);
        } catch (IOException e) {
            log.error("Could not load Quartz properties.");
            return null;
        }

        String rdsHostname = System.getenv("RDS_HOSTNAME");
        String rdsPort = System.getenv("RDS_PORT");
        String rdsDbName = System.getenv("RDS_DB_NAME");
        String rdsUrl = String.format("jdbc:postgresql://%s:%s/%s", rdsHostname, rdsPort, rdsDbName);

        properties.setProperty("org.quartz.dataSource.rmndrDS.URL", rdsUrl);
        properties.setProperty("org.quartz.dataSource.rmndrDS.user", System.getenv("RDS_USERNAME"));
        properties.setProperty("org.quartz.dataSource.rmndrDS.password", System.getenv("RDS_PASSWORD"));

        return properties;
    }
}
