package utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ElementUtils {
    private WebDriver driver;

    public ElementUtils(WebDriver driver) {
        this.driver = driver;
    }

    // ---------- Basic Actions ----------
    public void click(By locator) {
        driver.findElement(locator).click();
    }

    public void sendKeys(By locator, String text) {
        try {
            java.util.List<WebElement> elements = driver.findElements(locator);
            if (elements.isEmpty()) {
                System.out.println("DEBUG: Element not found for sendKeys -> " + locator);
                return;
            }
            WebElement element = elements.get(0);
            if (!element.isDisplayed() || !element.isEnabled()) {
                System.out.println("DEBUG: Element not interactable -> " + locator);
                return;
            }
            element.clear();
            element.sendKeys(text);
        } catch (Exception e) {
            System.out.println("DEBUG: sendKeys failed on " + locator + " -> " + e.getMessage());
        }
    }


    public boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- New Helper Methods ----------
    // Get visible text of an element
    public String getText(By locator) {
        return driver.findElement(locator).getText();
    }

    // Get attribute value (e.g. value, href, src, etc.)
    public String getAttribute(By locator, String attribute) {
        return driver.findElement(locator).getAttribute(attribute);
    }

    // Clear input field
    public void clear(By locator) {
        driver.findElement(locator).clear();
    }

    // Check if element exists (without throwing exception)
    public boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
