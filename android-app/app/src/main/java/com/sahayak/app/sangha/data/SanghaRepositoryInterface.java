package com.sahayak.app.sangha.data;

import com.sahayak.app.citizen.AssistanceRequest;
import com.sahayak.app.models.Donation;
import com.sahayak.app.models.Sangha;

import java.util.List;

/**
 * Interface defining contract for Sangha data operations.
 * Allows decoupling UI controllers from data source. Currently implemented by
 * {@link SanghaDataRepository} using shared mock repositories, and can easily be
 * swapped with a Retrofit/REST API implementation when the backend is ready.
 */
public interface SanghaRepositoryInterface {

    /**
     * Retrieves high-level operational statistics for a specific Sangha.
     */
    SanghaOperationalStats getOperationalStats(String sanghaId);

    /**
     * Retrieves all assistance requests assigned to this Sangha.
     */
    List<AssistanceRequest> getAssignedRequests(String sanghaId);

    /**
     * Retrieves assistance requests assigned to this Sangha filtered by status.
     */
    List<AssistanceRequest> getAssignedRequestsByFilter(String sanghaId, String statusFilter);

    /**
     * Retrieves full details for a specific assistance request.
     */
    AssistanceRequest getRequestDetails(String requestId);

    /**
     * Retrieves public profile and operational metadata for a Sangha.
     */
    Sangha getSanghaProfile(String sanghaId);

    /**
     * Updates profile details for a Sangha.
     */
    boolean updateSanghaProfile(Sangha sangha);

    /**
     * Retrieves all donations & sponsorships received by this Sangha.
     */
    List<Donation> getReceivedDonations(String sanghaId);

    /**
     * Retrieves the cumulative donation amount received by this Sangha.
     */
    double getTotalDonationsAmount(String sanghaId);

    /**
     * Updates the status of an assigned assistance request with verification/progress notes.
     */
    void updateRequestStatus(String requestId, String status, String note);

    /**
     * Updates approval status for a Sangha user account.
     */
    void updateSanghaApprovalStatus(String userId, String status, String reason);
}

