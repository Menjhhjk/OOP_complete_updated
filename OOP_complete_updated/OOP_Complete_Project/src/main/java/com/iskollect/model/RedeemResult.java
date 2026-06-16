package com.iskollect.model;

public final class RedeemResult {
    private final boolean success;
    private final String message;
    private final String couponCode;
    private final String couponName;
    private final double pointsDeducted;
    private final double remainingPoints;

    public RedeemResult(boolean success, String message, String couponCode, String couponName,
                        double pointsDeducted, double remainingPoints) {
        this.success = success;
        this.message = message;
        this.couponCode = couponCode;
        this.couponName = couponName;
        this.pointsDeducted = pointsDeducted;
        this.remainingPoints = remainingPoints;
    }

    public static RedeemResult failure(String message) {
        return new RedeemResult(false, message, null, null, 0, 0);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getCouponCode() { return couponCode; }
    public String getCouponName() { return couponName; }
    public double getPointsDeducted() { return pointsDeducted; }
    public double getRemainingPoints() { return remainingPoints; }
}
