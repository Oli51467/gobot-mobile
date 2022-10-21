package com.irlab.view.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.irlab.view.R;
import com.irlab.view.utils.MyFuntion;

import java.util.List;

public class FunctionAdapter extends RecyclerView.Adapter<FunctionAdapter.ViewHolder> {

    private Context context;
    private List<MyFuntion> funcList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageView;
        TextView textView;
        ImageView service_status_image;

        // 标红后生成，需要重写的方法
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            imageView = itemView.findViewById(R.id.func_image);
            textView = itemView.findViewById(R.id.func_name);
            service_status_image = itemView.findViewById(R.id.service_status);
        }
    }

    public FunctionAdapter(List<MyFuntion> list) {
        funcList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
            Log.d("onCreateViewHolder", "context == null");
        }
        View view = LayoutInflater.from(context).inflate(R.layout.function_item_layout, parent, false);
        // 添加cardView点击响应
        final ViewHolder holder = new ViewHolder(view);
        holder.cardView.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            MyFuntion myFuntion = funcList.get(position);

            if (myFuntion.getName().equals("电话寻人")) {

            } else if (myFuntion.getName().equals("跟随拍摄")) {

            } else if (myFuntion.getName().equals("手势识别")) {
            } else if (myFuntion.getName().equals("人脸识别")) {
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        MyFuntion function = funcList.get(position);
        holder.textView.setText(function.getName());
        Glide.with(context).load(function.getImageId()).into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return funcList.size();
    }
}
