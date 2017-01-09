package io.github.mayubao.kuaichuan.core.iOS;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.github.mayubao.kuaichuan.AppContext;
import io.github.mayubao.kuaichuan.Constant;
import io.github.mayubao.kuaichuan.R;
import io.github.mayubao.kuaichuan.core.BaseTransfer;
import io.github.mayubao.kuaichuan.core.MyWifiManager;
import io.github.mayubao.kuaichuan.core.receiver.WifiAPBroadcastReceiver;
import io.github.mayubao.kuaichuan.core.utils.TextUtils;
import io.github.mayubao.kuaichuan.ui.view.RadarLayout;

/**
 * Created by jhchen on 2017/1/3.
 */

public class TranslateWithIOS extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = TranslateWithIOS.class.getSimpleName();
    Context mContext;

    /**
     * 其他UI
     */
    RadarLayout radarLayout;
    TextView tv_device_name;
    TextView tv_desc;
    TextView tv_top_tip;

    WifiAPBroadcastReceiver mWifiAPBroadcastReceiver;
    boolean mIsInitialized = false;
    boolean mIsSendFile = false;

    String mIOSServerIp = "";

    /**
     * 与 ios 通信的 线程
     */
    Runnable mUdpServerRuannable;
    public static final int MSG_TO_START_SEND_TO_IOS = 0X89;
    public static final int MSG_TO_START_IOS_RECEIVE_SERVER = 0X90;
    public static final int MSG_TO_RECEIVE_BROADCAST = 0X91;
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MSG_TO_START_SEND_TO_IOS){
                //(Todo:jhchen) 开始发送文件
                Log.i(TAG, "go to send to ios ######>>>" + MSG_TO_START_SEND_TO_IOS);
                Intent intent = new Intent(mContext, IOSFileSenderActivity.class);
                intent.putExtra("serverIp", mIOSServerIp);
                startActivity(intent);
                finishNormal();
            }else if(msg.what == MSG_TO_START_IOS_RECEIVE_SERVER){
                //(Todo:jhchen)启动接收文件服务器
                Log.i(TAG, "go to send start ios receiver server ######>>>" + MSG_TO_START_IOS_RECEIVE_SERVER);
                startActivity(new Intent(mContext, IOSFileReceiverActivity.class));
                finishNormal();
            }else if (msg.what == MSG_TO_RECEIVE_BROADCAST){
                //broadcast test
                Toast.makeText(mContext, "get broadcast", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver_waiting);
        mContext = this;
        getBaseData();
        findViewById();
        init();
    }

    void findViewById(){
        TextView back = (TextView) findViewById(R.id.tv_back);
        back.setOnClickListener(this);
        TextView iosText = (TextView) findViewById(R.id.translate_to_ios);
        iosText.setVisibility(View.GONE);
        radarLayout = (RadarLayout) findViewById(R.id.radarLayout);
        tv_device_name = (TextView) findViewById(R.id.tv_device_name);
        tv_desc = (TextView) findViewById(R.id.tv_desc);
        tv_top_tip = (TextView) findViewById(R.id.tv_top_tip);
    }

    void getBaseData(){
        Bundle bundle = getIntent().getExtras();
        mIsSendFile = bundle.getBoolean("isSend");
    }

    void init(){
        radarLayout.setUseRing(true);
        radarLayout.setColor(getResources().getColor(R.color.white));
        radarLayout.setCount(4);
        radarLayout.start();

        mWifiAPBroadcastReceiver = new WifiAPBroadcastReceiver() {
            @Override
            public void onWifiApEnabled() {
                Log.i(TAG, "======>>>onWifiApEnabled !!!");
                if(!mIsInitialized){
                    mUdpServerRuannable = createUDPServerRunnable();
                    AppContext.MAIN_EXECUTOR.execute(mUdpServerRuannable);

                    mIsInitialized = true;
                    tv_desc.setText(getResources().getString(R.string.tip_now_init_is_finish));
                    tv_desc.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tv_desc.setText(getResources().getString(R.string.tip_is_waitting_connect));
                        }
                    }, 2*1000);
                }
            }
        };

        IntentFilter filter = new IntentFilter(WifiAPBroadcastReceiver.ACTION_WIFI_AP_STATE_CHANGED);
        registerReceiver(mWifiAPBroadcastReceiver, filter);
        String ssid = TextUtils.isNullOrBlank(android.os.Build.DEVICE) ? Constant.DEFAULT_SSID : android.os.Build.DEVICE;
        MyWifiManager.getInstance(mContext).enableAp(ssid); // enable ap

        tv_device_name.setText(ssid);
        tv_desc.setText(getResources().getString(R.string.tip_now_is_initial));
        tv_top_tip.setText("正在等待苹果设备连接");
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.tv_back){
            onBackPressed();
        }

        //(Todo:jhchen) udp broadcast debug
