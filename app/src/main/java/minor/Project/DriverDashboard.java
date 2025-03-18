package minor.Project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseException;
import com.parse.ParseObject;

public class DriverDashboard extends AppCompatActivity {

    private Button wetButton, dryButton;
    private String driverId;
    private String driverName; // This will now contain the actual driver name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        // Retrieve the driverId and driverName passed from the login activity
        Intent intent = getIntent();
        driverId = intent.getStringExtra("driverId");
        driverName = intent.getStringExtra("driverName");

        wetButton = findViewById(R.id.wetButton);
        dryButton = findViewById(R.id.dryWaste);

        wetButton.setOnClickListener(v -> saveWasteType("Wet Waste"));
        dryButton.setOnClickListener(v -> saveWasteType("Dry Waste"));
    }

    private void saveWasteType(String wasteType) {
        if (driverId != null && driverName != null) {
            ParseObject waste = new ParseObject("WasteType");
            waste.put("type", wasteType);
            waste.put("driverId", driverId);
            // Use the actual driver name instead of the username
            waste.put("driverName", driverName);

            waste.saveInBackground(e -> {
                if (e == null) {
                    Toast.makeText(DriverDashboard.this, "Waste Type Saved", Toast.LENGTH_SHORT).show();
                    // Go to the tracking screen
                    Intent intent = new Intent(DriverDashboard.this, startScreen.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(DriverDashboard.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(DriverDashboard.this, "Driver information missing", Toast.LENGTH_SHORT).show();
        }
    }
}
