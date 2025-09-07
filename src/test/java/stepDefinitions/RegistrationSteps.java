package stepDefinitions;

import base.DriverFactory;
import io.cucumber.java.en.*;
import org.testng.Assert;
import pages.RegistrationPage;
import utils.ConfigReader;

/**
 * Step definitions for Amazon Registration feature.
 */
public class RegistrationSteps {

    private RegistrationPage registrationPage;

    private RegistrationPage rp() {
        if (registrationPage == null) {
            registrationPage = new RegistrationPage(DriverFactory.getDriver());
        }
        return registrationPage;
    }

    // ================= Positive Flow =================

    @When("I register a new account")
    public void i_register_a_new_account() {
        // Use mobile by default for Amazon IN (or switch to regEmail if needed)
        String mobileOrEmail = System.getProperty("regMobile", ConfigReader.getProperty("regMobile"));
        String name          = System.getProperty("regName",   ConfigReader.getProperty("regName"));
        String password      = System.getProperty("regPassword", ConfigReader.getProperty("regPassword"));

        rp().openCreateAccountDirect();
        rp().fillCreateAccount(mobileOrEmail, name, password);
        rp().submitCreateAccount();
    }

    @Then("I should see OTP verification page")
    public void i_should_see_otp_verification_page() {
        Assert.assertTrue(rp().isOtpPageShown(), "OTP verification page not shown.");
    }

    // ================= Negative Flow =================

    @When("I register with blank details")
    public void i_register_with_blank_details() {
        rp().openCreateAccountDirect();
        // intentionally leave fields blank
        rp().submitCreateAccount();
    }

    @Then("I should see mandatory field validation messages")
    public void i_should_see_mandatory_field_validation_messages() {
        Assert.assertTrue(rp().hasValidationMessages(),
                "Expected mandatory field validation messages.");
    }

    // ================= Parameterized Flow =================

    @When("I register with mobile {string}, name {string}, and password {string}")
    public void i_register_with_parameters(String mobile, String name, String password) {
        rp().openCreateAccountDirect();
        
        rp().fillCreateAccount(mobile, name, password);
        rp().submitCreateAccount();
    }
}
