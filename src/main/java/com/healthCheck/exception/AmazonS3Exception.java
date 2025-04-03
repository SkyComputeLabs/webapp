package com.healthCheck.exception;

import com.amazonaws.AmazonServiceException;

public class AmazonS3Exception extends AmazonServiceException {

    private String extendedRequestId;
    private String cloudFrontId;

    public AmazonS3Exception(String message) {
        super(message);
    }

    public AmazonS3Exception(String message, Exception cause) {
        super(message, cause);
    }

    public String getExtendedRequestId() {
        return extendedRequestId;
    }

    public void setExtendedRequestId(String extendedRequestId) {
        this.extendedRequestId = extendedRequestId;
    }

    public String getCloudFrontId() {
        return cloudFrontId;
    }

    public void setCloudFrontId(String cloudFrontId) {
        this.cloudFrontId = cloudFrontId;
    }

    @Override
    public String toString() {
        return super.toString() + ", S3 Extended Request ID: " + extendedRequestId +
               (cloudFrontId != null ? ", CloudFront ID: " + cloudFrontId : "");
    }
}