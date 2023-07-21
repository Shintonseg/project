Feature: Acceptance tests for transactions via REST

  #Load tests takes more than hour per one 200(k) example, recommended to run only 10k examples
  Scenario Outline: Test load scenario REST SUCCESS
    Given transactionDto json file with <version> and transaction exists in <testNumber> directory
    When send transaction create request
    Then response is ACCEPTED
    Then rest transaction saved and accepted

    Examples:
      | testNumber | version |
      | 112_10     | 162     |
      | 112_10     | 170     |
      | 112_10     | 171     |
#      | 112_200    | 162     |
#      | 112_200    | 170     |

  Scenario Outline: Test scenario REST SUCCESS
    Given transactionDto json file with <version> and transaction exists in <testNumber> directory
    When send transaction create request
    Then rest transaction saved and accepted
    Then response is ACCEPTED

    Examples:
      | testNumber | version |
      | 100        | 162     |
      | 100        | 170     |
      | 100        | 171     |
      | 104        | 162     |
      | 104        | 170     |
      | 104        | 171     |
      | 117        | 162     |
      | 117        | 170     |
      | 117        | 171     |
      | 127        | 162     |
      | 127        | 170     |
      | 127        | 171     |
      | 130        | 162     |
      | 130        | 170     |
      | 130        | 171     |

  Scenario Outline: Test scenario REST FAILED
    Given transactionDto json file with <version> and transaction exists in <testNumber> directory for case <caseNumber>
    When send transaction create request
    Then transaction is rejected with error
    Then response is DECLINED

    Examples:
      | testNumber | version | caseNumber |
      | 101        | 162     | 1          |
      | 101        | 170     | 1          |
      | 101        | 171     | 1          |
      | 102        | 162     | 1          |
      | 102        | 170     | 1          |
      | 102        | 171     | 1          |
      | 105        | 162     | 1          |
      | 105        | 170     | 1          |
      | 105        | 171     | 1          |
      | 106        | 162     | 1          |
      | 106        | 170     | 1          |
      | 106        | 171     | 1          |
      | 107        | 162     | 1          |
      | 107        | 170     | 1          |
      | 107        | 171     | 1          |
      | 107        | 162     | 2          |
      | 107        | 170     | 2          |
      | 107        | 171     | 2          |
      | 108        | 162     | 1          |
      | 108        | 170     | 1          |
      | 108        | 171     | 1          |
      | 111        | 162     | 1          |
      | 111        | 170     | 1          |
      | 111        | 171     | 1          |
      | 111        | 162     | 2          |
      | 111        | 170     | 2          |
      | 111        | 171     | 2          |
      | 113        | 150     | 1          |
      | 114        | 162     | 1          |
      | 114        | 170     | 1          |
      | 114        | 171     | 1          |
      | 115        | 162     | 1          |
      | 115        | 170     | 1          |
      | 115        | 171     | 1          |
      | 119        | 150     | 1          |
      | 120        | 162     | 1          |
      | 120        | 170     | 1          |
      | 120        | 171     | 1          |
      | 120_1      | 162     | 1          |
      | 120_1      | 170     | 1          |
      | 120_1      | 171     | 1          |
      | 121        | 162     | 1          |
      | 121        | 170     | 1          |
      | 121        | 171     | 1          |
      | 121_1      | 162     | 1          |
      | 121_1      | 170     | 1          |
      | 121_1      | 171     | 1          |
      | 122        | 162     | 1          |
      | 122        | 170     | 1          |
      | 122        | 171     | 1          |
      | 122_1      | 162     | 1          |
      | 122_1      | 170     | 1          |
      | 122_1      | 171     | 1          |
      | 123        | 162     | 1          |
      | 123        | 170     | 1          |
      | 123        | 171     | 1          |
      | 124        | 162     | 1          |
      | 124        | 170     | 1          |
      | 124        | 171     | 1          |
      | 125        | 162     | 1          |
      | 125        | 170     | 1          |
      | 125        | 171     | 1          |
      | 126        | 162     | 1          |
      | 126        | 170     | 1          |
      | 126        | 171     | 1          |

  Scenario Outline: Test scenario REST FAILED and moved to necessary directory
    Given transactionDto json file with <version> and transaction exists in <testNumber> directory for case <caseNumber>
    When send transaction create request
    Then errorFile is created and moved to the <directory>
    Then response is <response>

    Examples:
      | testNumber | version | caseNumber | directory     | response  |
      | 103        | 162     | 1          | alreadyExists | DUPLICATE |
      | 103        | 170     | 1          | alreadyExists | DUPLICATE |
      | 103        | 171     | 1          | alreadyExists | DUPLICATE |
      | 103        | 162     | 2          | alreadyExists | DUPLICATE |
      | 103        | 170     | 2          | alreadyExists | DUPLICATE |
      | 103        | 171     | 2          | alreadyExists | DUPLICATE |
      | 103        | 162     | 3          | alreadyExists | DUPLICATE |
      | 103        | 170     | 3          | alreadyExists | DUPLICATE |
      | 103        | 171     | 3          | alreadyExists | DUPLICATE |
      | 128        | 162     | 1          | failed        | FAILED    |
      | 128        | 170     | 1          | failed        | FAILED    |
      | 128        | 171     | 1          | failed        | FAILED    |
      | 129        | 162     | 1          | alreadyExists | DUPLICATE |
      | 129        | 170     | 1          | alreadyExists | DUPLICATE |
      | 129        | 171     | 1          | alreadyExists | DUPLICATE |
      | 131        | 162     | 1          | rejected      | DECLINED  |
      | 131        | 170     | 1          | rejected      | DECLINED  |
      | 131        | 171     | 1          | rejected      | DECLINED  |
      | 132        | 162     | 1          | accepted      | ACCEPTED  |
      | 132        | 170     | 1          | accepted      | ACCEPTED  |
      | 132        | 171     | 1          | accepted      | ACCEPTED  |
      | 100_1      | 162     | 1          | alreadyExists | DUPLICATE |
      | 100_1      | 170     | 1          | alreadyExists | DUPLICATE |
      | 100_1      | 171     | 1          | alreadyExists | DUPLICATE |
