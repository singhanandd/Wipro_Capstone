package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.ElementUtils;

public class CartPage {
    private final WebDriver driver;
    private final ElementUtils elementUtils;

    // --- Demo Web Shop Locators ---
    private final By cartItemRow        = By.cssSelector(".cart-item-row");
    private final By termsOfServiceChk  = By.id("termsofservice");
    private final By checkoutBtn        = By.id("checkout");

    public CartPage(WebDriver driver) {
        this.driver = driver;
        this.elementUtils = new ElementUtils(driver);
    }

    /** Returns true if at least one product is in the cart */
    public boolean isProductInCart() {
        return elementUtils.isDisplayed(cartItemRow);
    }
    public boolean isBookAdded() {
        return isProductInCart();
    }


    /** Clicks Terms of Service checkbox + Checkout button */
    public void clickProceedToCheckout() {
        elementUtils.click(termsOfServiceChk);
        elementUtils.click(checkoutBtn);
    }
}
