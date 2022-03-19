package com.ssc.shakesocketcontroller.UI.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.ssc.shakesocketcontroller.R;
import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;
import com.ssc.shakesocketcontroller.Transaction.controller.TransactionController;
import com.ssc.shakesocketcontroller.Models.pojo.ComputerInfo;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class OnlyCPInfoAdapter extends RecyclerView.Adapter<OnlyCPInfoAdapter.ViewHolder> {

    /**
     * 设备信息ViewHolder类
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        final MaterialCardView cardView;    //子项的最外层布局（卡片布局）
        final TextView computerNickName;    //计算机用户名
        final TextView computerIP;          //IP
        final TextView computerDeviceName;  //计算机设备名
        final TextView curState;            //当前连接的状态

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            computerNickName = itemView.findViewById(R.id.computer_info_nick_name);
            computerIP = itemView.findViewById(R.id.computer_info_ip);
            computerDeviceName = itemView.findViewById(R.id.computer_info_device_name);
            curState = itemView.findViewById(R.id.computer_info_state);
        }
    }


    private final TransactionController controller;         //控制器的引用
    private final List<ComputerInfo> computerInfoList;      //当前设备连接列表
    private final boolean isOnlineView;                     //在线/历史设备信息界面

    public OnlyCPInfoAdapter(boolean isOnlineList) {
        isOnlineView = isOnlineList;
        controller = MyApplication.getController();
        //保存对数据源集合的引用
        computerInfoList = controller.getCurrentDevices(isOnlineView);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //加载布局
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_computer_info, parent, false);
        //创建ViewHolder
        final ViewHolder holder = new ViewHolder(view);
        //给卡片设置点击事件
        holder.cardView.setOnClickListener(v -> {
            final int position = holder.getAdapterPosition();
            final ComputerInfo info = computerInfoList.get(position);
            if (isOnlineView) {
                //在线设备界面则更改选中状态
                info.isChecked = !info.isChecked;
                //通知子项发生改变
                notifyItemChanged(position);
            } else {
                //历史设备界面则打开连接设置界面
                // TODO: 2022/3/6
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //获取当前设备信息的实例对象
        final ComputerInfo info = computerInfoList.get(position);
        //设置卡片内容文本
        holder.computerNickName.setText(info.nickName);
        holder.computerIP.setText(String.format("(%s)", info.getIP()));
        holder.computerDeviceName.setText(info.deviceName);
        //根据isSaved和isConnected组合情况设置状态提示信息
        if (info.isConnected && info.isSaved) {
            holder.curState.setText(R.string.tip_connected);
        } else if (info.isConnected) {
            holder.curState.setText(R.string.tip_connecting);
        } else if (info.isSaved) {
            holder.curState.setText(R.string.tip_saved);
        } else {
            holder.curState.setText(null);
        }
        //如果离线则覆盖状态提示信息
        if (!info.isOnline) {
            holder.curState.setText(R.string.tip_offline);
        }
        //要先令卡片可用，下面的Checked设置才能生效
        holder.cardView.setEnabled(true);
        //设置Checked属性
        holder.cardView.setChecked(info.isChecked);
        //历史设备界面中，利用Selected属性，将当前已连接的设备的CheckedIcon设置为有色
        holder.cardView.setSelected(!isOnlineView && info.isConnected);
        //设置Enabled属性
        holder.cardView.setEnabled(!(isOnlineView && controller.isCtrlON()));
    }

    @Override
    public int getItemCount() {
        return computerInfoList.size();
    }
}
