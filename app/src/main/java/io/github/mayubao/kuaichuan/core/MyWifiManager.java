package io.github.mayubao.kuaichuan.core;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mayubao on 2016/11/2.
 * Contact me 345269374@qq.com
 */
public class MyWifiManager {

    /**
     * 创建WifiConfiguration的类型
     */
    public static final int WIFICIPHER_NOPASS = 1;
    public static final int WIFICIPHER_WEP = 2;
    public static final int WIFICIPHER_WPA = 3;


    private static MyWifiManager mWifiMgr;
    private Context mContext;
    private WifiManager mWifiManager;

    //scan the result
    List<ScanResult> mScanResultList;
    List<WifiConfiguration> mWifiConfigurations;


    //current wifi configuration info
    WifiInfo mWifiInfo;

    private MyWifiManager(Context context){
        this.mContext = context;
        this.mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public static MyWifiManager getInstance(Context context){
        if(mWifiMgr == null){
            synchronized (MyWifiManager.class){
                if(mWifiMgr == null){
                    mWifiMgr = new MyWifiManager(context);
                }
            }
        }

        return mWifiMgr;
    }

    public List<ScanResult> getScanResultList() {
        return mScanResultList;
    }

    public List<WifiConfiguration> getWifiConfigurations() {
        return mWifiConfigurations;
    }

    /**
     * 打开wifi
     */
    public void openWifi(){
        if(isApOn()){
            disableAp();
        }
        if(!mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
        }
    }

    /**
     * 关闭wifi
     */
    public void closeWifi(){
        if(mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(false);
        }
    }


    /**
     * 判断wifi是否开启的状态
     * @return
     */
    public  boolean isWifiEnable(){
        return mWifiManager == null ? false : mWifiManager.isWifiEnabled();
    }

    //check whether wifi hotspot on or off
    public boolean isApOn() {
        try {
            Method method = mWifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(mWifiManager);
        }
        catch (Throwable ignored) {}
        return false;
    }

    //close wifi hotspot
    public void disableAp() {
        try {
            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(mWifiManager, null, false);
        } catch (Throwable ignored) {

        }
    }

    public boolean enableAp(String apName) {
        //打开热点要先关闭wifi和原来的热点
        closeWifi();
        if (isApOn()){
            disableAp();
        }

        WifiConfiguration wificonfiguration = null;
        try {
            wificonfiguration = MyWifiManager.createWifiCfg(apName, "", 0);
            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(mWifiManager, wificonfiguration, true);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * wifi扫描
     */
    public void startScan(){
        mWifiManager.startScan();
        mScanResultList = mWifiManager.getScanResults();
        mWifiConfigurations = mWifiManager.getConfiguredNetworks();
    }

    /**
     * 添加到指定Wifi网络 /切换到指定Wifi网络
     * @param wf
     * @return
     */
    public boolean addNetwork(WifiConfiguration wf){
        //断开当前的连接
        disconnectCurrentNetwork();

        //连接新的连接
        int netId = mWifiManager.addNetwork(wf);
        boolean enable = mWifiManager.enableNetwork(netId, true);
        return enable;
    }

    /**
     * 关闭当前的Wifi网络
     * @return
     */
    public boolean disconnectCurrentNetwork(){
        if(mWifiManager != null && mWifiManager.isWifiEnabled()){
            int netId = mWifiManager.getConnectionInfo().getNetworkId();
            mWifiManager.disableNetwork(netId);
            return mWifiManager.disconnect();
        }
        return false;
    }

    /**
     * 创建WifiConfiguration
     *
     * @param ssid
     * @param password
     * @param type
     * @return
     */
    static public WifiConfiguration createWifiCfg(String ssid, String password, int type){
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = ssid ;
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.preSharedKey = null;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP,false);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK,false);
        return config;
    }


    /**
     * 获取当前WifiInfo
     * @return
     */
    public WifiInfo getWifiInfo(){
        mWifiInfo = mWifiManager.getConnectionInfo();
        return mWifiInfo;
    }

    /**
     * 获取当前Wifi所分配的Ip地址
     * @return
     */
//  when connect the hotspot, is still returning "0.0.0.0".
    public String getCurrentIpAddress(){
        String ipAddress = "";
        int address= mWifiManager.getDhcpInfo().ipAddress;
        ipAddress = ((address & 0xFF)
                + "." + ((address >> 8) & 0xFF)
                + "." + ((address >> 16) & 0xFF)
                + "." + ((address >> 24) & 0xFF));
        return ipAddress;
    }


    /**
     * 设备连接Wifi之后， 设备获取Wifi热点的IP地址
     * @return
     */
    public String getIpAddressFromHotspot(){
//        String ipAddress = "192.168.43.1";
        String ipAddress;
        int address = 0;
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();//动态分配器信息
        int gateway = dhcpInfo.gateway;  //动态分配器网关，一般等于serverAddress
        int serverAddress = dhcpInfo.serverAddress;  //连接的ap的ip
        if (gateway != 0){
            address = gateway;
        } else {
            address = serverAddress;
        }
        ipAddress = ((address & 0xFF)
                + "." + ((address >> 8) & 0xFF)
                + "." + ((address >> 16) & 0xFF)
                + "." + ((address >> 24) & 0xFF));
        return ipAddress;
    }


    /**
     * 开启热点之后，获取自身热点的IP地址
     * @return
     */
    public String getHotspotLocalIpAddress(){
        // WifiAP ip address is hardcoded in Android.
        /* IP/netmask: 192.168.43.1/255.255.255.0 */
        String ipAddress = "192.168.43.1";
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
        int address = dhcpInfo.serverAddress;
        ipAddress = ((address & 0xFF)
                + "." + ((address >> 8) & 0xFF)
                + "." + ((address >> 16) & 0xFF)
                + "." + ((address >> 24) & 0xFF));
        return ipAddress;
    }

    /**
     * 读取连接的热备的ip，由于要读取文件，所以不建议使用，仅仅用来测试查看
     * @return
     */
    private ArrayList<String> getConnectedHotIP() {
        ArrayList<String> connectedIP = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    "/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String ip = splitted[0];
                    connectedIP.add(ip);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connectedIP;
    }

    //输出链接到当前设备的IP地址
    public void printHotIp() {
        ArrayList<String> connectedIP = getConnectedHotIP();
        StringBuilder resultList = new StringBuilder();
        for (String ip : connectedIP) {
            resultList.append(ip);
            resultList.append("\n");
        }
        System.out.print("printHotIp ==== ip \n");
        System.out.print("wifiAp:" + getIpAddressFromHotspot() + "\n");
        System.out.print(resultList);
    }
}
