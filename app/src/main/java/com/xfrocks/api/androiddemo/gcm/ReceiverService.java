package com.xfrocks.api.androiddemo.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.xfrocks.api.androiddemo.BuildConfig;
import com.xfrocks.api.androiddemo.MainActivity;
import com.xfrocks.api.androiddemo.R;

public class ReceiverService extends GcmListenerService {

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String notificationId = data.getString("notification_id");
        String message = data.getString("notification");
        sendNotification(notificationId, message);
    }

    private void sendNotification(String notificationId, String message) {
        if (BuildConfig.DEBUG) {
            Log.i(ReceiverService.class.getSimpleName(), String.format("notification #%s: %s", notificationId, message));
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_URL, "notifications/content?notification_id=" + notificationId);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }

}
