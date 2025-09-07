Feature: Amazon Home Page Validation
@Positive
  Scenario: Verify company logo is visible
    Given I launch the application
    Then I should see the Amazon logo displayed

  
@Positive
  Scenario: Verify product images and banners load correctly
    Given I launch the application
    Then I should see homepage banners displayed
    And I should see product images loaded

 @Negative
 
  Scenario: Verify broken images
    Given I launch the application
    Then I should not see any broken images on home page

  Scenario: Verify broken links
    Given I launch the application
    Then I should not see any broken links on home page

  
