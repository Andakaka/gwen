# tests gwen-web issue #93
Feature: DSL conflicts

  Scenario: Test if keyword conflict 1
    Given I do this if condition
     When I capture keyword
     Then keyword should be "if"

  Scenario: Test if keyword conflict 2
    Given I check if the dashboard is displaying "Admin" role
     When I capture role name
     Then role name should be "Admin"

  Scenario: Test while keyword conflict 1
    Given I do this while condition
     When I capture keyword
     Then keyword should be "while"

  Scenario: Test while keyword conflict 2
    Given I validate error while creating new dashboard
     When I capture something
     Then something should be "value"

  Scenario: Test until keyword conflict
    Given I do this until condition
     When I capture keyword
     Then keyword should be "until"

  Scenario: Test for each keyword conflict 1
    Given I do this for each entry in list
     When I capture keyword
     Then keyword should be "for each"

  Scenario: Test for each keyword conflict 2
    Given I validate hyperlink is available for each of an entity ID in Timeline summary
     When I capture something
     Then something should be "value"

  Scenario: Test if keyword conflict in string literal
    Given value is "This contains an if literal if"
     When I capture value
     Then value should be "${value}"

  Scenario: Test delimited string
    Given strings is "some string1, some string2, some string3"
     When some function "${str}" for each str in strings delimited by ","
     Then attr should not be ""
    
