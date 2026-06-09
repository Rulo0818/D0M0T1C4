package com.domotica.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.domotica.app.data.AuthRepository;
import com.domotica.app.data.DeviceRepository;


public class DashboardActivity extends AppCompatActivity {

    private TextView tvVinculacion, tvLedState, tvLastUpdate;
    private View ledIndicator;
    private SwitchMaterial switchLed;
    private ProgressBar progressBar;
    private AuthRepository authRepo;
    private DeviceRepository deviceRepo;
    private String currentDeviceId;
    private ValueEventListener deviceListener;
    private DatabaseReference deviceRef;
    private boolean isUpdatingFromFirebase = false;
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
        ledIndicator = findViewById(R.id.ledIndicator);
        switchLed = findViewById(R.id.switchLed);
        progressBar = findViewById(R.id.progressBar);

        switchLed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingFromFirebase) {
                setLed(isChecked ? 1 : 0);
            }
        });

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
                            getColor(R.color.success));
                    switchLed.setEnabled(true);
                    listenDeviceChanges(deviceId);
                } else {
                    tvVinculacion.setText("No vinculado a ningún dispositivo");
                    tvVinculacion.setTextColor(
                            getColor(R.color.error));
                    switchLed.setEnabled(false);
                    tvLedState.setText("LED: ---");
                    tvLastUpdate.setText("");
                }
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                tvVinculacion.setText("Error al verificar vinculación");
                switchLed.setEnabled(false);
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
                    isUpdatingFromFirebase = true;
                    switchLed.setChecked(isOn);
                    isUpdatingFromFirebase = false;

                    tvLedState.setText(isOn ? "LED: ENCENDIDO" : "LED: APAGADO");
                    tvLedState.setTextColor(getColor(
                            isOn ? R.color.led_on : R.color.text_tertiary));

                    int ledColor = getColor(isOn ? R.color.led_on : R.color.text_tertiary);
                    ViewCompat.setBackgroundTintList(ledIndicator,
                            ColorStateList.valueOf(ledColor));

                    switchLed.setEnabled(true);
                } else {
                    isUpdatingFromFirebase = true;
                    switchLed.setChecked(false);
                    isUpdatingFromFirebase = false;

                    tvLedState.setText("LED: esperando ESP32...");
                    tvLedState.setTextColor(getColor(R.color.text_tertiary));

                    ViewCompat.setBackgroundTintList(ledIndicator,
                            ColorStateList.valueOf(getColor(R.color.text_tertiary)));
                }

                if (Boolean.TRUE.equals(online)) {
                    tvLastUpdate.setText("ESP32 en línea");
                    tvLastUpdate.setTextColor(getColor(R.color.success));
                } else {
                    tvLastUpdate.setText("ESP32 desconectado");
                    tvLastUpdate.setTextColor(getColor(R.color.text_tertiary));
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

        switchLed.setEnabled(false);

        deviceRepo.setLed(currentDeviceId, state, (success, error) -> {
            if (success) {
                Toast.makeText(DashboardActivity.this,
                        state == 1 ? "LED encendido" : "LED apagado",
                        Toast.LENGTH_SHORT).show();
            } else {
                switchLed.setEnabled(true);
                String msg = error != null ? error.getMessage() : "Error desconocido";
                tvLedState.setText("Error Firebase: " + msg);
                Toast.makeText(DashboardActivity.this,
                        "Error al escribir: " + msg,
                        Toast.LENGTH_LONG).show();
            }
        });

        tvLedState.setText(state == 1 ? "LED: ENCENDIDO" : "LED: APAGADO");

        int ledColor = getColor(state == 1 ? R.color.led_on : R.color.text_tertiary);
        ViewCompat.setBackgroundTintList(ledIndicator,
                ColorStateList.valueOf(ledColor));
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
