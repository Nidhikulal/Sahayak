package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.sahayak.app.R;
import com.sahayak.app.sangha.SanghaLoginActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Citizen Option → Citizen Login (with Register Here link for new citizens)
        View.OnClickListener openCitizenLogin = v ->
                startActivity(new Intent(RoleSelectionActivity.this, CitizenLoginActivity.class));
        findViewById(R.id.cardCitizen).setOnClickListener(openCitizenLogin);
        findViewById(R.id.btnContinueCitizen).setOnClickListener(openCitizenLogin);

        // Sangha Option → Sangha Login (or Register with invitation username)
        View.OnClickListener openSanghaLogin = v ->
                startActivity(new Intent(RoleSelectionActivity.this, SanghaLoginActivity.class));
        findViewById(R.id.cardSangha).setOnClickListener(openSanghaLogin);
        findViewById(R.id.btnContinueSangha).setOnClickListener(openSanghaLogin);
    }
}
