package com.sunmi.printerconfig;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PrinterConfigHelper {
    private static final String TAG = "PrinterConfigHelper";
    // Standard SPP UUID for Bluetooth Serial Port Profile
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Context context;

    public PrinterConfigHelper(Context context) {
        this.context = context;
    }

    /**
     * Configure Sunmi printer Wi-Fi settings via Bluetooth
     * @param device The Bluetooth device (printer)
     * @param ssid Wi-Fi network SSID
     * @param password Wi-Fi network password
     * @return true if configuration was sent successfully
     */
    public boolean configurePrinterWifi(BluetoothDevice device, String ssid, String password) {
        BluetoothSocket socket = null;
        OutputStream outputStream = null;

        try {
            // Check Bluetooth permission
            if (!checkBluetoothPermission()) {
                Log.e(TAG, "Bluetooth permission not granted");
                return false;
            }

            // Create Bluetooth socket
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID);

            // Connect to the device
            Log.d(TAG, "Connecting to printer...");
            socket.connect();
            Log.d(TAG, "Connected successfully");

            // Get output stream
            outputStream = socket.getOutputStream();

            // Send Wi-Fi configuration commands
            boolean success = sendWifiConfig(outputStream, ssid, password);

            Log.d(TAG, "Configuration sent: " + success);
            return success;

        } catch (IOException e) {
            Log.e(TAG, "Error configuring printer: " + e.getMessage(), e);
            return false;
        } finally {
            // Clean up resources
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Send Wi-Fi configuration to the printer
     * Tries multiple command formats for maximum compatibility
     */
    private boolean sendWifiConfig(OutputStream out, String ssid, String password) {
        try {
            byte[] ssidBytes = ssid.getBytes(StandardCharsets.UTF_8);
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);

            Log.d(TAG, "Attempting WiFi configuration...");
            Log.d(TAG, "SSID: " + ssid + " (" + ssidBytes.length + " bytes)");
            Log.d(TAG, "Password: " + passwordBytes.length + " bytes");

            // Initialize printer
            out.write(new byte[]{0x1B, 0x40}); // ESC @
            out.flush();
            Thread.sleep(200);

            // Try Format 1: Sunmi proprietary format with complete packet
            Log.d(TAG, "Trying Format 1: Sunmi proprietary packet");
            out.write(new byte[]{0x1F, 0x1B, 0x1F, (byte)0x91});
            out.write((byte)ssidBytes.length);
            out.write(ssidBytes);
            out.write((byte)passwordBytes.length);
            out.write(passwordBytes);
            out.write(0x03); // WPA2-PSK
            out.flush();
            Thread.sleep(1000);

            // Try Format 2: Alternative Sunmi format with length prefix
            Log.d(TAG, "Trying Format 2: Length-prefixed format");
            int totalLen = ssidBytes.length + passwordBytes.length + 2;
            out.write(new byte[]{0x1B, 0x1F, 0x91});
            out.write((byte)totalLen);
            out.write((byte)ssidBytes.length);
            out.write(ssidBytes);
            out.write((byte)passwordBytes.length);
            out.write(passwordBytes);
            out.flush();
            Thread.sleep(1000);

            // Try Format 3: Simple concatenation format
            Log.d(TAG, "Trying Format 3: Simple format");
            String simpleCmd = String.format("WIFI_SET:%s,%s\n", ssid, password);
            out.write(simpleCmd.getBytes(StandardCharsets.UTF_8));
            out.flush();
            Thread.sleep(1000);

            // Try Format 4: JSON-style format (some printers use this)
            Log.d(TAG, "Trying Format 4: JSON format");
            String jsonCmd = String.format("{\"ssid\":\"%s\",\"pwd\":\"%s\"}\n", ssid, password);
            out.write(jsonCmd.getBytes(StandardCharsets.UTF_8));
            out.flush();
            Thread.sleep(1000);

            // Try Format 5: Extended ESC/POS format
            Log.d(TAG, "Trying Format 5: Extended ESC/POS");
            out.write(new byte[]{0x1D, 0x28, 0x46}); // GS ( F - Extended function
            out.write((byte)(ssidBytes.length + passwordBytes.length + 4)); // Length
            out.write(0x00); // pL
            out.write(0x05); // fn = 5 (network config)
            out.write((byte)ssidBytes.length);
            out.write(ssidBytes);
            out.write((byte)passwordBytes.length);
            out.write(passwordBytes);
            out.flush();
            Thread.sleep(1000);

            Log.d(TAG, "All WiFi configuration formats sent");
            return true;

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error sending Wi-Fi config: " + e.getMessage());
            return false;
        }
    }

    private boolean checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // Helper method to convert string to hex for debugging
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}
