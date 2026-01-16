package com.sunmi.printerconfig;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sunmi.cloudprinter.bean.PrinterDevice;

import java.util.List;

public class PrinterDeviceAdapter extends RecyclerView.Adapter<PrinterDeviceAdapter.ViewHolder> {
    private final List<PrinterDevice> devices;
    private final OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(PrinterDevice device);
    }

    public PrinterDeviceAdapter(List<PrinterDevice> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_bluetooth_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PrinterDevice device = devices.get(position);
        String deviceName = "Sunmi Printer";

        try {
            // On Android 12+, need BLUETOOTH_CONNECT permission to access device name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(holder.itemView.getContext(),
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    String name = device.getName();
                    if (name != null && !name.isEmpty()) {
                        deviceName = name;
                    }
                }
            } else {
                String name = device.getName();
                if (name != null && !name.isEmpty()) {
                    deviceName = name;
                }
            }
        } catch (SecurityException e) {
            // Ignore - will use default name
        }

        holder.deviceName.setText(deviceName);
        holder.deviceAddress.setText(device.getAddress());
        holder.itemView.setOnClickListener(v -> listener.onDeviceClick(device));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceAddress;

        ViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceAddress = itemView.findViewById(R.id.deviceAddress);
        }
    }
}
