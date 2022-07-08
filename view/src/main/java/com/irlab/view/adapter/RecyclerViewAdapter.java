package com.irlab.view.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.irlab.base.entity.CellData;
import com.irlab.view.R;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>
        implements View.OnClickListener, View.OnLongClickListener {

    // 数据容器
    private List<CellData> list;

    private int mPosition = -1;

    public RecyclerViewAdapter(List<CellData> list) {
        this.list = list;
    }

    private setClick onItemClickListener;
    private setLongClick onItemLongClickListener;

    // 内部类实现viewHolder 拿到cardView中的布局元素
    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView check;
        private TextView player, desc, rule;
        private View root;

        public ViewHolder(View root) {
            super(root);
            this.root = root;
            check = root.findViewById(R.id.iv_check);
            player = root.findViewById(R.id.tv_player);
            rule = root.findViewById(R.id.tv_rule);
            desc = root.findViewById(R.id.tv_desc);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public int getmPosition() { return this.mPosition; }

    public void setmPosition(int mPosition) { this.mPosition = mPosition; }

    // 绑定视图管理者
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // 双方信息
        holder.player.setText(list.get(position).getPlayerBlack() + "  VS  " + list.get(position).getPlayerWhite());
        // description
        holder.desc.setText(list.get(position).getDesc());
        // 规则
        holder.rule.setText(list.get(position).getRule() == 0 ? "中国规则" : "日本规则");
        // 设置tag
        holder.root.setTag(position);
        if (position == getmPosition()) {
            //選中的顔色就設成了  黃色
            holder.check.setVisibility(View.VISIBLE);
        } else {
            //未選中的顔色 就設成了 白色
            holder.check.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_setting_item, parent, false);
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
