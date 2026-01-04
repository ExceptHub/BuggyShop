package com.buggyshop.exception;

public class BuggyShopException extends RuntimeException {
    public BuggyShopException(String message) {
        super(message);
    }

    public BuggyShopException(String message, Throwable cause) {
        super(message, cause);
    }
}
