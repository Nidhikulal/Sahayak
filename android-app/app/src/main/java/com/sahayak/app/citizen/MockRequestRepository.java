package com.sahayak.app.citizen;

import com.sahayak.app.data.SanghaRepository;
import com.sahayak.app.models.Sangha;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * In-memory singleton repository for {@link AssistanceRequest} objects.
 * Acts as the single source of truth for all requests throughout the app session.
 * Pre-populated with demo data for testing.
 */
public final class MockRequestRepository {

    private static final List<AssistanceRequest> requests = new ArrayList<>();

    static {
        // --- Demo Request 1: Under Verification ---
        AssistanceRequest r1 = new AssistanceRequest();
        r1.requestId = "REQ-2026-0001";
        r1.citizenId = "usr-cit-001";
        r1.citizenName = "Ramesh Kumar";
        r1.beneficiaryName = "Shantha Bai";
        r1.age = 72;
        r1.gender = "Female";
        r1.houseAddress = "12/4 MG Road";
        r1.locality = "Kadri";
        r1.city = "Mangaluru";
        r1.district = "Dakshina Kannada";
        r1.pincode = "575002";
        r1.latitude = 12.8756;
        r1.longitude = 74.8432;
        r1.assistanceType = "Support for Elderly People";
        r1.description = "Elderly widow living alone, requires regular medical support and grocery assistance.";
        r1.reporterRelationship = "Neighbour";
        r1.consentConfirmed = true;
        r1.status = RequestStatus.UNDER_VERIFICATION;
        r1.assignedSanghaId = "SNG-001";
        r1.assignedSanghaName = "Seva Bharathi Community Welfare Trust";
        r1.targetAmount = 15000;
        r1.raisedAmount = 4500;
        requests.add(r1);

        // --- Demo Request 2: Verified / In-Progress ---
        AssistanceRequest r2 = new AssistanceRequest();
        r2.requestId = "REQ-2026-0002";
        r2.citizenId = "usr-cit-001";
        r2.citizenName = "Ramesh Kumar";
        r2.beneficiaryName = "Mohan Das";
        r2.age = 45;
        r2.gender = "Male";
        r2.houseAddress = "7/B Jyothi Nagar";
        r2.locality = "Kodialbail";
        r2.city = "Mangaluru";
        r2.district = "Dakshina Kannada";
        r2.pincode = "575003";
        r2.latitude = 12.8601;
        r2.longitude = 74.8352;
        r2.assistanceType = "Medical Treatment";
        r2.description = "Patient requires dialysis treatment twice a week. Family unable to afford recurring medical expenses.";
        r2.reporterRelationship = "Friend";
        r2.consentConfirmed = true;
        r2.status = RequestStatus.IN_PROGRESS;
        r2.assignedSanghaId = "SNG-002";
        r2.assignedSanghaName = "Karuna Social Support & Care Sangha";
        r2.targetAmount = 50000;
        r2.raisedAmount = 22000;
        requests.add(r2);

        // --- Demo Request 3: Completed ---
        AssistanceRequest r3 = new AssistanceRequest();
        r3.requestId = "REQ-2026-0003";
        r3.citizenId = "usr-cit-001";
        r3.citizenName = "Ramesh Kumar";
        r3.beneficiaryName = "Kavitha K";
        r3.age = 34;
        r3.gender = "Female";
        r3.houseAddress = "Near Bus Stop, 3rd Cross";
        r3.locality = "Surathkal";
        r3.city = "Mangaluru";
        r3.district = "Dakshina Kannada";
        r3.pincode = "575014";
        r3.assistanceType = "Groceries and Food";
        r3.description = "Single mother with two children, lost job during illness. Needs monthly grocery support.";
        r3.reporterRelationship = "Self";
        r3.consentConfirmed = true;
        r3.status = RequestStatus.COMPLETED;
        r3.assignedSanghaId = "SNG-001";
        r3.assignedSanghaName = "Seva Bharathi Community Welfare Trust";
        r3.targetAmount = 8000;
        r3.raisedAmount = 8000;
        requests.add(r3);

        // --- Demo Request 4: Assigned to SNG-001 (for Sangha dashboard) ---
        AssistanceRequest r4 = new AssistanceRequest();
        r4.requestId = "REQ-2026-0004";
        r4.citizenId = "usr-cit-002";
        r4.citizenName = "Priya Shenoy";
        r4.beneficiaryName = "Raju Poojary";
        r4.age = 60;
        r4.gender = "Male";
        r4.houseAddress = "15, 4th Main, Derebail";
        r4.locality = "Derebail";
        r4.city = "Mangaluru";
        r4.district = "Dakshina Kannada";
        r4.pincode = "575006";
        r4.assistanceType = "Financial Assistance";
        r4.description = "Retired daily-wage worker with no income after injury. Needs urgent financial help for rent and food.";
        r4.reporterRelationship = "Relative";
        r4.consentConfirmed = true;
        r4.status = RequestStatus.ASSIGNED;
        r4.assignedSanghaId = "SNG-001";
        r4.assignedSanghaName = "Seva Bharathi Community Welfare Trust";
        r4.targetAmount = 12000;
        r4.raisedAmount = 0;
        requests.add(r4);

        // --- Demo Request 5: Rejected ---
        AssistanceRequest r5 = new AssistanceRequest();
        r5.requestId = "REQ-2026-0005";
        r5.citizenId = "usr-cit-002";
        r5.citizenName = "Priya Shenoy";
        r5.beneficiaryName = "Unknown Person";
        r5.age = 50;
        r5.gender = "Male";
        r5.houseAddress = "Unknown Address";
        r5.locality = "Mangaluru City";
        r5.city = "Mangaluru";
        r5.district = "Dakshina Kannada";
        r5.pincode = "575001";
        r5.assistanceType = "Other Emergency Needs";
        r5.description = "Insufficient details provided for verification.";
        r5.reporterRelationship = "Unknown";
        r5.consentConfirmed = true;
        r5.status = RequestStatus.REJECTED;
        r5.rejectionReason = "Beneficiary address verification unconfirmed by Sangha field visit.";
        r5.assignedSanghaId = "SNG-002";
        r5.assignedSanghaName = "Karuna Social Support & Care Sangha";
        r5.targetAmount = 5000;
        r5.raisedAmount = 0;
        requests.add(r5);
    }

