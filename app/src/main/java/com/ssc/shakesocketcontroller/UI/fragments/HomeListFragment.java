package com.ssc.shakesocketcontroller.UI.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ssc.shakesocketcontroller.R;
import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;
import com.ssc.shakesocketcontroller.Transaction.events.signal.CtrlStateChangedEvent;
import com.ssc.shakesocketcontroller.Transaction.events.signal.EndRefreshEvent;
import com.ssc.shakesocketcontroller.UI.adapters.OnlyCPInfoAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class HomeListFragment extends Fragment {
    private static final String TAG = "HomeListFragment";

    // TODO: 2022/3/10 Ctrl开了以后，已勾选的要逐个去连接，用EventBus进行通知↓？
    //  EventBus.getDefault().post(new LaunchConnectEvent(computerInfo.getMacStr()));

    private boolean curIsOnlineView;            //当前处于哪种界面（在线/历史）

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取当前处于哪种界面
        curIsOnlineView = getArguments() == null || getArguments().getBoolean(
                MyApplication.appContext.getString(R.string.arg_name_online_flag), true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //初始化下拉刷新组件
        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(this::refreshContentData);
        swipeRefresh.setEnabled(!MyApplication.getController().isCtrlON());

        //如果之前已经启动控制，则重新进入APP时显示上次退出前的最后一个Fragment界面
        if (curIsOnlineView && MyApplication.getController().isCtrlON()) {
            NavController navController = Navigation.findNavController(view);
            int lastID = MyApplication.getController().getLastDestinationID();
            if (lastID != -1 && lastID != navController.getGraph().getStartDestination()) {
                //导航到上次的界面
                navController.navigate(lastID, null,
                        new NavOptions.Builder().setLaunchSingleTop(true).build());
                //重置为初始状态
                MyApplication.getController().setLastDestinationID(-1);
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!curIsOnlineView) {
            //历史设备界面，修改屏幕中央的提示文字
            TextView tipsTextView = requireView().findViewById(R.id.tips_text_view);
            tipsTextView.setText(R.string.tip_empty_history);
        }
        //初始化列表
        RecyclerView recyclerView = requireView().findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new OnlyCPInfoAdapter(curIsOnlineView));
        //更新中央提示文字的可见性
        updateTipsVisibility();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        Log.i(TAG, "onStart: ");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!MyApplication.getController().isCtrlON()) {
            //未启动控制，进入界面时自动执行一次刷新
            setSwipeRefreshing(true);
            refreshContentData();
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: " + curIsOnlineView);
    }

    /**
     * 更新屏幕中央提示文字的可见性
     */
    private void updateTipsVisibility() {
        requireView().findViewById(R.id.tips_text_view).setVisibility(
                MyApplication.getController().getCurrentDevicesCount(curIsOnlineView) == 0
                        ? View.VISIBLE : View.GONE);
    }

    /**
     * 设置下拉刷新组件的刷新状态
     */
    private void setSwipeRefreshing(boolean refresh) {
        SwipeRefreshLayout swipeRefresh = requireView().findViewById(R.id.swipe_refresh_layout);
        swipeRefresh.setRefreshing(refresh);
    }

    /**
     * 设置下拉刷新组件的可用状态
     */
    private void setSwipeEnabled(boolean enabled) {
        requireView().findViewById(R.id.swipe_refresh_layout).setEnabled(enabled);
    }

    /**
     * 刷新列表
     */
    private void refreshContentData() {
        //通知Controller执行刷新（如果上一次同类型刷新尚未结束，则会忽略本次调用）
        if (curIsOnlineView) {
            //执行一次广播监听
            MyApplication.getController().observeBCOnce();
        } else {
            //执行一次历史读取
            MyApplication.getController().readHistoryOnce();
        }
    }

    /**
     * 通知更新整个列表
     */
    private void notifyDataAllChanged() {
        RecyclerView recyclerView = requireView().findViewById(R.id.recycler_view);
        Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
    }

    /**
     * 刷新完毕事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onEndRefreshEvent(EndRefreshEvent event) {
        if (event.isOnlineRefresh() == curIsOnlineView) {
            //通知Adapter更新
            notifyDataAllChanged();
            //停止刷新进度条
            setSwipeRefreshing(false);
            //更新中央提示文字的可见性
            updateTipsVisibility();
        }
    }

    /**
     * 控制状态变更事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCtrlStateChangedEvent(CtrlStateChangedEvent event) {
        //通知Adapter更新
        notifyDataAllChanged();
        //停止刷新进度条（如果正在刷新）
        setSwipeRefreshing(false);
        //设置下拉刷新的可用性
        setSwipeEnabled(!event.isCtrlON());
    }

}
