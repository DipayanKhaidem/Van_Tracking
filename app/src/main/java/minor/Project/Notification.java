package minor.Project;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class Notification extends AppCompatActivity {

    private RecyclerView notificationRecyclerView;
    private NotificationAdapter notificationAdapter;
    private static List<String> notificationList = new ArrayList<>();
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        notificationRecyclerView = findViewById(R.id.notificationRecyclerView);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Initialize RecyclerView with saved notifications
        notificationAdapter = new NotificationAdapter(notificationList);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationRecyclerView.setAdapter(notificationAdapter);

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.profile) {
                startActivity(new Intent(this, User_Profile.class));
                return true;
            } else if (item.getItemId() == R.id.home) {
                startActivity(new Intent(this, User_Dashboard.class));
                return true;
            } else if (item.getItemId() == R.id.settings) {
                startActivity(new Intent(this, Settings.class));
                return true;
            } else {
                return false;
            }
        });
    }

    public static void addNotification(String message) {
        notificationList.add(message);
    }
}
