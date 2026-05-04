package com.team.apk.Dto;


public class VerificationResponse {
    private boolean success;
    private double similarity;
    private String message;

    public VerificationResponse(boolean success, double similarity, String message) {
        this.success = success;
        this.similarity = similarity;
        this.message = message;
    }

    public boolean getSuccess() {
        return success;
    }

    public double getSimilarity() {
        return similarity;
    }

    public String getMessage() {
        return message;
    }
}