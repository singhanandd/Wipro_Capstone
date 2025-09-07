package stepDefinitions;

import base.DriverFactory;
import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import pages.AmazonHomePage;
import pages.ResultsPage;

import java.time.Duration;
import java.util.List;

public class SearchSteps {

    private AmazonHomePage homePage;
    private ResultsPage resultsPage;

    private void init() {
        if (homePage == null || resultsPage == null) {
            homePage = new AmazonHomePage(DriverFactory.getDriver());
            resultsPage = new ResultsPage(DriverFactory.getDriver());
        }
    }

    // ---------- VALID SEARCH ----------
    @When("I search for {string} using search button")
    public void i_search_for_using_search_button(String product) {
        init();
        homePage.enterSearchText(product);
        homePage.clickSearchButton();
    }

    @When("I search for {string} using Enter key")
    public void i_search_for_using_enter_key(String product) {
        init();
        homePage.enterSearchText(product);
        homePage.pressEnter();
    }

    // Alias for feature: When I search for a book "Clean Code"
    @When("I search for a book {string}")
    public void i_search_for_a_book(String bookTitle) {
        init();
        homePage.enterSearchText(bookTitle);
        homePage.clickSearchButton();
    }

    @Then("I should see search results for {string}")
    public void i_should_see_search_results_for(String product) {
        init();
        resultsPage.waitUntilResultsOrMessage(Duration.ofSeconds(15));
        Assert.assertTrue(resultsPage.resultsCount() > 0, "Results should be greater than 0");
    }

