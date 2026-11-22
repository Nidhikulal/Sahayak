package com.sahayak.app.citizen;

public final class RequestStatus {
    public static final String SUBMITTED = "SUBMITTED";
    public static final String ASSIGNED = "ASSIGNED";
    public static final String UNDER_VERIFICATION = "UNDER_VERIFICATION";
    public static final String VERIFIED = "VERIFIED";
    public static final String REJECTED = "REJECTED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private RequestStatus() {}

    public static String friendlyLabel(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case SUBMITTED: return "Submitted";
            case ASSIGNED: return "Sent to Sangha";
            case UNDER_VERIFICATION: return "Under verification";
            case VERIFIED: return "Verified";
            case REJECTED: return "Rejected";
            case IN_PROGRESS: return "Assistance in progress";
            case COMPLETED: return "Completed";
            case CANCELLED: return "Cancelled";
            default: return status;
        }
    }
}