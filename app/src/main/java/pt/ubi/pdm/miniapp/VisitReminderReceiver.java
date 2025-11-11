package pt.ubi.pdm.miniapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class VisitReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String cliente = intent.getStringExtra("cliente");
        String morada  = intent.getStringExtra("morada");

        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Visita de técnico";
        String text  = "Cliente: " + (cliente == null ? "—" : cliente)
                + " • Morada: " + (morada == null ? "—" : morada);

        NotificationCompat.Builder n = new NotificationCompat.Builder(context, MyApp.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), n.build());

        context.stopService(new Intent(context, VisitSchedulerService.class));
    }
}
