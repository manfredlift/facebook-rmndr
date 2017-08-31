package manfredlift.facebook.rmndr;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import manfredlift.facebook.rmndr.client.FbClient;
import manfredlift.facebook.rmndr.properties.QuartzPropertiesFactory;
import manfredlift.facebook.rmndr.resources.HealthCheckResource;
import manfredlift.facebook.rmndr.resources.WebhookResource;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

import javax.ws.rs.client.Client;
import java.util.Properties;

@Slf4j
public class RmndrApplication extends Application<RmndrConfiguration> {

    public static void main(String[] args) throws Exception {
        new RmndrApplication().run(args);
    }

    @Override
    public void run(RmndrConfiguration configuration, Environment environment) throws Exception {
        final Client client = new JerseyClientBuilder(environment).using(configuration.getJerseyClientConfiguration())
            .build(getName());

        final FbClient fbClient = new FbClient(configuration, client);

        final Properties quartzProperties = QuartzPropertiesFactory.create();
        final StdSchedulerFactory schedulerFactory = new StdSchedulerFactory(quartzProperties);
        final Scheduler scheduler = schedulerFactory.getScheduler();

        scheduler.getContext().put(RmndrConstants.FB_CLIENT, fbClient);
        scheduler.getContext().put(RmndrConstants.ACCESS_TOKEN, configuration.getPageAccessToken());
        scheduler.start();

        environment.jersey().register(new HealthCheckResource());
        environment.jersey().register(new WebhookResource(configuration, fbClient, scheduler));
    }


    @Override
    public void initialize(Bootstrap<RmndrConfiguration> bootstrap) {
        // Enable variable substitution with environment variables in config file
        bootstrap.setConfigurationSourceProvider(
            new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)
            )
        );
    }
}
