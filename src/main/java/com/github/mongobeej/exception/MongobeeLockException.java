package com.github.mongobeej.exception;

/**
 * Error while can not obtain process lock
 */
public class MongobeeLockException extends MongobeeException {
    public MongobeeLockException(String message) {
        super(message);
    }
}
