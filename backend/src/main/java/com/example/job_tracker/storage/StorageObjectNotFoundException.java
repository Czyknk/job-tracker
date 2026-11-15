package com.example.job_tracker.storage;

public class StorageObjectNotFoundException extends StorageException {

    public StorageObjectNotFoundException(String objectKey, Throwable cause) {
        super("Object does not exist: " + objectKey, cause);
    }
}
