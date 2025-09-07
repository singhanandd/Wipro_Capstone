package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.ConfigReader;

import java.time.Duration;
import java.util.List;

public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // --------- URLs ---------
    private final String loginUrl = ConfigReader.getProperty(
            "loginUrl", "https://www.amazon.in/ap/signin");

    // --------- Sign-in selectors ---------
    // Amazon allows mobile OR email in the same field
    private final By identifierInput = By.cssSelector("#ap_email, #ap_email_login");
    private final By continueBtn = By.cssSelector(
            "input#continue, input.a-button-input[aria-labelledby='continue-announce']");
    private final By passwordInput = By.id("ap_password");
    private final By signInBtn = By.id("signInSubmit");

    // --------- Interstitials ---------
    private final By proceedCreateAccountBtn = By.cssSelector(
            "input.a-button-input[aria-labelledby='intention-submit-button-announce']");
    private final By altSignInLink = By.xpath(
            "//a[contains(.,'Sign in with another email') or contains(.,'Already a customer') or contains(.,'Sign in instead')]");

    // --------- Messages / Alerts (widened coverage) ---------
    private final By errorBoxes = By.cssSelector(
            "#auth-error-message-box .a-alert-content," +          // red error content
            "#auth-error-message-box .a-list-item," +              // list items inside error
            "#auth-warning-message-box .a-alert-content," +        // yellow warning
            "#auth-info-message-box .a-alert-content," +           // info
            "#auth-email-missing-alert .a-alert-content," +        // inline email alert
            "#auth-password-missing-alert .a-alert-content," +     // inline password alert
            ".a-box.a-alert .a-alert-content," +                   // any alert content
            ".a-alert-content"                                     // generic fallback
    );
    private final By errorHeadings = By.cssSelector(
            "#auth-error-message-box .a-alert-heading," +
            "#auth-warning-message-box .a-alert-heading," +
            "#auth-info-message-box .a-alert-heading"
    );

    // --------- Misc ---------
    private final By cookieAccept = By.cssSelector("#sp-cc-accept, input[name='accept'], button[name='accept']");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        int explicit = Integer.parseInt(ConfigReader.getProperty("explicitWait", "25"));
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(explicit));
    }

    // --------- Core Methods ---------
    public void openSignInPage() {
        driver.navigate().to(loginUrl);

        // try cookie accept first (you already had this)
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(cookieAccept)).click();
        } catch (Exception ignored) {}

        // Minimal overlay dismissal to handle the "Continue shopping" interstitial and similar
        try { dismissOverlays(); } catch (Exception ignored) {}

        // existing wait for the key sign-in elements (unchanged logic)
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(identifierInput),
                ExpectedConditions.visibilityOfElementLocated(passwordInput),
                ExpectedConditions.visibilityOfElementLocated(proceedCreateAccountBtn)
        ));
    }

    public boolean isOnSignInPage() {
        String url = driver.getCurrentUrl().toLowerCase();
        return url.contains("/ap/signin") || url.contains("/ax/claim/intent")
                || url.contains("signin") || url.contains("claim");
    }

    /** Type mobile/email. */
    public void enterIdentifier(String mobileOrEmail) {
        WebElement box = wait.until(ExpectedConditions.visibilityOfElementLocated(identifierInput));
        box.clear();
        box.sendKeys(mobileOrEmail);
    }

    public void clickContinue() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(continueBtn));
        btn.click();
    }

    /** If an interstitial appears, proceed or return to classic sign-in when needed. */
    public boolean clickProceedToCreateAccountIfPresent() {
        try {
            WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(6))
                    .until(ExpectedConditions.elementToBeClickable(proceedCreateAccountBtn));
            btn.click();
            return true;
        } catch (TimeoutException ignored) {
            return false;
        }
    }

    public boolean clickAltSignInIfPresent() {
        try {
            WebElement link = new WebDriverWait(driver, Duration.ofSeconds(6))
                    .until(ExpectedConditions.elementToBeClickable(altSignInLink));
            link.click();
            return true;
        } catch (TimeoutException ignored) {
            return false;
        }
    }

    public void enterPassword(String pwd) {
        WebElement box = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordInput));
        box.clear();
        box.sendKeys(pwd);
    }

    /** Wait for either the password box (valid identifier) OR an error message (invalid identifier). */
    public boolean waitForPasswordOrError(int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.or(
                            ExpectedConditions.visibilityOfElementLocated(passwordInput),
                            ExpectedConditions.visibilityOfElementLocated(errorBoxes),
                            ExpectedConditions.visibilityOfElementLocated(errorHeadings)
                    ));
        } catch (TimeoutException ignored) {}
        return isPasswordVisible();
    }

    private boolean isPasswordVisible() {
        try {
            WebElement el = driver.findElement(passwordInput);
            return el.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /** More robust click: if still on identifier page, try Continue; if no sign-in button, just return. */
    public void clickSignIn() {
        if (!isPasswordVisible()) {
            try {
                WebElement cont = new WebDriverWait(driver, Duration.ofSeconds(6))
                        .until(ExpectedConditions.elementToBeClickable(continueBtn));
                cont.click();
            } catch (Exception ignored) {}
        }

        try {
            WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(signInBtn));
            btn.click();
        } catch (TimeoutException e) {
            // negative flow: no sign-in button present; that's fine
        } catch (Exception e) {
            try {
                WebElement btn = driver.findElement(signInBtn);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click()", btn);
            } catch (Exception ignored) {}
        }
    }

    /** Collects any visible auth message text (robust). */
    public String getErrorMessage() {
        // 1) try content items
        try {
            String msg = new WebDriverWait(driver, Duration.ofSeconds(6))
                    .until(ExpectedConditions.visibilityOfElementLocated(errorBoxes))
                    .getText().trim();
            if (!msg.isBlank()) return msg;
        } catch (Exception ignored) {}

        // 2) try headings
        try {
            String heading = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.visibilityOfElementLocated(errorHeadings))
                    .getText().trim();
            if (!heading.isBlank()) return heading;
        } catch (Exception ignored) {}

        // 3) last-ditch: any visible .a-alert text on the page
        try {
            List<WebElement> alerts = driver.findElements(By.cssSelector(".a-box.a-alert, #auth-error-message-box, #auth-warning-message-box"));
            StringBuilder sb = new StringBuilder();
            for (WebElement a : alerts) {
                if (a.isDisplayed()) sb.append(a.getText()).append(" ");
            }
            String all = sb.toString().trim();
            if (!all.isBlank()) return all;
        } catch (Exception ignored) {}

        return "";
    }

    // ----------------- Minimal helpers added (do not modify) -----------------

    /**
     * Best-effort dismiss common overlays (Continue shopping interstitial, cookie banner, modal backdrops).
     * Safe to call repeatedly; does not throw on failure.
     */
    private void dismissOverlays() {
        try {
            // 1) Try any Continue/Continue shopping-like visible button (broad text match)
            try {
                By cont = By.xpath(
                    "//*[self::button or self::input or self::a][contains(normalize-space(.),'Continue shopping') or contains(normalize-space(.),'Continue Shopping') or contains(normalize-space(.),'Continue') or contains(normalize-space(.),'Shop now')]");
                for (WebElement e : driver.findElements(cont)) {
                    try {
                        if (e.isDisplayed() && e.isEnabled()) { e.click(); Thread.sleep(300); return; }
                    } catch (StaleElementReferenceException ignored) {}
                }
            } catch (Exception ignored) {}

            // 2) Try known accept/close selectors (cookies/modals)
            try {
                By closeBtn = By.cssSelector("#sp-cc-accept, input[name='glowDoneButton'], .a-button-close, button[aria-label='Close'], button[aria-label='close']");
                for (WebElement e : driver.findElements(closeBtn)) {
                    try { if (e.isDisplayed() && e.isEnabled()) e.click(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            // 3) Try clicking backdrop / overlay elements
            try {
                By backdrop = By.xpath("//div[contains(@class,'overlay') or contains(@class,'modal-backdrop') or contains(@class,'a-popover-overlay') or contains(@class,'sp-landing')]");
                for (WebElement b : driver.findElements(backdrop)) {
                    try { if (b.isDisplayed()) { new Actions(driver).moveToElement(b).click().perform(); Thread.sleep(200); } } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            // 4) Try ESC key
            try { new Actions(driver).sendKeys(Keys.ESCAPE).perform(); Thread.sleep(200); } catch (Exception ignored) {}

            // 5) Fallback tiny offset click on page background
            try { new Actions(driver).moveByOffset(5, 5).click().perform(); Thread.sleep(150); } catch (Exception ignored) {}

        } catch (Exception e) {
            System.out.println("dismissOverlays (login) exception: " + e.getMessage());
        }
    }

    /** Quick visibility check used internally; non-failing. */
    @SuppressWarnings("unused")
    private boolean isIdentifierVisibleQuick() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.visibilityOfElementLocated(identifierInput));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if the identifier input is visible and its current value is empty/blank.
     * Useful for detecting the case where Continue leaves the user on identifier page without visible error text.
     */
    public boolean isIdentifierEmpty() {
        try {
            WebElement el = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.visibilityOfElementLocated(identifierInput));
            String v = el.getAttribute("value");
            return v == null || v.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
