package minor.Project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class User_Dashboard extends AppCompatActivity {

    private MapView mapView;
    private SearchView searchView;
    private TextView etaTextView, distanceTextView;
    private BottomNavigationView bottomNavigationView;
    private Marker userMarker,driverMarker;
    private ImageButton myLocationButton;
    private GeoPoint userLocation,driverLocation;



    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "UserDashboard";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setTileDownloadThreads((short) 4);

        setContentView(R.layout.activity_user_dashboard);


        mapView = findViewById(R.id.map);
        searchView = findViewById(R.id.searchView);
        etaTextView = findViewById(R.id.etaTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        myLocationButton = findViewById(R.id.myLocationButton);
        myLocationButton.setOnClickListener(v -> {
            if (userLocation != null) {
                centerOnUserLocation(userLocation);
                if (driverLocation != null) {
                    showDriverLocation(driverLocation.getLatitude(), driverLocation.getLongitude());
                }
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            }
        });
        setupMap();
        setupSearch();
        setupBottomNavigation();
        checkLocationPermission();
    }
    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18.0);
        mapView.setBuiltInZoomControls(true);
    }
    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }
    private void searchLocation(String query) {
        new Thread(() -> {
            try {
                String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + query;
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONArray jsonArray = new JSONArray(result.toString());
                if (jsonArray.length() > 0) {
                    JSONObject location = jsonArray.getJSONObject(0);
                    double lat = location.getDouble("lat");
                    double lon = location.getDouble("lon");

                    runOnUiThread(() -> updateUserMarker(new GeoPoint(lat, lon)));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching location: ", e);
            }
        }).start();
    }
    private void updateUserMarker(GeoPoint geoPoint) {
        if (userMarker != null) {
            mapView.getOverlays().remove(userMarker);
        }
        userMarker = new Marker(mapView);
        userMarker.setPosition(geoPoint);
        userMarker.setTitle("Your Location");
        mapView.getOverlays().add(userMarker);
        mapView.getController().setCenter(geoPoint);
        mapView.invalidate();
    }
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchUserLocationFromBack4App();
            fetchDriverLocationFromBack4App();
        }
    }
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocationFromBack4App();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void fetchUserLocationFromBack4App() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("_User");
        String currentUsername = ParseUser.getCurrentUser().getUsername();
        query.whereEqualTo("username", currentUsername);

        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject user, ParseException e) {
                if (e == null) {
                    double lat = user.getDouble("latitude");
                    double lon = user.getDouble("longitude");
                    Log.d(TAG, "Fetched location: lat=" + lat + ", lon=" + lon);
                    showUserLocation(lat, lon);
                } else {
                    Log.e(TAG, "Failed to fetch location", e);
                }
            }
        });
    }
    private void showUserLocation(double lat, double lon) {
        userLocation = new GeoPoint(lat, lon);

        if (userMarker != null) {
            mapView.getOverlays().remove(userMarker);
        }

        userMarker = new Marker(mapView);
        userMarker.setPosition(userLocation);
        userMarker.setTitle("Resident Location");

        mapView.getOverlays().add(userMarker);
        centerOnUserLocation(userLocation);

        drawGeofence(userLocation,200);
    }
    private void centerOnUserLocation(GeoPoint location) {
        if (userMarker != null) {
            mapView.getOverlays().remove(userMarker);
        }
        userMarker = new Marker(mapView);
        userMarker.setPosition(location);
        userMarker.setTitle("Resident Location");

        mapView.getOverlays().add(userMarker);

        mapView.getController().setZoom(18.0);
        mapView.getController().setCenter(location);
        mapView.invalidate();
    }
    private void fetchDriverLocationFromBack4App() {
        double fixedLat = 12.9254;
        double fixedLon = 77.4993;
        Log.d(TAG, "Using fixed driver location: lat=" + fixedLat + ", lon=" + fixedLon);
        showDriverLocation(fixedLat, fixedLon);
        if (userLocation != null) {
            fetchRoute(new GeoPoint(fixedLat, fixedLon), userLocation);
        }

//        ParseQuery<ParseObject> query = ParseQuery.getQuery("DriverLocation");
//
//        query.getFirstInBackground(new GetCallback<ParseObject>() {
//            @Override
//            public void done(ParseObject driver, ParseException e) {
//                if (e == null) {
//                    double lat = driver.getDouble("latitude");
//                    double lon = driver.getDouble("longitude");
//
//                    Log.d(TAG, "Fetched driver location: lat=" + lat + ", lon=" + lon);
//
//
//                    showDriverLocation(lat, lon);
//                } else {
//                    Log.e(TAG, "Failed to fetch driver location", e);
//                }
//            }
//        });
    }
    private void showDriverLocation(double lat, double lon) {
//        driverLocation = new GeoPoint(lat, lon);
        double fixedLat = 12.9149;
        double fixedLon = 77.5206;

        driverLocation = new GeoPoint(fixedLat, fixedLon);
        if (driverMarker != null) {
            mapView.getOverlays().remove(driverMarker);
        }
        driverMarker = new Marker(mapView);
        driverMarker.setPosition(driverLocation);
        driverMarker.setTitle("Driver Location");

        Bitmap originalBitmap= BitmapFactory.decodeResource(getResources(),R.drawable.vehicle);
        Bitmap smallMarker=Bitmap.createScaledBitmap(originalBitmap,150,150,false);
        driverMarker.setIcon(new BitmapDrawable(getResources(),smallMarker));
//        driverMarker.setIcon(ContextCompat.getDrawable(this,R.drawable.location));
        mapView.getOverlays().add(driverMarker);
//      mapView.getController().setCenter(driverLocation);
        if (userLocation != null) {
            calculateDistanceAndETA(userLocation, driverLocation);
//            checkGeofence(driverLocation, userLocation, 200);
            BoundingBox boundingBox =BoundingBox.fromGeoPoints(
                    new ArrayList<>(Arrays.asList(userLocation,driverLocation))
            );
            mapView.zoomToBoundingBox(boundingBox,true);
        }

        mapView.invalidate();
    }
    private void calculateDistanceAndETA(GeoPoint userLoc, GeoPoint driverLoc) {
        double distance = calculateDistance(userLoc.getLatitude(), userLoc.getLongitude(),
                driverLoc.getLatitude(), driverLoc.getLongitude());

        double speed = 30.0;
        double eta = distance / speed * 60;

        distanceTextView.setText(String.format("Distance: %.2f km", distance));
        etaTextView.setText(String.format("ETA: %.0f min", eta));
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.profile) {
                startActivity(new Intent(this, User_Profile.class));
                return true;
            } else if (item.getItemId() == R.id.home) {
                Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (item.getItemId() == R.id.settings) {
                startActivity(new Intent(this, Settings.class));
                return true;
            } else {
                return false;
            }
        });
    }

    private void drawGeofence(GeoPoint centerPoint, double radiusInMeters) {
        Polygon geofence = new Polygon(mapView);

        ArrayList<GeoPoint> circlePoints = new ArrayList<>();
        int numPoints = 60; // More points = smoother circle

        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double latitude = centerPoint.getLatitude() + (radiusInMeters / 111320f) * Math.cos(angle);
            double longitude = centerPoint.getLongitude() + (radiusInMeters / (111320f * Math.cos(Math.toRadians(centerPoint.getLatitude())))) * Math.sin(angle);
            circlePoints.add(new GeoPoint(latitude, longitude));
        }

        geofence.setPoints(circlePoints);
        geofence.setFillColor(0x44FF0000);
        geofence.setStrokeColor(0x88FF0000);
        geofence.setStrokeWidth(4.0f);

        mapView.getOverlays().add(geofence);
        mapView.invalidate();
    }
    private void fetchRoute(GeoPoint start, GeoPoint end) {
        new Thread(() -> {
            try {
                String apiKey = "5b3ce3597851110001cf6248ed6282c7433b4515aa8a42e9551e3d29";
                String url = "https://api.openrouteservice.org/v2/directions/driving-car?"
                        + "api_key=" + apiKey
                        + "&start=" + start.getLongitude() + "," + start.getLatitude()
                        + "&end=" + end.getLongitude() + "," + end.getLatitude();

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray coordinates = jsonResponse.getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                ArrayList<GeoPoint> routePoints = new ArrayList<>();
                for (int i = 0; i < coordinates.length(); i++) {
                    JSONArray point = coordinates.getJSONArray(i);
                    double lon = point.getDouble(0);
                    double lat = point.getDouble(1);
                    routePoints.add(new GeoPoint(lat, lon));
                }

                runOnUiThread(() -> drawRoute(routePoints));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching route: ", e);
            }
        }).start();
    }
    private void drawRoute(ArrayList<GeoPoint> routePoints) {
        if (routePoints == null || routePoints.isEmpty()) {
            Log.e("drawRoute", "Route points are empty!");
            return;
        }

        runOnUiThread(() -> {
            clearOldRoute(); // Remove old routes before adding a new one

            Polyline routeOverlay = new Polyline();
            routeOverlay.setPoints(routePoints);

            // ✅ Use a custom Paint object
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);  // Change to Color.RED to check visibility
            paint.setStrokeWidth(8.0f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);

            routeOverlay.getOutlinePaint().setColor(Color.BLUE);
            routeOverlay.getOutlinePaint().setStrokeWidth(8.0f);
            routeOverlay.getOutlinePaint().setStyle(Paint.Style.STROKE);
            routeOverlay.getOutlinePaint().setAntiAlias(true);


            // ✅ Add overlay correctly
            mapView.getOverlayManager().add(routeOverlay);
            mapView.invalidate(); // Refresh map
        });
    }

    // ✅ Function to clear old routes
    private void clearOldRoute() {
        List<Overlay> overlays = mapView.getOverlays();
        for (int i = overlays.size() - 1; i >= 0; i--) {
            if (overlays.get(i) instanceof Polyline) {
                overlays.remove(i);
            }
        }
    }



//    private void checkGeofence(GeoPoint driverLoc, GeoPoint userLoc, double radiusInMeters) {
//        double distance = calculateDistance(userLoc.getLatitude(), userLoc.geti Longitude(),
//                driverLoc.getLatitude(), driverLoc.getLongitude());
//
//        if (distance <= (radiusInMeters / 1000.0)) {
//            sendGeofenceNotification();
//        }
//    }
//    private void sendGeofenceNotification() {
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "geofence_channel")
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle("Garbage Van Nearby")
//                .setContentText("The garbage van has entered within 200m area.")
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(true);
//
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        notificationManager.notify(1, builder.build());
//    }


}
