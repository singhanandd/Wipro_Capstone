Feature: User Registration

  @requiresLogin
  Scenario: Register with blank details
    Given I launch the application
    When I register with blank details
    Then I should see mandatory field validation messages

