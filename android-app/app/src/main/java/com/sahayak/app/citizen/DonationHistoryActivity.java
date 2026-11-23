package com.sahayak.app.citizen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sahayak.app.R;
import com.sahayak.app.data.DonationRepository;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.Donation;
import com.sahayak.app.models.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DonationHistoryActivity extends AppCompatActivity {

    private RecyclerView rvDonations;
    private TextView tvTotalDonated;
    private View layoutEmptyDonations;

    private DonationRepository donationRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation_history);

        donationRepository = DonationRepository.getInstance();
        sessionManager = new SessionManager(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvTotalDonated = findViewById(R.id.tvTotalDonated);
        rvDonations = findViewById(R.id.rvDonations);
        layoutEmptyDonations = findViewById(R.id.layoutEmptyDonations);

        rvDonations.setLayoutManager(new LinearLayoutManager(this));

        loadDonations();
    }

    private void loadDonations() {
        User user = sessionManager.getCurrentUser();
        String cId = user != null ? user.getUserId() : "usr-cit-001";

        List<Donation> userDonations = donationRepository.getDonationsByCitizen(cId);
        double total = donationRepository.getTotalDonatedByCitizen(cId);

        tvTotalDonated.setText("₹" + String.format(Locale.US, "%,.0f", total));

        if (userDonations.isEmpty()) {
            layoutEmptyDonations.setVisibility(View.VISIBLE);
            rvDonations.setVisibility(View.GONE);
        } else {
            layoutEmptyDonations.setVisibility(View.GONE);
            rvDonations.setVisibility(View.VISIBLE);
            DonationAdapter adapter = new DonationAdapter(userDonations);
            rvDonations.setAdapter(adapter);
        }
    }

    private class DonationAdapter extends RecyclerView.Adapter<DonationAdapter.ViewHolder> {
        private final List<Donation> list;

        DonationAdapter(List<Donation> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_donation_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Donation d = list.get(position);

            holder.tvDonationId.setText(d.getDonationId());
            holder.tvAmount.setText("₹" + String.format(Locale.US, "%,.0f", d.getAmount()));
            holder.tvSanghaName.setText(d.getSanghaName());

            String typeStr = "REQUEST_SPECIFIC".equals(d.getContributionType()) ? "Request Sponsorship" : "General Welfare";
            holder.tvTypeAndPurpose.setText(typeStr + " • " + d.getPurpose());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            holder.tvTxnDate.setText("Txn Ref: " + d.getTransactionRef() + " • " + sdf.format(new Date(d.getTransactionDate())));

            holder.cardDonation.setOnClickListener(v -> showReceiptDialog(d));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View cardDonation;
            TextView tvDonationId, tvAmount, tvSanghaName, tvTypeAndPurpose, tvTxnDate;

            ViewHolder(View itemView) {
                super(itemView);
                cardDonation = itemView.findViewById(R.id.cardDonation);
                tvDonationId = itemView.findViewById(R.id.tvDonationId);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvSanghaName = itemView.findViewById(R.id.tvSanghaName);
                tvTypeAndPurpose = itemView.findViewById(R.id.tvTypeAndPurpose);
                tvTxnDate = itemView.findViewById(R.id.tvTxnDate);
            }
        }
    }

    private void showReceiptDialog(Donation donation) {
        View receiptView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_receipt, null);
        AlertDialog receiptDialog = new AlertDialog.Builder(this)
                .setView(receiptView)
                .create();

        TextView tvReceiptDonationId = receiptView.findViewById(R.id.tvReceiptDonationId);
        TextView tvReceiptAmount = receiptView.findViewById(R.id.tvReceiptAmount);
        TextView tvReceiptSanghaName = receiptView.findViewById(R.id.tvReceiptSanghaName);
        TextView tvReceiptTypePurpose = receiptView.findViewById(R.id.tvReceiptTypePurpose);
        TextView tvReceiptTxnRef = receiptView.findViewById(R.id.tvReceiptTxnRef);

        tvReceiptDonationId.setText(donation.getDonationId());
        tvReceiptAmount.setText("₹" + String.format(Locale.US, "%,.0f", donation.getAmount()));
        tvReceiptSanghaName.setText("To: " + donation.getSanghaName());
        tvReceiptTypePurpose.setText("Type: " + ("REQUEST_SPECIFIC".equals(donation.getContributionType()) ? "Request Sponsorship" : "General Welfare") + "\nNote: " + donation.getPurpose());
        tvReceiptTxnRef.setText("Txn Ref: " + donation.getTransactionRef() + " • " + donation.getPaymentMethod());

        receiptView.findViewById(R.id.btnDoneReceipt).setOnClickListener(v -> receiptDialog.dismiss());

        receiptDialog.show();
    }
}