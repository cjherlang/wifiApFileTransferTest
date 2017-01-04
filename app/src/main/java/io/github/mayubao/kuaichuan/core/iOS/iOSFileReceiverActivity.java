package io.github.mayubao.kuaichuan.core.iOS;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import io.github.mayubao.kuaichuan.R;

/**
 * Created by jhchen on 2017/1/4.
 */

public class iOSFileReceiverActivity extends AppCompatActivity{
    Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_file_receiver);
        init();
    }

    public void init(){
        new FileReceiveServer(mContext, "").execute();
    }
}
