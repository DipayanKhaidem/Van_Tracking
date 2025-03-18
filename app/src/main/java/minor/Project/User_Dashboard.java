package minor.Project;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.IOException;
import java.util.List;

public class User_Dashboard extends AppCompatActivity implements OnMapReadyCallback {

    private static final int FINE_PERMISSION_CODE = 1;
    private static final float GEOFENCE_RADIUS = 100.0f; // 100 meters

    private GoogleMap myMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GeofencingClient geofencingClient;
    private LatLng userLatLng;
    private LatLng driverLatLng;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        geofencingClient = LocationServices.getGeofencingClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupSearchView();
        setupBottomNavigation();// ✅ Fixed: Included setupBottomNavigation()

        handler = new Handler();
    }

    private void setupSearchView() {
        SearchView mapSearchView = findViewById(R.id.mapSearch);
        mapSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String location) {
                if (location != null) {
                    searchLocation(location);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.home) {
                Toast.makeText(this, "Home Selected", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.profile) {
                startActivity(new Intent(User_Dashboard.this, User_Profile.class));
            } else if (id == R.id.settings) {
                startActivity(new Intent(User_Dashboard.this, Settings.class));
            } else if (id == R.id.notification) {
                startActivity(new Intent(User_Dashboard.this, Notification.class));
            }
            return true;
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
        myMap.getUiSettings().setZoomControlsEnabled(true);
        myMap.getUiSettings().setZoomGesturesEnabled(true);

        requestUserLocation();

        // Auto-refresh driver location every 5 seconds
        handler.post(refreshLocationRunnable);
    }

    private void requestUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_PERMISSION_CODE);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(10000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build();

        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            if (myMap != null) {
                                myMap.addMarker(new MarkerOptions().position(userLatLng).title("My Location"));
                                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
                            }
                        } else {
                            Toast.makeText(User_Dashboard.this, "Failed to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                getMainLooper());
    }

    private void searchLocation(String location) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addressList = geocoder.getFromLocationName(location, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                myMap.addMarker(new MarkerOptions().position(latLng).title(location));
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchDriverLocation() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("DriverLocation");
        query.orderByDescending("createdAt");
        query.getFirstInBackground((object, e) -> {
            if (e == null && object != null) {
                double latitude = object.getDouble("latitude");
                double longitude = object.getDouble("longitude");

                driverLatLng = new LatLng(latitude, longitude);
                drawRoute();
                calculateDistanceAndETA();

                setupGeofence(driverLatLng);
            }
        });
    }

    private void drawRoute() {
        if (userLatLng != null && driverLatLng != null) {
            myMap.addPolyline(new PolylineOptions()
                    .add(userLatLng, driverLatLng)
                    .width(8)
                    .color(getResources().getColor(R.color.purple_500)));
        }
    }

    private void calculateDistanceAndETA() {
        if (userLatLng != null && driverLatLng != null) {
            float[] results = new float[1];
            Location.distanceBetween(userLatLng.latitude, userLatLng.longitude,
                    driverLatLng.latitude, driverLatLng.longitude, results);

            float distance = results[0] / 1000;
            float eta = (distance / 30) * 60;

            ((TextView) findViewById(R.id.etaTextView)).setText("ETA: " + String.format("%.2f", eta) + " mins");
            ((TextView) findViewById(R.id.distanceTextView)).setText("Distance: " + String.format("%.2f", distance) + " km");
        }
    }

    private void setupGeofence(LatLng latLng) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        Geofence geofence = new Geofence.Builder()
                .setRequestId("DRIVER_GEOFENCE")
                .setCircularRegion(latLng.latitude, latLng.longitude, GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(this, GeofenceBroadcastReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        geofencingClient.addGeofences(new GeofencingRequest.Builder()
                        .addGeofence(geofence)
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        .build(), pendingIntent)
                .addOnSuccessListener(unused -> Log.d("GEOFENCE", "Geofence added"))
                .addOnFailureListener(e -> Log.e("GEOFENCE", "Failed to add geofence", e));

        // ✅ Draw circle on map to show geofence
        if (myMap != null) {
            myMap.addCircle(new com.google.android.gms.maps.model.CircleOptions()
                    .center(latLng)
                    .radius(GEOFENCE_RADIUS) // Geofence radius
                    .strokeColor(ContextCompat.getColor(this, R.color.purple_500)) // Circle outline color
                    .fillColor(ContextCompat.getColor(this, R.color.purple_200)) // Circle fill color (with transparency)
                    .strokeWidth(4));
        }
    }



    private final Runnable refreshLocationRunnable = new Runnable() {
        @Override
        public void run() {
            fetchDriverLocation();
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(refreshLocationRunnable);
    }
}
