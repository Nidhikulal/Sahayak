package com.sahayak.app.data;

import com.sahayak.app.models.Sangha;
import com.sahayak.app.models.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SanghaRepository {
    private static SanghaRepository instance;
    private final List<Sangha> sanghaList = new ArrayList<>();

    private SanghaRepository() {
        // Approved Sangha 1
        Sangha s1 = new Sangha(
                "SNG-001",
                "Seva Bharathi Community Welfare Trust",
                "REG-SNG-2021-042",
                "Kadri",
                "Mangaluru",
                "Dakshina Kannada",
                "Kadri Park Road, Near Temple, Mangaluru - 575002",
                "Providing monthly grocery kits, medical relief, and educational scholarships to underprivileged families.",
                "Seva Bharathi is a registered non-profit community welfare organisation serving the Dakshina Kannada district. Our volunteers perform on-ground verification for reported beneficiaries and ensure transparent delivery of aid.",
                "+91 9812345678",
                "contact@sevabharathi-mgl.org",
                User.STATUS_APPROVED
        );
        s1.setAssistanceActivities(Arrays.asList(
                "Monthly Grocery Distribution",
                "School Fee & Book Sponsorship",
                "Free Health Checkup Camps",
                "Emergency Financial Support"
        ));
        s1.setRating(4.9);
        s1.setTotalBeneficiariesHelped(340);
        sanghaList.add(s1);

        // Approved Sangha 2
        Sangha s2 = new Sangha(
                "SNG-002",
                "Karuna Social Support & Care Sangha",
                "REG-SNG-2022-109",
                "Kodialbail",
                "Mangaluru",
                "Dakshina Kannada",
                "Main Street, Kodialbail, Mangaluru - 575003",
                "Dedicated to elder care support, rehabilitation of persons with disabilities, and disaster relief.",
                "Karuna Support Sangha mobilizes active local volunteers to visit families reported in distress. We work closely with municipal authorities and healthcare professionals.",
                "+91 9845012345",
                "support@karunasangha.org",
                User.STATUS_APPROVED
        );
        s2.setAssistanceActivities(Arrays.asList(
                "Support for Elderly & Bedridden",
                "Assistive Devices for Persons with Disabilities",
                "Medical Treatment Assistance"
        ));
        s2.setRating(4.8);
        s2.setTotalBeneficiariesHelped(215);
        sanghaList.add(s2);

        // Approved Sangha 3
        Sangha s3 = new Sangha(
                "SNG-003",
                "Asha Deepa Educational & Welfare Foundation",
                "REG-SNG-2023-015",
                "Surathkal",
                "Mangaluru",
                "Dakshina Kannada",
                "Surathkal Town Center, Mangaluru - 575014",
                "Empowering bright students from financially weak backgrounds and supporting single-mother households.",
                "Asha Deepa focuses on education equality, skill development, and urgent financial relief for families undergoing sudden medical crisis.",
                "+91 9731298765",
                "info@ashadeepa.org",
                User.STATUS_APPROVED
        );
        s3.setAssistanceActivities(Arrays.asList(
                "Higher Education Scholarships",
                "Single-Mother Family Support",
                "Nutritional Food Distribution"
        ));
        s3.setRating(4.7);
        s3.setTotalBeneficiariesHelped(180);
        sanghaList.add(s3);

        // Pending Sangha (For testing pending condition)
        Sangha s4 = new Sangha(
                "SNG-004",
                "Janata Seva Sangha",
                "REG-SNG-2025-089",
                "Bantwal",
                "Mangaluru",
                "Dakshina Kannada",
                "Bantwal Main Road - 574211",
                "Newly registered local welfare group awaiting administrative verification.",
                "Awaiting verification.",
                "+91 9900112233",
                "admin@janataseva.org",
                User.STATUS_PENDING
        );
        sanghaList.add(s4);
    }

    public static synchronized SanghaRepository getInstance() {
        if (instance == null) {
            instance = new SanghaRepository();
        }
        return instance;
    }

    public synchronized List<Sangha> getApprovedSanghas() {
        List<Sangha> approved = new ArrayList<>();
        for (Sangha s : sanghaList) {
            if (User.STATUS_APPROVED.equalsIgnoreCase(s.getApprovalStatus())) {
                approved.add(s);
            }
        }
        return approved;
    }

    public synchronized List<Sangha> getAllSanghas() {
        return new ArrayList<>(sanghaList);
    }

    public synchronized Sangha getSanghaById(String id) {
        if (id == null) return null;
        for (Sangha s : sanghaList) {
            if (s.getSanghaId().equalsIgnoreCase(id)) return s;
        }
        return null;
    }

    public synchronized void addSangha(Sangha sangha) {
        sanghaList.add(sangha);
    }
}