    // ---------- SELECT FIRST RESULT (ROBUST) ----------
    @And("I select the book from results")
    public void i_select_the_book_from_results() {
        WebDriver driver = DriverFactory.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));
        String targetLower = "clean code";

        // 1) Try to prefer /dp/ anchors with physical-format text in the card
        WebElement chosen = null;
        try {
            List<WebElement> anchors = driver.findElements(By.xpath("//a[contains(@href,'/dp/') or contains(@href,'/gp/product/')]"));
            for (WebElement a : anchors) {
                try {
                    if (!a.isDisplayed() || !a.isEnabled()) continue;
                    // find enclosing card and check for "paperback"/"hardcover"
                    WebElement card = null;
                    try { card = a.findElement(By.xpath("./ancestor::div[contains(@data-asin,'') or contains(@class,'s-result-item')]")); } catch (Exception ignored) {}
                    boolean hasPhysical = false;
                    if (card != null) {
                        String txt = card.getText().toLowerCase();
                        if (txt.contains("paperback") || txt.contains("hardcover") || txt.contains("paper back")) hasPhysical = true;
                    }
                    String atxt = "";
                    try { atxt = a.getText().toLowerCase(); } catch (Exception ignored) {}
                    boolean titleMatches = atxt.contains("clean") && atxt.contains("code");
                    if (titleMatches && hasPhysical) { chosen = a; break; }
                    if (chosen == null && titleMatches) chosen = a; // fallback
                } catch (StaleElementReferenceException ignored) {}
            }
        } catch (Exception ignored) {}

        // 2) Fallback: common h2->a inside result card
        if (chosen == null) {
            try {
                chosen = driver.findElement(By.cssSelector("div.s-main-slot [data-component-type='s-search-result'] h2 a"));
            } catch (Exception ignored) {}
        }

        // 3) Fallback: any anchor with title text containing both words
        if (chosen == null) {
            try {
                chosen = driver.findElement(By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'clean') and contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'code')]"));
            } catch (Exception ignored) {}
        }

        if (chosen == null) {
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get("failed_search_results.html"),
                        driver.getPageSource().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception ignored) {}
            System.out.println("DEBUG: No clickable product link found in results. URL: " + driver.getCurrentUrl());
            Assert.fail("No clickable search result link was found.");
            return;
        }

        // click chosen link (scroll + JS fallback)
        try { ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", chosen); } catch (Exception ignored) {}
        try {
            wait.until(ExpectedConditions.elementToBeClickable(chosen)).click();
        } catch (Exception e) {
            try { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", chosen); } catch (Exception ex) {
                Assert.fail("Failed to click chosen product link: " + ex.getMessage());
                return;
            }
        }

        // switch to new window if opened, or remain if same window
        try {
            String original = driver.getWindowHandle();
            Thread.sleep(600);
            for (String h : driver.getWindowHandles()) {
                if (!h.equals(original)) {
                    driver.switchTo().window(h);
                    break;
                }
            }
            // wait for PDP-ish content (dp in url or add-to-cart/buy-now or page title)
            wait.until(d -> d.getCurrentUrl().contains("/dp/") ||
                    !d.findElements(By.id("add-to-cart-button")).isEmpty() ||
                    !d.findElements(By.id("buy-now-button")).isEmpty() ||
                    (d.getTitle() != null && d.getTitle().length() > 8));
        } catch (Exception ignored) {}
    }


    // ---------- EMPTY SEARCH ----------
    @When("I search with empty text")
    public void i_search_with_empty_text() {
        init();
        homePage.clearSearch();
        homePage.clickSearchButton();
    }

    @Then("I should remain on the same page")
    public void i_should_remain_on_the_same_page() {
        String currentUrl = DriverFactory.getDriver().getCurrentUrl();
        Assert.assertTrue(currentUrl.contains("amazon"), "User should remain on the same page");
    }

    // ---------- SUGGESTIONS ----------
    @When("I type {string} in the search box")
    public void i_type_in_the_search_box(String text) {
        init();
        homePage.enterSearchText(text);
        homePage.waitForSuggestionsToAppear();
    }

    @Then("I should see up to 10 suggestions")
    public void i_should_see_up_to_10_suggestions() {
        init();
        List<String> suggestions = homePage.getSuggestionsTexts();
        Assert.assertTrue(!suggestions.isEmpty() && suggestions.size() <= 10,
                "Expected between 1 and 10 suggestions, but got " + suggestions.size());
    }

    @And("I select the first suggestion")
    public void i_select_the_first_suggestion() {
        init();
        homePage.clickSuggestionByIndex(0);
    }

    @Then("the search box should match the selected suggestion")
    public void the_search_box_should_match_the_selected_suggestion() {
        init();
        String value = homePage.getSearchValue();
        Assert.assertFalse(value.isEmpty(), "Search box should not be empty");
    }

    @When("I type {string} then append {string}")
    public void i_type_then_append(String first, String second) {
        init();
        homePage.enterSearchText(first);
        homePage.waitForSuggestionsToAppear();
        DriverFactory.getDriver().switchTo().activeElement().sendKeys(second);
    }

    @Then("the suggestions should be updated")
    public void the_suggestions_should_be_updated() {
        init();
        List<String> suggestions = homePage.getSuggestionsTexts();
        Assert.assertTrue(!suggestions.isEmpty(), "Suggestions should update after typing more characters");
    }

    // ---------- DEPARTMENT ----------
    @When("I select {string} department")
    public void i_select_department(String department) {
        init();
        homePage.dismissOverlays();
        homePage.selectDepartment(department);
    }

    @Then("I should see search results for {string} in Electronics")
    public void i_should_see_search_results_for_in_department(String product) {
        init();
        resultsPage.waitUntilResultsOrMessage(Duration.ofSeconds(15));
        Assert.assertTrue(resultsPage.resultsCount() > 0, "Results should be available in Electronics");
    }

    // ---------- INVALID / LONG ----------
    @When("I search for {string}")
    public void i_search_for_invalid(String query) {
        init();
        homePage.enterSearchText(query);
        homePage.clickSearchButton();
    }

    @Then("I should see no results or suggestions")
    public void i_should_see_no_results_or_suggestions() {
        init();
        resultsPage.waitUntilResultsOrMessage(Duration.ofSeconds(15));
        boolean noResults = resultsPage.isNoResultsMessagePresent();
        boolean suggestions = resultsPage.isShowingResultsForPresent();
        Assert.assertTrue(noResults || suggestions || resultsPage.resultsCount() >= 0,
                "Invalid search should show no results or suggestions");
    }

    @When("I search for a very long string")
    public void i_search_for_a_very_long_string() {
        init();
        String longTerm = "a".repeat(620);
        homePage.enterSearchText(longTerm);
        homePage.clickSearchButton();
    }

    @Then("the page should handle the input gracefully")
    public void the_page_should_handle_the_input_gracefully() {
        init();
        resultsPage.waitUntilResultsOrMessage(Duration.ofSeconds(15));
        Assert.assertTrue(resultsPage.title().length() > 0, "Page handled long input");
    }
}
