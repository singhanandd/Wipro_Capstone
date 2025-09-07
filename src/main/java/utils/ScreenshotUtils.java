package utils;

import org.openqa.selenium.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotUtils {

    // Run folder is set once per suite by BaseTest
    private static volatile String RUN_FOLDER = null;

    /** Called by BaseTest @BeforeSuite */
    public static synchronized void initRunFolder(String runFolder) {
        if (RUN_FOLDER == null) {
            RUN_FOLDER = runFolder;
            try { Files.createDirectories(Paths.get(RUN_FOLDER)); } catch (Exception ignored) {}
        }
    }

    private static String safe(String s) {
        return s == null ? "image" : s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /** Takes a screenshot and returns the relative path (used by Extent + Excel). */
    public static String takeScreenshot(WebDriver driver, String name) {
        if (RUN_FOLDER == null) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            initRunFolder("test-output/screenshots/run_" + ts);
        }

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String fileName = safe(name) + "_" + ts + ".png";
        Path dest = Paths.get(RUN_FOLDER, fileName);

        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), dest);
            System.out.println("Screenshot saved: " + dest.toString().replace('\\', '/'));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dest.toString().replace('\\', '/');
    }
}
