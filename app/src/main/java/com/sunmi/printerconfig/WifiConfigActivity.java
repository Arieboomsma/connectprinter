package com.sunmi.printerconfig;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class WifiConfigActivity extends AppCompatActivity {
    private BluetoothDevice device;
    private TextView printerNameText;
    private EditText ssidInput;
    private EditText passwordInput;
    private Button configureButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private PrinterConfigHelper printerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_config);

        device = getIntent().getParcelableExtra("device");
        if (device == null) {
            Toast.makeText(this, "Error: No device selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        printerNameText = findViewById(R.id.printerNameText);
        ssidInput = findViewById(R.id.ssidInput);
        passwordInput = findViewById(R.id.passwordInput);
        configureButton = findViewById(R.id.configureButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);

        printerHelper = new PrinterConfigHelper(this);

        boolean hasPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }

        if (hasPermission) {
            String deviceName = device.getName();
            printerNameText.setText(getString(R.string.connected_to, deviceName != null ? deviceName : "Unknown"));
        }

        configureButton.setOnClickListener(v -> {
            String ssid = ssidInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (ssid.isEmpty()) {
                Toast.makeText(this, "Please enter Wi-Fi network name", Toast.LENGTH_SHORT).show();
                ssidInput.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter Wi-Fi password", Toast.LENGTH_SHORT).show();
                passwordInput.requestFocus();
                return;
            }

            configurePrinter(ssid, password);
        });
    }

    private void configurePrinter(String ssid, String password) {
        configureButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText(R.string.configuring);

        // Run configuration in background thread
        new Thread(() -> {
            try {
                boolean success = printerHelper.configurePrinterWifi(device, ssid, password);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    configureButton.setEnabled(true);

                    if (success) {
                        statusText.setText(R.string.success);
                        Toast.makeText(this, "Printer configured successfully!\nIt should connect to Wi-Fi shortly.", Toast.LENGTH_LONG).show();
                        // Return to main activity after success
                        finish();
                    } else {
                        statusText.setText(getString(R.string.error, "Configuration failed"));
                        Toast.makeText(this, "Configuration failed. Please try again.",
                            Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    configureButton.setEnabled(true);
                    statusText.setText(getString(R.string.error, e.getMessage()));
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
