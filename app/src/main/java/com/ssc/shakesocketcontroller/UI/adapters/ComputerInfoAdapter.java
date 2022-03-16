package com.ssc.shakesocketcontroller.UI.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.ssc.shakesocketcontroller.R;
import com.ssc.shakesocketcontroller.Transaction.pojo.ComputerInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class ComputerInfoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // TODO: 2022/3/4 原计划是同一列表内分类（当前在线/历史连接）显示，UI逻辑已基本完成，
    //  但实现过程中发现比较复杂，时间不充裕，固该方案暂时流产，另建OnlyCPInfoAdapter实现单独分开列表显示；
    //  等有时间再考虑融合两个方案，以可选设置的方式留给用户自己选择喜欢的显示模式。


    /**
     * 设备信息ViewHolder类
     */
    static class CPInfoViewHolder extends RecyclerView.ViewHolder {

        final MaterialCardView cardView;    //子项的最外层布局（卡片布局）
        final TextView computerNickName;    //计算机用户名
        final TextView computerIP;          //IP
        final TextView computerDeviceName;  //计算机设备名
        final TextView curState;            //当前连接的状态

        public CPInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            computerNickName = itemView.findViewById(R.id.computer_info_nick_name);
            computerIP = itemView.findViewById(R.id.computer_info_ip);
            computerDeviceName = itemView.findViewById(R.id.computer_info_device_name);
            curState = itemView.findViewById(R.id.computer_info_state);
        }
    }

    /**
     * 分界线ViewHolder类
     */
    static class DivLineViewHolder extends RecyclerView.ViewHolder {

        final TextView dividingTip;         //分界线分类名

        public DivLineViewHolder(@NonNull View itemView) {
            super(itemView);
            dividingTip = itemView.findViewById(R.id.dividing_text_view);
        }
    }


    private static final int TYPE_CP_INFO = 492;        //设备信息子项类型FLAG
    private static final int TYPE_DIV_LINE = 976;       //分界线子项类型FLAG
    private final List<String> CACHE_DIV_LINE_LIST;     //分界线名称的固定缓存集合

    private final List<ComputerInfo> computerInfoList;  //当前在线的设备连接
    private final List<ComputerInfo> historyInfoList;   //当前不在线的历史设备连接
    private final List<String> dividingLineList;        //当前有效分界线名称集合

    public ComputerInfoAdapter(@NonNull Collection<ComputerInfo> onlineInfo,
                               @NonNull Collection<ComputerInfo> historyInfo,
                               @NonNull Context context) {
        //保存对数据源集合的引用
        computerInfoList = (List<ComputerInfo>) onlineInfo;
        historyInfoList = (List<ComputerInfo>) historyInfo;
        //目前仅设定一条分界线
        CACHE_DIV_LINE_LIST = Collections.singletonList(context.getString(R.string.div_line_history_offline));
        dividingLineList = new ArrayList<>(CACHE_DIV_LINE_LIST.size());
        //历史设备不为空才显示分界线
        historyInfoListSizeChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //根据viewType判断子项应加载哪个布局、创建哪种ViewHolder
        if (viewType == TYPE_DIV_LINE) {                                //分界线子项
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dividing_line, parent, false);
            return new DivLineViewHolder(view);

        } else if (viewType == TYPE_CP_INFO) {                          //设备信息子项
            //加载布局
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_computer_info, parent, false);
            //创建ViewHolder
            final CPInfoViewHolder holder = new CPInfoViewHolder(view);
            //给卡片设置点击事件
            holder.cardView.setOnClickListener(v -> {
                final int position = holder.getAdapterPosition();
                final ComputerInfo info = getActualCPInfo(position);
                if (info != null) {
                    //修改选中状态
                    info.isChecked = !info.isChecked;
                    //通知子项发生改变
                    notifyItemChanged(position);
                }
            });

            return holder;

        } else {                                                        //其它未知类型
            //返回空白卡片
            return new CPInfoViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_computer_info, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        //根据holder的类型判断应如何加载数据到子项中
        if (holder.getItemViewType() == TYPE_DIV_LINE) {                //分界线子项
            ((DivLineViewHolder) holder).dividingTip.setText(getActualDivText(position));

        } else if (holder.getItemViewType() == TYPE_CP_INFO) {          //设备信息子项
            //获取当前设备信息的实例对象
            final ComputerInfo info = getActualCPInfo(position);
            if (info != null) {
                final CPInfoViewHolder cpInfoHolder = (CPInfoViewHolder) holder;
                cpInfoHolder.computerNickName.setText(info.nickName);
                cpInfoHolder.computerIP.setText(String.format("(%s)", info.getIP()));
                cpInfoHolder.computerDeviceName.setText(info.deviceName);
                cpInfoHolder.curState.setText(null);
                cpInfoHolder.cardView.setEnabled(true); //先设置可用，Checked的设置才有效
                cpInfoHolder.cardView.setChecked(info.isChecked);
                //根据Ctrl的On/Off设置Enabled
                //cpInfoHolder.cardView.setEnabled(!(info.isChecked && position % 2 == 0));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        //由于目前只有一条分界线，所以仅简单实现
        if (position == computerInfoList.size()) {
            //设置在“在线设备”和“历史记录”之间，即历史记录分界线
            return TYPE_DIV_LINE;
        } else {
            //其余均为设备信息类型
            return TYPE_CP_INFO;
        }
    }

    @Override
    public int getItemCount() {
        //子项的总数量包含【在线设备数、历史记录数、分界线数量】
        return computerInfoList.size() + historyInfoList.size() + dividingLineList.size();
    }

    /**
     * 判断指定位置是不是在线设备信息子项
     */
    private boolean isOnlineCPInfo(int position) {
        return (position >= 0 && position < computerInfoList.size());
    }

    /**
     * 判断指定位置是不是历史设备信息子项
     */
    private boolean isHistoryCPInfo(int position) {
        return (position >= (computerInfoList.size() + dividingLineList.size())
                && position < getItemCount());
    }

    /**
     * 判断指定位置是不是设备信息子项
     */
    private boolean isCPInfoPosition(int position) {
        //根据原始Position判断该位置是否为设备信息子项
        return isOnlineCPInfo(position) || isHistoryCPInfo(position);
    }

    /**
     * 获取指定位置所在数据源列表中的实际下标
     *
     * @param position 基类方法提供的原始Position
     * @return 返回用于访问其对应数据源列表的下标，若position超出范围则返回-1
     */
    private int getActualIndex(int position) {
        //根据原始Position计算实际Index
        if (isOnlineCPInfo(position)) {
            return position;
        } else if (isHistoryCPInfo(position)) {
            return position - (computerInfoList.size() + dividingLineList.size());
        } else if (!isCPInfoPosition(position)) {
            return position - computerInfoList.size();
        } else {
            return -1;
        }
    }

    /**
     * 根据指定的原始Position获取设备信息子项的源对象
     *
     * @return 如果对应位置是设备信息子项则返回其实例对象，否则返回null
     */
    @Nullable
    private ComputerInfo getActualCPInfo(int position) {
        if (isOnlineCPInfo(position)) {
            //在线设备
            return computerInfoList.get(getActualIndex(position));
        } else if (isHistoryCPInfo(position)) {
            //不在线的历史设备
            return historyInfoList.get(getActualIndex(position));
        } else {
            //非设备信息子项
            return null;
        }
    }

    /**
     * 根据指定的原始Position获取分界线子项的显示文本
     *
     * @return 如果对应位置是分界线子项则返回其显示文本，否则返回null
     */
    @Nullable
    private String getActualDivText(int position) {
        //如果后续迭代版本中存在多条分界线，应考虑到分界线在UI中不是连续出现的，
        //      所有与position相关的方法都将需要重新设计
        if (!isCPInfoPosition(position)) {
            return dividingLineList.get(getActualIndex(position));
        } else {
            return null;
        }
    }

    /**
     * 历史设备列表数量发生改变时调用，该方法不通知Adapter
     */
    private void historyInfoListSizeChanged() {
        //控制分界线列表，以便控制分界线是否显示
        if (historyInfoList.size() == 0) {
            //历史设备为空，不显示分界线
            dividingLineList.clear();
        } else if (dividingLineList.size() == 0) {
            //有历史设备，且分界线为空，需显示分界线
            dividingLineList.addAll(CACHE_DIV_LINE_LIST);
        }
    }

    public void notifyInfoListChanged() {
        //通知Adapter
        notifyDataSetChanged();
    }

}
