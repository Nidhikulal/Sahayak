package com.sahayak.app.data;

import com.sahayak.app.models.NotificationItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationRepository {
    private static NotificationRepository instance;
    private final List<NotificationItem> notifications = new ArrayList<>();

    private NotificationRepository() {
        // Pre-populate realistic notifications for demo user
        NotificationItem n1 = new NotificationItem(
                "NOTIF-01",
                "usr-cit-001",
                "Welcome to Sahayak!",
                "Thank you for registering as a Citizen. Together we can support and uplift communities in need.",
                "REGISTRATION"
        );
        n1.setTimestamp(System.currentTimeMillis() - 86400000L * 7);
        n1.setRead(true);
        notifications.add(n1);

        NotificationItem n2 = new NotificationItem(
                "NOTIF-02",
                "usr-cit-001",
                "Request REQ-2026-0001 Assigned",
                "Your assistance request for Suresh Bhat has been assigned to Seva Bharathi Community Welfare Trust for ground verification.",
                "REQUEST_UPDATE"
        );
        n2.setTimestamp(System.currentTimeMillis() - 86400000L * 2);
        n2.setRead(false);
        notifications.add(n2);

        NotificationItem n3 = new NotificationItem(
                "NOTIF-03",
                "usr-cit-001",
                "Donation Successful",
                "Your sponsorship of ₹5,000 for Sharada Amma's cataract treatment has been received. Thank you for your generosity!",
                "DONATION"
        );
        n3.setTimestamp(System.currentTimeMillis() - 86400000L * 1);
        n3.setRead(false);
        notifications.add(n3);

        NotificationItem n4 = new NotificationItem(
                "NOTIF-04",
                "usr-cit-001",
                "Request REQ-2026-0003 Completed",
                "Assistance for Ganesh & Family has been successfully delivered by Asha Deepa Foundation. Thank you for reporting!",
                "REQUEST_UPDATE"
        );
        n4.setTimestamp(System.currentTimeMillis() - 43200000L);
        n4.setRead(false);
        notifications.add(n4);
    }

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    public synchronized void addNotification(String userId, String title, String message, String type) {
        NotificationItem item = new NotificationItem("NOTIF-" + UUID.randomUUID().toString().substring(0, 6), userId, title, message, type);
        notifications.add(0, item);
    }

    public synchronized List<NotificationItem> getNotifications(String userId) {
        if (userId == null || userId.isEmpty()) return new ArrayList<>(notifications);
        List<NotificationItem> userList = new ArrayList<>();
        for (NotificationItem item : notifications) {
            if (userId.equalsIgnoreCase(item.getUserId()) || item.getUserId() == null) {
                userList.add(item);
            }
        }
        return userList;
    }

    public synchronized int getUnreadCount(String userId) {
        int count = 0;
        for (NotificationItem item : getNotifications(userId)) {
            if (!item.isRead()) count++;
        }
        return count;
    }

    public synchronized void markAllAsRead(String userId) {
        for (NotificationItem item : getNotifications(userId)) {
            item.setRead(true);
        }
    }

    public synchronized void clearAll(String userId) {
        List<NotificationItem> toRemove = new ArrayList<>();
        for (NotificationItem item : notifications) {
            if (userId == null || userId.equalsIgnoreCase(item.getUserId())) {
                toRemove.add(item);
            }
        }
        notifications.removeAll(toRemove);
    }
}
