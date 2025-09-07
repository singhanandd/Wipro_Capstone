package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.ElementUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class CheckoutPage {
    private WebDriver driver;
    private ElementUtils elementUtils;
    private WebDriverWait wait;

    private By orderReviewSection = By.id("subtotals");
    private By placeOrderBtn = By.name("placeYourOrder1");
    private By confirmationMsg = By.cssSelector(".a-box-inner h1");

    public CheckoutPage(WebDriver driver) {
        this.driver = driver;
        this.elementUtils = new ElementUtils(driver);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(25));
    }

    // ---------- SHIPPING ----------
    public void enterShippingDetails(String name, String street, String city, String pin) {
        elementUtils.sendKeys(By.id("address-ui-widgets-enterAddressFullName"), name);
        elementUtils.sendKeys(By.id("address-ui-widgets-enterAddressLine1"), street);
        elementUtils.sendKeys(By.id("address-ui-widgets-enterAddressCity"), city);
        elementUtils.sendKeys(By.id("address-ui-widgets-enterAddressPostalCode"), pin);
    }

    public boolean isAddressAccepted() {
        return elementUtils.isDisplayed(orderReviewSection);
    }

    // ---------- CHECKOUT ----------
    public void clickProceedToCheckout() {
        List<By> proceedLocators = Arrays.asList(
            By.id("sc-buy-box-ptc-button"),
            By.id("proceedToCheckout"),
            By.name("proceedToCheckout"),
            By.xpath("//input[@name='proceedToRetailCheckout' or @id='proceedToCheckout']"),
            By.xpath("//a[contains(@href,'/gp/cart/view.html') and " +
                     "(contains(.,'Proceed to checkout') or contains(.,'Checkout'))]"),
            By.xpath("//*[self::button or self::a or self::input]" +
                     "[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'proceed to checkout')]"),
            By.xpath("//*[self::button or self::a]" +
                     "[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'place your order')]")
        );

        boolean clicked = false;
        for (By by : proceedLocators) {
            try {
                WebElement el = wait.until(ExpectedConditions.elementToBeClickable(by));
                try {
                    el.click();
                } catch (ElementClickInterceptedException e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                }
                // Wait for checkout page indicators
                wait.until(d -> d.getCurrentUrl().contains("/gp/buy") ||
                                d.getCurrentUrl().contains("/checkout") ||
                                d.getTitle().toLowerCase().contains("checkout") ||
                                !d.findElements(placeOrderBtn).isEmpty());
                clicked = true;
                break;
            } catch (TimeoutException ignored) {}
        }

        if (!clicked) {
            dumpPage("proceed_to_checkout_failed.html");
            throw new NoSuchElementException("Proceed to checkout button not found on cart / overlay / PDP.");
        }
    }

    // ---------- ORDER REVIEW & CONFIRM ----------
    public boolean isOrderReviewDisplayed() {
        return elementUtils.isDisplayed(orderReviewSection);
    }

    public void placeOrder() {
        elementUtils.click(placeOrderBtn);
    }

    public boolean isOrderConfirmed() {
        return elementUtils.isDisplayed(confirmationMsg);
    }

    // ---------- DEBUG ----------
    private void dumpPage(String filename) {
        try {
            Files.write(Paths.get(filename),
                driver.getPageSource().getBytes(StandardCharsets.UTF_8));
            System.out.println("DEBUG: dumped page to " + filename);
        } catch (Exception e) {
            System.out.println("DEBUG: failed to dump page: " + e.getMessage());
        }
    }
}
