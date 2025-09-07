Feature: Amazon Product Search

  # ====================== Positive Scenarios ======================
  @positive
  Scenario: Valid search using search button
    Given I launch the application
    When I search for "LED" using search button
    Then I should see search results for "LED"

 
 

 

 

  @positive
  Scenario: Search within Electronics department
    Given I launch the application
    When I select "Electronics" department
    And I search for "Speakers"
    Then I should see search results for "SPeakers" in Electronics

 

  # ====================== Negative Scenarios ======================
  @negative
  Scenario: Empty search should not navigate
    Given I launch the application
    When I search with empty text
    Then I should remain on the same page

  @negative
  Scenario: Invalid search with no results
    Given I launch the application
    When I search for "lljhgcfdsestrdyfugihjkbnbnvcvz"
    Then I should see no results or suggestions

  
