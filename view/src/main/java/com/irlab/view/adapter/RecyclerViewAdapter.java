package com.irlab.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.irlab.base.entity.CellData;
import com.irlab.view.R;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    // 数据容器
    private List<CellData> configList;

    private int mPosition = -1;

    public RecyclerViewAdapter(List<CellData> configList) {
        this.configList = configList;
    }

    private setClick onItemClickListener;
    private setLongClick onItemLongClickListener;

    // 内部类实现viewHolder 拿到cardView中的布局元素
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView check;
        private final TextView playerBlack, playerWhite, level, rule;
        private final View root;

        public ViewHolder(View root) {
            super(root);
            this.root = root;
            check = root.findViewById(R.id.iv_check);
            playerBlack = root.findViewById(R.id.tv_player_black);
            playerWhite = root.findViewById(R.id.tv_player_white);
            rule = root.findViewById(R.id.tv_rule);
            level = root.findViewById(R.id.tv_level);
        }
    }

    @Override
    public int getItemCount() {
        return configList.size();
    }

    public int getmPosition() { return this.mPosition; }

    public void setmPosition(int mPosition) { this.mPosition = mPosition; }

    // 绑定视图管理者
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // 双方信息
        holder.playerBlack.setText(configList.get(position).getPlayerBlack().trim());
        holder.playerWhite.setText(configList.get(position).getPlayerWhite().trim());
        // 段位信息
        holder.level.setText(configList.get(position).getEngine().trim());
        // 规则
        holder.rule.setText(configList.get(position).getRule() == 0 ? "中国规则" : "日本规则");
        // 设置tag
        holder.root.setTag(position);
        if (position == getmPosition()) {
            holder.check.setVisibility(View.VISIBLE);
        } else {
            holder.check.setVisibility(View.INVISIBLE);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_setting_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        // 为Item设置点击事件
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        return viewHolder;
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
