package com.sahayak.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.sahayak.app.models.User;

public class SessionManager {
    private static final String PREF_NAME = "sahayak_user_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_DOB = "date_of_birth";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_LOCALITY_CITY = "locality_city";
    private static final String KEY_PINCODE = "pincode";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_APPROVAL_STATUS = "approval_status";
    private static final String KEY_REGISTRATION_NO = "registration_no";
    private static final String KEY_SANGHA_ID = "sangha_id";
    private static final String KEY_REJECTION_REASON = "rejection_reason";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void createSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, user.getUserId());
        editor.putString(KEY_FULL_NAME, user.getFullName());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_MOBILE, user.getMobile());
        editor.putString(KEY_ADDRESS, user.getAddress());
        editor.putString(KEY_DOB, user.getDateOfBirth());
        editor.putString(KEY_GENDER, user.getGender());
        editor.putString(KEY_LOCALITY_CITY, user.getLocalityCity());
        editor.putString(KEY_PINCODE, user.getPinCode());
        editor.putString(KEY_ROLE, user.getRole());
        editor.putString(KEY_APPROVAL_STATUS, user.getApprovalStatus());
        editor.putString(KEY_REGISTRATION_NO, user.getRegistrationNo());
        editor.putString(KEY_SANGHA_ID, user.getSanghaId());
        editor.putString(KEY_REJECTION_REASON, user.getRejectionReason());
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public User getCurrentUser() {
        if (!isLoggedIn()) return null;
        User user = new User();
        user.setUserId(prefs.getString(KEY_USER_ID, ""));
        user.setFullName(prefs.getString(KEY_FULL_NAME, "Citizen"));
        user.setEmail(prefs.getString(KEY_EMAIL, ""));
        user.setMobile(prefs.getString(KEY_MOBILE, ""));
        user.setAddress(prefs.getString(KEY_ADDRESS, ""));
        user.setDateOfBirth(prefs.getString(KEY_DOB, ""));
        user.setGender(prefs.getString(KEY_GENDER, ""));
        user.setLocalityCity(prefs.getString(KEY_LOCALITY_CITY, ""));
        user.setPinCode(prefs.getString(KEY_PINCODE, ""));
        user.setRole(prefs.getString(KEY_ROLE, User.ROLE_CITIZEN));
        user.setApprovalStatus(prefs.getString(KEY_APPROVAL_STATUS, User.STATUS_APPROVED));
        user.setRegistrationNo(prefs.getString(KEY_REGISTRATION_NO, ""));
        user.setSanghaId(prefs.getString(KEY_SANGHA_ID, ""));
        user.setRejectionReason(prefs.getString(KEY_REJECTION_REASON, ""));
        return user;
    }

    public String getUserRole() {
        return prefs.getString(KEY_ROLE, User.ROLE_CITIZEN);
    }

    public String getApprovalStatus() {
        return prefs.getString(KEY_APPROVAL_STATUS, User.STATUS_APPROVED);
    }

    public String getSanghaId() {
        return prefs.getString(KEY_SANGHA_ID, "");
    }

    public String getRejectionReason() {
        return prefs.getString(KEY_REJECTION_REASON, "");
    }

    public void updateApprovalStatus(String status) {
        editor.putString(KEY_APPROVAL_STATUS, status);
        editor.apply();
    }

    public void updateApprovalStatus(String status, String reason) {
        editor.putString(KEY_APPROVAL_STATUS, status);
        editor.putString(KEY_REJECTION_REASON, reason);
        editor.apply();
    }

    public void updateUserProfile(String fullName, String mobile, String address) {
        editor.putString(KEY_FULL_NAME, fullName);
        editor.putString(KEY_MOBILE, mobile);
        editor.putString(KEY_ADDRESS, address);
        editor.apply();
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}