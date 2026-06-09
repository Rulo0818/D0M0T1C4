package com.domotica.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.domotica.app.data.AuthRepository;
import com.domotica.app.data.DeviceRepository;


public class DashboardActivity extends AppCompatActivity {

    private TextView tvVinculacion, tvLedState, tvLastUpdate;
    private Button btnOn, btnOff;
    private ProgressBar progressBar;
    private AuthRepository authRepo;
    private DeviceRepository deviceRepo;
    private String currentDeviceId;
    private ValueEventListener deviceListener;
    private DatabaseReference deviceRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        authRepo = new AuthRepository();
        deviceRepo = new DeviceRepository();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvVinculacion = findViewById(R.id.tvVinculacion);
        tvLedState = findViewById(R.id.tvLedState);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
        btnOn = findViewById(R.id.btnOn);
        btnOff = findViewById(R.id.btnOff);
        progressBar = findViewById(R.id.progressBar);

        btnOn.setOnClickListener(v -> setLed(1));
        btnOff.setOnClickListener(v -> setLed(0));

        checkBinding();
    }

    private void checkBinding() {
        FirebaseUser user = authRepo.getCurrentUser();
        if (user == null) {
            redirectToLogin();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        deviceRepo.getLinkedDeviceId(user.getUid(), new DeviceRepository.OnDeviceIdCallback() {
            @Override
            public void onResult(String deviceId) {
                progressBar.setVisibility(View.GONE);
                if (deviceId != null && !deviceId.isEmpty()) {
                    currentDeviceId = deviceId;
                    tvVinculacion.setText("Vinculado con: " + deviceId);
                    tvVinculacion.setTextColor(
                            getColor(android.R.color.holo_green_dark));
                    btnOn.setEnabled(true);
                    btnOff.setEnabled(true);
                    listenDeviceChanges(deviceId);
                } else {
                    tvVinculacion.setText("No vinculado a ningún dispositivo");
                    tvVinculacion.setTextColor(
                            getColor(android.R.color.holo_red_dark));
                    btnOn.setEnabled(false);
                    btnOff.setEnabled(false);
                    tvLedState.setText("LED: ---");
                    tvLastUpdate.setText("");
                }
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                tvVinculacion.setText("Error al verificar vinculación");
                btnOn.setEnabled(false);
                btnOff.setEnabled(false);
                tvLedState.setText("LED: ---");
                tvLastUpdate.setText("");
            }
        });
    }

    private void listenDeviceChanges(String deviceId) {
        deviceRef = deviceRepo.getDeviceRef(deviceId);
        deviceListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Integer led = snapshot.child("led").getValue(Integer.class);
                Boolean online = snapshot.child("online").getValue(Boolean.class);

                if (led != null) {
                    boolean isOn = led == 1;
                    tvLedState.setText(isOn ? "LED: ENCENDIDO" : "LED: APAGADO");
                    tvLedState.setTextColor(getColor(
                            isOn ? android.R.color.holo_green_dark
                                 : android.R.color.darker_gray));
                    btnOn.setEnabled(true);
                    btnOff.setEnabled(true);
                } else {
                    tvLedState.setText("LED: esperando ESP32...");
                    tvLedState.setTextColor(getColor(android.R.color.darker_gray));
                }

                if (Boolean.TRUE.equals(online)) {
                    tvLastUpdate.setText("ESP32 en línea");
                } else {
                    tvLastUpdate.setText("ESP32 desconectado");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(DashboardActivity.this,
                        "Error de sincronización: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };
        deviceRef.addValueEventListener(deviceListener);
    }

    private void setLed(int state) {
        if (currentDeviceId == null) {
            Toast.makeText(this, "Error: no hay dispositivo vinculado", Toast.LENGTH_SHORT).show();
            return;
        }

        btnOn.setEnabled(false);
        btnOff.setEnabled(false);

        deviceRepo.setLed(currentDeviceId, state, (success, error) -> {
            if (success) {
                Toast.makeText(DashboardActivity.this,
                        state == 1 ? "LED encendido" : "LED apagado",
                        Toast.LENGTH_SHORT).show();
            } else {
                btnOn.setEnabled(true);
                btnOff.setEnabled(true);
                String msg = error != null ? error.getMessage() : "Error desconocido";
                tvLedState.setText("Error Firebase: " + msg);
                Toast.makeText(DashboardActivity.this,
                        "Error al escribir: " + msg,
                        Toast.LENGTH_LONG).show();
            }
        });

        tvLedState.setText(state == 1 ? "LED: ENCENDIDO" : "LED: APAGADO");
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    if (deviceRef != null && deviceListener != null)
                        deviceRef.removeEventListener(deviceListener);
                    authRepo.logout();
                    redirectToLogin();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            confirmLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceRef != null && deviceListener != null)
            deviceRef.removeEventListener(deviceListener);
    }
}
