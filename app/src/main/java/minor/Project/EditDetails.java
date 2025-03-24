package minor.Project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.parse.ParseUser;

public class EditDetails extends AppCompatActivity {

    EditText userName, email, address, phone, latitude, longitude;
    Button editButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_details);

        // Initialize fields
        userName = findViewById(R.id.editTextUsername);
        email = findViewById(R.id.editTextEmail);
        address = findViewById(R.id.editTextAddress);
        phone = findViewById(R.id.editTextPhone);
        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);
        editButton = findViewById(R.id.btnEdit);

        // Load existing user data
        fetchCurrentUserData();

        // Save updated details when clicked
        editButton.setOnClickListener(v -> saveUserDetails());
    }

    // Fetch current user details from Back4App
    private void fetchCurrentUserData() {
        ParseUser currentUser = ParseUser.getCurrentUser();

        if (currentUser != null) {
            userName.setText(currentUser.getUsername());
            email.setText(currentUser.getEmail());
            phone.setText(currentUser.getString("phone"));
            address.setText(currentUser.getString("address"));

            // Fetch latitude and longitude if available
            if (currentUser.get("latitude") != null) {
                latitude.setText(String.valueOf(currentUser.getDouble("latitude")));
            }
            if (currentUser.get("longitude") != null) {
                longitude.setText(String.valueOf(currentUser.getDouble("longitude")));
            }
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // Save updated details to Back4App
    private void saveUserDetails() {
        ParseUser currentUser = ParseUser.getCurrentUser();

        if (currentUser != null) {
            if (!userName.getText().toString().isEmpty()) {
                currentUser.setUsername(userName.getText().toString());
            }
            if (!email.getText().toString().isEmpty()) {
                currentUser.setEmail(email.getText().toString());
            }
            if (!phone.getText().toString().isEmpty()) {
                currentUser.put("phone", phone.getText().toString());
            }
            if (!address.getText().toString().isEmpty()) {
                currentUser.put("address", address.getText().toString());
            }
            if (!latitude.getText().toString().isEmpty()) {
                try {
                    double lat = Double.parseDouble(latitude.getText().toString());
                    currentUser.put("latitude", lat);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid latitude format", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (!longitude.getText().toString().isEmpty()) {
                try {
                    double lon = Double.parseDouble(longitude.getText().toString());
                    currentUser.put("longitude", lon);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid longitude format", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Save to Back4App
            currentUser.saveInBackground(e -> {
                if (e == null) {
                    Toast.makeText(this, "Details updated successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after saving
                } else {
                    Toast.makeText(this, "Failed to update details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
        }
    }
}
