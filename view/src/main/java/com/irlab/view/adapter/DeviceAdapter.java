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
import java.util.Set;

public class DeviceAdapter extends BaseAdapter {

    private Context context;
    private List<BluetoothDevice> list;

    public DeviceAdapter(Context context) {
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

    @SuppressLint({"MissingPermission"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        DeviceViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_lv_devices_item, null);
            viewHolder = new DeviceViewHolder();
            viewHolder.tvDeviceName = view.findViewById(R.id.tv_name);
            viewHolder.tvDeviceState = view.findViewById(R.id.tv_bond_state);
            view.setTag(viewHolder);
        } else {
            viewHolder = (DeviceViewHolder) view.getTag();
        }

        if (list.get(i).getName() == null) {
            viewHolder.tvDeviceName.setText("NULL");
        } else {
            viewHolder.tvDeviceName.setText(list.get(i).getName());
        }

        if (list.get(i).getBondState() == 12) {
            viewHolder.tvDeviceState.setText("已配对");
        } else if (list.get(i).getBondState() == 11) {
            viewHolder.tvDeviceState.setText("正在配对");
        } else if (list.get(i).getBondState() == 10) {
            viewHolder.tvDeviceState.setText("未配对");
        }
        return view;
    }

    public void removeDevice(BluetoothDevice device) {
        if (list != null) {
            list.remove(device);
        }
    }

    /**
     * 获取已绑定设备
     */
    @SuppressLint("MissingPermission")
    public void addBondedDevice(Set<BluetoothDevice> bondedDevices) {
        if (bondedDevices.size() > 0) { //  如果获取的结果大于0，则开始逐个解析
            for (BluetoothDevice device : bondedDevices) {
                if (!list.contains(device) && device.getName() != null) {  // 防止重复添加
                    list.add(device);
                }
            }
        }
    }


    public void addAllDevice(List<BluetoothDevice> bluetoothDevices) {
        if (list != null) {
            list.clear();
            list.addAll(bluetoothDevices);
        }
        notifyDataSetChanged();
    }

    public void addDevice(BluetoothDevice bluetoothDevice) {
        if (list == null) {
            return;
        }
        if (!list.contains(bluetoothDevice)) {
            list.add(bluetoothDevice);
        }
        notifyDataSetChanged();   //刷新
    }

    public void clear() {
        if (list != null) {
            list.clear();
        }
        notifyDataSetChanged(); //刷新
    }

    static class DeviceViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceState;
    }
}
