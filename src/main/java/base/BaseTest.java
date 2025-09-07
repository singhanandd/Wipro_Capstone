package base;

import java.awt.Desktop;
import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.testng.ITestResult;
import org.testng.annotations.*;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;

import reporters.ExtentManager;          // <-- make sure this file exists under src/main/java/reporters
import utils.ResultsExcelWriter;        // <-- simple stub provided below
import utils.ScreenshotUtils;           // <-- simple helper provided below

public class BaseTest {

    protected WebDriver driver;
    protected static ExtentReports extent;

    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();
    private static String RUN_FOLDER;

    protected void startTest(String name) { TEST.set(extent.createTest(name)); }
    protected ExtentTest getTest() { return TEST.get(); }

    @BeforeSuite(alwaysRun = true)
    public void setupExtent() {
        extent = ExtentManager.getInstance();

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        RUN_FOLDER = "test-output/screenshots/run_" + ts;

        ScreenshotUtils.initRunFolder(RUN_FOLDER);
        ResultsExcelWriter.init();
    }

    @Parameters({ "baseUrl", "browser" })
    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method,
                      @Optional("https://www.amazon.in/") String baseUrl,
                      @Optional("chrome") String browser) {

        driver = DriverFactory.create(browser);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

        TEST.set(extent.createTest(method.getName()));
        openUrlWithRetries(baseUrl, 4, Duration.ofSeconds(5));
    }

    private void openUrlWithRetries(String url, int maxAttempts, Duration waitBetween) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                driver.get(url);
                return;
            } catch (WebDriverException e) {
                String msg = String.valueOf(e.getMessage());
                boolean transientNet =
                        msg.contains("ERR_INTERNET_DISCONNECTED")
                     || msg.contains("ERR_PROXY_CONNECTION_FAILED")
                     || msg.contains("ERR_TIMED_OUT")
                     || msg.contains("ERR_NAME_NOT_RESOLVED")
                     || msg.contains("ERR_NETWORK_CHANGED");

                if (!transientNet || attempt == maxAttempts) { last = e; break; }
                try { Thread.sleep(waitBetween.toMillis()); } catch (InterruptedException ignored) {}
            } catch (RuntimeException e) { last = e; break; }
        }
        if (last != null) throw last;
    }

    @AfterMethod(alwaysRun = true)
    public void afterEach(ITestResult result) {
        String status;
        String snapPath = null;

        try {
            snapPath = ScreenshotUtils.takeScreenshot(driver, result.getName());

            if (result.getStatus() == ITestResult.FAILURE) {
                status = "FAIL";
                getTest().fail(result.getThrowable(),
                        MediaEntityBuilder.createScreenCaptureFromPath(snapPath).build());
            } else if (result.getStatus() == ITestResult.SUCCESS) {
                status = "PASS";
                getTest().pass("Test passed",
                        MediaEntityBuilder.createScreenCaptureFromPath(snapPath).build());
            } else {
                status = "SKIP";
                getTest().skip("Test skipped",
                        MediaEntityBuilder.createScreenCaptureFromPath(snapPath).build());
            }
        } catch (Exception e) {
            status = (result.getStatus() == ITestResult.SUCCESS) ? "PASS"
                   : (result.getStatus() == ITestResult.FAILURE ? "FAIL" : "SKIP");
        }

        try { ResultsExcelWriter.append(result.getName(), status, snapPath); } catch (Exception ignored) {}

        if (driver != null) driver.quit();
        try { TEST.remove(); } catch (Exception ignored) {}
    }

    @AfterSuite(alwaysRun = true)
    public void tearDownSuite() {
        try { if (extent != null) extent.flush(); } catch (Exception ignored) {}
        try { ResultsExcelWriter.close(); } catch (Exception ignored) {}
        openExtentSparkReport();
    }

    private void openExtentSparkReport() {
        try {
            File report = new File("reports/ExtentSparkReport.html");
            if (report.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(report.toURI());
                System.out.println("✅ Report: " + report.getAbsolutePath());
            } else {
                System.out.println("⚠️ Report not found at reports/ExtentSparkReport.html");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not auto-open Extent report:");
            e.printStackTrace();
        }
    }
}
