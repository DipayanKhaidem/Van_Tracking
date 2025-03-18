package minor.Project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

public class Driver_login extends AppCompatActivity {

    private EditText driverUsername, driverPassword;
    private Button driverLogin, driverReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        driverUsername = findViewById(R.id.driverUsername);
        driverPassword = findViewById(R.id.driverPassword);
        driverLogin = findViewById(R.id.driverLogin);
        driverReport = findViewById(R.id.driverReport);

        driverLogin.setOnClickListener(v -> authenticateDriver());
        driverReport.setOnClickListener(v -> sendReport());
    }

    // ✅ Authenticate Driver
    private void authenticateDriver() {
        String username = driverUsername.getText().toString().trim();
        String password = driverPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Authenticate driver using Back4App
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Driver");
        query.whereEqualTo("username", username);
        query.whereEqualTo("password", password);

        query.findInBackground((drivers, e) -> {
            if (e == null) {
                if (drivers != null && drivers.size() > 0) {
                    // Successful login
                    ParseObject driver = drivers.get(0);
                    String driverId = driver.getString("driverId");
                    Log.d("Login", "Driver logged in: " + driverId);

                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();

                    // ✅ Pass driver data to next activity
                    Intent intent = new Intent(Driver_login.this, DriverDashboard.class);
                    intent.putExtra("driverId", driverId);
                    intent.putExtra("driverName", username);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                    Log.e("Login", "Invalid credentials");
                }
            } else {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("Login", "Query error: " + e.getMessage());
            }
        });
    }

    // ✅ Send Report to Back4App
    private void sendReport() {
        String username = driverUsername.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter your username", Toast.LENGTH_SHORT).show();
            return;
        }

        ParseObject report = new ParseObject("Report");
        report.put("driverUsername", username);
        report.put("issueDescription", "Driver is facing issues logging in");
        report.put("status", "Pending");

        report.saveInBackground(e -> {
            if (e == null) {
                Toast.makeText(this, "Report sent to admin", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to send report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
