package com.irlab.view.adapter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.irlab.view.R;

import java.util.List;
import java.util.Set;

public class DeviceAdapter extends BaseQuickAdapter<BluetoothDevice, BaseViewHolder> {

    private final List<BluetoothDevice> mList;

    public DeviceAdapter(int layoutResId, @NonNull List<BluetoothDevice> data) {
        super(layoutResId, data);
        mList = data;
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void convert(@NonNull BaseViewHolder helper, BluetoothDevice item) {

        if (item.getName() == null) {
            helper.setText(R.id.tv_name, "UnKnown");
        } else {
            helper.setText(R.id.tv_name, item.getName());
            if (!mList.contains(item)) mList.add(item);
        }

        ImageView imageView = helper.getView(R.id.iv_device_type);
        getDeviceType(item.getBluetoothClass().getMajorDeviceClass(), imageView);

        //蓝牙设备绑定状态判断
        switch (item.getBondState()) {
            case 12:
                helper.setText(R.id.tv_bond_state, "已配对");
                break;
            case 11:
                helper.setText(R.id.tv_bond_state, "正在配对...");
                break;
            case 10:
                helper.setText(R.id.tv_bond_state, "未配对");
                break;
        }
    }

    @Override
    public BluetoothDevice getItem(int i) {
        if (mList == null) {
            return null;
        }
        return mList.get(i);
    }

    @SuppressLint({"MissingPermission", "NotifyDataSetChanged"})
    public void addDevice(BluetoothDevice bluetoothDevice) {
        if (mList == null) {
            return;
        }
        if (!mList.contains(bluetoothDevice)) {
            if (bluetoothDevice.getName() != null) {
                mList.add(bluetoothDevice);
            }
        }
        notifyDataSetChanged();   //刷新
    }

    /**
     * 获取已绑定设备
     */
    @SuppressLint("MissingPermission")
    public void getBondedDevice(Set<BluetoothDevice> bondedDevices) {
        if (bondedDevices.size() > 0) { //  如果获取的结果大于0，则开始逐个解析
            for (BluetoothDevice device : bondedDevices) {
                if (!mList.contains(device) && device.getName() != null) {  // 防止重复添加
                    mList.add(device);
                }
            }
        }
    }

    public void removeDevice(BluetoothDevice device) {
        mList.remove(device);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clear() {
        if (mList != null) {
            mList.clear();
        }
        notifyDataSetChanged(); //刷新
    }

    /**
     * 根据类型设置图标
     *
     * @param type      类型码
     * @param imageView 图标
     */
    private void getDeviceType(int type, ImageView imageView) {
        switch (type) {
            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES://耳机
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET://穿戴式耳机
            case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE://蓝牙耳机
            case BluetoothClass.Device.Major.AUDIO_VIDEO://音频设备
                imageView.setImageResource(R.mipmap.icon_headset);
                break;
            case BluetoothClass.Device.Major.COMPUTER://电脑
                imageView.setImageResource(R.mipmap.icon_computer);
                break;
            case BluetoothClass.Device.Major.PHONE://手机
                imageView.setImageResource(R.mipmap.icon_phone);
                break;
            case BluetoothClass.Device.Major.HEALTH://健康类设备
                imageView.setImageResource(R.mipmap.icon_health);
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER://照相机录像机
            case BluetoothClass.Device.AUDIO_VIDEO_VCR://录像机
                imageView.setImageResource(R.mipmap.icon_vcr);
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO://车载设备
                imageView.setImageResource(R.mipmap.icon_car);
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER://扬声器
                imageView.setImageResource(R.mipmap.icon_loudspeaker);
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE://麦克风
                imageView.setImageResource(R.mipmap.icon_microphone);
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO://打印机
                imageView.setImageResource(R.mipmap.icon_printer);
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX://音频视频机顶盒
                imageView.setImageResource(R.mipmap.icon_top_box);
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING://音频视频视频会议
                imageView.setImageResource(R.mipmap.icon_meeting);
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER://显示器和扬声器
                imageView.setImageResource(R.mipmap.icon_tv);
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY://游戏
                imageView.setImageResource(R.mipmap.icon_game);
                break;
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR://可穿戴设备
                imageView.setImageResource(R.mipmap.icon_wearable_devices);
                break;
            default://其它
                imageView.setImageResource(R.mipmap.icon_bluetooth);
                break;
        }
    }
}
