package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AmazonHomePage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By searchBox = By.id("twotabsearchtextbox");
    private final By searchButton = By.id("nav-search-submit-button");
    private final By deptDropdown = By.id("searchDropdownBox");

    // Autocomplete (selectors can change; adjust if needed)
    private final By suggestionsContainer = By.id("nav-flyout-searchAjax");
    private final By suggestionsItems = By.cssSelector("#nav-flyout-searchAjax .s-suggestion");

    public AmazonHomePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void clearSearch() {
        // Attempt to dismiss overlays before interacting
        try { dismissOverlays(); } catch (Exception ignored) {}

        try {
            WebElement box = wait.until(ExpectedConditions.elementToBeClickable(searchBox));
            box.clear();
        } catch (TimeoutException e) {
            // Try dismissing overlays and retry once
            try { dismissOverlays(); } catch (Exception ignored) {}
            WebElement box = wait.until(ExpectedConditions.elementToBeClickable(searchBox));
            box.clear();
        }
    }

    public void enterSearchText(String product) {
        // Attempt to dismiss overlays before interacting
        try { dismissOverlays(); } catch (Exception ignored) {}

        try {
            WebElement box = wait.until(ExpectedConditions.elementToBeClickable(searchBox));
            box.clear();
            box.sendKeys(product);
        } catch (TimeoutException e) {
            // Dismiss overlays and retry one time
            try { dismissOverlays(); } catch (Exception ignored) {}
            WebElement box = wait.until(ExpectedConditions.elementToBeClickable(searchBox));
            box.clear();
            box.sendKeys(product);
        }
    }

    public void pressEnter() {
        driver.findElement(searchBox).sendKeys(Keys.ENTER);
    }

    public void clickSearchButton() {
        wait.until(ExpectedConditions.elementToBeClickable(searchButton)).click();
    }

    public void dismissOverlays() {
        // best-effort overlay dismissal; safe to call repeatedly
        try {
            // 1) Try Continue/Continue shopping-like buttons (broad text match)
            try {
                By continueBtn = By.xpath(
                        "//*[self::button or self::input or self::a][contains(normalize-space(.),'Continue shopping') or contains(normalize-space(.),'Continue Shopping') or contains(normalize-space(.),'Continue') or contains(normalize-space(.),'Shop now')]");
                List<WebElement> cont = driver.findElements(continueBtn);
                for (WebElement e : cont) {
                    try {
                        if (e.isDisplayed() && e.isEnabled()) {
                            e.click();
                            // if clicked, overlay likely gone
                            return;
                        }
                    } catch (StaleElementReferenceException ignored) {}
                }
            } catch (Exception ignored) {}

            // 2) Try close buttons / accept buttons (cookies etc.)
            try {
                By closeBtn = By.cssSelector("input#sp-cc-accept, input[name='glowDoneButton'], .a-button-close, button[aria-label='Close'], button[aria-label='close']");
                List<WebElement> closeEls = driver.findElements(closeBtn);
                for (WebElement e : closeEls) {
                    try {
                        if (e.isDisplayed() && e.isEnabled()) {
                            e.click();
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            // 3) Try clicking on common backdrop / overlay nodes
            try {
                By backdrop = By.xpath("//div[contains(@class,'overlay') or contains(@class,'modal-backdrop') or contains(@class,'a-popover-overlay') or contains(@class,'sp-landing')]");
                List<WebElement> backdrops = driver.findElements(backdrop);
                for (WebElement b : backdrops) {
                    try {
                        if (b.isDisplayed()) {
                            new Actions(driver).moveToElement(b).click().perform();
                            Thread.sleep(250);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            // 4) Try ESC key on active element (many modals close on ESC)
            try {
                new Actions(driver).sendKeys(Keys.ESCAPE).perform();
                Thread.sleep(250);
                if (isSearchBoxClickableQuick()) return;
            } catch (Exception ignored) {}

            // 5) Try a small offset click (click page background)
            try {
                new Actions(driver).moveByOffset(5, 5).click().perform();
                Thread.sleep(200);
            } catch (Exception ignored) {}

        } catch (Exception e) {
            // swallow — overlay dismissal is best-effort and should not break tests
            System.out.println("dismissOverlays exception: " + e.getMessage());
        }
    }
    

    /** quick check whether search box is clickable */
    private boolean isSearchBoxClickableQuick() {
        try {
            WebDriverWait small = new WebDriverWait(driver, Duration.ofSeconds(3));
            small.until(ExpectedConditions.elementToBeClickable(searchBox));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void selectDepartment(String visibleText) {
        dismissOverlays();
        WebElement dd = wait.until(ExpectedConditions.presenceOfElementLocated(deptDropdown));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'})", dd);

        try {
            wait.until(ExpectedConditions.elementToBeClickable(dd)).click();
        } catch (Exception e) {
            try { ((JavascriptExecutor) driver).executeScript("arguments[0].click()", dd); } catch (Exception ignored) {}
        }
        new Select(dd).selectByVisibleText(visibleText);
    }

    public String getSearchValue() {
        return driver.findElement(searchBox).getAttribute("value");
    }

    public void waitForSuggestionsToAppear() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(suggestionsContainer));
        wait.until(d -> !driver.findElements(suggestionsItems).isEmpty());
    }

    public List<String> getSuggestionsTexts() {
        List<WebElement> els = driver.findElements(suggestionsItems);
        List<String> out = new ArrayList<>();
        for (WebElement el : els) {
            try { out.add(el.getText().trim()); } catch (StaleElementReferenceException ignored) {}
        }
        return out;
    }

    public void clickSuggestionByIndex(int index) {
        List<WebElement> els = driver.findElements(suggestionsItems);
        if (index < 0 || index >= els.size()) throw new IllegalArgumentException("Bad suggestion index");
        els.get(index).click();
    }
}

