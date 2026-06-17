package com.iskollect.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {
    private int userId;
    private String name;
    private String username;
    private String webmail;
    private String password;
    private String course;
    private int yearLevel;
    private int age;
    private String profilePhoto;
    private double totalPoints;
    private int rawBottleCount;
    private int weeklyBottles;
    private int streak;
    private LocalDate lastSubmitDate;
    private String accountStatus;
    private int failedLoginAttempts = 0;
    private String sessionToken;
    private LocalDateTime lastActivity;
    private LocalDateTime createdAt;

    //for login
    public User(int id, String webmail, String password) {
        this.userId = id;
        this.webmail = webmail;
        this.password = password;
    }

    //for signup
    public User(String username, String webmail, String password) {
        this.username = username;
        this.name = username;
        this.webmail = webmail;
        this.password = password;
    }

    //complete User info
    public User(int userId, String username, String webmail, String password,
                   int age, String profilePhoto, double totalPoints,
                   int rawBottleCount, String accountStatus, int failedLoginAttempts,
                   String sessionToken, LocalDateTime lastActivity) {
        this.userId = userId;
        this.username = username;
        this.webmail = webmail;
        this.password = password;
        this.age = age;
        this.profilePhoto = profilePhoto;
        this.totalPoints = totalPoints;
        this.rawBottleCount = rawBottleCount;
        this.accountStatus = accountStatus;
        this.failedLoginAttempts = failedLoginAttempts;
        this.sessionToken = sessionToken;
        this.lastActivity = lastActivity;
    }

    public User() {}

    //getters and setters
    //userId
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name != null && !name.isBlank() ? name : username;
    }

    public void setName(String name) {
        this.name = name;
    }

    //username
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    //webmail
    public String getWebmail() {
        return webmail;
    }
    public void setWebmail(String webmail) {
        this.webmail = webmail;
    }

    //password
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public int getYearLevel() {
        return yearLevel;
    }

    public void setYearLevel(int yearLevel) {
        this.yearLevel = yearLevel;
    }

    //age
    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }

    //photo
    public String getProfilePhoto() {
        return profilePhoto;
    }
    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    //total pts
    public double getTotalPoints() {
        return totalPoints;
    }
    public void setTotalPoints(double totalPoints) {
        this.totalPoints = totalPoints;
    }

    //raw bottle count
    public int getRawBottleCount() {
        return rawBottleCount;
    }
    public void setRawBottleCount(int rawBottleCount) {
        this.rawBottleCount = rawBottleCount;
    }

    public int getWeeklyBottles() {
        return weeklyBottles;
    }

    public void setWeeklyBottles(int weeklyBottles) {
        this.weeklyBottles = weeklyBottles;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
    }

    public LocalDate getLastSubmitDate() {
        return lastSubmitDate;
    }

    public void setLastSubmitDate(LocalDate lastSubmitDate) {
        this.lastSubmitDate = lastSubmitDate;
    }

    //account status
    public String getAccountStatus() {
        return accountStatus;
    }
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    //failed login attempts
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    //session token
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    //last activity
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
