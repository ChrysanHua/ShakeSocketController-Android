package com.ssc.shakesocketcontroller;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ssc.shakesocketcontroller.Models.pojo.FunctionStr;

import java.util.Arrays;
import java.util.List;

public class FunctionAdapter extends RecyclerView.Adapter<FunctionAdapter.ViewHolder> {

    private Context context;
    private List<FunctionStr> funList;

    public FunctionAdapter(FunctionStr[] funStrs) {
        funList = Arrays.asList(funStrs);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (context == null) {
            context = viewGroup.getContext();
        }
        View view = LayoutInflater.from(context).inflate(R.layout.function_item,
                viewGroup, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FunctionStr functionStr = funList.get(
                        holder.getAdapterPosition());
                context.startActivity(new Intent(context, functionStr.getCls()));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        FunctionStr functionStr = funList.get(i);
        viewHolder.functionName.setText(functionStr.getName());
        viewHolder.functionInfo.setText(functionStr.getInfo());
    }

    @Override
    public int getItemCount() {
        return funList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView functionName;
        TextView functionInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            functionName = itemView.findViewById(R.id.function_name);
            functionInfo = itemView.findViewById(R.id.function_info);
        }
    }
}
