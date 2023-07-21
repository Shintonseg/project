package com.tible.ocm.utils;

import com.tible.ocm.models.OcmMessage;
import com.tible.ocm.models.OcmResponse;
import com.tible.ocm.models.mongo.Company;

import java.time.LocalDateTime;
import java.util.List;

import static com.tible.ocm.models.OcmStatus.DECLINED;

public class ValidationUtils {

    public static final int VALID_DATE_PERIOD = 28;

    public static final List<String> acceptableBagTypes = List.of("BB", "SM", "CP", "MB");

    /**
     * Date should not be in the future.
     * Date cannot be more than 28 days ago.
     *
     * @param date
     * @return
     */
    public static boolean isDateValid(LocalDateTime date, int rejectDataOlderThanDays) {
        return date != null &&
                date.isAfter(LocalDateTime.now().minusDays(rejectDataOlderThanDays)) &&
                (date.isEqual(LocalDateTime.now()) || date.isBefore(LocalDateTime.now()));
    }

    public static boolean versionIsNotValid(String requestVersion, String machineVersion) {
        return requestVersion == null || !requestVersion.equals(machineVersion);
    }

    public static boolean validateIpAddress(String requestIpAddress, String clientIpAddress) {
        if (requestIpAddress.equals("0:0:0:0:0:0:0:1")) {
            requestIpAddress = "127.0.0.1";
        }

        return requestIpAddress.equals(clientIpAddress);
    }

    public static OcmResponse decline(OcmMessage message) {
        return OcmResponse.builder()
                .status(DECLINED).build()
                .addMessage(message);
    }

    public static OcmMessage defaultValidation(String ipAddress, String version, Company company) {
        if (company == null) {
            return new OcmMessage(String.format("Company is not valid for IP address %s.", ipAddress));
        }

        if (versionIsNotValid(version, company.getVersion())) {
            return new OcmMessage("Request version is not valid.");
        }

        return null;
    }
}
