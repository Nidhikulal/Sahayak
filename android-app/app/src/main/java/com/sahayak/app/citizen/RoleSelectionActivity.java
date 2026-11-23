package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.R;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Citizen Option -> Opens Citizen Registration Page
        View.OnClickListener openCitizenRegistration = v -> {
            startActivity(new Intent(RoleSelectionActivity.this, CitizenRegistrationActivity.class));
        };

        findViewById(R.id.cardCitizen).setOnClickListener(openCitizenRegistration);
        findViewById(R.id.btnContinueCitizen).setOnClickListener(openCitizenRegistration);

        // Sangha Option -> Directs to Sangha Registration
        View.OnClickListener openSanghaRegistration = v -> {
            startActivity(new Intent(RoleSelectionActivity.this, SanghaRegistrationActivity.class));
        };

        findViewById(R.id.cardSangha).setOnClickListener(openSanghaRegistration);
        findViewById(R.id.btnContinueSangha).setOnClickListener(openSanghaRegistration);
    }
}