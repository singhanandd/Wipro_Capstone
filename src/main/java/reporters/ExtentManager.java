package reporters;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExtentManager {
    private static ExtentReports extent;

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            extent = createInstance("target/extent-report.html");
        }
        return extent;
    }

    private static ExtentReports createInstance(String reportPath) {
        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);

        Path cfg = Paths.get("src/test/resources/extent-config.xml");
        if (Files.exists(cfg)) {
            try {
                spark.loadXMLConfig(cfg.toFile());
            } catch (Exception e) {
                System.err.println("Extent XML is present but failed to load: " + e.getMessage());
            }
        } else {
            System.err.println("Extent XML not found; proceeding with defaults.");
        }

        ExtentReports ex = new ExtentReports();
        ex.attachReporter(spark);
        return ex;
    }

    public static void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
}
