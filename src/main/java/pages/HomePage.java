package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/** Resilient helpers for header + hamburger navigation on Amazon. */
public class HomePage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By logo = By.id("nav-logo-sprites");
    private final By hamburger = By.id("nav-hamburger-menu");
    private final By visibleMenu = By.cssSelector("div.hmenu-visible");
    private final By menuItems = By.cssSelector("a.hmenu-item");
    private final By searchResults = By.cssSelector("[data-component-type='s-search-result']");

    public HomePage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(25));
    }

    public boolean isLogoDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(logo)).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Opens the hamburger and tries to click a category whose visible text
     * equals or contains the provided name (case-insensitive).
     */
    public void navigateToCategory(String category) {
        // Open hamburger
        wait.until(ExpectedConditions.elementToBeClickable(hamburger)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(visibleMenu));

        // Try exact match first
        List<WebElement> items = driver.findElements(menuItems);
        for (WebElement it : items) {
            String text = it.getText().trim();
            if (text.equalsIgnoreCase(category)) {
                it.click();
                return;
            }
        }
        // Fallback: contains() match
        for (WebElement it : items) {
            String text = it.getText().trim();
            if (text.toLowerCase().contains(category.toLowerCase())) {
                it.click();
                return;
            }
        }
        throw new NoSuchElementException("Category not found in hamburger: " + category);
    }

    public boolean waitForAnyProductResults() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(d -> d.findElements(searchResults).size() > 0);
            return driver.findElements(searchResults).size() > 0;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
