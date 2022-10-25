package com.irlab.view.adapter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.irlab.view.R;

import java.util.ArrayList;
import java.util.List;

public class LVDevicesAdapter extends BaseAdapter {

    private Context context;
    private List<BluetoothDevice> list;

    public LVDevicesAdapter(Context context) {
        this.context = context;
        list = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public Object getItem(int i) {
        if (list == null) {
            return null;
        }
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("MissingPermission")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        DeviceViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_lv_devices_item, null);
            viewHolder = new DeviceViewHolder();
            viewHolder.tvDeviceName = view.findViewById(R.id.tv_device_name);
            viewHolder.tvDeviceAddress = view.findViewById(R.id.tv_device_address);
            view.setTag(viewHolder);
        } else {
            viewHolder = (DeviceViewHolder) view.getTag();
        }

        if (list.get(i).getName() == null) {
            viewHolder.tvDeviceName.setText("NULL");
        } else {
            viewHolder.tvDeviceName.setText(list.get(i).getName());
        }

        viewHolder.tvDeviceAddress.setText(list.get(i).getAddress());
        return view;
    }

    public void addAllDevice(List<BluetoothDevice> bluetoothDevices) {
        if (list != null) {
            list.clear();
        }
        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            list.add(bluetoothDevice);
        }
        notifyDataSetChanged();
    }

    public void addDevice(BluetoothDevice bluetoothDevice) {
        if (list == null) {
            return;
        }
        if (!list.contains(bluetoothDevice)) {
            list.add(bluetoothDevice);
//            Log.e("list",list+"");
        }
        notifyDataSetChanged();   //刷新
    }

    public void clear() {
        if (list != null) {
            list.clear();
        }
        notifyDataSetChanged(); //刷新
    }

    class DeviceViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceAddress;
    }
}
