package com.irlab.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.irlab.view.R;

import java.util.List;
import java.util.Map;

public class ArchiveAdapter extends BaseAdapter {

    // viewHolder将各个位置的元素安排到各自的位置
    private ViewHolder viewHolder;

    private List<Map<String, Object>> data = null;

    private Context mContext;

    public ArchiveAdapter(Context mContext, List<Map<String, Object>> data) {
        this.mContext = mContext;
        this.data = data;
    }

    public int getCount() {
        return this.data.size();
    }

    public Object getItem(int position) {
        return data.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View view, ViewGroup arg2) {
        if (view == null) {
            viewHolder = new ViewHolder();
            // 获取Listview对应的item布局
            view = LayoutInflater.from(mContext).inflate(R.layout.archive_item, null);
            // 初始化组件
            viewHolder.title = view.findViewById(R.id.title);
            viewHolder.description = view.findViewById(R.id.description);
            viewHolder.state = view.findViewById(R.id.state);
            viewHolder.divider = view.findViewById(R.id.divider);
            view.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) view.getTag();
        }
        Map<String, Object> map = data.get(position);
        viewHolder.title.setText(map.get("title").toString());
        viewHolder.description.setText(map.get("desc").toString());
        viewHolder.state.setText(map.get("result").toString());
        viewHolder.divider.setVisibility(View.VISIBLE);
        return view;
    }

    final static class ViewHolder {
        TextView title;
        TextView description;
        TextView state;
        View divider;
    }
}
