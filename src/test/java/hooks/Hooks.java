package hooks;

import base.DriverFactory;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import io.cucumber.java.*;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import reporters.ExtentManager;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class Hooks {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();

    @BeforeAll
    public static void beforeAll() {
        DriverFactory.getDriver();

        try {
            extent = ExtentManager.getInstance();
        } catch (Exception e) {
            System.err.println("Extent init warning: " + e.getMessage());
            extent = null;
        }
    }

    @AfterAll
    public static void afterAll() {
        try {
            if (extent != null) extent.flush();
        } catch (Exception e) {
            System.err.println("Extent flush warning: " + e.getMessage());
        }
        DriverFactory.quitDriver();
    }

    @Before
    public void beforeScenario(Scenario scenario) {
        if (extent != null) {
            TEST.set(extent.createTest(scenario.getName()));
        }
    }

    @After
    public void afterScenario(Scenario scenario) {
        WebDriver driver = DriverFactory.getDriver();
        try {
            if (driver instanceof TakesScreenshot) {
                byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                String base64 = Base64.getEncoder().encodeToString(png);

                // ✅ Save screenshot in screenshots folder
                saveScreenshotToFile(png, scenario);

                if (extent != null && TEST.get() != null) {
                    if (scenario.isFailed()) {
                        TEST.get().fail("Scenario failed",
                            MediaEntityBuilder.createScreenCaptureFromBase64String(base64, scenario.getName()).build());
                    } else {
                        TEST.get().pass("Scenario passed",
                            MediaEntityBuilder.createScreenCaptureFromBase64String(base64, scenario.getName()).build());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("After scenario reporting warning: " + e.getMessage());
        } finally {
            TEST.remove();
            try {
                driver.manage().deleteAllCookies();
            } catch (Exception ignored) {}
        }
    }

    private void saveScreenshotToFile(byte[] pngBytes, Scenario scenario) {
        try {
            String screenshotDir = System.getProperty("user.dir") + File.separator + "screenshots";
            File dir = new File(screenshotDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = screenshotDir + File.separator + scenario.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".png";
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                fos.write(pngBytes);
            }
        } catch (Exception e) {
            System.err.println("Failed to save screenshot: " + e.getMessage());
        }
    }
}
