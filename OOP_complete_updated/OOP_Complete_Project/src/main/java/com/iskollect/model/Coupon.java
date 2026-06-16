package com.iskollect.model;

public class Coupon {
    private int couponId;
    private String name;
    private double pointsRequired;

    public Coupon() {
    }

    public Coupon(int couponId, String name, double pointsRequired) {
        this.couponId = couponId;
        this.name = name;
        this.pointsRequired = pointsRequired;
    }

    public int getCouponId() { return couponId; }
    public void setCouponId(int couponId) { this.couponId = couponId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPointsRequired() { return pointsRequired; }
    public void setPointsRequired(double pointsRequired) { this.pointsRequired = pointsRequired; }

    @Override
    public String toString() {
        return "Coupon{couponId=" + couponId + ", name='" + name + "', pointsRequired="
                + pointsRequired + "}";
    }
}
