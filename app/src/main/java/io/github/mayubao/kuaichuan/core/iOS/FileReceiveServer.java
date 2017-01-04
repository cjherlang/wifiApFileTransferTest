package io.github.mayubao.kuaichuan.core.iOS;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import io.github.mayubao.kuaichuan.Constant;

/**
 * Created by jhchen on 2016/12/23.
 */

public class FileReceiveServer extends AsyncTask<Void, Void, String> {
    private static String TAG = FileReceiveServer.class.getSimpleName();


    private Context context;
    private String mPath;

    /**
     * @param context
     * @param path
     */
    public FileReceiveServer(Context context, String path) {
        this.context = context;
        mPath = path;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            ServerSocket serverSocket = new ServerSocket(Constant.DEFAULT_SERVER_PORT);
            Log.d(FileReceiveServer.TAG, "Server: Socket opened");
            Socket client = serverSocket.accept();
            Log.d(FileReceiveServer.TAG, "Server: connection done");
            final File f = new File(Environment.getExternalStorageDirectory() + "/"
                    + "wifitest/" + "/wifip2pshared-" + System.currentTimeMillis()
                    + ".jpg");

            File dirs = new File(f.getParent());
            if (!dirs.exists())
                dirs.mkdirs();
            f.createNewFile();

            Log.d(FileReceiveServer.TAG, "server: copying files " + f.toString());
            InputStream inputstream = client.getInputStream();
            //copyFile(inputstream, new FileOutputStream(f));
            OutputStream outputStream = new ByteArrayOutputStream(2048);
            copyFile(inputstream, outputStream);
            String string = outputStream.toString();
            Log.d(TAG, "doInBackground: " + string);
            serverSocket.close();
            return f.getAbsolutePath();
        } catch (IOException e) {
            Log.e(FileReceiveServer.TAG, e.getMessage());
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + result), "image/*");
            context.startActivity(intent);
        }

    }

    /*
     * (non-Javadoc)
     * @see android.os.AsyncTask#onPreExecute()
     */
    @Override
    protected void onPreExecute() {
        Toast.makeText(context, "Opening a server socket", Toast.LENGTH_LONG);
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d("WifiP2PUtils", e.toString());
            return false;
        }
        return true;
    }
}
