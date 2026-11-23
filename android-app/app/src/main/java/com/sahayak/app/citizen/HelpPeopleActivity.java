package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.R;

public class HelpPeopleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_people);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnReportSomeone).setOnClickListener(v -> {
            startActivity(new Intent(HelpPeopleActivity.this, ReportBeneficiaryActivity.class));
        });

        findViewById(R.id.btnViewMyRequests).setOnClickListener(v -> {
            startActivity(new Intent(HelpPeopleActivity.this, MyRequestsActivity.class));
        });
    }
}