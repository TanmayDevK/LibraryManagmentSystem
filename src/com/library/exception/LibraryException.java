package com.library.exception;

/** Base class for business-rule problems (as opposed to SQL/database problems). */
public class LibraryException extends Exception {
    public LibraryException(String message) {
        super(message);
    }
}
