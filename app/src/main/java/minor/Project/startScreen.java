package minor.Project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

public class startScreen extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    private Button startButton, stopButton;
    private TextView timerTextView;
    private boolean isTracking = false;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private long startTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        timerTextView = findViewById(R.id.timerTextView);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        startButton.setOnClickListener(v -> startTracking());
        stopButton.setOnClickListener(v -> stopTracking());
    }

    private void startTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            return;
        }

        if (isTracking) {
            Toast.makeText(this, "Already tracking", Toast.LENGTH_SHORT).show();
            return;
        }

        isTracking = true;
        startTime = System.currentTimeMillis();

        // Timer logic
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                String time = String.format("%02d:%02d", minutes, seconds);
                timerTextView.setText(time);

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);

        // Location request setup
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000) // Fastest interval
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult != null) {
                    locationResult.getLocations().forEach(location -> {
                        Log.d("StartScreen", "Location: " + location.getLatitude() + ", " + location.getLongitude());

                        // ✅ Save to Back4App here
                        ParseUser currentUser = ParseUser.getCurrentUser();
                        if (currentUser != null) {
                            ParseObject locationData = new ParseObject("DriverLocation");
                            locationData.put("latitude", location.getLatitude());
                            locationData.put("longitude", location.getLongitude());
                            locationData.put("driverId", currentUser.getObjectId());

                            locationData.saveInBackground(e -> {
                                if (e != null) {
                                    Log.e("StartScreen", "Failed to save location: " + e.getMessage());
                                } else {
                                    Log.d("StartScreen", "Driver location saved successfully!");
                                }
                            });
                        } else {
                            Log.e("StartScreen", "Failed to get driverId - User not logged in");
                        }
                    });
                }
            }
        };


        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show();
    }

    private void stopTracking() {
        if (!isTracking) {
            Toast.makeText(this, "Not tracking", Toast.LENGTH_SHORT).show();
            return;
        }

        isTracking = false;
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);


        timerHandler.removeCallbacks(timerRunnable);

        Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
