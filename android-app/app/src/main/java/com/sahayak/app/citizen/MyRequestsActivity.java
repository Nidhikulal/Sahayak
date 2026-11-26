package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import com.sahayak.app.R;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MyRequestsActivity extends AppCompatActivity {

    private RecyclerView rvRequests;
    private View layoutEmpty;

    private List<AssistanceRequest> allUserRequests = new ArrayList<>();
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvRequests = findViewById(R.id.rvRequests);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        ChipGroup chipGroupFilter = findViewById(R.id.chipGroupFilter);
        //noinspection deprecation
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipVerification) {
                currentFilter = RequestStatus.UNDER_VERIFICATION;
            } else if (checkedId == R.id.chipVerified) {
                currentFilter = RequestStatus.VERIFIED;
            } else if (checkedId == R.id.chipCompleted) {
                currentFilter = RequestStatus.COMPLETED;
            } else {
                currentFilter = "ALL";
            }
            filterAndDisplayRequests();
        });

        loadRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    private void loadRequests() {
        SessionManager sessionManager = new SessionManager(this);
        User user = sessionManager.getCurrentUser();
        String cId = user != null ? user.getUserId() : "usr-cit-001";

        allUserRequests = MockRequestRepository.getMine(cId);
        filterAndDisplayRequests();
    }

    private void filterAndDisplayRequests() {
        List<AssistanceRequest> filtered = new ArrayList<>();
        for (AssistanceRequest r : allUserRequests) {
            if ("ALL".equals(currentFilter)) {
                filtered.add(r);
            } else if (RequestStatus.VERIFIED.equals(currentFilter)
                    && (RequestStatus.VERIFIED.equals(r.status) || RequestStatus.IN_PROGRESS.equals(r.status))) {
                // "Verified" chip shows both Verified and In-Progress requests
                filtered.add(r);
            } else if (Objects.equals(currentFilter, r.status)) {
                filtered.add(r);
            }
        }

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvRequests.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvRequests.setVisibility(View.VISIBLE);
            rvRequests.setAdapter(new RequestAdapter(filtered));
        }
    }

    private class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {
        private final List<AssistanceRequest> list;

        RequestAdapter(List<AssistanceRequest> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assistance_request, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AssistanceRequest r = list.get(position);

            holder.tvRequestId.setText(r.requestId);
            holder.tvBeneficiaryName.setText(r.beneficiaryName);
            holder.tvCategory.setText(r.assistanceType + " \u2022 " + Objects.requireNonNullElse(r.locality, "Local"));

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            holder.tvDate.setText(sdf.format(new Date(r.createdAt)));

            String statusLabel = RequestStatus.friendlyLabel(r.status);
            holder.tvStatusBadge.setText(statusLabel.toUpperCase(Locale.US));

            int badgeColor;
            if (RequestStatus.UNDER_VERIFICATION.equals(r.status)) {
                badgeColor = ContextCompat.getColor(MyRequestsActivity.this, R.color.status_verification);
            } else if (RequestStatus.VERIFIED.equals(r.status)) {
                badgeColor = ContextCompat.getColor(MyRequestsActivity.this, R.color.status_verified);
            } else if (RequestStatus.IN_PROGRESS.equals(r.status)) {
                badgeColor = ContextCompat.getColor(MyRequestsActivity.this, R.color.status_in_progress);
            } else if (RequestStatus.COMPLETED.equals(r.status)) {
                badgeColor = ContextCompat.getColor(MyRequestsActivity.this, R.color.status_completed);
            } else if (RequestStatus.REJECTED.equals(r.status)) {
                badgeColor = ContextCompat.getColor(MyRequestsActivity.this, R.color.status_rejected);
            } else {
                badgeColor = ContextCompat.getColor(MyRequestsActivity.this, R.color.status_submitted);
            }
            holder.tvStatusBadge.setBackgroundColor(badgeColor);

            holder.tvAssignedSangha.setVisibility(View.VISIBLE);
            if (r.assignedSanghaName != null && !r.assignedSanghaName.isEmpty()) {
                holder.tvAssignedSangha.setText("Assigned Sangha: " + r.assignedSanghaName);
            } else {
                holder.tvAssignedSangha.setText("Awaiting Sangha Assignment");
            }

            holder.cardRequest.setOnClickListener(v -> {
                Intent intent = new Intent(MyRequestsActivity.this, RequestDetailsActivity.class);
                intent.putExtra("REQUEST_ID", r.requestId);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View cardRequest;
            TextView tvRequestId, tvStatusBadge, tvBeneficiaryName, tvCategory, tvDate, tvAssignedSangha;

            ViewHolder(View itemView) {
                super(itemView);
                cardRequest = itemView.findViewById(R.id.cardRequest);
                tvRequestId = itemView.findViewById(R.id.tvRequestId);
                tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
                tvBeneficiaryName = itemView.findViewById(R.id.tvBeneficiaryName);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvAssignedSangha = itemView.findViewById(R.id.tvAssignedSangha);
            }
        }
    }
}
