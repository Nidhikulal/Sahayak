package com.sahayak.app.sangha.data;

import com.sahayak.app.citizen.AssistanceRequest;
import com.sahayak.app.models.Donation;
import com.sahayak.app.models.Sangha;
import com.sahayak.app.models.SanghaContribution;
import com.sahayak.app.models.SanghaExpense;

import java.util.List;

/**
 * Interface defining contract for Sangha data operations.
 */
public interface SanghaRepositoryInterface {

    SanghaOperationalStats getOperationalStats(String sanghaId);

    List<AssistanceRequest> getAssignedRequests(String sanghaId);

    List<AssistanceRequest> getAssignedRequestsByFilter(String sanghaId, String statusFilter);

    AssistanceRequest getRequestDetails(String requestId);

    Sangha getSanghaProfile(String sanghaId);

    boolean updateSanghaProfile(Sangha sangha);

    List<Donation> getReceivedDonations(String sanghaId);

    double getTotalDonationsAmount(String sanghaId);

    List<SanghaExpense> getSanghaExpenses(String sanghaId);

    void addExpense(SanghaExpense expense);

    void addDonation(Donation donation);

    double getTotalExpensesAmount(String sanghaId);

    List<SanghaContribution> getSanghaContributions(String sanghaId);

    void addContribution(SanghaContribution contribution);

    double getTotalContributionsNetAmount(String sanghaId);

    double getAnnualBudget(String sanghaId);

    void setAnnualBudget(String sanghaId, double budget);

    double getAvailableBalance(String sanghaId);

    void updateRequestStatus(String requestId, String status, String note);

    void updateSanghaApprovalStatus(String userId, String status, String reason);
}
