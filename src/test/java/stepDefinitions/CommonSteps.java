package stepDefinitions;

import base.DriverFactory;
import io.cucumber.java.en.Given;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import utils.ConfigReader;

public class CommonSteps {
    @Given("I launch the application")
    public void i_launch_the_application() {
        WebDriver driver = DriverFactory.getDriver();
        String baseUrl = ConfigReader.getProperty("baseUrl", "https://www.amazon.in/");
       // String basUel1 = ConfigReader.
        driver.get(baseUrl);
    }

    @Given("I launch the application in mobile view")
    public void i_launch_the_application_in_mobile_view() {
        WebDriver driver = DriverFactory.getDriver();
        driver.manage().window().setSize(new Dimension(390, 844)); // iPhone-ish
        String baseUrl = ConfigReader.getProperty("baseUrl", "https://www.amazon.in/");
        
        driver.get(baseUrl);
    }
}
