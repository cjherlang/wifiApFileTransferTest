package io.github.mayubao.kuaichuan.core.iOS;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.mayubao.kuaichuan.AppContext;
import io.github.mayubao.kuaichuan.Constant;
import io.github.mayubao.kuaichuan.R;
import io.github.mayubao.kuaichuan.core.entity.FileInfo;

/**
 * Created by jhchen on 2017/1/4.
 */

public class iOSFileSenderActivity extends AppCompatActivity{

    Context mContext;
    String mServerIp = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_sender);
        mContext = this;
        getBaseData();
        init();
    }

    public void getBaseData(){
        Bundle bundle = getIntent().getExtras();
        mServerIp = bundle.getString("serverIp");
    }

    public void init(){
        List<Map.Entry<String, FileInfo>> fileInfoMapList = new ArrayList<Map.Entry<String, FileInfo>>(AppContext.getAppContext().getFileInfoMap().entrySet());
        Collections.sort(fileInfoMapList, Constant.DEFAULT_COMPARATOR);
        if (fileInfoMapList.isEmpty()){
            return;
        }
        FileInfo fileInfo = fileInfoMapList.get(0).getValue();

        Intent serviceIntent = new Intent(mContext, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, fileInfo.getFilePath());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, mServerIp);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, Constant.DEFAULT_SERVER_PORT);
        mContext.startService(serviceIntent);
    }
}
