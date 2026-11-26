package com.sahayak.app.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sahayak.app.R;
import com.sahayak.app.data.SanghaRepository;
import com.sahayak.app.models.Sangha;

import java.util.List;

public class SponsorDashboardActivity extends AppCompatActivity {

    private RecyclerView rvApprovedSanghas;
    private SanghaRepository sanghaRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sponsor_dashboard);

        sanghaRepository = SanghaRepository.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvApprovedSanghas = findViewById(R.id.rvApprovedSanghas);
        rvApprovedSanghas.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnViewDonationHistory).setOnClickListener(v -> {
            startActivity(new Intent(SponsorDashboardActivity.this, DonationHistoryActivity.class));
        });

        loadApprovedSanghas();
    }

    private void loadApprovedSanghas() {
        // Fetch only approved Sanghas as required
        List<Sangha> approvedSanghas = sanghaRepository.getApprovedSanghas();
        SanghaAdapter adapter = new SanghaAdapter(approvedSanghas);
        rvApprovedSanghas.setAdapter(adapter);
    }

    private class SanghaAdapter extends RecyclerView.Adapter<SanghaAdapter.ViewHolder> {
        private final List<Sangha> list;

        SanghaAdapter(List<Sangha> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sangha_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Sangha s = list.get(position);

            holder.tvSanghaName.setText(s.getName());
            holder.tvLocation.setText(s.getLocality() + ", " + s.getCity() + " • Reg: " + s.getRegistrationNo());
            holder.tvDescription.setText(s.getShortDescription());
            holder.tvContactPhone.setText("Contact: " + s.getContactPhone());

            View.OnClickListener openDetails = v -> {
                Intent intent = new Intent(SponsorDashboardActivity.this, SanghaDetailsActivity.class);
                intent.putExtra("SANGHA_ID", s.getSanghaId());
                startActivity(intent);
            };

            holder.cardSangha.setOnClickListener(openDetails);
            holder.btnViewDetails.setOnClickListener(openDetails);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View cardSangha, btnViewDetails;
            TextView tvSanghaName, tvLocation, tvDescription, tvContactPhone;

            ViewHolder(View itemView) {
                super(itemView);
                cardSangha = itemView.findViewById(R.id.cardSangha);
                btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
                tvSanghaName = itemView.findViewById(R.id.tvSanghaName);
                tvLocation = itemView.findViewById(R.id.tvLocation);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvContactPhone = itemView.findViewById(R.id.tvContactPhone);
            }
        }
    }
}
