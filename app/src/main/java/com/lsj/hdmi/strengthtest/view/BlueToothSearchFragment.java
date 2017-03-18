package com.lsj.hdmi.strengthtest.view;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.lsj.hdmi.strengthtest.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by hdmi on 17-3-9.
 */
public class BlueToothSearchFragment extends Fragment {
    public static final UUID MyUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private Activity activity;
    private ListView listView;//设备列表
    private Button searchStartButton;//扫描蓝牙按钮
    private Button searchCancleButton;//取消扫描/后退按钮

    private BroadcastReceiver register;//蓝牙广播接收器
    private BluetoothAdapter bluetoothAdapter;//蓝牙适配器
    private ArrayAdapter<String> deviceAdapter;//listView设备适配器
    private List<String> devices;//设备列表,String
    private List<BluetoothDevice> deviceList;//设备列表,设备

    private BluetoothDevice connectDevice;//选择连接的设备
    private static final String TAG = "BluetoothSearchFragment";

    private static BluetoothSocket clientSocket;
    private OutputStream os = null;
    private InputStream is = null;
    private String adress;
    public static boolean isListener = true;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        view = inflater.inflate(R.layout.fragment_bluetooth_search, null);
        peimissionRequest();
        initView(view);
        initRegisit();
        return view;
    }

    //android 6.0后需要跟用户要求定位权限，才能扫描到蓝牙
    private void peimissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
    }

    //注册蓝牙扫描广播
    private void initRegisit() {
        register = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "onReceive: ------------found--------------");
                    String str = device.getName() + "|" + device.getAddress();
                    devices.add(str);
                    deviceList.add(device);
                    deviceAdapter.notifyDataSetChanged();
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    activity.setTitle("蓝牙扫描");
                }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                    activity.setTitle("扫描中...");
                }
            }
        };

        //扫描到蓝牙的意图过滤器
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(register, filter);
        //结束扫描蓝牙的意图过滤器
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getActivity().registerReceiver(register, filter);
        //开始扫描蓝牙的意图过滤器
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        getActivity().registerReceiver(register,filter);
    }

    private void initView(View view) {
        getActivity().setTitle("蓝牙扫描");
        listView = (ListView) view.findViewById(R.id.bluetooth_listview);
        searchStartButton = (Button) view.findViewById(R.id.search_start);
        searchCancleButton = (Button) view.findViewById(R.id.search_cancle);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        devices = new ArrayList<String>();
        deviceList = new ArrayList<BluetoothDevice>();
        deviceAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, devices);
        listView.setAdapter(deviceAdapter);
        listView.setOnItemClickListener(new ItemclickListener());


        searchStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断蓝牙是否打开
                if(bluetoothAdapter.isEnabled()){
                    //如果设备信息不为空,先清空先前的设备列表
                    if (devices != null) {
                        Log.d(TAG, "onClick: beforesearch-------------device-" + devices.toString());
                        devices.clear();
                        if (deviceAdapter != null) {
                            deviceAdapter.notifyDataSetChanged();
                        }
                    }
                    //如果先前蓝牙在扫描中,则停止扫描
                    if(bluetoothAdapter.isDiscovering()){
                        bluetoothAdapter.cancelDiscovery();
                    }
                    bluetoothAdapter.startDiscovery();
                }else{
                    checkBluetooth();
                }


            }
        });

        searchCancleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //如果当前在扫描蓝牙,则停止
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();

                }
                //如果当前没在扫描蓝牙,则返回上一个界面
                else {
                    getActivity().getFragmentManager().beginTransaction().replace(R.id.container, new ResultShowFragment()).commit();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        //取消广播接收器注册
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(register);
        super.onDestroy();
    }

    //请求权限回应结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO request success
                    Log.d(TAG, "onRequestPermissionsResult: permissionsuccessfully----------------");
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: permissionfailed----------------");
                }

                break;
        }
    }

    public class ItemclickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //检查蓝牙是否打开
            if(bluetoothAdapter.isEnabled()){
                //获取设备mac地址
                String deviceInfo = devices.get(position);
                adress = deviceInfo.substring(deviceInfo.indexOf("|") + 1).trim();
                Log.d(TAG, "onItemClick: ---------------------" + adress);
                //取消扫描蓝牙
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                //根据mac地址获取设备
                if (connectDevice == null) {
                    connectDevice = bluetoothAdapter.getRemoteDevice(adress);
                    Log.d(TAG, "onItemClick: ------------remotedevice---------" + connectDevice.getName());
                }
                //如果设备不为空则返回上一个界面
                if(connectDevice!=null){
                    Fragment resultfragment=new ResultShowFragment();
                    Bundle bundle=new Bundle();
                    bundle.putString("adress",adress);
                    resultfragment.setArguments(bundle);
                    getActivity().getFragmentManager().beginTransaction().replace(R.id.container,resultfragment).commit();
                }
            }else{
                checkBluetooth();
            }

        }
    }

    private void checkBluetooth(){


        if(bluetoothAdapter==null){
            Toast.makeText(getActivity(),"该设备不支持蓝牙!",Toast.LENGTH_LONG).show();
        }
        else{
            if (!bluetoothAdapter.isEnabled()){
                Intent bluetoothIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                bluetoothIntent.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                bluetoothIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,3600);
                startActivityForResult(bluetoothIntent,1);


            }
        }
    }

    //测试方法,客户端发送消息
    private void sendmessage(final String string) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (clientSocket == null) {
                    try {
                        clientSocket = connectDevice.createRfcommSocketToServiceRecord(MyUUID);
                        clientSocket.connect();
                        if (clientSocket.isConnected()) {
                            Log.d(TAG, "onItemClick: ------------clientSocketIsAlive---------");
                        }
                        os = clientSocket.getOutputStream();

                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            clientSocket.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }

                }
                if (os != null) {
                    try {
                        Log.d(TAG, "onItemClick: ------------osWrite---------");
                        os.write(string.getBytes("utf-8"));
                        os.flush();
                    } catch (IOException e) {
                        try {
                            clientSocket = connectDevice.createRfcommSocketToServiceRecord(MyUUID);
                            clientSocket.connect();
                            os = clientSocket.getOutputStream();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "onItemClick: -----------osIsNULL");
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    clientSocket = null;
                }
            }
        }).start();
    }

    //客户端接收消息线程
    public class clientAcceptThread extends Thread {
        Handler handler;
        public clientAcceptThread(Handler handler){
            try {
                clientSocket = connectDevice.createRfcommSocketToServiceRecord(MyUUID);
                clientSocket.connect();
                is=clientSocket.getInputStream();
            } catch (IOException e) {
                isListener=false;
                e.printStackTrace();
            }

        }
        @Override
        public void run() {
            while (isListener) {
                byte[] buffer = new byte[128];
                int count = 0;
                try {
                    count = is.read(buffer);
                    Message msg = new Message();
                    msg.obj = new String(buffer, 0, count, "utf-8");
                    msg.what = 1;
                    handler.sendMessage(msg);
                } catch (IOException e) {
                    isListener=false;
                    e.printStackTrace();
                }
            }
            isListener=true;
        }
    }
}
