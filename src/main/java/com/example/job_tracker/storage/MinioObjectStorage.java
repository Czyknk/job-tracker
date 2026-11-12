package com.example.job_tracker.storage;

import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.errors.ErrorResponseException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

final class MinioObjectStorage implements ObjectStorage {

    private final MinioClient client;
    private final StorageProperties properties;
    private final AtomicBoolean bucketReady = new AtomicBoolean();

    MinioObjectStorage(MinioClient client, StorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void put(String objectKey, InputStream content, long contentLength, String contentType) {
        ensureBucket();
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(content, contentLength, -1L)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new StorageException("Could not store object " + objectKey, ex);
        }
    }

    @Override
    public StoredObject get(String objectKey) {
        try {
            var response = client.getObject(GetObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
            String contentType = response.headers().get("content-type");
            String contentLengthHeader = response.headers().get("content-length");
            long contentLength = contentLengthHeader == null ? -1L : Long.parseLong(contentLengthHeader);
            return new StoredObject(response,
                    contentType == null ? "application/octet-stream" : contentType,
                    contentLength);
        } catch (Exception ex) {
            if (isMissingObject(ex)) {
                throw new StorageObjectNotFoundException(objectKey, ex);
            }
            throw new StorageException("Could not read object " + objectKey, ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.removeObject(io.minio.RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new StorageException("Could not delete object " + objectKey, ex);
        }
    }

    private void ensureBucket() {
        if (bucketReady.get()) {
            return;
        }
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.bucket())
                    .build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(properties.bucket())
                        .region(properties.region())
                        .build());
            }
            bucketReady.set(true);
        } catch (Exception ex) {
            throw new StorageException("Could not initialize bucket " + properties.bucket(), ex);
        }
    }

    private static boolean isMissingObject(Exception ex) {
        return ex instanceof ErrorResponseException error
                && ("NoSuchKey".equals(error.errorResponse().code())
                || "NoSuchObject".equals(error.errorResponse().code()));
    }
}
