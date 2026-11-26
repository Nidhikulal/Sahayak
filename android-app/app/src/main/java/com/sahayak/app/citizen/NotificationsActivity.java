package com.sahayak.app.citizen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sahayak.app.R;
import com.sahayak.app.data.NotificationRepository;
import com.sahayak.app.data.SessionManager;
import com.sahayak.app.models.NotificationItem;
import com.sahayak.app.models.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private View layoutEmptyNotif;

    private NotificationRepository notificationRepository;
    private SessionManager sessionManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notificationRepository = NotificationRepository.getInstance();
        sessionManager = new SessionManager(this);
        currentUser = sessionManager.getCurrentUser();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmptyNotif = findViewById(R.id.layoutEmptyNotif);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> {
            if (currentUser != null) {
                notificationRepository.markAllAsRead(currentUser.getUserId());
                Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
                loadNotifications();
            }
        });

        loadNotifications();
    }

    private void loadNotifications() {
        String cId = currentUser != null ? currentUser.getUserId() : "usr-cit-001";
        List<NotificationItem> list = notificationRepository.getNotifications(cId);

        if (list.isEmpty()) {
            layoutEmptyNotif.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            layoutEmptyNotif.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
            NotificationAdapter adapter = new NotificationAdapter(list);
            rvNotifications.setAdapter(adapter);
        }
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private final List<NotificationItem> list;

        NotificationAdapter(List<NotificationItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = list.get(position);

            holder.tvNotifTitle.setText(item.getTitle());
            holder.tvNotifMessage.setText(item.getMessage());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.US);
            holder.tvNotifTime.setText(sdf.format(new Date(item.getTimestamp())));

            if (item.isRead()) {
                holder.viewUnreadDot.setVisibility(View.GONE);
            } else {
                holder.viewUnreadDot.setVisibility(View.VISIBLE);
            }

            holder.cardNotification.setOnClickListener(v -> {
                item.setRead(true);
                holder.viewUnreadDot.setVisibility(View.GONE);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View cardNotification, viewUnreadDot;
            TextView tvNotifTitle, tvNotifTime, tvNotifMessage;

            ViewHolder(View itemView) {
                super(itemView);
                cardNotification = itemView.findViewById(R.id.cardNotification);
                viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
                tvNotifTitle = itemView.findViewById(R.id.tvNotifTitle);
                tvNotifTime = itemView.findViewById(R.id.tvNotifTime);
                tvNotifMessage = itemView.findViewById(R.id.tvNotifMessage);
            }
        }
    }
}
