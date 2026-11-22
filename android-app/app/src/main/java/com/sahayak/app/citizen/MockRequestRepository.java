package com.sahayak.app.citizen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MockRequestRepository {
    private static final List<AssistanceRequest> requests = new ArrayList<>();
    private static int counter = 0;

    static {
        // Fictional demo data only
        AssistanceRequest r = new AssistanceRequest();
        r.requestId = "REQ-2026-0001";
        r.beneficiaryName = "Demo Family";
        r.houseAddress = "House 12, Demo Street";
        r.locality = "Kadri";
        r.district = "Dakshina Kannada";
        r.assistanceType = "EDUCATION";
        r.description = "School fee support needed.";
        r.reporterRelationship = "Neighbour";
        r.consentConfirmed = true;
        r.status = RequestStatus.UNDER_VERIFICATION;
        r.createdAt = System.currentTimeMillis() - 86400000L;
        requests.add(r);
        counter = 1;
    }

    public static synchronized AssistanceRequest submit(AssistanceRequest r) {
        counter++;
        r.requestId = String.format(Locale.US, "REQ-2026-%04d", counter);
        r.status = RequestStatus.SUBMITTED;
        r.createdAt = System.currentTimeMillis();
        requests.add(0, r);
        return r;
    }

    public static List<AssistanceRequest> getMine() {
        return new ArrayList<>(requests);
    }

    public static AssistanceRequest getById(String requestId) {
        for (AssistanceRequest r : requests) {
            if (r.requestId.equals(requestId)) return r;
        }
        return null;
    }
}