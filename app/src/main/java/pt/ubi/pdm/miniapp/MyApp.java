package pt.ubi.pdm.miniapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class MyApp extends Application {
    public static final String CHANNEL_ID = "visita_tecnico_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Agendamentos de técnico",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setDescription("Lembretes para visitas técnicas agendadas");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }
}
