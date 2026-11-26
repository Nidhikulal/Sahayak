package com.sahayak.app.data;

import com.sahayak.app.models.Donation;

import java.util.ArrayList;
import java.util.List;

public class DonationRepository {
    private static DonationRepository instance;
    private final List<Donation> donations = new ArrayList<>();
    private static int counter = 10;

    private DonationRepository() {
        // Pre-populate sample donation for demo citizen
        Donation d1 = new Donation(
                "DON-2026-0001",
                "usr-cit-001",
                "Ramesh Kumar",
                "SNG-002",
                "Karuna Social Support & Care Sangha",
                Donation.TYPE_REQUEST_SPECIFIC,
                "REQ-2026-0002",
                5000.0,
                "Sponsorship for Sharada Amma Cataract Treatment",
                "TXN-84920482",
                "UPI (Google Pay)"
        );
        d1.setTransactionDate(System.currentTimeMillis() - 86400000L * 3);
        donations.add(d1);

        Donation d2 = new Donation(
                "DON-2026-0002",
                "usr-cit-001",
                "Ramesh Kumar",
                "SNG-001",
                "Seva Bharathi Community Welfare Trust",
                Donation.TYPE_GENERAL,
                null,
                2500.0,
                "General Community Welfare Fund",
                "TXN-91028374",
                "Net Banking (SBI)"
        );
        d2.setTransactionDate(System.currentTimeMillis() - 86400000L * 7);
        donations.add(d2);
    }

    public static synchronized DonationRepository getInstance() {
        if (instance == null) {
            instance = new DonationRepository();
        }
        return instance;
    }

    public synchronized void recordDonation(Donation donation) {
        counter++;
        if (donation.getDonationId() == null || donation.getDonationId().isEmpty()) {
            donation.setDonationId(String.format("DON-2026-%04d", counter));
        }
        donations.add(0, donation);
    }

    public synchronized List<Donation> getDonationsByCitizen(String citizenId) {
        if (citizenId == null || citizenId.isEmpty()) return new ArrayList<>(donations);
        List<Donation> result = new ArrayList<>();
        for (Donation d : donations) {
            if (citizenId.equalsIgnoreCase(d.getCitizenId()) || d.getCitizenId() == null) {
                result.add(d);
            }
        }
        return result;
    }

    public synchronized double getTotalDonatedByCitizen(String citizenId) {
        double total = 0;
        for (Donation d : getDonationsByCitizen(citizenId)) {
            total += d.getAmount();
        }
        return total;
    }
}
