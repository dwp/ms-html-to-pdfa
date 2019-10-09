package uk.gov.dwp.pdfa.application;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.dwp.pdfa.VersionInformationResource;
import uk.gov.dwp.pdfa.transform.HtmlToPdfGenerator;
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
    final HtmlToPdfResource instance = new HtmlToPdfResource(new HtmlToPdfGenerator());
    final VersionInformationResource versionInfo = new VersionInformationResource();

    environment.jersey().register(versionInfo);
    environment.jersey().register(instance);
  }

  public static void main(String[] args) throws Exception {
    new HtmlToPdfApplication().run(args);
  }
}
