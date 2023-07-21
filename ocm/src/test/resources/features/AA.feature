Feature: Acceptance tests for transactions via AA

  Scenario Outline: Test scenario AA FAILED
    Given company delivers the AA transaction file with <version> and name 2021012613362385-0100359038827169 from <testNumber> directory for case <caseNumber>
    When AA files is being processed
    Then AA files with is rejected with an error

    Examples:
      | testNumber | version | caseNumber |
      | 109        | 162     | 1          |
      | 109        | 170     | 1          |
      | 110        | 162     | 1          |
      | 110        | 170     | 1          |
