package com.tible.ocm.exceptions;

import com.tible.hawk.core.exceptions.HawkNotFoundException;

public class ClientNotFoundException extends HawkNotFoundException {

    public ClientNotFoundException() {
        this("not_found: client");
    }

    public ClientNotFoundException(String message) {
        this(message, 7800);
    }

    public ClientNotFoundException(String message, int... errorCodes) {
        super(message, errorCodes);
    }
}
