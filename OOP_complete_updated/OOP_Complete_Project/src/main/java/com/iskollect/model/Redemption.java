package com.iskollect.model;

import java.time.LocalDate;

public class Redemption {
    private int redemptionId;
    private int userId;
    private int couponId;
    private LocalDate redemptionDate;
    private String couponCode;
    private boolean fulfilled;
    private double pointsDeducted;
    private String couponName;

    public Redemption() {
    }

    public Redemption(int redemptionId, int userId, int couponId, LocalDate redemptionDate,
                          String couponCode, boolean fulfilled, double pointsDeducted) {
        this.redemptionId = redemptionId;
        this.userId = userId;
        this.couponId = couponId;
        this.redemptionDate = redemptionDate;
        this.couponCode = couponCode;
        this.fulfilled = fulfilled;
        this.pointsDeducted = pointsDeducted;
    }

    public int getRedemptionId() { return redemptionId; }
    public void setRedemptionId(int redemptionId) { this.redemptionId = redemptionId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getCouponId() { return couponId; }
    public void setCouponId(int couponId) { this.couponId = couponId; }
    public LocalDate getRedemptionDate() { return redemptionDate; }
    public void setRedemptionDate(LocalDate redemptionDate) { this.redemptionDate = redemptionDate; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public boolean isFulfilled() { return fulfilled; }
    public void setFulfilled(boolean fulfilled) { this.fulfilled = fulfilled; }
    public double getPointsDeducted() { return pointsDeducted; }
    public void setPointsDeducted(double pointsDeducted) { this.pointsDeducted = pointsDeducted; }
    public String getCouponName() { return couponName; }
    public void setCouponName(String couponName) { this.couponName = couponName; }

    @Override
    public String toString() {
        return "Redemption{redemptionId=" + redemptionId + ", userId=" + userId
                + ", couponId=" + couponId + ", couponName='" + couponName + "', redemptionDate="
                + redemptionDate + ", couponCode='" + couponCode + "', fulfilled=" + fulfilled
                + ", pointsDeducted=" + pointsDeducted + "}";
    }
}
