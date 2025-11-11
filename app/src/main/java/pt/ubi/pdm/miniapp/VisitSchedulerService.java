package pt.ubi.pdm.miniapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

public class VisitSchedulerService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long whenMillis = intent.getLongExtra("whenMillis", -1);
        String cliente  = intent.getStringExtra("cliente");
        String morada   = intent.getStringExtra("morada");
        boolean demoModoSimples = intent.getBooleanExtra("demoModoSimples", false);

        if (whenMillis <= 0) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // 1) Intent + extras + URI única (permite várias visitas)
        Intent fire = new Intent(this, VisitReminderReceiver.class)
                .putExtra("cliente", cliente)
                .putExtra("morada",  morada);
        fire.setData(Uri.parse("miniapp://visita?when=" + whenMillis));

        // 2) PendingIntent único por visita
        int requestCode = (int) (whenMillis & 0x7fffffff);
        PendingIntent pi = PendingIntent.getBroadcast(
                this, requestCode, fire, PendingIntent.FLAG_IMMUTABLE
        );

        // 3) AlarmManager
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (demoModoSimples) {
            // DEMO: pode atrasar em Doze
            am.set(AlarmManager.RTC_WAKEUP, whenMillis, pi);
            Toast.makeText(this, "Demo: agendado com set() (pode atrasar em Doze)", Toast.LENGTH_SHORT).show();
        } else {
            // Produção: exato
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, whenMillis, pi);
                    Toast.makeText(this, "Sem permissão de alarme exato (S+): usado set() (pode atrasar).", Toast.LENGTH_LONG).show();
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, whenMillis, pi);
            }
        }

        // 4) Termina (não é preciso foreground para um agendamento curto)
        stopSelf();
        return START_NOT_STICKY;
    }

    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}
