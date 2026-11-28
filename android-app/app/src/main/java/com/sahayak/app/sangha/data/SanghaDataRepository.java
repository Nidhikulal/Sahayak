package com.sahayak.app.sangha.data;

import com.google.firebase.firestore.FirebaseFirestore;
import com.sahayak.app.citizen.AssistanceRequest;
import com.sahayak.app.citizen.MockRequestRepository;
import com.sahayak.app.citizen.RequestStatus;
import com.sahayak.app.data.DonationRepository;
import com.sahayak.app.data.SanghaRepository;
import com.sahayak.app.data.UserRepository;
import com.sahayak.app.models.Donation;
import com.sahayak.app.models.Sangha;
import com.sahayak.app.models.SanghaContribution;
import com.sahayak.app.models.SanghaExpense;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory & Firestore-backed repository implementation of {@link SanghaRepositoryInterface}.
 * Initializes Total Funds / Annual Budget to ₹0 by default and calculates summaries dynamically.
 */
public class SanghaDataRepository implements SanghaRepositoryInterface {

    private static SanghaDataRepository instance;

    private static final List<SanghaExpense> mockExpenses = new ArrayList<>();
    private static final List<SanghaContribution> mockContributions = new ArrayList<>();
    private static final Map<String, Double> annualBudgets = new HashMap<>();

    static {
        // Default Total Funds / Annual Budget set to ₹0
        annualBudgets.put("SNG-001", 0.0);
    }

    private SanghaDataRepository() {
        resetBudgetToZeroIfNeeded("SNG-001");
    }

    public static synchronized SanghaDataRepository getInstance() {
        if (instance == null) {
            instance = new SanghaDataRepository();
        }
        return instance;
    }

    private void resetBudgetToZeroIfNeeded(String sanghaId) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("sangha_budgets").document(sanghaId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            db.collection("sangha_budgets").document(sanghaId)
                                    .set(Collections.singletonMap("budget", 0.0));
                        }
                    });
        } catch (Exception ignored) {}
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
            try {
                FirebaseFirestore.getInstance().collection("sangha_expenses")
                        .document(expense.getExpenseId())
                        .set(expense);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void addDonation(Donation donation) {
        if (donation != null) {
            DonationRepository.getInstance().recordDonation(donation);
            try {
                FirebaseFirestore.getInstance().collection("sangha_donations")
                        .document(donation.getDonationId())
                        .set(donation);
            } catch (Exception ignored) {}
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
    public List<SanghaContribution> getSanghaContributions(String sanghaId) {
        List<SanghaContribution> result = new ArrayList<>();
        if (sanghaId == null || sanghaId.trim().isEmpty()) return result;

        for (SanghaContribution c : mockContributions) {
            if (c.getSanghaId() != null && c.getSanghaId().equalsIgnoreCase(sanghaId.trim())) {
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public void addContribution(SanghaContribution contribution) {
        if (contribution != null) {
            mockContributions.add(0, contribution);
            try {
                FirebaseFirestore.getInstance().collection("sangha_contributions")
                        .document(contribution.getContributionId())
                        .set(contribution);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public double getTotalContributionsNetAmount(String sanghaId) {
        double net = 0;
        for (SanghaContribution c : getSanghaContributions(sanghaId)) {
            if (SanghaContribution.TYPE_RECEIVED.equalsIgnoreCase(c.getDirection())) {
                net += c.getAmount();
            } else if (SanghaContribution.TYPE_GIVEN.equalsIgnoreCase(c.getDirection())) {
                net -= c.getAmount();
            }
        }
        return net;
    }

    @Override
    public double getAnnualBudget(String sanghaId) {
        if (sanghaId == null) return 0.0;
        return annualBudgets.getOrDefault(sanghaId, 0.0);
    }

    @Override
    public double getAvailableBalance(String sanghaId) {
        double totalFunds = getAnnualBudget(sanghaId);
        double totalDonations = getTotalDonationsAmount(sanghaId);
        double netContributions = getTotalContributionsNetAmount(sanghaId);
        double totalExpenses = getTotalExpensesAmount(sanghaId);
        return (totalFunds + totalDonations + netContributions) - totalExpenses;
    }

    @Override
    public void setAnnualBudget(String sanghaId, double budget) {
        if (sanghaId != null) {
            annualBudgets.put(sanghaId, budget);
            try {
                FirebaseFirestore.getInstance().collection("sangha_budgets")
                        .document(sanghaId)
                        .set(Collections.singletonMap("budget", budget));
            } catch (Exception ignored) {}
        }
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
