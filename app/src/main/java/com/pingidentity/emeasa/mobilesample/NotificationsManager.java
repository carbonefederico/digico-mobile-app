package com.pingidentity.emeasa.mobilesample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class NotificationsManager {

    private final String NOTIFICATION_CHANNEL_ID = "sampleapp.channel";
    public static final int NOTIFICATION_ID_SAMPLE_APP = 1003;

    private Context context;
    public NotificationsManager(Context context){
        this.context = context;
        createNotificationChannel(context);
    }

    /*
     * Because you must create the notification channel before posting any notifications on Android 8.0
     * and higher, you should execute this code as soon as your app starts. It's safe to call this
     * repeatedly because creating an existing notification channel performs no operation.
     */
    private void createNotificationChannel(Context context) {
        /*
         * Create the NotificationChannel, but only on API 26+ because
         * the NotificationChannel class is new and not in the support library
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, Consts.CHANNEL_NAME, importance);
            channel.setDescription(Consts.CHANNEL_DESCRIPTION);
            /*
             * Register the channel with the system; you can't change the importance
             * or other notification behaviors after this
             */
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void buildAndSendNotification(Intent notificationIntent){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        builder.setSmallIcon(R.mipmap.ic_launcher);
        /*
         * show the notification over the lock screen
         */
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if(notificationIntent.hasExtra("title")) {
            builder.setContentTitle(notificationIntent.getStringExtra("title"));
        }
        if (notificationIntent.hasExtra("body")){
            builder.setContentText(notificationIntent.getStringExtra("body"));
        }

        Intent resultIntent = new Intent(context, ApprovalActivity.class);
        resultIntent.putExtras(notificationIntent.getExtras());

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(notificationIntent.getStringExtra("body"));
        bigText.setBigContentTitle(Consts.CHANNEL_NAME);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent)
                .setStyle(bigText)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL | Notification.FLAG_AUTO_CANCEL);

        Notification newMessageNotification = builder.build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1234, newMessageNotification);
    }


}


