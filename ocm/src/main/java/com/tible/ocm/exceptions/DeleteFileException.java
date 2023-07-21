package com.tible.ocm.exceptions;

public class DeleteFileException extends RuntimeException {

    public DeleteFileException(String fileName) {
        super("Failed to delete file " + fileName);
    }
}
