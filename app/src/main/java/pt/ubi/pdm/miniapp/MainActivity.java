package pt.ubi.pdm.miniapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.Calendar;

public class MainActivity extends ComponentActivity {

    private static final int REQ_POST_NOTIF = 2001;

    private EditText edtCliente, edtMorada;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private TextView txtInfo;
    private MaterialCheckBox chkDemoSimples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pedirPermNotificacoes();
        maybeAskExactAlarmOnce();

        edtCliente = findViewById(R.id.edtCliente);
        edtMorada  = findViewById(R.id.edtMorada);
        datePicker = findViewById(R.id.datePicker);
        timePicker = findViewById(R.id.timePicker);
        txtInfo    = findViewById(R.id.txtInfo);
        chkDemoSimples = findViewById(R.id.chkDemoSimples);

        timePicker.setIs24HourView(true);

        Button btnAgendar  = findViewById(R.id.btnAgendar);
        Button btnTeste10s = findViewById(R.id.btnTeste10s);

        btnAgendar.setOnClickListener(v -> agendarVisitaNormal());
        btnTeste10s.setOnClickListener(v -> agendarTeste10s());
    }

    private void agendarVisitaNormal() {
        Calendar c = Calendar.getInstance();
        int hour = getHour();
        int minute = getMinute();
        c.set(Calendar.YEAR, datePicker.getYear());
        c.set(Calendar.MONTH, datePicker.getMonth());
        c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        long when = c.getTimeInMillis();
        if (when <= System.currentTimeMillis()) {
            Toast.makeText(this, "Escolhe uma data/hora futura", Toast.LENGTH_SHORT).show();
            return;
        }

        String cliente = edtCliente.getText().toString().trim();
        String morada  = edtMorada.getText().toString().trim();
        boolean demo   = chkDemoSimples.isChecked();


        arrancarServicoAgendamento(when, cliente, morada, demo);

        String info = "Visita agendada para " + String.format(
                "%02d/%02d/%04d %02d:%02d",
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.YEAR),
                hour, minute
        );
        txtInfo.setText(info);
        Toast.makeText(this, "Agendado!", Toast.LENGTH_SHORT).show();
    }

    private void agendarTeste10s() {
        String cliente = edtCliente.getText().toString().trim();
        String morada  = edtMorada.getText().toString().trim();
        boolean demo   = chkDemoSimples.isChecked();

        long when = System.currentTimeMillis() + 10_000L; // 10 s
        arrancarServicoAgendamento(when, cliente, morada, demo);
        txtInfo.setText("Teste: notificação em ~10s");
    }

    private void arrancarServicoAgendamento(long whenMillis, String cliente, String morada, boolean demoModoSimples) {
        Intent svc = new Intent(this, VisitSchedulerService.class)
                .putExtra("whenMillis", whenMillis)
                .putExtra("cliente",    cliente)
                .putExtra("morada",     morada)
                .putExtra("demoModoSimples", demoModoSimples);

        // Como o serviço só agenda e termina, startService é suficiente (estamos em foreground)
        startService(svc);
    }

    private int getHour() {
        return Build.VERSION.SDK_INT >= 23 ? timePicker.getHour() : timePicker.getCurrentHour();
    }

    private int getMinute() {
        return Build.VERSION.SDK_INT >= 23 ? timePicker.getMinute() : timePicker.getCurrentMinute();
    }

    private void pedirPermNotificacoes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIF
                );
            }
        }
    }

    private boolean hasExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            return am.canScheduleExactAlarms();
        }
        return true;
    }

    private void maybeAskExactAlarmOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean alreadyAsked = prefs.getBoolean("asked_exact_alarm", false);

        if (!hasExactAlarmPermission() && !alreadyAsked) {
            new AlertDialog.Builder(this)
                    .setTitle("Permitir alarmes exatos?")
                    .setMessage("Para disparar exatamente à hora marcada, permite \"Alarmes e lembretes\". Podes usar a app sem isso (usamos um modo alternativo).")
                    .setPositiveButton("Abrir definições", (d, w) -> {
                        try {
                            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                        } catch (Exception ignored) { }
                        prefs.edit().putBoolean("asked_exact_alarm", true).apply();
                    })
                    .setNegativeButton("Agora não", (d, w) -> {
                        prefs.edit().putBoolean("asked_exact_alarm", true).apply();
                    })
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(requestCode, p, r);
        if (requestCode == REQ_POST_NOTIF &&
                (r.length == 0 || r[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Sem permissão: as notificações podem não aparecer", Toast.LENGTH_LONG).show();
        }
    }
}
