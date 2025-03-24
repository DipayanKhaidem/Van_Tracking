package minor.Project;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.security.ProviderInstaller;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;

public class App extends Application {
    private static final String TAG = "AppInit";

    @Override
    public void onCreate() {
        super.onCreate();


        try {
            ProviderInstaller.installIfNeeded(this);
            Log.d(TAG, "SSL Provider installed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to install SSL Provider", e);
        }

        new Thread(() -> {
            try {
                Parse.initialize(new Parse.Configuration.Builder(this)
                        .applicationId("e5GtmnPWd72hvrwohVasca64Qx6pRdWkIUdlJx5a")
                        .clientKey("ozrEPdRzciRpjZIYfNYvwLhgmXqNHTzgQLkggwRg")
                        .server("https://parseapi.back4app.com/")
                        .enableLocalDataStore() // ✅ Enable local datastore for offline support
                        .build()
                );

                Log.d(TAG, "Back4App Initialized Successfully");


                try {
                    ParseInstallation installation = ParseInstallation.getCurrentInstallation();
                    installation.saveInBackground(e -> {
                        if (e == null) {
                            Log.d(TAG, "Installation saved: " + installation.getObjectId());
                        } else {
                            Log.e(TAG, "Failed to save installation", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error saving installation", e);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Back4App", e);
            }
        }).start();
    }
}
