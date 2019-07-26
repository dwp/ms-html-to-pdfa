package uk.gov.dwp.pdfa;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import io.dropwizard.Configuration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.dwp.pdfa.application.HtmlToPdfApplication;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

@RunWith(Cucumber.class)
@SuppressWarnings({"squid:S2187", "squid:S1118"}) // deliberately has no tests and no private constructor needed
@CucumberOptions(plugin = "json:target/cucumber-report.json", tags = {})
public class RunCukesTest {

    @ClassRule
    public static final DropwizardAppRule<Configuration> RULE = new DropwizardAppRule<>(HtmlToPdfApplication.class, resourceFilePath("test.yml"));
}
