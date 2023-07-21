package com.tible.ocm.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportMessage {

    private int lineNumber;
    private String message;

}
