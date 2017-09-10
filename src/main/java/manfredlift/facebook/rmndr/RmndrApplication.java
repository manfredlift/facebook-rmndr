package manfredlift.facebook.rmndr;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import manfredlift.facebook.rmndr.client.FbClient;
import manfredlift.facebook.rmndr.client.WitClient;
import manfredlift.facebook.rmndr.properties.QuartzSchedulerFactory;
import manfredlift.facebook.rmndr.resources.HealthCheckResource;
import manfredlift.facebook.rmndr.resources.WebhookResource;
import org.quartz.Scheduler;

import javax.ws.rs.client.Client;

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
        final WitClient witClient = new WitClient(configuration, client);

        final Scheduler scheduler = QuartzSchedulerFactory.create(configuration);

        scheduler.getContext().put(RmndrConstants.FB_CLIENT, fbClient);
        scheduler.getContext().put(RmndrConstants.ACCESS_TOKEN, configuration.getPageAccessToken());
        scheduler.start();

        environment.jersey().register(new HealthCheckResource());
        environment.jersey().register(new WebhookResource(configuration, fbClient, witClient, scheduler));
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
