package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.ElementUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class PaymentPage {
    private WebDriver driver;
    private ElementUtils elementUtils;
    private WebDriverWait wait;
    private JavascriptExecutor js;

    public PaymentPage(WebDriver driver) {
        this.driver = driver;
        this.elementUtils = new ElementUtils(driver);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.js = (JavascriptExecutor) driver;
    }

    // --- locators used by the class ---
    private List<By> deliverButtonCandidates = Arrays.asList(
            By.xpath("//button[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'deliver to this address')]"),
            By.xpath("//a[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'deliver to this address')]"),
            By.xpath("//button[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'deliver to this')]"),
            By.xpath("//a[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'deliver to this')]"),
            By.xpath("//button[contains(.,'Deliver to this address')]"),
            By.xpath("//a[contains(.,'Deliver to this address')]")
    );

    private List<By> addAddressCandidates = Arrays.asList(
            By.xpath("//button[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'add a new delivery address')]"),
            By.xpath("//a[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'add a new delivery address')]"),
            By.xpath("//button[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'add a new address')]"),
            By.xpath("//a[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'add a new address')]")
    );

    private List<By> paymentPanelLocators = Arrays.asList(
            By.xpath("//h2[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'payment method')]/ancestor::div[1]"),
            By.xpath("//div[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'payment method') and (contains(@class,'section') or contains(@class,'panel'))]"),
            By.xpath("//h2[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'payment method')]")
    );

    private List<By> creditCardLocators = Arrays.asList(
            By.xpath("//input[@value='creditCard']"),
            By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'credit')]"),
            By.xpath("//input[contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'card')]"),
            By.xpath("//input[@type='radio' and (contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'card') or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'card') or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'payment'))]"),
            By.xpath("//label[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'credit') or contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'card')]/input"),
            By.xpath("//input[@aria-label and contains(translate(@aria-label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'card')]")
    );

    private List<By> paymentErrorLocators = Arrays.asList(
            By.cssSelector(".a-alert-content"),
            By.xpath("//*[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'card declined')]"),
            By.xpath("//*[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'invalid card')]"),
            By.xpath("//*[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'payment failed')]"),
            By.xpath("//*[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'try again')]")
    );

    // --------------------
    // 1) Ensure delivery address is selected (or at least attempt)
    // --------------------
    public void ensureDeliveryAddressSelected() {
        System.out.println("[PaymentPage] ensureDeliveryAddressSelected: checking for deliver/add address buttons...");

        // Try deliver button candidates
        for (By by : deliverButtonCandidates) {
            try {
                List<WebElement> els = driver.findElements(by);
                for (WebElement el : els) {
                    if (el != null && el.isDisplayed()) {
                        safeClick(el);
                        System.out.println("[PaymentPage] Clicked deliver button using: " + by);
                        waitForPaymentSectionToLoad();
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // If no deliver button, try Add a new delivery address (only useful if test is prepared to fill address)
        for (By by : addAddressCandidates) {
            try {
                List<WebElement> els = driver.findElements(by);
                for (WebElement el : els) {
                    if (el != null && el.isDisplayed()) {
                        System.out.println("[PaymentPage] Clicked 'Add delivery address' using: " + by);
                        safeClick(el);
                        // If you click add address, you normally need to fill address fields; we wait a bit
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        waitForPaymentSectionToLoad();
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Nothing clickable found — log a helpful message
        System.out.println("[PaymentPage] No 'Deliver to this address' or 'Add delivery address' button found. " +
                "If there is no saved address you must create one earlier in the test so payment options appear.");
    }

    private void waitForPaymentSectionToLoad() {
        System.out.println("[PaymentPage] waitForPaymentSectionToLoad: waiting for Payment method panel or any payment inputs to appear...");
        try {
            // Wait for either the payment panel header or any common credit card input to appear
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            shortWait.until(driver -> {
                // payment panel header visible?
                for (By by : paymentPanelLocators) {
                    try {
                        List<WebElement> els = driver.findElements(by);
                        for (WebElement e : els) {
                            if (e.isDisplayed()) return true;
                        }
                    } catch (Exception ignored) {}
                }
                // or any known card input present?
                for (By by : creditCardLocators) {
                    try {
                        List<WebElement> els = driver.findElements(by);
                        for (WebElement e : els) {
                            if (e.isDisplayed()) return true;
                        }
                    } catch (Exception ignored) {}
                }
                return false;
            });
            // small buffer
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        } catch (Exception e) {
            System.out.println("[PaymentPage] waitForPaymentSectionToLoad: timed out waiting for payment section. " +
                    "Payment UI may still be hidden until address selection is complete or until bank iframe loads.");
        }
    }

    // --------------------
    // 2) Select payment method (ensures address first)
    // --------------------
    public boolean selectPaymentMethod(String method) {
        System.out.println("[PaymentPage] selectPaymentMethod: " + method);
        // Ensure address is selected before trying to manipulate payment controls
        ensureDeliveryAddressSelected();

        if (method.equalsIgnoreCase("Cash on Delivery") || method.equalsIgnoreCase("COD")) {
            By codOption = By.xpath("//input[@value='Cash on Delivery' or contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'cod')]");
            try {
                elementUtils.click(codOption);
                System.out.println("[PaymentPage] clicked COD");
                return true;
            } catch (Exception e) {
                System.out.println("[PaymentPage] COD click failed: " + e.getMessage());
            }
        }

        if (method.equalsIgnoreCase("Credit Card") || method.equalsIgnoreCase("Card")) {
            // Try locating in top-level DOM
            for (By by : creditCardLocators) {
                try {
                    List<WebElement> els = driver.findElements(by);
                    for (WebElement el : els) {
                        if (el != null && el.isDisplayed()) {
                            safeClick(el);
                            System.out.println("[PaymentPage] clicked credit-card using locator: " + by);
                            return true;
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Try top-level iframes
            List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
            System.out.println("[PaymentPage] top-level iframe count: " + iframes.size());
            for (WebElement iframe : iframes) {
                try {
                    driver.switchTo().frame(iframe);
                    for (By by : creditCardLocators) {
                        try {
                            List<WebElement> els = driver.findElements(by);
                            for (WebElement el : els) {
                                if (el != null && el.isDisplayed()) {
                                    safeClick(el);
                                    System.out.println("[PaymentPage] clicked credit-card inside iframe using locator: " + by);
                                    driver.switchTo().defaultContent();
                                    return true;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception fr) {
                    // ignore frame switching errors
                } finally {
                    driver.switchTo().defaultContent();
                }
            }

            System.out.println("[PaymentPage] credit-card element NOT found using any locator.");
            return false;
        }

        System.out.println("[PaymentPage] Unsupported payment method: " + method);
        return false;
    }

    // --------------------
    // Helper: safeClick with JS fallback
    // --------------------
    private void safeClick(WebElement el) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(el));
            el.click();
        } catch (Exception e) {
            try {
                js.executeScript("arguments[0].scrollIntoView(true);", el);
                js.executeScript("arguments[0].click();", el);
            } catch (Exception ex) {
                System.out.println("[PaymentPage] safeClick fallback failed: " + ex.getMessage());
                throw ex;
            }
        }
    }

    // --------------------
    // 3) Fill invalid card details (robust)
    // --------------------
    public boolean selectInvalidPayment(String method) {
        if (!method.equalsIgnoreCase("Credit Card") && !method.equalsIgnoreCase("Card")) {
            return false;
        }

        System.out.println("[PaymentPage] selectInvalidPayment: filling invalid CC values");

        List<By> cardNumberCandidates = Arrays.asList(
                By.id("addCreditCardNumber"),
                By.name("addCreditCardNumber"),
                By.xpath("//input[contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'card') and (contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'number') or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'number'))]"),
                By.xpath("//input[@placeholder and contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'card')]"),
                By.cssSelector("input[type='tel']"),
                By.xpath("//input[contains(translate(@aria-label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'card number')]")
        );

        boolean filledCard = false;

        // Try top-level DOM first
        for (By by : cardNumberCandidates) {
            try {
                List<WebElement> els = driver.findElements(by);
                for (WebElement el : els) {
                    if (el != null && el.isDisplayed()) {
                        el.clear();
                        el.sendKeys("1234567890123456");
                        System.out.println("[PaymentPage] filled card number using: " + by);
                        filledCard = true;
                        break;
                    }
                }
                if (filledCard) break;
            } catch (Exception ignored) {}
        }

        // Try inside iframes if not filled
        if (!filledCard) {
            List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
            for (WebElement iframe : iframes) {
                try {
                    driver.switchTo().frame(iframe);
                    for (By by : cardNumberCandidates) {
                        try {
                            List<WebElement> els = driver.findElements(by);
                            for (WebElement el : els) {
                                if (el != null && el.isDisplayed()) {
                                    el.clear();
                                    el.sendKeys("1234567890123456");
                                    System.out.println("[PaymentPage] filled card number inside iframe using: " + by);
                                    filledCard = true;
                                    break;
                                }
                            }
                            if (filledCard) break;
                        } catch (Exception ignored) {}
                    }
                } catch (Exception fr) {
                    // ignore
                } finally {
                    driver.switchTo().defaultContent();
                }
                if (filledCard) break;
            }
        }

        // Fill simple month/year/cvv top-level attempts (add iframe search if needed)
        trySendKeysIfPresent(By.id("ppw-expirationDate_month"), "01");
        trySendKeysIfPresent(By.id("ppw-expirationDate_year"), "2000");
        trySendKeysIfPresent(By.id("addCreditCardVerificationNumber"), "000");

        // Click any Add/Use/Continue button if present
        List<By> addCardButtons = Arrays.asList(
                By.xpath("//input[@type='submit' and (contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'add') or contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'use this card') or contains(translate(@value,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'continue'))]"),
                By.xpath("//button[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'add card') or contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'use this card') or contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'continue')]")
        );

        for (By by : addCardButtons) {
            try {
                List<WebElement> btns = driver.findElements(by);
                for (WebElement btn : btns) {
                    if (btn != null && btn.isDisplayed()) {
                        safeClick(btn);
                        System.out.println("[PaymentPage] clicked Add/Use/Continue button: " + by);
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        return filledCard;
    }

    private void trySendKeysIfPresent(By by, String value) {
        try {
            List<WebElement> els = driver.findElements(by);
            for (WebElement el : els) {
                if (el != null && el.isDisplayed()) {
                    el.clear();
                    el.sendKeys(value);
                    System.out.println("[PaymentPage] set " + by + " => " + value);
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    // --------------------
    // 4) Detect payment failure message heuristically
    // --------------------
    public boolean isPaymentFailed() {
        for (By by : paymentErrorLocators) {
            try {
                List<WebElement> els = driver.findElements(by);
                for (WebElement e : els) {
                    String txt = e.getText();
                    if (txt != null && !txt.trim().isEmpty()) {
                        String lower = txt.toLowerCase();
                        if (lower.contains("invalid") || lower.contains("declined") || lower.contains("failed") || lower.contains("error") || lower.contains("try again")) {
                            System.out.println("[PaymentPage] Found error text: " + txt);
                            return true;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Fallback: search body for keywords
        try {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            if (body.contains("invalid card") || body.contains("card declined") || body.contains("payment failed") || body.contains("enter a valid") || body.contains("try again")) {
                System.out.println("[PaymentPage] Found error keywords in body.");
                return true;
            }
        } catch (Exception ignored) {}

        System.out.println("[PaymentPage] No payment error message found.");
        return false;
    }
}
