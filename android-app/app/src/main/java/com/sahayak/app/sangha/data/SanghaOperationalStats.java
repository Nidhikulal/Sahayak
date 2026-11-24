package com.sahayak.app.sangha.data;

import java.io.Serializable;

public class SanghaOperationalStats implements Serializable {
    private int totalAssigned;
    private int pendingVerification;
    private int approvedAwaitingAid;
    private int assistanceInProgress;
    private int completedCases;
    private int rejectedCases;
    private double totalSponsorships;

    public SanghaOperationalStats() {
    }

    public SanghaOperationalStats(int totalAssigned, int pendingVerification, int approvedAwaitingAid,
                                  int assistanceInProgress, int completedCases, int rejectedCases,
                                  double totalSponsorships) {
        this.totalAssigned = totalAssigned;
        this.pendingVerification = pendingVerification;
        this.approvedAwaitingAid = approvedAwaitingAid;
        this.assistanceInProgress = assistanceInProgress;
        this.completedCases = completedCases;
        this.rejectedCases = rejectedCases;
        this.totalSponsorships = totalSponsorships;
    }

    public int getTotalAssigned() {
        return totalAssigned;
    }

    public void setTotalAssigned(int totalAssigned) {
        this.totalAssigned = totalAssigned;
    }

    public int getPendingVerification() {
        return pendingVerification;
    }

    public void setPendingVerification(int pendingVerification) {
        this.pendingVerification = pendingVerification;
    }

    public int getApprovedAwaitingAid() {
        return approvedAwaitingAid;
    }

    public void setApprovedAwaitingAid(int approvedAwaitingAid) {
        this.approvedAwaitingAid = approvedAwaitingAid;
    }

    public int getAssistanceInProgress() {
        return assistanceInProgress;
    }

    public void setAssistanceInProgress(int assistanceInProgress) {
        this.assistanceInProgress = assistanceInProgress;
    }

    public int getCompletedCases() {
        return completedCases;
    }

    public void setCompletedCases(int completedCases) {
        this.completedCases = completedCases;
    }

    public int getRejectedCases() {
        return rejectedCases;
    }

    public void setRejectedCases(int rejectedCases) {
        this.rejectedCases = rejectedCases;
    }

    public double getTotalSponsorships() {
        return totalSponsorships;
    }

    public void setTotalSponsorships(double totalSponsorships) {
        this.totalSponsorships = totalSponsorships;
    }
}

