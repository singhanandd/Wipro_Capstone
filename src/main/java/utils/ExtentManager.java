package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.aventstack.extentreports.reporter.configuration.ViewName;

public class ExtentManager {
    private static ExtentReports extent;

    public static ExtentReports getInstance() {
        if (extent == null) {
            String outDir = System.getProperty("user.dir") + "/test-output/";
            new java.io.File(outDir).mkdirs();

            // Dashboard-first file
            ExtentSparkReporter dash = new ExtentSparkReporter(outDir + "ExtentReport_Dashboard.html");
            dash.config().setDocumentTitle("Amazon Automation Report");
            dash.config().setReportName("Amazon Search Functionality");
            dash.config().setTheme(Theme.STANDARD);
            dash.viewConfigurer().viewOrder().as(new ViewName[]{
                    ViewName.DASHBOARD, ViewName.TEST, ViewName.CATEGORY, ViewName.AUTHOR, ViewName.DEVICE, ViewName.EXCEPTION
            });

            // Tests-first file
            ExtentSparkReporter tests = new ExtentSparkReporter(outDir + "ExtentReport_Tests.html");
            tests.config().setDocumentTitle("Amazon Automation Report");
            tests.config().setReportName("Amazon Search Functionality");
            tests.config().setTheme(Theme.STANDARD);
            tests.viewConfigurer().viewOrder().as(new ViewName[]{
                    ViewName.TEST, ViewName.DASHBOARD, ViewName.CATEGORY, ViewName.AUTHOR, ViewName.DEVICE, ViewName.EXCEPTION
            });

            extent = new ExtentReports();
            extent.attachReporter(dash, tests);

            extent.setSystemInfo("Project", "amazon-search-automation");
            extent.setSystemInfo("Env", "Local");
            extent.setSystemInfo("Browser", System.getProperty("browser", "chrome"));
        }
        return extent;
    }
}
