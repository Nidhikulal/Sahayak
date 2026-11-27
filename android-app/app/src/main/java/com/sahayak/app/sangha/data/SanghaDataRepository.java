package com.sahayak.app.sangha.data;

import com.sahayak.app.citizen.AssistanceRequest;
import com.sahayak.app.citizen.MockRequestRepository;
import com.sahayak.app.citizen.RequestStatus;
import com.sahayak.app.data.DonationRepository;
import com.sahayak.app.data.SanghaRepository;
import com.sahayak.app.data.UserRepository;
import com.sahayak.app.models.Donation;
import com.sahayak.app.models.Sangha;
import com.sahayak.app.models.SanghaExpense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory repository implementation of {@link SanghaRepositoryInterface}.
 * Interacts with shared application repositories and maintains financial ledger state.
 * Follows singleton pattern for consistent mock state throughout the app session.
 */
public class SanghaDataRepository implements SanghaRepositoryInterface {

    private static SanghaDataRepository instance;

    private static final List<SanghaExpense> mockExpenses = new ArrayList<>();
    private static final Map<String, Double> annualBudgets = new HashMap<>();

    static {
        if (mockExpenses.isEmpty()) {
            mockExpenses.add(new SanghaExpense("EXP-001", "SNG-001", 3500.0, "Emergency medical aid", "Medical Treatment", "REQ-101", "TXN-EXP-901"));
            mockExpenses.add(new SanghaExpense("EXP-002", "SNG-001", 2000.0, "Monthly food kit distribution", "Groceries and Food", "REQ-102", "TXN-EXP-902"));
        }
        annualBudgets.put("SNG-001", 100000.0);
    }

    private SanghaDataRepository() {
    }

    public static synchronized SanghaDataRepository getInstance() {
        if (instance == null) {
            instance = new SanghaDataRepository();
        }
        return instance;
    }

    @Override
    public SanghaOperationalStats getOperationalStats(String sanghaId) {
        List<AssistanceRequest> assigned = getAssignedRequests(sanghaId);

        int totalAssigned = assigned.size();
        int pendingVerification = 0;
        int approvedAwaitingAid = 0;
        int assistanceInProgress = 0;
        int completedCases = 0;
        int rejectedCases = 0;

        for (AssistanceRequest r : assigned) {
            if (r.status == null) continue;
            switch (r.status) {
                case RequestStatus.SUBMITTED:
                case RequestStatus.ASSIGNED:
                case RequestStatus.UNDER_VERIFICATION:
                    pendingVerification++;
                    break;
                case RequestStatus.VERIFIED:
                    approvedAwaitingAid++;
                    break;
                case RequestStatus.IN_PROGRESS:
                    assistanceInProgress++;
                    break;
                case RequestStatus.COMPLETED:
                    completedCases++;
                    break;
                case RequestStatus.REJECTED:
                    rejectedCases++;
                    break;
            }
        }

        double totalSponsorships = getTotalDonationsAmount(sanghaId);

        return new SanghaOperationalStats(
                totalAssigned,
                pendingVerification,
                approvedAwaitingAid,
                assistanceInProgress,
                completedCases,
                rejectedCases,
                totalSponsorships
        );
    }

    @Override
    public List<AssistanceRequest> getAssignedRequests(String sanghaId) {
        List<AssistanceRequest> all = MockRequestRepository.getAllRequests();
        List<AssistanceRequest> assigned = new ArrayList<>();

        if (sanghaId == null || sanghaId.trim().isEmpty()) {
            return assigned;
        }

        for (AssistanceRequest r : all) {
            if (r.assignedSanghaId != null && r.assignedSanghaId.equalsIgnoreCase(sanghaId.trim())) {
                assigned.add(r);
            }
        }
        return assigned;
    }

    @Override
    public List<AssistanceRequest> getAssignedRequestsByFilter(String sanghaId, String statusFilter) {
        List<AssistanceRequest> assigned = getAssignedRequests(sanghaId);
        if (statusFilter == null || "ALL".equalsIgnoreCase(statusFilter)) {
            return assigned;
        }

        List<AssistanceRequest> filtered = new ArrayList<>();
        for (AssistanceRequest r : assigned) {
            if (statusFilter.equalsIgnoreCase(r.status)) {
                filtered.add(r);
            }
        }
        return filtered;
    }

