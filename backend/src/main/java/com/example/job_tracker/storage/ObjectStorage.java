package com.example.job_tracker.storage;

import java.io.InputStream;

public interface ObjectStorage {

    void put(String objectKey, InputStream content, long contentLength, String contentType);

    StoredObject get(String objectKey);

    void delete(String objectKey);

    record StoredObject(InputStream content, String contentType, long contentLength) {
    }
}