    private MockRequestRepository() {}

    /**
     * Returns all requests in the repository.
     */
    public static synchronized List<AssistanceRequest> getAllRequests() {
        return new ArrayList<>(requests);
    }

    /**
     * Returns all requests submitted by a specific citizen.
     */
    public static synchronized List<AssistanceRequest> getMine(String citizenId) {
        List<AssistanceRequest> result = new ArrayList<>();
        if (citizenId == null) return result;
        for (AssistanceRequest r : requests) {
            if (Objects.equals(citizenId, r.citizenId)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Returns the request with the given ID, or null if not found.
     */
    public static synchronized AssistanceRequest getById(String requestId) {
        if (requestId == null) return null;
        for (AssistanceRequest r : requests) {
            if (Objects.equals(requestId, r.requestId)) return r;
        }
        return null;
    }

    /**
     * Submits a new {@link AssistanceRequest}, assigns a request ID, sets initial status,
     * and auto-assigns the first approved Sangha from {@link SanghaRepository}.
     *
     * @return the submitted request (mutated in place)
     */
    public static synchronized AssistanceRequest submit(AssistanceRequest request) {
        if (request.requestId == null || request.requestId.isEmpty()) {
            request.requestId = "REQ-" + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                    + "-" + String.format(java.util.Locale.US, "%04d", requests.size() + 1);
        }
        request.status = RequestStatus.ASSIGNED;
        request.createdAt = System.currentTimeMillis();
        request.updatedAt = System.currentTimeMillis();

        // Auto-assign the first available approved Sangha
        if (request.assignedSanghaId == null) {
            List<Sangha> approved = SanghaRepository.getInstance().getApprovedSanghas();
            if (!approved.isEmpty()) {
                Sangha sangha = approved.get(0);
                request.assignedSanghaId = sangha.getSanghaId();
                request.assignedSanghaName = sangha.getName();
            }
        }

        requests.add(request);
        return request;
    }

    /**
     * Increments the {@link AssistanceRequest#raisedAmount} of a request by the given amount.
     * Called after a successful donation payment to reflect updated sponsorship progress.
     *
     * @param requestId the ID of the request to update
     * @param amount    the amount to add to raisedAmount
     */
    public static synchronized void addRaisedAmount(String requestId, double amount) {
        AssistanceRequest request = getById(requestId);
        if (request != null) {
            request.raisedAmount += amount;
            request.updatedAt = System.currentTimeMillis();
        }
    }

    /**
     * Updates the status and optional note/rejection reason of a request.
     *
     * @param requestId the ID of the request to update
     * @param status    the new status (see {@link RequestStatus})
     * @param note      optional rejection reason or status note; ignored if null
     */
    public static synchronized void updateStatus(String requestId, String status, String note) {
        AssistanceRequest request = getById(requestId);
        if (request != null) {
            request.status = status;
            request.updatedAt = System.currentTimeMillis();
            if (note != null) {
                request.rejectionReason = note;
            }
        }
    }
}