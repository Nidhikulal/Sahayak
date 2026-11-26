package com.sahayak.app.data;

import com.sahayak.app.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserRepository {
    private static UserRepository instance;
    private final List<User> registeredUsers = new ArrayList<>();

    private UserRepository() {
        // Pre-populate demo accounts for easy testing
        User demoCitizen = new User("usr-cit-001", "Ramesh Kumar", "citizen@sahayak.org", "9876543210", "Password@123", "12/4 MG Road, Mangaluru", User.ROLE_CITIZEN);
        registeredUsers.add(demoCitizen);

        User demoSanghaApproved = new User("usr-sng-001", "Seva Bharathi Community Welfare Trust", "sangha@sahayak.org", "9812345678", "Password@123", "Kadri Park Road, Mangaluru", User.ROLE_SANGHA);
        demoSanghaApproved.setSanghaId("SNG-001");
        demoSanghaApproved.setRegistrationNo("REG-SNG-2021-042");
        demoSanghaApproved.setContactPerson("Ramachandra Bhat");
        demoSanghaApproved.setDistrict("Dakshina Kannada");
        demoSanghaApproved.setDescription("Registered NGO serving senior citizens and needy families since 2012.");
        demoSanghaApproved.setApprovalStatus(User.STATUS_APPROVED);
        registeredUsers.add(demoSanghaApproved);

        User demoSanghaPending = new User("usr-sng-002", "Karuna Social Support & Care Sangha", "karuna@sahayak.org", "9899887766", "Password@123", "Kodialbail, Mangaluru", User.ROLE_SANGHA);
        demoSanghaPending.setSanghaId("SNG-002");
        demoSanghaPending.setRegistrationNo("REG-SNG-2022-109");
        demoSanghaPending.setContactPerson("Sister Mary");
        demoSanghaPending.setDistrict("Dakshina Kannada");
        demoSanghaPending.setDescription("Welfare association dedicated to child education and emergency medical aid.");
        demoSanghaPending.setApprovalStatus(User.STATUS_PENDING);
        registeredUsers.add(demoSanghaPending);

        User demoSanghaRejected = new User("usr-sng-003", "Janata Seva Mandali", "rejected@sahayak.org", "9871122334", "Password@123", "Bantwal Road, Mangaluru", User.ROLE_SANGHA);
        demoSanghaRejected.setSanghaId("SNG-004");
        demoSanghaRejected.setRegistrationNo("REG-SNG-2023-011");
        demoSanghaRejected.setContactPerson("Anand Rao");
        demoSanghaRejected.setDistrict("Dakshina Kannada");
        demoSanghaRejected.setDescription("Local welfare group.");
        demoSanghaRejected.setApprovalStatus(User.STATUS_REJECTED);
        demoSanghaRejected.setRejectionReason("Official NGO Darpan registration certificate could not be verified with Ministry database.");
        registeredUsers.add(demoSanghaRejected);
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public synchronized boolean isEmailRegistered(String email) {
        if (email == null) return false;
        for (User user : registeredUsers) {
            if (email.equalsIgnoreCase(user.getEmail())) return true;
        }
        return false;
    }

    public synchronized boolean isMobileRegistered(String mobile) {
        if (mobile == null) return false;
        for (User user : registeredUsers) {
            if (mobile.equals(user.getMobile())) return true;
        }
        return false;
    }

    public synchronized User registerUser(User user) {
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            user.setUserId("usr-" + UUID.randomUUID().toString().substring(0, 8));
        }
        registeredUsers.add(user);
        return user;
    }

    public synchronized User authenticate(String emailOrMobile, String password) {
        if (emailOrMobile == null || password == null) return null;
        String query = emailOrMobile.trim();
        for (User user : registeredUsers) {
            if ((query.equalsIgnoreCase(user.getEmail()) || query.equals(user.getMobile()))
                    && password.equals(user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    public synchronized User findById(String userId) {
        for (User user : registeredUsers) {
            if (user.getUserId().equals(userId)) return user;
        }
        return null;
    }

    public synchronized void updateApprovalStatus(String userId, String status) {
        User user = findById(userId);
        if (user != null) {
            user.setApprovalStatus(status);
        }
    }

    public synchronized void updateApprovalStatusWithReason(String userId, String status, String reason) {
        User user = findById(userId);
        if (user != null) {
            user.setApprovalStatus(status);
            user.setRejectionReason(reason);
        }
    }
}