    @Override
    public AssistanceRequest getRequestDetails(String requestId) {
        return MockRequestRepository.getById(requestId);
    }

    @Override
    public Sangha getSanghaProfile(String sanghaId) {
        return SanghaRepository.getInstance().getSanghaById(sanghaId);
    }

    @Override
    public boolean updateSanghaProfile(Sangha updated) {
        if (updated == null || updated.getSanghaId() == null) return false;
        Sangha existing = SanghaRepository.getInstance().getSanghaById(updated.getSanghaId());
        if (existing != null) {
            existing.setName(updated.getName());
            existing.setRegistrationNo(updated.getRegistrationNo());
            existing.setLocality(updated.getLocality());
            existing.setCity(updated.getCity());
            existing.setDistrict(updated.getDistrict());
            existing.setFullAddress(updated.getFullAddress());
            existing.setShortDescription(updated.getShortDescription());
            existing.setFullDescription(updated.getFullDescription());
            existing.setContactPhone(updated.getContactPhone());
            existing.setContactEmail(updated.getContactEmail());
            existing.setAssistanceActivities(updated.getAssistanceActivities());
            return true;
        }
        return false;
    }

    @Override
    public List<Donation> getReceivedDonations(String sanghaId) {
        List<Donation> result = new ArrayList<>();
        if (sanghaId == null || sanghaId.trim().isEmpty()) {
            return result;
        }

        List<Donation> all = DonationRepository.getInstance().getDonationsByCitizen(null);
        for (Donation d : all) {
            if (d.getSanghaId() != null && d.getSanghaId().equalsIgnoreCase(sanghaId.trim())) {
                result.add(d);
            }
        }
        return result;
    }

    @Override
    public double getTotalDonationsAmount(String sanghaId) {
        double total = 0;
        for (Donation d : getReceivedDonations(sanghaId)) {
            total += d.getAmount();
        }
        return total;
    }

    @Override
    public List<SanghaExpense> getSanghaExpenses(String sanghaId) {
        List<SanghaExpense> result = new ArrayList<>();
        if (sanghaId == null || sanghaId.trim().isEmpty()) return result;

        for (SanghaExpense exp : mockExpenses) {
            if (exp.getSanghaId() != null && exp.getSanghaId().equalsIgnoreCase(sanghaId.trim())) {
                result.add(exp);
            }
        }
        return result;
    }

    @Override
    public void addExpense(SanghaExpense expense) {
        if (expense != null) {
            mockExpenses.add(0, expense);
        }
    }

    @Override
    public void addDonation(Donation donation) {
        if (donation != null) {
            DonationRepository.getInstance().recordDonation(donation);
        }
    }

    @Override
    public double getTotalExpensesAmount(String sanghaId) {
        double total = 0;
        for (SanghaExpense exp : getSanghaExpenses(sanghaId)) {
            total += exp.getAmount();
        }
        return total;
    }

    @Override
    public double getAnnualBudget(String sanghaId) {
        if (sanghaId == null) return 100000.0;
        return annualBudgets.getOrDefault(sanghaId, 100000.0);
    }

    @Override
    public void setAnnualBudget(String sanghaId, double budget) {
        if (sanghaId != null) {
            annualBudgets.put(sanghaId, budget);
        }
    }

    @Override
    public double getAvailableBalance(String sanghaId) {
        double totalFunds = getAnnualBudget(sanghaId);
        double totalDonations = getTotalDonationsAmount(sanghaId);
        double totalExpenses = getTotalExpensesAmount(sanghaId);
        return (totalFunds + totalDonations) - totalExpenses;
    }

    @Override
    public void updateRequestStatus(String requestId, String status, String note) {
        MockRequestRepository.updateStatus(requestId, status, note);
    }

    @Override
    public void updateSanghaApprovalStatus(String userId, String status, String reason) {
        UserRepository.getInstance().updateApprovalStatusWithReason(userId, status, reason);
    }
}
