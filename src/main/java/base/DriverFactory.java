package base;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class DriverFactory {
    private static WebDriver driver;

    public static WebDriver getDriver() {
        if (driver == null) {
            driver = create("chrome"); // default to chrome
        }
        return driver;
    }

    public static WebDriver create(String browser) {
        if (browser == null) browser = "chrome";

        switch (browser.toLowerCase()) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver();
                break;

            case "edge":
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver();
                break;

            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                if ("true".equalsIgnoreCase(System.getProperty("headless"))) {
                    options.addArguments("--headless=new");
                }
                driver = new ChromeDriver(options);
                break;
        }

        driver.manage().window().maximize();
        return driver;
    }

    public static void quitDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
}
