package com.irlab.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.irlab.base.entity.CellData;
import com.irlab.view.R;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>
        implements View.OnClickListener, View.OnLongClickListener {

    private static final int TYPE_EMPTY = 0;
    public static final int TYPE_NORMAL = 1;

    // 数据容器
    private List<CellData> list = new ArrayList<>();

    private int mPosition = -1;

    public RecyclerViewAdapter(List<CellData> list) {
        this.list = list;
    }

    private setClick onItemClickListener;
    private setLongClick onItemLongClickListener;

    // 内部类实现viewHolder 拿到cardView中的布局元素
    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView check;
        private TextView playerBlack, playerWhite, desc, rule;
        private View root;

        public ViewHolder(View root) {
            super(root);
            this.root = root;
            check = root.findViewById(R.id.iv_check);
            playerBlack = root.findViewById(R.id.tv_player_black);
            playerWhite = root.findViewById(R.id.tv_player_white);
            rule = root.findViewById(R.id.tv_rule);
            desc = root.findViewById(R.id.tv_desc);
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

    public int getmPosition() { return this.mPosition; }

    public void setmPosition(int mPosition) { this.mPosition = mPosition; }

    // 绑定视图管理者
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // 双方信息
        holder.playerBlack.setText(list.get(position).getPlayerBlack().trim());
        holder.playerWhite.setText(list.get(position).getPlayerWhite().trim());
        // description
        holder.desc.setText(list.get(position).getDesc().trim());
        // 规则
        holder.rule.setText(list.get(position).getRule() == 0 ? "中国规则" : "日本规则");
        // 设置tag
        holder.root.setTag(position);
        if (position == getmPosition()) {
            holder.check.setVisibility(View.VISIBLE);
        } else {
            holder.check.setVisibility(View.INVISIBLE);
        }
    }

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
