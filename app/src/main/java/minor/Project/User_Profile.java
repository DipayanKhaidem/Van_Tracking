package minor.Project;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseUser;
import minor.Project.databinding.ActivityUserProfileBinding;

public class User_Profile extends AppCompatActivity {
    private ActivityUserProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fetchUserData();

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                startActivity(new Intent(this, User_Dashboard.class));
            } else if (itemId == R.id.notification) {
                startActivity(new Intent(this, Notification.class));
            } else if (itemId == R.id.settings) {
                startActivity(new Intent(this, Settings.class));
            }
            return true;
        });
    }

    private void fetchUserData() {
        ParseUser user = ParseUser.getCurrentUser();
        if (user != null) {
            binding.usernameValue.setText(user.getUsername());
            binding.emailValue.setText(user.getEmail());
            binding.phoneValue.setText(user.getString("phone"));
            binding.addressValue.setText(user.getString("address"));
            binding.LatitudeValue.setText(String.valueOf(user.getDouble("latitude")));
            binding.longitudeValue.setText(String.valueOf(user.getDouble("longitude")));
        }
    }
}
