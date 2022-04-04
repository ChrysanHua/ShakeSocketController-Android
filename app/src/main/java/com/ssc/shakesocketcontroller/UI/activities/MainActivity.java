package com.ssc.shakesocketcontroller.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.ssc.shakesocketcontroller.FunctionActivity;
import com.ssc.shakesocketcontroller.Models.events.signal.CtrlStateChangedEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.TCPConnectedEvent;
import com.ssc.shakesocketcontroller.Models.events.signal.TCPDisConnectEvent;
import com.ssc.shakesocketcontroller.R;
import com.ssc.shakesocketcontroller.Transaction.controller.MyApplication;
import com.ssc.shakesocketcontroller.Transaction.controller.TransactionController;
import com.ssc.shakesocketcontroller.Utils.DeviceUtil;
import com.ssc.shakesocketcontroller.Utils.StrUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    /**
     * 抽屉菜单顶栏内容ViewHolder
     */
    static class NavHeaderViewHolder {

        final View rootView;        //顶栏根布局
        final ImageView imgView;    //头像
        final TextView deviceName;  //本机设备名
        final TextView deviceIP;    //本机IP
        final TextView ctrlCount;   //控制连接数量
        final TextView ctrlState;   //控制状态

        public NavHeaderViewHolder(@NonNull View rootView) {
            this.rootView = rootView;
            this.imgView = rootView.findViewById(R.id.nav_header_img);
            this.deviceName = rootView.findViewById(R.id.nav_header_device_name);
            this.deviceIP = rootView.findViewById(R.id.nav_header_ip);
            this.ctrlCount = rootView.findViewById(R.id.nav_header_ctrl_count);
            this.ctrlState = rootView.findViewById(R.id.nav_header_ctrl_state);
        }
    }


    private final TransactionController controller = MyApplication.getController();

    private AppBarConfiguration mAppBarConfiguration;       //控制左上角导航按钮行为的配置
    private NavController mNavController;                   //NavHost里的导航控制器
    private int mLastDestinationID;                         //最后一个导航目的地ID


    // TODO: 2022/3/13 实现前台服务功能，建议发送和接收分开Socket，然后一起常驻在前台服务当中
    // TODO: 2022/3/9 Controller里的on/off状态和start/stop状态分开
    // TODO: 2022/3/9 考虑已发现设备的心跳问题：
    //                  已连接：等到发具体指令的时候再判断是否已断开；
    //                  未连接：在每次下拉刷新监听广播的时候整体替换为新列表；


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "before super onCreate: for HomeListFragment");
        super.onCreate(savedInstanceState);
        Log.i(TAG, "after super onCreate: for HomeListFragment");
        setContentView(R.layout.activity_main);
        //标题栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //悬浮按钮
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            if (!controller.isCtrlON() && !DeviceUtil.isNetworkConnected()) {
                //want to set Ctrl-ON, but no network
                Snackbar.make(view, R.string.tip_no_network, Snackbar.LENGTH_LONG).show();
            } else {
                //change state
                toggleCtrlOnOffState();
                //change appearance
                setUIState(controller.isCtrlON());
                //show tips
                Snackbar.make(view,
                        controller.isCtrlON() ? R.string.tip_ctrl_on : R.string.tip_ctrl_off,
                        Snackbar.LENGTH_SHORT).show();
            }
        });

        //抽屉菜单以及导航图
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        //创建导航按钮配置，每一个抽屉菜单项中的ID都应该加到里面，
        //  因为抽屉菜单中的每一项Fragment都是顶级页面（无需向上“←”按钮）
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home_list, R.id.nav_history_list,
                R.id.nav_media_control, R.id.nav_power_control)
                .setDrawerLayout(drawer)
                .build();
        //从NavHost中获取导航控制器
        mNavController = ((NavHostFragment) Objects.requireNonNull(getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment))).getNavController();
        //绑定导航控制器、导航按钮配置、标题栏、导航图，以便导航组件自动处理UI交互
        NavigationUI.setupActionBarWithNavController(this, mNavController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, mNavController);

        //设置导航监听器
        final WeakReference<NavigationView> weakRefNavView = new WeakReference<>(navigationView);
        mNavController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController,
                                             @NonNull NavDestination destination,
                                             @Nullable Bundle arguments) {
                Log.i(TAG, "onDestinationChanged: " + destination.getLabel());
                //通过弱引用获取导航图
                NavigationView navView = weakRefNavView.get();
                if (navView == null) {
                    mNavController.removeOnDestinationChangedListener(this);
                    return;
                }
                //显式设置导航菜单的选中项，以解决显式导航时嵌套子菜单无法被选中的问题
                MenuItem item = navView.getMenu().findItem(destination.getId());
                if (item != null && !item.isChecked()) {
                    navView.setCheckedItem(item);
                }

                //按需记录目的地ID
                boolean shouldNav = arguments == null || arguments.getBoolean(
                        MyApplication.appContext.getString(R.string.arg_name_should_nav_flag), true);
                if (shouldNav) {
                    mLastDestinationID = destination.getId();
                } else {
                    mLastDestinationID = navController.getGraph().getStartDestination();
                }
                // TODO: 2022/3/15 根据needShowFabOnFragment参数确定是否显示Fab按钮
            }
        });

        //缓存抽屉菜单顶栏内容的引用
        if (navigationView.getTag() == null) {
            NavHeaderViewHolder holder = new NavHeaderViewHolder(navigationView.getHeaderView(0));
            navigationView.setTag(holder);
            //设置本机设备名
            holder.deviceName.setText(DeviceUtil.getDeviceName());
        }

        //进入Activity先根据Ctrl状态更新UI
        setUIState(controller.isCtrlON());
        Log.i(TAG, "finish onCreate: for HomeListFragment");
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        if (!EventBus.getDefault().isRegistered(controller)) {
            //临时
            EventBus.getDefault().register(controller);
        }
        //进入界面时如果已经处于上次退出时的界面（即未显式导航就已经在目的地），则需要重置为初始状态
        if (controller.isCtrlON() &&
                Objects.requireNonNull(mNavController.getCurrentDestination()).getId()
                        == controller.getLastDestinationID()) {
            controller.setLastDestinationID(-1);
        }
        Log.i(TAG, "onStart: ");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e(TAG, "onSaveInstanceState: ");
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.e(TAG, "onRestoreInstanceState: ");
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        //退出界面时才真正保存目的地ID
        if (controller.isCtrlON()) {
            controller.setLastDestinationID(mLastDestinationID);
        }
        Log.i(TAG, "onStop: ");
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(controller);
        //controller.stop();
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
    }

    /**
     * 处理标题栏向上“←”按钮该作何反应（向上返回/打开抽屉菜单）
     */
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(mNavController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /**
     * 用户按下返回键的事件
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            //关闭抽屉菜单（如果处于展开状态）
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 创建标题栏Action按钮菜单
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * 标题栏Action按钮点击事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //处理标题栏各Action按钮
        if (id == R.id.action_settings) {
            //Log.i(TAG, "cur destination: " + mNavController.getCurrentDestination().getLabel());
            //Log.i(TAG, "dest ID: " + mNavController.getCurrentDestination().getId()
            //        + ", last ID: " + controller.getLastDestinationID());
            return true;
        } else if (id == R.id.action_about) {
            //NavigationView navigationView = findViewById(R.id.nav_view);
            //Log.i(TAG, "cur checkedItem: " + (navigationView.getCheckedItem() == null ? null
            //        : navigationView.getCheckedItem().getTitle()));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 切换控制（Ctrl-ON/OFF）状态
     */
    private void toggleCtrlOnOffState() {
        controller.setCtrlON(!controller.isCtrlON());
        //post控制状态变更事件
        EventBus.getDefault().post(new CtrlStateChangedEvent(controller.isCtrlON()));

        // TODO: 2022/3/24 全部发送完连接信号后，启动延时任务，同时每隔一段时间再发一次，
        //  超时还是未连接（未收到确认信号）则通知变更UI并对已连接设备统一执行保存历史操作，等待电脑用户确认的情况除外；
        //  如果收到确认信号，则用EventBus进行通知↓？通知改变对应连接的数据以及UI；
        //  EventBus.getDefault().post(new LaunchConnectEvent(computerInfo.getMacStr()));
        //变更连接监听服务的状态

        //执行连接握手，逐一连接控制列表里的设备

    }

    /**
     * 设置UI界面的状态，包括悬浮按钮背景色、抽屉菜单顶栏等
     *
     * @param enabled 如果设置为开启状态则为true，否则为false
     */
    private void setUIState(boolean enabled) {
        //悬浮按钮
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setSelected(enabled);

        //通过抽屉菜单导航栏获取顶栏ViewHolder
        NavigationView navigationView = findViewById(R.id.nav_view);
        NavHeaderViewHolder holder = (NavHeaderViewHolder) navigationView.getTag();
        if (enabled) {
            holder.rootView.setBackgroundResource(R.drawable.bg_side_nav_bar_on);
            String ipStr = DeviceUtil.getLocalIP();
            holder.deviceIP.setText(StrUtil.isNullOrEmpty(ipStr) ? null : String.format("(%s)", ipStr));
            int ctrlCount = controller.getCtrlDevicesCount();
            if (ctrlCount == 1) {
                holder.ctrlCount.setText(controller.getCtrlDevices().get(0).getIP());
                holder.ctrlState.setText(R.string.tip_nav_ctrl_on_single);
            } else if (ctrlCount == 0) {
                holder.ctrlCount.setText(null);
                holder.ctrlState.setText(R.string.tip_nav_ctrl_on_listen);
            } else {
                holder.ctrlCount.setText(String.valueOf(ctrlCount));
                holder.ctrlState.setText(R.string.tip_nav_ctrl_on_multi);
            }
        } else {
            holder.rootView.setBackgroundResource(R.drawable.bg_side_nav_bar_off);
            holder.deviceIP.setText(null);
            holder.ctrlCount.setText(null);
            holder.ctrlState.setText(R.string.tip_nav_ctrl_off);
        }
        //fab.setBackgroundTintList(ResourcesCompat.getColorStateList(
        //        getResources(),
        //        enabled ? R.color.fabON : R.color.fabOFF,
        //        getTheme()));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTCPConnectedEvent(TCPConnectedEvent event) {
        //computerInfoAdapter.notifyConnected(event.getTargetInfo());
        //start functionActivity
        Intent intent = new Intent(this, FunctionActivity.class);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTCPDisConnectEvent(TCPDisConnectEvent event) {
        if (!event.isSendFailed()) {
            //computerInfoAdapter.notifyDisConnect();
        }
    }
}
