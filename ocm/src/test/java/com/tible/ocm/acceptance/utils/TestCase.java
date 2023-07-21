package com.tible.ocm.acceptance.utils;

import java.util.Collections;
import java.util.List;

public enum TestCase {

    SCENARIO_100("100", Collections.emptyList(), Collections.emptyList(), ""),
    SCENARIO_101("101", Collections.singletonList("RVM Owner does not have the following rvm serial (.*)"), Collections.singletonList("RVM Serial Number (.*) is not valid for IP (.*)"), ""),
    SCENARIO_102("102", Collections.singletonList("Date is too far in the past (.*), expected date after: (.*)"), Collections.singletonList("Date is not valid."), ""),
    SCENARIO_103("103", Collections.emptyList(), Collections.emptyList(), ""),
    SCENARIO_104("104", Collections.emptyList(), Collections.emptyList(), ""),
    SCENARIO_105("105", Collections.singletonList("The following ean (.*) does not exist"), Collections.singletonList("Article with number (.*) does not exist."), ""),
    SCENARIO_106("106", Collections.singletonList("The following ean (.*) does not exist"), Collections.singletonList("Article with number (.*) does not exist."), ""),
    SCENARIO_107("107", Collections.singletonList("The activation date is in the future for article with number (.*)"),
            Collections.singletonList("The activation date is in the future for article with number (.*)"), ""),
    SCENARIO_108("108", Collections.singletonList("Wrong version number was (.*), expected: (.*)"), Collections.singletonList("Request version is not valid."), ""),
    SCENARIO_109("109", Collections.singletonList("Moving 937010348000000002101.csv transaction file to rejected folder, because hash is wrong or missing"), Collections.emptyList(),
            "Moving (.*) AA files to rejected folder, because not all required hash files \\(_ready\\.hash\\/_batch\\.hash\\/_sls\\.hash\\/_nls\\.hash\\) are present"),
    SCENARIO_110("110", Collections.singletonList("Moving 937010348000000002101.csv transaction file to rejected folder, because hash is wrong or missing"), Collections.emptyList(),
            "Moving AA files to rejected folder, because hash of (.+) file is wrong or missing in hash file"),
    SCENARIO_111("111", Collections.singletonList("Article with number (.*) scanned weight is (.*) than expected weight."),
            Collections.singletonList("Article with number (.*) scanned weight is (.*) than expected weight."), ""),
    SCENARIO_112_10("112_10", Collections.emptyList(), Collections.emptyList(), ""),
    SCENARIO_112_200("112_200", Collections.emptyList(), Collections.emptyList(), ""),
    SCENARIO_113("113", Collections.singletonList("Article with number (.*) material is different than expected material."), Collections.singletonList("Article with number (.*) material is different than expected material."), ""),
    SCENARIO_114("114", Collections.singletonList("Refunded field is empty"), Collections.singletonList("Refunded field is missing"), ""),
    SCENARIO_115("115", Collections.singletonList("Collected field is empty"), Collections.singletonList("Collected field is missing"), ""),
    SCENARIO_116("116", List.of("Article with number (.*) material is different than expected material.", "Refunded field is empty"), List.of("Refunded field is missing", "Article with number (.*) material is different than expected material."), ""),
    SCENARIO_117("117", Collections.emptyList(), Collections.emptyList(), ""),
    SCENARIO_119("119", Collections.singletonList("Article with number (.*) material is different than expected material."), Collections.singletonList("Article with number (.*) material is different than expected material."), ""),
    SCENARIO_120("120", Collections.singletonList("Total read amount is (.*), does not equal total amount field value: (.*)"),
            Collections.singletonList("Total sum is not equal to number of articles"), ""),
    SCENARIO_120_1("120_1", Collections.singletonList("Total amount field is empty"), Collections.singletonList("Total sum is missing"), ""),
    SCENARIO_121("121", Collections.singletonList("Total refunded amount of articles is (.+), does not equal refunded sum amount field value: (.+)"),
            Collections.singletonList("Refundable sum is not equal to refund amount of articles"), ""),
    SCENARIO_121_1("121_1", Collections.singletonList("Refunded sum amount field is empty"), Collections.singletonList("Refundable sum is missing"), ""),
    SCENARIO_122("122", Collections.singletonList("Total collected amount of articles is (.+), does not equal collected sum amount field value: (.+)"),
            Collections.singletonList("Collected sum is not equal to collected amount of articles"), ""),
    SCENARIO_122_1("122_1", Collections.singletonList("Collected sum amount field is empty"), Collections.singletonList("Collected sum is missing"), ""),
    SCENARIO_123("123", Collections.singletonList("Total manual amount of articles is (.+), does not equal manual sum amount field value: (.+)"),
            Collections.singletonList("Manual sum is not equal to manual amount of articles"), ""),
    SCENARIO_124("124", Collections.singletonList("Rejected sum amount is (.+), which is lower than 0 and not possible"),
            Collections.singletonList("Rejected sum amount is (.+), which is lower than 0 and not possible"), ""),
    SCENARIO_125("125", Collections.singletonList("Manual amount of articles is (.+), which is greater than the rejected sum amount field value: (.+)"),
            Collections.singletonList("Manual sum amount of articles is (.+), which is greater than the rejected sum amount field value: (.+)"), ""),
    SCENARIO_126("126", List.of("Number \\(label\\) is longer than 17 characters (.+)"),
            Collections.singletonList("Number \\(label\\) is longer than 17 characters (.+)"), ""),
    SCENARIO_127("127", Collections.emptyList(), Collections.emptyList(), ""),
    SCENARIO_128("128", Collections.emptyList(), Collections.emptyList(), ""),
    SCENARIO_129("129", Collections.singletonList("Label number (.+) is already in use"), Collections.emptyList(), ""),
    SCENARIO_130("130", Collections.emptyList(), Collections.emptyList(), ""),
    SCENARIO_131("131", Collections.singletonList("Date is too far in the past (.*), expected date after: (.*)"), Collections.singletonList("Date is not valid."), ""),
    SCENARIO_132("132", Collections.emptyList(), Collections.emptyList(), ""),
    SCENARIO_100_1("100_1", Collections.emptyList(), Collections.emptyList(), "");

    private final List<String> sftpErrorMessages;
    private final List<String> restErrorMessages;
    private final String aaErrorMessage;
    private final String number;

    TestCase(String number, List<String> sftpErrorMessages, List<String> restErrorMessages, String aaErrorMessage) {
        this.number = number;
        this.sftpErrorMessages = sftpErrorMessages;
        this.restErrorMessages = restErrorMessages;
        this.aaErrorMessage = aaErrorMessage;
    }

    public List<String> getSftpErrorMessages() {
        return sftpErrorMessages;
    }

    public List<String> getRestErrorMessages() {
        return restErrorMessages;
    }

    public String getAAErrorMessage() {
        return aaErrorMessage;
    }

    public String getNumber() {
        return number;
    }
}
