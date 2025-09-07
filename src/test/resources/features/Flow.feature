Feature: E-commerce Book Purchase Flow (Reduced)

 

  @negative @requiresLogin
  Scenario: Invalid Login (Negative Flow)
    Given I launch the application
    And I try to login with invalid credentials "wrong@email.com" and "wrongpass"
    Then I should see login error message
    
    

 

