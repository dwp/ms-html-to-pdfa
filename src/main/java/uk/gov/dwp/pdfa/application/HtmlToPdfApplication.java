package uk.gov.dwp.pdfa.application;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.metrics.servlets.MetricsServlet;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import uk.gov.dwp.pdf.generator.HtmlToPdfFactory;
import uk.gov.dwp.pdfa.VersionInformationResource;
import uk.gov.dwp.pdfa.HtmlToPdfResource;

public class HtmlToPdfApplication extends Application<Configuration> {

  @Override
  protected void bootstrapLogging() {
    // to prevent dropwizard using its own standard logger
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
  }

  @Override
  public void run(Configuration configuration, Environment environment) throws Exception {
    final HtmlToPdfResource instance = new HtmlToPdfResource(HtmlToPdfFactory.create());
    final VersionInformationResource versionInfo = new VersionInformationResource();

    environment
        .admin()
        .addServlet("metrics", new MetricsServlet(environment.metrics()))
        .addMapping("/metrics");

    environment.jersey().register(versionInfo);
    environment.jersey().register(instance);
  }

  public static void main(String[] args) throws Exception {
    new HtmlToPdfApplication().run(args);
  }
}
