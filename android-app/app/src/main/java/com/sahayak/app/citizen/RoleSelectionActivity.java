package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.R;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ViewOnClickListener openCitizen = v -> {
            startActivity(new Intent(RoleSelectionActivity.this, CitizenRegistrationActivity.class));
        };

        findViewById(R.id.cardSelectCitizen).setOnClickListener(openCitizen);
        findViewById(R.id.btnRegisterCitizen).setOnClickListener(openCitizen);

        ViewOnClickListener openSangha = v -> {
            startActivity(new Intent(RoleSelectionActivity.this, SanghaRegistrationActivity.class));
        };

        findViewById(R.id.cardSelectSangha).setOnClickListener(openSangha);
        findViewById(R.id.btnRegisterSangha).setOnClickListener(openSangha);
    }

    private interface ViewOnClickListener extends android.view.View.OnClickListener {}
}