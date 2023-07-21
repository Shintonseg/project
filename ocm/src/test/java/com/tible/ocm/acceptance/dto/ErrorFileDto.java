package com.tible.ocm.acceptance.dto;

import java.io.Serializable;
import java.util.List;

public class ErrorFileDto implements Serializable {
    private String details;
    private List<MessageDetails> importMessages;

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<MessageDetails> getImportMessages() {
        return importMessages;
    }

    public void setImportMessages(List<MessageDetails> importMessages) {
        this.importMessages = importMessages;
    }

    public static class MessageDetails {
        private String lineNumber;
        private String message;

        public String getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(String lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}