package com.irlab.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.irlab.view.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
棋谱ListView的适配器
 */
public class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    private static final int TYPE_EMPTY = 0;
    public static final int TYPE_NORMAL = 1;

    // 数据容器
    private List<Map<String, Object>> list;

    private setClick onItemClickListener;
    private setLongClick onItemLongClickListener;

    public ArchiveAdapter(List<Map<String, Object>> list) {
        this.list = list;
    }

    // 内部类实现viewHolder 拿到cardView中的布局元素
    public class ArchiveViewHolder extends RecyclerView.ViewHolder {
        private String code;
        private TextView playerInfo, date;
        private View root;

        public ArchiveViewHolder(View root) {
            super(root);
            this.root = root;
            playerInfo = root.findViewById(R.id.tv_player_info);
            date = root.findViewById(R.id.tv_date);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (list.size() <= 0) {
            return TYPE_EMPTY;
        }
        return TYPE_NORMAL;
    }

    @NonNull
    @Override
    public ArchiveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.archive_item, parent, false);
        ArchiveViewHolder viewHolder = new ArchiveViewHolder(view);
        // 为Item设置点击事件
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ArchiveViewHolder holder, int position) {
        // 双方信息
        holder.playerInfo.setText(list.get(position).get("info").toString().trim());
        holder.date.setText(list.get(position).get("date").toString().trim());
        // 日期
        holder.code = list.get(position).get("code").toString();
        // 设置tag
        holder.root.setTag(position);
    }

    @Override
    public void onClick(View v) {
        if (onItemClickListener != null) {
            // 注意这里使用getTag方法获取数据
            onItemClickListener.onItemClickListener(v, (Integer) v.getTag());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return onItemLongClickListener != null && onItemLongClickListener.onItemLongClickListener(v, (Integer) v.getTag());
    }

    // 设置点击事件
    public void setOnItemClickListener(setClick onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    // 设置长按事件
    public void setOnItemLongClickListener(setLongClick onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    // 声明点击和长按接口 将当前item对应的View返回
    public interface setClick {
        void onItemClickListener(View view, int position);
    }

    public interface setLongClick {
        boolean onItemLongClickListener(View view, int position);
    }
}



    /*// viewHolder将各个位置的元素安排到各自的位置
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
    }*/