//        if (viewId == R.id.tv_back){
//            MyWifiManager.getInstance(mContext).printHotIp();
//            new MyWifiManager.UdpBroadCast(Constant.MSG_IOS_ON_CONNECTED).start();
//        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishNormal();
    }

    private void finishNormal(){
        if(mWifiAPBroadcastReceiver != null){
            unregisterReceiver(mWifiAPBroadcastReceiver);
            mWifiAPBroadcastReceiver = null;
        }
        closeSocket();
        //关闭热点
        MyWifiManager.getInstance(mContext).disableAp();
        finish();
    }

    /**
     * 创建发送UDP消息到 文件发送方 的服务线程
     */
    private Runnable createUDPServerRunnable(){
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "start ios UDPReceiverServer ######>>>");
                    startUDPReceiverServer(Constant.DEFAULT_SERVER_COM_PORT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }


    /**
     * 开启 文件接收方 通信服务 (必须在子线程执行)
     * @param serverPort
     * @throws Exception
     */
    DatagramSocket mDatagramSocket;
    private void startUDPReceiverServer(int serverPort) throws Exception{
        byte[] sendData = null;
        mDatagramSocket = new DatagramSocket(null);
        mDatagramSocket.setReuseAddress(true);
        mDatagramSocket.bind(new InetSocketAddress(serverPort));
        byte[] receiveData = new byte[1024];

        while(true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            mDatagramSocket.receive(receivePacket);
            String msg = new String( receivePacket.getData()).trim();
            InetAddress ipAddress = receivePacket.getAddress();
            int udpPort = receivePacket.getPort(); //端口号取到不对应，有待调查，目前先写死
            int port = Constant.DEFAULT_SERVER_COM_PORT;
            if(msg != null && msg.startsWith(Constant.MSG_IOS_ON_CONNECTED)){
                Log.i(TAG, "Get the msg from ios ###### connected>>>" + Constant.MSG_IOS_ON_CONNECTED);
                if (mIsSendFile){
                    //发送文件，通知ios初始化
                    sendData = Constant.MSG_IOS_ON_SERVER_INIT.getBytes(BaseTransfer.UTF_8);
                    new MyWifiManager.UdpSendPacket(mDatagramSocket, sendData, ipAddress, port).start();
                } else {
                    //接收文件，通知ios已经初始化完毕
                    sendData = Constant.MSG_NOTIFY_IOS_ON_SERVER_INIT_SUCCESS.getBytes(BaseTransfer.UTF_8);
                    new MyWifiManager.UdpSendPacket(mDatagramSocket, sendData, ipAddress, port).start();
                    //(Todo:jhchen)启动接收文件服务器
                    mHandler.obtainMessage(MSG_TO_START_IOS_RECEIVE_SERVER).sendToTarget();
                }
            }else if (msg != null && msg.startsWith(Constant.MSG_IOS_ON_SERVER_INIT_SUCCESS)){
                //发送文件，接收ios通知初始化完毕，可以直接开始发送文件
                Log.i(TAG, "Get the msg from ios######>>>" + Constant.MSG_IOS_ON_SERVER_INIT_SUCCESS);
                //(Todo:jhchen) 开始发送文件
                mIOSServerIp = ipAddress.toString();
                mIOSServerIp = mIOSServerIp.substring(mIOSServerIp.lastIndexOf("/") + 1);
                mHandler.obtainMessage(MSG_TO_START_SEND_TO_IOS).sendToTarget();
            } else {
                //broadcast test
                //mHandler.obtainMessage(MSG_TO_RECEIVE_BROADCAST).sendToTarget();
            }
        }
    }

    /**
     * 关闭UDP Socket 流
     */
    private void closeSocket(){
        if(mDatagramSocket != null){
            mDatagramSocket.disconnect();
            mDatagramSocket.close();
            mDatagramSocket = null;
        }
    }
}
