package com.sahayak.app.data;

import com.sahayak.app.models.Donation;

import java.util.ArrayList;
import java.util.List;

public class DonationRepository {
    private static DonationRepository instance;
    private final List<Donation> donations = new ArrayList<>();
    private static int counter = 30;

    private DonationRepository() {
        // Pre-populate sample donations for SNG-001 with duplicate/similar categories for robust filtering
        Donation d1 = new Donation(
                "DON-2026-0001",
                "usr-cit-001",
                "Ramesh Kumar",
                "SNG-001",
                "Seva Bharathi Community Welfare Trust",
                Donation.TYPE_REQUEST_SPECIFIC,
                "REQ-2026-0002",
                5000.0,
                "Sponsorship for Sharada Amma Cataract Treatment (Medical Treatment)",
                "TXN-84920482",
                "UPI (Google Pay)"
        );
        d1.setTransactionDate(System.currentTimeMillis() - 86400000L * 10);
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
        d2.setTransactionDate(System.currentTimeMillis() - 86400000L * 9);
        donations.add(d2);

        Donation d3 = new Donation(
                "DON-2026-0003",
                "usr-cit-002",
                "Anitha Shetty",
                "SNG-001",
                "Seva Bharathi Community Welfare Trust",
                Donation.TYPE_REQUEST_SPECIFIC,
                "REQ-101",
                7500.0,
                "Medical Assistance donation for emergency surgery",
                "TXN-33445566",
                "Google Pay"
        );
        d3.setTransactionDate(System.currentTimeMillis() - 86400000L * 8);
        donations.add(d3);

        Donation d4 = new Donation(
                "DON-2026-0004",
                "usr-cit-003",
                "Suresh Rao",
                "SNG-001",
                "Seva Bharathi Community Welfare Trust",
                Donation.TYPE_GENERAL,
                null,
                6000.0,
                "Grocery Distribution sponsorship for rural families",
                "TXN-77889900",
                "PhonePe"
        );
        d4.setTransactionDate(System.currentTimeMillis() - 86400000L * 7);
        donations.add(d4);

        Donation d5 = new Donation(
                "DON-2026-0005",
                "usr-cit-004",
                "Pooja Hegde",
                "SNG-001",
                "Seva Bharathi Community Welfare Trust",
                Donation.TYPE_REQUEST_SPECIFIC,
                "REQ-103",
                4500.0,
                "School Fee Support for needy orphans",
                "TXN-11223344",
                "Paytm"
        );
        d5.setTransactionDate(System.currentTimeMillis() - 86400000L * 6);
        donations.add(d5);

        Donation d6 = new Donation(
                "DON-2026-0006",
                "usr-cit-005",
                "Dr. Raghavendra",
                "SNG-001",
                "Seva Bharathi Community Welfare Trust",
                Donation.TYPE_GENERAL,
                null,
                10000.0,
                "Elderly Care and Medicine Purchase fund",
                "TXN-55667788",
                "NEFT"
        );
        d6.setTransactionDate(System.currentTimeMillis() - 86400000L * 5);
        donations.add(d6);

        // Additional duplicate/similar donations for testing filtering
        Donation d7 = new Donation(
                "DON-2026-0007",
                "usr-cit-006",
                "Kiran Nayak",
                "SNG-001",
                "Seva Bharathi Community Welfare Trust",
                Donation.TYPE_REQUEST_SPECIFIC,
                "REQ-108",
                3000.0,
                "Education Fees donation for college student",
                "TXN-99887766",
                "UPI"
        );
        d7.setTransactionDate(System.currentTimeMillis() - 86400000L * 4);
        donations.add(d7);

        Donation d8 = new Donation(
                "DON-2026-0008",
                "usr-cit-007",
                "Meenakshi Pai",
                "SNG-001",
                "Seva Bharathi Community Welfare Trust",
                Donation.TYPE_REQUEST_SPECIFIC,
                "REQ-109",
                5500.0,
                "Medical Treatment contribution for dialysis",
                "TXN-44332211",
                "Google Pay"
        );
        d8.setTransactionDate(System.currentTimeMillis() - 86400000L * 2);
        donations.add(d8);
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
