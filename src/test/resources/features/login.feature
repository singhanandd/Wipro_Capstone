@requiresLogin
Feature: Login Page Validation

  # -------- Positive Scenarios --------
  Scenario: Login with valid email and password
    Given I launch the application
    When I enter email "number" and password "your password"
    And I click on login button
    Then I should be logged in successfully

  Scenario: Verify session persistence while browsing
    Given I am logged in with email "number" and password "write your password"
    When I navigate to "Electronics" category
    Then My session should remain active


  # -------- Negative Scenarios --------
  Scenario: Login with blank fields
    Given I launch the application
    When I try to login with blank email and password
    Then I should see "Email/Password required" message

  Scenario: Login with invalid email and password
    Given I launch the application
    When I enter email "wronguser@test.com" and password "WrongPass123"
    And I click on login button
    Then I should see "Invalid credentials" error messagea

 

 
