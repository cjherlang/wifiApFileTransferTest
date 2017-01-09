package io.github.mayubao.kuaichuan.core.iOS;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.mayubao.kuaichuan.AppContext;
import io.github.mayubao.kuaichuan.Constant;
import io.github.mayubao.kuaichuan.R;
import io.github.mayubao.kuaichuan.core.MyWifiManager;
import io.github.mayubao.kuaichuan.core.entity.FileInfo;

/**
 * Created by jhchen on 2017/1/4.
 */

public class IOSFileSenderActivity extends AppCompatActivity implements View.OnClickListener{

    Context mContext;
    String mServerIp = "";
    List<IOSFileSender> mFileSenderList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_sender);
        mContext = this;
        getBaseData();
        findViewById();
        init();
    }

    public void getBaseData(){
        Bundle bundle = getIntent().getExtras();
        mServerIp = bundle.getString("serverIp");
    }

    public void findViewById(){
        TextView backText = (TextView) findViewById(R.id.tv_back);
        backText.setOnClickListener(this);
    }

    public void init(){
        List<Map.Entry<String, FileInfo>> fileInfoMapList = new ArrayList<Map.Entry<String, FileInfo>>(AppContext.getAppContext().getFileInfoMap().entrySet());
        Collections.sort(fileInfoMapList, Constant.DEFAULT_COMPARATOR);
        String serverIp = MyWifiManager.getInstance(mContext).getIpAddressFromHotspot();
        for(Map.Entry<String, FileInfo> entry : fileInfoMapList){
            final FileInfo fileInfo = entry.getValue();
            IOSFileSender fileSender = new IOSFileSender(mContext, fileInfo, serverIp, Constant.DEFAULT_SERVER_PORT);
            fileSender.setOniOSFileSendListener(new IOSFileSender.OnIOSFileSendListener() {
                @Override
                public void onStart() {

                }

                @Override
                public void onProgress(long progress, long total) {

                }

                @Override
                public void onSuccess(FileInfo fileInfo) {
                    //=====更新进度 流量 时间视图 start ====//

                }

                @Override
                public void onFailure(Throwable t, FileInfo fileInfo) {

                }
            });
            mFileSenderList.add(fileSender);
            AppContext.FILE_SENDER_EXECUTOR.execute(fileSender);
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.tv_back){
            onBackPressed();
        }
    }

    /**
     * 退出处理
     */
    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        //需要判断是否有文件在发送？
        if(hasFileSending()){
            showExistDialog();
            return;
        }

        finishNormal();
    }

    /**
     * 判断是否有文件在传送
     */
    private boolean hasFileSending(){
        for(IOSFileSender fileSender : mFileSenderList){
            if(fileSender.isRunning()){
                return true;
            }
        }
        return false;
    }

    /**
     * 显示是否退出 对话框
     */
    private void showExistDialog(){
//        new AlertDialog.Builder(getContext())
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(getResources().getString(R.string.tip_now_has_task_is_running_exist_now))
                .setPositiveButton(getResources().getString(R.string.str_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishNormal();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.str_no), null)
                .create()
                .show();
    }

    /**
     * 正常退出
     */
    private void finishNormal(){
//        AppContext.FILE_SENDER_EXECUTOR.
        stopAllFileSendingTask();
        AppContext.getAppContext().getFileInfoMap().clear();
        MyWifiManager.getInstance(mContext).disableAp();
        finish();
    }

    /**
     * 停止所有的文件发送任务
     */
    private void stopAllFileSendingTask(){
        for(IOSFileSender fileSender : mFileSenderList){
            if(fileSender != null){
                fileSender.finish();
                fileSender.stop();
            }
        }
    }
}
