package com.irlab.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.irlab.view.R;
import com.irlab.view.fragment.ArchiveFragment;

import java.util.List;
import java.util.Map;

/*
棋谱ListView的适配器
 */
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
            viewHolder.info = view.findViewById(R.id.info);
            viewHolder.date = view.findViewById(R.id.date);
            viewHolder.divider = view.findViewById(R.id.divider);
            viewHolder.hint = view.findViewById(android.R.id.empty);
            view.setTag(viewHolder);
        }
        // 若已初始化过, 则不需要重新构建viewHolder
        else {
            viewHolder = (ViewHolder) view.getTag();
        }
        // 通过viewHolder向布局填充数据
        Map<String, Object> map = data.get(position);
        viewHolder.info.setText(map.get("info").toString());
        viewHolder.date.setText(map.get("date").toString());
        viewHolder.code = map.get("code").toString();
        viewHolder.divider.setVisibility(View.VISIBLE);
        return view;
    }

    // ViewHolder中的各个组件
    final static class ViewHolder {
        TextView info;
        TextView date;
        TextView hint;
        View divider;
        String code;
    }
}
