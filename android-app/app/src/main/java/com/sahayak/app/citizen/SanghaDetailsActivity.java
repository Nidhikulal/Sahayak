package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.R;
import com.sahayak.app.data.SanghaRepository;
import com.sahayak.app.models.Sangha;

public class SanghaDetailsActivity extends AppCompatActivity {

    private String sanghaId;
    private Sangha sangha;

    private TextView tvHeaderSanghaName, tvName, tvRegNo, tvAddress, tvContact, tvFullDescription, tvActivitiesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sangha_details);

        sanghaId = getIntent().getStringExtra("SANGHA_ID");
        if (sanghaId == null) sanghaId = "SNG-001";

        initViews();
        loadData();
    }

    private void initViews() {
        tvHeaderSanghaName = findViewById(R.id.tvHeaderSanghaName);
        tvName = findViewById(R.id.tvName);
        tvRegNo = findViewById(R.id.tvRegNo);
        tvAddress = findViewById(R.id.tvAddress);
        tvContact = findViewById(R.id.tvContact);
        tvFullDescription = findViewById(R.id.tvFullDescription);
        tvActivitiesList = findViewById(R.id.tvActivitiesList);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // General Donation
        findViewById(R.id.cardGeneralDonation).setOnClickListener(v -> {
            Intent intent = new Intent(SanghaDetailsActivity.this, DonationActivity.class);
            intent.putExtra("SANGHA_ID", sanghaId);
            intent.putExtra("CONTRIBUTION_TYPE", "GENERAL_DONATION");
            startActivity(intent);
        });

        // Request-Specific Sponsorship
        findViewById(R.id.cardRequestSponsorship).setOnClickListener(v -> {
            Intent intent = new Intent(SanghaDetailsActivity.this, DonationActivity.class);
            intent.putExtra("SANGHA_ID", sanghaId);
            intent.putExtra("CONTRIBUTION_TYPE", "REQUEST_SPECIFIC");
            intent.putExtra("REQUEST_ID", "REQ-2026-0002");
            startActivity(intent);
        });
    }

    private void loadData() {
        sangha = SanghaRepository.getInstance().getSanghaById(sanghaId);
        if (sangha == null) return;

        tvHeaderSanghaName.setText(sangha.getName());
        tvName.setText(sangha.getName());
        tvRegNo.setText("Registration No: " + sangha.getRegistrationNo());
        tvAddress.setText("Address: " + sangha.getFullAddress());
        tvContact.setText("Contact: " + sangha.getContactPhone() + " | " + sangha.getContactEmail());
        tvFullDescription.setText(sangha.getFullDescription());

        if (sangha.getAssistanceActivities() != null && !sangha.getAssistanceActivities().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String act : sangha.getAssistanceActivities()) {
                sb.append("• ").append(act).append("\n");
            }
            tvActivitiesList.setText(sb.toString().trim());
        } else {
            tvActivitiesList.setText("• Monthly Ration & Grocery Kits\n• Emergency Medical Aid\n• Student Education Grants");
        }
    }
}