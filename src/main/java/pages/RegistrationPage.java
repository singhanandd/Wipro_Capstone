package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import org.testng.SkipException;
import utils.ConfigReader;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Amazon Create Account page object (robust for /ap/register variations & overlays).
 */
public class RegistrationPage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Actions actions;

    // -------- URLs (from config with defaults) --------
    private final String regUrl = ConfigReader.getProperty(
            "registrationUrl", "https://www.amazon.in/ap/register");

    // -------- Primary locators --------
    private final By createAccountOnSignin = By.id("createAccountSubmit");
    private final By registerFormContainer = By.id("ap_register_form");
    private final By continueCreate        = By.id("continue");

    // Field variants (Amazon may render phone OR email)
    private final List<By> phoneOrEmailFields = Arrays.asList(
            By.id("ap_phone_number"),
            By.id("ap_email"),
            By.name("email"),
            By.cssSelector("input[type='tel'][id*='phone']"),
            By.cssSelector("input[name='email']")
    );
    private final List<By> nameFields = Arrays.asList(
            By.id("ap_customer_name"),
            By.name("customerName")
    );
    private final List<By> passwordFields = Arrays.asList(
            By.id("ap_password"),
            By.name("password"),
            By.cssSelector("input[type='password'][id*='password']")
    );

    // Cookie / overlays / captcha
    private final List<By> cookieAccepts = Arrays.asList(
            By.id("sp-cc-accept"),
            By.cssSelector("input[name='accept']"),
            By.cssSelector("button[name='accept']")
    );
    private final List<By> captchaSignals = Arrays.asList(
            By.id("auth-captcha-guess"),
            By.cssSelector("img[alt*='captcha']"),
            By.xpath("//*[contains(.,'Type the characters') and contains(@class,'a-spacing')]")
    );

    // OTP page signals
    private final List<By> otpSignals = Arrays.asList(
            By.id("cvf-input-code"),
            By.name("code"),
            By.cssSelector("[data-csa-c-slot-id*='cvf']"),
            By.xpath("//*[contains(.,'Verify mobile number') or contains(.,'Enter OTP') or contains(.,'One Time Password') or contains(.,'verification code')]")
    );

    // Validation message blocks
    private final List<By> validationBlocks = Arrays.asList(
            By.cssSelector(".a-alert-content"),
            By.cssSelector(".a-form-error"),
            By.cssSelector(".auth-error-message-box"),
            By.cssSelector("[aria-invalid='true']"),
            By.id("auth-customerName-missing-alert"),
            By.id("auth-email-missing-alert"),
            By.id("auth-password-missing-alert")
    );

    private final boolean skipOnCaptcha = Boolean.parseBoolean(
            ConfigReader.getProperty("skipOnCaptcha", "true"));

    public RegistrationPage(WebDriver driver) {
        this.driver = driver;
        int explicit = Integer.parseInt(ConfigReader.getProperty("explicitWait", "25"));
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(explicit));
        this.actions = new Actions(driver);
    }

    // =================== Navigation ===================

    /**
     * Open the Create Account page robustly:
     * 1) Try homepage -> hover "Hello, sign in" -> click "Start here."
     * 2) If that fails, try the previous robust fallbacks (direct /ap/register, sign-in create account, home -> nav -> create account).
     */
    public void openCreateAccountDirect() {
        String baseUrl = ConfigReader.getProperty("baseUrl", "https://www.amazon.in/");

        // Primary: go to Amazon home and use "Start here." under "Hello, sign in"
        driver.navigate().to(baseUrl);
        waitForReady();
        handleContinueShoppingInterstitialIfPresent();
        acceptCookiesIfPresent();

        boolean navigatedToRegister = false;
        try {
            By accountList = By.id("nav-link-accountList");
            WebElement accountElement = wait.until(ExpectedConditions.visibilityOfElementLocated(accountList));

            // Hover to reveal dropdown
            actions.moveToElement(accountElement).pause(Duration.ofMillis(300)).perform();

            // Click "Start here." link (exact text Amazon uses)
            By startHere1 = By.linkText("Start here.");
            By startHere2 = By.xpath("//*[contains(text(),'New customer') and contains(text(),'Start here')]");
            if (!driver.findElements(startHere1).isEmpty()) {
                wait.until(ExpectedConditions.elementToBeClickable(startHere1)).click();
                navigatedToRegister = true;
            } else if (!driver.findElements(startHere2).isEmpty()) {
                wait.until(ExpectedConditions.elementToBeClickable(startHere2)).click();
                navigatedToRegister = true;
            } else {
                // try clicking account list to open sign-in page which has "Create your Amazon account"
                accountElement.click();
                waitForReady();
                handleContinueShoppingInterstitialIfPresent();
                acceptCookiesIfPresent();
                if (!driver.findElements(createAccountOnSignin).isEmpty()) {
                    wait.until(ExpectedConditions.elementToBeClickable(createAccountOnSignin)).click();
                    navigatedToRegister = true;
                }
            }

            if (navigatedToRegister) {
                waitForReady();
                handleContinueShoppingInterstitialIfPresent();
            }
        } catch (Exception ignored) {
            // we'll fallback to other strategies below
        }

        // If primary method didn't navigate to the registration form, run the robust fallback flow
        if (!navigatedToRegister) {
            try {
                // Try direct register URL first (best-effort)
                driver.navigate().to(regUrl);
                waitForReady();
                handleContinueShoppingInterstitialIfPresent();
                acceptCookiesIfPresent();

                if (isPresent(registerFormContainer)
                        || firstVisible(nameFields) != null
                        || firstVisible(phoneOrEmailFields) != null
                        || firstVisible(passwordFields) != null) {
                    if (isAnyPresent(captchaSignals)) handleCaptcha();
                    return;
                }

                // Detect the broken "Looking for Something?" page
                boolean looksLikeRegisterError =
                        driver.getCurrentUrl().contains("/ap/register")
                                && (driver.getPageSource().toLowerCase().contains("looking for something")
                                || (!driver.findElements(createAccountOnSignin).isEmpty() && driver.findElements(createAccountOnSignin).get(0).getText().toLowerCase().contains("create"))
                        );

                if (looksLikeRegisterError) {
                    String loginUrl = ConfigReader.getProperty("loginUrl", "https://www.amazon.in/ap/signin");
                    driver.navigate().to(loginUrl);
                    waitForReady();
                    handleContinueShoppingInterstitialIfPresent();
                    acceptCookiesIfPresent();
                    try {
                        clickIfClickable(createAccountOnSignin, 10);
                        waitForReady();
                        handleContinueShoppingInterstitialIfPresent();
                    } catch (Exception ignore) {
                        // fallback to base home nav path
                        driver.navigate().to(baseUrl);
                        waitForReady();
                        handleContinueShoppingInterstitialIfPresent();
                        acceptCookiesIfPresent();
                        try {
                            By accountList = By.id("nav-link-accountList");
                            new WebDriverWait(driver, Duration.ofSeconds(15))
                                    .until(ExpectedConditions.elementToBeClickable(accountList)).click();
                            waitForReady();
                            clickIfClickable(createAccountOnSignin, 15);
                            waitForReady();
                        } catch (Exception ignore2) {}
                    }
                } else {
                    // Not error but still didn't find form – go to signin and click create account
                    String loginUrl = ConfigReader.getProperty("loginUrl", "https://www.amazon.in/ap/signin");
                    driver.navigate().to(loginUrl);
                    waitForReady();
                    handleContinueShoppingInterstitialIfPresent();
                    acceptCookiesIfPresent();
                    if (!driver.findElements(createAccountOnSignin).isEmpty()) {
                        clickIfClickable(createAccountOnSignin, 10);
                        waitForReady();
                        handleContinueShoppingInterstitialIfPresent();
                    }
                }
            } catch (Exception e) {
                // As a last resort, go home and try the hover/click sequence once more
                try {
                    driver.navigate().to(baseUrl);
                    waitForReady();
                    handleContinueShoppingInterstitialIfPresent();
                    acceptCookiesIfPresent();
                    By accountList = By.id("nav-link-accountList");
                    WebElement accountElement = new WebDriverWait(driver, Duration.ofSeconds(15))
                            .until(ExpectedConditions.visibilityOfElementLocated(accountList));
                    actions.moveToElement(accountElement).perform();
                    By startHere1 = By.linkText("Start here.");
                    if (!driver.findElements(startHere1).isEmpty()) {
                        wait.until(ExpectedConditions.elementToBeClickable(startHere1)).click();
                        waitForReady();
                        handleContinueShoppingInterstitialIfPresent();
                    }
                } catch (Exception ignore) {}
            }
        }

        // Final checks before returning control
        if (isAnyPresent(captchaSignals)) handleCaptcha();

        wait.until(d -> isPresent(registerFormContainer)
                || firstVisible(nameFields) != null
                || firstVisible(phoneOrEmailFields) != null
                || firstVisible(passwordFields) != null);
    }

    // =================== Form actions ===================

    /** Fill mobile/email, name, and password with resilient typing. */
    public void fillCreateAccount(String mobileOrEmail, String fullName, String password) {
        WebElement name = require(firstVisible(nameFields), "Name field not found on register page");
        robustType(name, fullName);

        WebElement phoneOrEmail = require(firstVisible(phoneOrEmailFields),
                "Phone/Email field not found on register page");
        robustType(phoneOrEmail, mobileOrEmail);

        WebElement pwd = require(firstVisible(passwordFields), "Password field not found on register page");
        robustType(pwd, password);

        js("arguments[0].blur()", name);
        js("arguments[0].blur()", phoneOrEmail);
        js("arguments[0].blur()", pwd);
    }

    public void submitCreateAccount() {
        WebElement cont = wait.until(ExpectedConditions.elementToBeClickable(continueCreate));
        safeClick(cont);
    }

    // =================== Assertions/helpers ===================

    public boolean isOtpPageShown() {
        try {
            wait.until(d ->
                    d.getCurrentUrl().contains("/ap/cvf")
                            || d.getCurrentUrl().contains("/ap/claim")
                            || firstVisible(otpSignals) != null
            );
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public boolean hasValidationMessages() {
        for (By by : validationBlocks) if (!driver.findElements(by).isEmpty()) return true;
        return !driver.findElements(By.cssSelector(".a-color-error, .auth-inlined-error")).isEmpty();
    }

    // =================== Utilities ===================

    private void robustType(WebElement el, String text) {
        scrollIntoView(el);

        try { el.click(); } catch (Exception ignore) { js("arguments[0].click()", el); }

        try {
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            el.sendKeys(text);

            if (!Objects.equals(text, valueOf(el))) jsSetValue(el, text);

            if (!Objects.equals(text, valueOf(el))) {
                actions.moveToElement(el).click().pause(Duration.ofMillis(60));
                for (char c : text.toCharArray()) actions.sendKeys(String.valueOf(c)).pause(Duration.ofMillis(10));
                actions.build().perform();
            }
        } catch (StaleElementReferenceException sere) {
            String id = attr(el, "id");
            if (id != null && !id.isBlank()) {
                WebElement fresh = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(id)));
                robustType(fresh, text);
            }
        }
    }

    private void jsSetValue(WebElement el, String value) {
        js("var e=arguments[0],v=arguments[1];" +
                "e.value=v;" +
                "e.setAttribute('value', v);" +
                "e.dispatchEvent(new Event('input',{bubbles:true}));" +
                "e.dispatchEvent(new Event('change',{bubbles:true}));", el, value);
    }

    private void safeClick(WebElement el) {
        try { el.click(); } catch (Exception e) { js("arguments[0].click()", el); }
    }

    private void scrollIntoView(WebElement el) {
        js("arguments[0].scrollIntoView({block:'center'});", el);
    }

    private String valueOf(WebElement el) {
        try { return el.getAttribute("value"); } catch (Exception e) { return null; }
    }

    private String attr(WebElement el, String name) {
        try { return el.getAttribute(name); } catch (Exception e) { return null; }
    }

    private void waitForReady() {
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d ->
                "complete".equals(js("return document.readyState")));
    }

    private void clickIfClickable(By by, int seconds) {
        new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(ExpectedConditions.elementToBeClickable(by)).click();
    }

    private boolean isPresent(By by) {
        return !driver.findElements(by).isEmpty();
    }

    private boolean isAnyPresent(List<By> locators) {
        for (By by : locators) if (isPresent(by)) return true;
        return false;
    }

    private WebElement firstVisible(List<By> locators) {
        for (By by : locators) {
            List<WebElement> els = driver.findElements(by);
            for (WebElement el : els) {
                try { if (el.isDisplayed()) return el; } catch (StaleElementReferenceException ignored) {}
            }
        }
        return null;
    }

    private void acceptCookiesIfPresent() {
        for (By by : cookieAccepts) {
            try {
                WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(by));
                btn.click();
                return;
            } catch (Exception ignored) {}
        }
    }

    /**
     * Handle the "Click the button below to continue shopping" interstitial that Amazon sometimes serves.
     * Safe best-effort: will click the "Continue shopping" CTA if present.
     */
    private void handleContinueShoppingInterstitialIfPresent() {
        try {
            // Wait briefly for the specific message / button to appear
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            boolean hasMessage = driver.getPageSource().toLowerCase().contains("click the button below to continue shopping");
            if (hasMessage) {
                List<By> continueBtns = Arrays.asList(
                        By.xpath("//button[contains(.,'Continue shopping')]"),
                        By.xpath("//input[@value='Continue shopping']"),
                        By.cssSelector("button.a-button-input"),
                        By.cssSelector("button")
                );

                for (By by : continueBtns) {
                    try {
                        WebElement btn = shortWait.until(ExpectedConditions.elementToBeClickable(by));
                        String text = "";
                        try { text = btn.getText(); } catch (Exception ignore) {}
                        if ((text != null && text.trim().toLowerCase().contains("continue shopping"))
                                || text == null || text.isBlank()) {
                            safeClick(btn);
                            waitForReady();
                            return;
                        }
                    } catch (Exception ignored) {}
                }

                // Last resort: click the first visible enabled button
                try {
                    List<WebElement> buttons = driver.findElements(By.tagName("button"));
                    for (WebElement b : buttons) {
                        try {
                            if (b.isDisplayed() && b.isEnabled()) {
                                String t = b.getText() == null ? "" : b.getText().toLowerCase();
                                if (t.contains("continue") || t.contains("continue shopping") || t.length() < 40) {
                                    safeClick(b);
                                    waitForReady();
                                    return;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
            // ignore if not present
        }
    }

    private void handleCaptcha() {
        if (skipOnCaptcha) throw new SkipException("Captcha detected on Amazon register page; skipping scenario.");
        throw new IllegalStateException("Captcha detected; cannot proceed reliably.");
    }

    private Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    private <T> T require(T element, String messageIfNull) {
        if (element == null) throw new TimeoutException(messageIfNull);
        return element;
    }
}
