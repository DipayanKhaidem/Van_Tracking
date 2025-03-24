package minor.Project;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import java.util.Random;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.e(TAG, "Received null intent");
            return;
        }

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event is null");
            return;
        }

        if (geofencingEvent.hasError()) {
            int errorCode = geofencingEvent.getErrorCode();
            Log.e(TAG, "Geofencing event error: " + errorCode);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        String message;
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            message = "The garbage van is nearby!";
            Log.d(TAG, "Geofence transition: ENTER");
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            message = "The garbage van has moved away.";
            Log.d(TAG, "Geofence transition: EXIT");
        } else {
            Log.e(TAG, "Unknown geofence transition: " + geofenceTransition);
            return;
        }

        showNotification(context, "Garbage Van Alert", message);
        Notification.addNotification(message);
    }

    private void showNotification(Context context, String title, String message) {
        String channelId = "GEOFENCE_NOTIFICATION_CHANNEL";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Geofence Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify(new Random().nextInt(), builder.build()); // ✅ Unique ID for each notification
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for notifications: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed to show notification: " + e.getMessage());
        }
    }
}
