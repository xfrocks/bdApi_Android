package com.xfrocks.api.androiddemo.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.google.gson.annotations.SerializedName;
import com.xfrocks.api.androiddemo.App;
import com.xfrocks.api.androiddemo.BuildConfig;
import com.xfrocks.api.androiddemo.MainActivity;
import com.xfrocks.api.androiddemo.R;

public class ReceiverService extends GcmListenerService {

    @Override
    public void onMessageReceived(String from, Bundle data) {
        if (BuildConfig.DEBUG) {
            Log.v(getClass().getSimpleName(), String.format("%s: %s", from, data));
        }

        String notificationId = data.getString("notification_id");
        String notification = data.getString("notification");
        if (!TextUtils.isEmpty(notificationId)
                && !TextUtils.isEmpty(notification)) {
            sendNotification(notificationId, notification);
        } else if (data.containsKey("message")) {
            NotificationMessage message = App.getGsonInstance().fromJson(data.getString("message"), NotificationMessage.class);
            if (message.conversationId > 0
                    && message.messageId > 0
                    && message.creatorUsername != null
                    && message.conversationTitle != null
                    && message.messageBody != null) {
                ChatOrNotifReceiver.broadcast(this, message.conversationId, message.messageId,
                        message.creatorUsername, message.conversationTitle, message.messageBody);
            }
        }
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

    static class NotificationMessage {
        @SerializedName("conversation_id")
        Integer conversationId;

        @SerializedName("message_id")
        Integer messageId;

        @SerializedName("creator_username")
        String creatorUsername;

        @SerializedName("title")
        String conversationTitle;

        @SerializedName("message")
        String messageBody;
    }
}
