package runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

@CucumberOptions(
    // point to the folder that contains all your .feature files
    features = "src/test/resources/features",
    // glue code package where your step definitions live
    glue = {"stepDefinitions", "hooks"},
    // output reports
    plugin = {
        "pretty",
        "html:target/cucumber.html",
        "json:target/cucumber.json"
    },
    monochrome = true
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {

    // Single-threaded data provider (change to true to run scenarios in parallel)
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
