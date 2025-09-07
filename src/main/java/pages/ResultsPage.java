package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ResultsPage {
    private final WebDriver driver;

    private final By resultCards = By.cssSelector("div.s-main-slot [data-component-type='s-search-result']");
    private final By searchBox   = By.id("twotabsearchtextbox");

    // Case-insensitive "no results" messages
    private final By noResultsMessage = By.xpath(
        "//*[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'no results')" +
        " or contains(., 'did not match any products')" +
        " or contains(., \"couldn't find a match\")" +
        " or contains(., 'could not find a match')]"
    );

    // Case-insensitive "showing results for" / "did you mean"	
    private final By showingResultsForMessage = By.xpath(
        "//*[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'showing results for')" +
        " or contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'did you mean')]"
    );

    public ResultsPage(WebDriver driver) { this.driver = driver; }

    public void waitUntilResultsOrMessage(Duration timeout) {
        new WebDriverWait(driver, timeout).until((ExpectedCondition<Boolean>) d ->
            !d.findElements(resultCards).isEmpty()
            || !d.findElements(noResultsMessage).isEmpty()
            || !d.findElements(showingResultsForMessage).isEmpty()
        );
    }

    public int resultsCount() { return driver.findElements(resultCards).size(); }

    public boolean isNoResultsMessagePresent() { return !driver.findElements(noResultsMessage).isEmpty(); }

    public boolean isShowingResultsForPresent() { return !driver.findElements(showingResultsForMessage).isEmpty(); }

    public boolean searchBoxDiffersFrom(String original) {
        try {
            String val = driver.findElement(searchBox).getAttribute("value");
            return val != null && !val.trim().equalsIgnoreCase(original.trim());
        } catch (NoSuchElementException e) { return false; }
    }

    public boolean pageTextSuggestsRewriteOrNoResults() {
        try {
            String body = driver.findElement(By.tagName("body")).getText().toLowerCase();
            return body.contains("showing results for")
                || body.contains("did you mean")
                || body.contains("no results")
                || body.contains("did not match any products")
                || body.contains("couldn't find a match")
                || body.contains("could not find a match");
        } catch (Exception e) { return false; }
    }

    public String title() { return driver.getTitle(); }
    public String url() { return driver.getCurrentUrl(); }

    public String firstResultTitle() {
        List<WebElement> cards = driver.findElements(resultCards);
        if (cards.isEmpty()) return "";
        try {
            WebElement title = cards.get(0).findElement(By.cssSelector("h2 a"));
            return title.getText().trim();
        } catch (NoSuchElementException ignored) { return ""; }
    }

    /** Return up to N visible result titles (best-effort). */
    public List<String> topResultTitles(int limit) {
        List<String> out = new ArrayList<>();
        List<WebElement> cards = driver.findElements(resultCards);
        int n = Math.min(limit, cards.size());
        for (int i = 0; i < n; i++) {
            try {
                WebElement title = cards.get(i).findElement(By.cssSelector("h2 a"));
                String text = title.getText();
                if (text != null && !text.isBlank()) out.add(text.trim());
            } catch (NoSuchElementException ignored) { }
        }
        return out;
    }
}
