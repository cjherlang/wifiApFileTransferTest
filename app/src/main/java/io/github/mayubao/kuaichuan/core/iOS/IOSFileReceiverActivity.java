package io.github.mayubao.kuaichuan.core.iOS;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import io.github.mayubao.kuaichuan.AppContext;
import io.github.mayubao.kuaichuan.Constant;
import io.github.mayubao.kuaichuan.R;
import io.github.mayubao.kuaichuan.core.MyWifiManager;
import io.github.mayubao.kuaichuan.core.entity.FileInfo;
import io.github.mayubao.kuaichuan.core.utils.ToastUtils;

/**
 * Created by jhchen on 2017/1/4.
 */

public class IOSFileReceiverActivity extends AppCompatActivity implements View.OnClickListener{
    public static String TAG = IOSFileReceiverActivity.class.getSimpleName();

    Context mContext;
    FileInfo mCurFileInfo;

    IOSServerRunnable mReceiverServer;



    public static final int MSG_ADD_FILE_INFO = 0X5555;
    public static final int MSG_UPDATE_FILE_INFO = 0X6666;
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MSG_ADD_FILE_INFO){
                //ADD FileInfo 到 Adapter
                FileInfo fileInfo = (FileInfo) msg.obj;
                ToastUtils.show(mContext, "收到一个任务：" + (fileInfo != null ? fileInfo.getFilePath() : ""));
            }else if(msg.what == MSG_UPDATE_FILE_INFO){
                //ADD FileInfo 到 Adapter

            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_file_receiver);
        findViewById();
        init();
    }

    public void findViewById(){
        TextView backText = (TextView) findViewById(R.id.tv_back);
        backText.setOnClickListener(this);
    }

    public void init(){
        mReceiverServer = new IOSServerRunnable();
        new Thread(mReceiverServer).start();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.tv_back){
            finishNormal();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishNormal();
    }

    public void finishNormal(){
        //清除选中文件的信息
        //关闭热点
        if(mReceiverServer != null){
            mReceiverServer.close();
            mReceiverServer = null;
        }

        AppContext.getAppContext().getReceiverFileInfoMap().clear();

        MyWifiManager.getInstance(mContext).disableAp();
        finish();
    }

    /**
     * ServerSocket启动线程
     */
    class IOSServerRunnable implements Runnable{
        ServerSocket serverSocket;

        @Override
        public void run() {
            Log.i(TAG, "------>>>Socket已经开启");
            try {
                serverSocket = new ServerSocket(Constant.DEFAULT_SERVER_PORT);
                while (!Thread.currentThread().isInterrupted()){
                    Socket socket = serverSocket.accept();

                    IOSFileReceiver fileReceiver = new IOSFileReceiver(socket);
                    fileReceiver.setOniOSFileReceiveListener(new IOSFileReceiver.OniOSFileReceiveListener() {
                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onGetFileInfo(FileInfo fileInfo) {
                            mHandler.obtainMessage(MSG_ADD_FILE_INFO, fileInfo).sendToTarget();
                            mCurFileInfo = fileInfo;
                            AppContext.getAppContext().addReceiverFileInfo(mCurFileInfo);
                            mHandler.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                        }

                        @Override
                        public void onGetScreenshot(Bitmap bitmap) {

                        }

                        @Override
                        public void onProgress(long progress, long total) {
                            //=====更新进度 流量 时间视图 start ====//
                        }

                        @Override
                        public void onSuccess(FileInfo fileInfo) {
                            //=====更新进度 流量 时间视图 start ====//

                            //=====更新进度 流量 时间视图 end ====//
                            fileInfo.setResult(FileInfo.FLAG_SUCCESS);
                            AppContext.getAppContext().updateReceiverFileInfo(fileInfo);
                            mHandler.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                        }

                        @Override
                        public void onFailure(Throwable t, FileInfo fileInfo) {
                            fileInfo.setResult(FileInfo.FLAG_FAILURE);
                            AppContext.getAppContext().updateFileInfo(fileInfo);
                            mHandler.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                        }
                    });

//                    mFileReceiver = fileReceiver;
//                    new Thread(fileReceiver).start();
                    AppContext.getAppContext().MAIN_EXECUTOR.execute(fileReceiver);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * 关闭Socket 通信 (避免端口占用)
         */
        public void close(){
            if(serverSocket != null){
                try {
                    serverSocket.close();
                    serverSocket = null;
                } catch (IOException e) {
                }
            }
        }
    }
}
