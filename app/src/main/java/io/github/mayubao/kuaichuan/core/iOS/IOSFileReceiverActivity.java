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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import io.github.mayubao.kuaichuan.AppContext;
import io.github.mayubao.kuaichuan.Constant;
import io.github.mayubao.kuaichuan.R;
import io.github.mayubao.kuaichuan.core.MyWifiManager;
import io.github.mayubao.kuaichuan.core.entity.FileInfo;
import io.github.mayubao.kuaichuan.core.utils.FileUtils;
import io.github.mayubao.kuaichuan.core.utils.ToastUtils;
import io.github.mayubao.kuaichuan.ui.adapter.FileReceiverAdapter;

/**
 * Created by jhchen on 2017/1/4.
 */

public class IOSFileReceiverActivity extends AppCompatActivity implements View.OnClickListener{
    public static String TAG = IOSFileReceiverActivity.class.getSimpleName();

    /**
     * 进度条 已传 耗时等UI组件
     */
    TextView tv_title;
    ProgressBar pb_total;
    TextView tv_value_storage;
    TextView tv_unit_storage;
    TextView tv_value_time;
    TextView tv_unit_time;

    ListView lv_result;
    FileReceiverAdapter mFileReceiverAdapter;

    long mTotalLen = 0;     //所有总文件的进度
    long mCurOffset = 0;    //每次传送的偏移量
    long mLastUpdateLen = 0; //每个文件传送onProgress() 之前的进度
    String[] mStorageArray = null;

    long mTotalTime = 0;
    long mCurTimeOffset = 0;
    long mLastUpdateTime = 0;
    String[] mTimeArray = null;

    int mHasSendedFileCount = 0;

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
                //ADD FileInfo 到 Adapter
                updateTotalProgressView();
                if(mFileReceiverAdapter != null) mFileReceiverAdapter.update();
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
        startServer();
    }

    public void findViewById(){
        TextView backText = (TextView) findViewById(R.id.tv_back);
        backText.setOnClickListener(this);
        tv_title = (TextView) findViewById(R.id.tv_title);
        pb_total = (ProgressBar) findViewById(R.id.pb_total);
        tv_value_storage = (TextView) findViewById(R.id.tv_value_storage);
        tv_unit_storage = (TextView) findViewById(R.id.tv_unit_storage);
        tv_value_time = (TextView) findViewById(R.id.tv_value_time);
        tv_unit_time = (TextView) findViewById(R.id.tv_unit_time);

        lv_result = (ListView) findViewById(R.id.lv_result);
    }

    public void init(){
        //界面初始化
        tv_title.setVisibility(View.VISIBLE);
        tv_title.setText(getResources().getString(R.string.title_file_transfer));
        mFileReceiverAdapter = new FileReceiverAdapter(mContext);
        lv_result.setAdapter(mFileReceiverAdapter);
    }

    public void startServer(){
        mReceiverServer = new IOSServerRunnable();
        new Thread(mReceiverServer).start();
    }

    /**
     * 更新进度 和 耗时的 View
     */
    private void updateTotalProgressView() {
        try{
            //设置传送的总容量大小
            mStorageArray = FileUtils.getFileSizeArrayStr(mTotalLen);
            tv_value_storage.setText(mStorageArray[0]);
            tv_unit_storage.setText(mStorageArray[1]);

            //设置传送的时间情况
            mTimeArray = FileUtils.getTimeByArrayStr(mTotalTime);
            tv_value_time.setText(mTimeArray[0]);
            tv_unit_time.setText(mTimeArray[1]);


            //设置传送的进度条情况
            if(mHasSendedFileCount == AppContext.getAppContext().getReceiverFileInfoMap().size()){
                pb_total.setProgress(0);
                tv_value_storage.setTextColor(getResources().getColor(R.color.color_yellow));
                tv_value_time.setTextColor(getResources().getColor(R.color.color_yellow));
                return;
            }

            long total = AppContext.getAppContext().getAllReceiverFileInfoSize();
            int percent = (int)(mTotalLen * 100 /  total);
            pb_total.setProgress(percent);

            if(total  == mTotalLen){
                pb_total.setProgress(0);
                tv_value_storage.setTextColor(getResources().getColor(R.color.color_yellow));
                tv_value_time.setTextColor(getResources().getColor(R.color.color_yellow));
            }
        }catch (Exception e){
            //convert storage array has some problem
        }
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
                            mLastUpdateLen = 0;
                            mLastUpdateTime = System.currentTimeMillis();
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
                            mCurOffset = progress - mLastUpdateLen > 0 ? progress - mLastUpdateLen : 0;
                            mTotalLen = mTotalLen + mCurOffset;
                            mLastUpdateLen = progress;

                            mCurTimeOffset = System.currentTimeMillis() - mLastUpdateTime > 0 ? System.currentTimeMillis() - mLastUpdateTime : 0;
                            mTotalTime = mTotalTime + mCurTimeOffset;
                            mLastUpdateTime = System.currentTimeMillis();
                            //=====更新进度 流量 时间视图 end ====//

                            mCurFileInfo.setProcceed(progress);
                            AppContext.getAppContext().updateReceiverFileInfo(mCurFileInfo);
                            mHandler.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                        }

                        @Override
                        public void onSuccess(FileInfo fileInfo) {
                            //=====更新进度 流量 时间视图 start ====//
                            mHasSendedFileCount ++;

                            mTotalLen = mTotalLen + (fileInfo.getSize() - mLastUpdateLen);
                            mLastUpdateLen = 0;
                            mLastUpdateTime = System.currentTimeMillis();
                            //=====更新进度 流量 时间视图 end ====//

                            fileInfo.setResult(FileInfo.FLAG_SUCCESS);
                            AppContext.getAppContext().updateReceiverFileInfo(fileInfo);
                            mHandler.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                        }

                        @Override
                        public void onFailure(Throwable t, FileInfo fileInfo) {
                            mHasSendedFileCount ++;//统计发送文件

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
