package com.lsj.hdmi.strengthtest.view;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ParallelExecutorCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.lsj.hdmi.strengthtest.R;
import com.lsj.hdmi.strengthtest.model.AcceptThread;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.TreeMap;

/**
 * Created by hdmi on 17-3-9.
 */
public class ResultShowFragment extends Fragment{
    public static String TAG="ResultShowFragment";
    private Button startButton;//开始接收按钮
    private Button blueToothButton;//蓝牙按钮
    private Button clearButton;//重置按钮
    private Button targetSetButton;//目标设置按钮
    private EditText targetEdittext;//目标输入
    private TextView targetTextView;//目标显示
    private TextView lefttargetTextView;//剩余目标显示
    private TextView singleStrengthTextView;//单次力道显示
    private NumberProgressBar allnumberProgressBar;//血槽



    private BluetoothAdapter bluetoothAdapter;//蓝牙适配器
    private AcceptThread acceptThread=null;//
    private clientAcceptThread clientAcceptThread=null;//

    private String adress="";//蓝牙设备地址

    private static boolean firstThread=true;//判断是否是首次启动


    private static BluetoothSocket clientSocket;//蓝牙客户端
    private BluetoothDevice connectDevice;//所要连接的蓝牙设备
    private boolean haConnectDevice=false;
    public static boolean isListener = true;//蓝牙控制参数
    private InputStream is = null;//输入流

    private String allmessage=new String();//消息存储String

    private int allnumber=0;
    private int target=100;//默认目标
    private float lefttarget=100;//默认剩余目标
    private int  singleStrenth=0;
    private int allStrength=0;




    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    if(msg.obj!=null){
                        allmessage=allmessage+msg.obj.toString();
                        int index=allmessage.indexOf('\n');
                        if(index!=-1){
                            String singleStrig =allmessage.substring(0,index);
                            Log.d("Result", "handleMessage: --------------"+singleStrig.trim());
                            allmessage="";
                            Log.d("Result", "handleMessage: --------------"+allmessage);
                            //allStrength+=Integer.parseInt(allStrength+singleStrig);
                            singleStrenth=Integer.parseInt(singleStrig);
                            lefttarget= (float) (lefttarget-singleStrenth*1.0/10);
                            if(lefttarget<=0){
                                lefttarget=0;
                                lefttargetTextView.setText(0+"");
                                allnumberProgressBar.setProgress(0);
                                isListener=false;
                                startButton.setText("开始");
                                setDialog();

                            }else{
                                int allprogresslength= (int) (lefttarget*100/target);
                                DecimalFormat fnum = new DecimalFormat("##0");
                                String dd=fnum.format(lefttarget);
                                lefttargetTextView.setText(dd+"");
                                singleStrengthTextView.setText(singleStrenth*1.0/10+"");
                                allnumberProgressBar.setProgress(allprogresslength);
                            }
                        }
                    }
                    break;
                case 2:
                    Toast.makeText(getActivity(),"蓝牙连接断开",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle=getArguments();
        if(bundle!=null){
            adress=bundle.getString("adress");
            SharedPreferences sp = getActivity().getSharedPreferences("adress", Context.MODE_PRIVATE);
            sp.edit().putString("adress",adress).commit();
            Log.d(TAG, "onCreateView: ----------bundle!=null------"+adress);
        }
        if (adress==null){
            SharedPreferences sp = getActivity().getSharedPreferences("adress", Context.MODE_PRIVATE);
            adress=sp.getString("adress","");
            Log.d(TAG, "onCreateView: ----------adress==null------"+adress);
        }
//        Log.d("Result", "onCreateView: --------------------------"+adress);
//        SharedPreferences sp = getActivity().getSharedPreferences("adress", Context.MODE_PRIVATE);
//        haConnectDevice=sp.getBoolean("connectDevice",false);
        SharedPreferences sp = getActivity().getSharedPreferences("init", Context.MODE_PRIVATE);
        target=sp.getInt("mytarget",100);
        lefttarget=target;
        Log.d("ResultFragment", "onCreateView: -----------------"+adress);
        View view=null;
        view=inflater.inflate(R.layout.fragment_resultshow,null);
        init();
        initView(view);
        return view;
    }

    private void initView(View view){
        getActivity().setTitle("空手道训练");
        startButton= (Button) view.findViewById(R.id.button_start);
        blueToothButton= (Button) view.findViewById(R.id.button_buletooth);
        allnumberProgressBar = (NumberProgressBar) view.findViewById(R.id.all_progressbar);
        allnumberProgressBar.setProgress(100);
        clearButton= (Button) view.findViewById(R.id.button_clear);
        targetSetButton= (Button) view.findViewById(R.id.target_button);
        targetEdittext= (EditText) view.findViewById(R.id.target_edittext);
        targetTextView= (TextView) view.findViewById(R.id.target_textview);
        lefttargetTextView= (TextView) view.findViewById(R.id.lefttarget_textview);
        singleStrengthTextView= (TextView) view.findViewById(R.id.singlestrength_textvew);


        lefttargetTextView.setText(target+"");
        targetTextView.setText(target+"");
        targetEdittext.setInputType( InputType.TYPE_CLASS_NUMBER);
        targetSetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mytarget=targetEdittext.getText().toString();
                if (!TextUtils.isEmpty(mytarget)){
                    try {
                        if (Integer.parseInt(mytarget) <=50000) {
                            targetTextView.setText(mytarget);
                            lefttargetTextView.setText(mytarget);
                            target = Integer.parseInt(mytarget);
                            lefttarget = Float.parseFloat(mytarget);
                            allnumberProgressBar.setProgress(100);
                            SharedPreferences sp = getActivity().getSharedPreferences("init", Context.MODE_PRIVATE);
                            sp.edit().putInt("mytarget", Integer.parseInt(mytarget)).commit();
                        } else {
                            Toast.makeText(getActivity(), "最大值为50000", Toast.LENGTH_SHORT).show();
                        }
                    }catch (NumberFormatException e){
                        Toast.makeText(getActivity(), "最大值为50000", Toast.LENGTH_SHORT).show();
                    }


                }
            }
        });

        //重置按钮
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBluetooth();
                if (bluetoothAdapter.isEnabled()){
//                    if (acceptThread==null){
//                        acceptThread=new AcceptThread(bluetoothAdapter, handler, new AcceptThread.OnBluetoothListener() {
//                            @Override
//                            public void disconnect() {
//                                startButton.setText("开始");
//                            }
//                        });
//                    }
//                    if(firstThread){
//                        acceptThread.start();
//                        startButton.setText("暂停");
//                    }
//                    if(!firstThread){
//                        if(AcceptThread.isListener){
//                            AcceptThread.isListener=false;
//                            startButton.setText("开始");
//                            acceptThread.cancle();
//
//                        }else {
//                            AcceptThread.isListener=true;
//                            startButton.setText("暂停");
//                        }
//                    }
//                    firstThread=false;
                    if(!TextUtils.isEmpty(adress)){
                        if(connectDevice==null){
                            clientAcceptThread=new clientAcceptThread(handler);
                        }
                        if (firstThread){
                            clientAcceptThread.start();
                            startButton.setText("暂停");
                        }
                        if(!firstThread){
                            if(isListener){
                                isListener=false;
                                startButton.setText("开始");
                            }else {
                                new clientAcceptThread(handler).start();
                                isListener=true;
                                startButton.setText("暂停");
                            }
                        }
                        firstThread=false;
                    }

                }
            }
        });


        //启动蓝牙界面
        blueToothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBluetooth();
                if(bluetoothAdapter.isEnabled()){
                    Fragment  fragment=new BlueToothSearchFragment();
                    getActivity().getFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.container,fragment).commit();
                }

            }
        });
    }
    //重置
    private void reset(){
        targetTextView.setText(target+"");
        lefttargetTextView.setText(target+"");
        singleStrengthTextView.setText(0+"");
        allnumberProgressBar.setProgress(100);
        lefttarget=target;
        allStrength=0;
        singleStrenth=0;
    }
    private void setDialog(){
        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        builder.setMessage("您已完成了训练!");
        builder.setTitle("成功!");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                reset();
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void init(){
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        isListener=true;
        firstThread=true;
    }

    //检查蓝牙是否打开
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==1){
            switch (resultCode){
                case Activity.RESULT_OK:
                    Fragment  fragment=new BlueToothSearchFragment();
                    getActivity().getFragmentManager().beginTransaction().replace(R.id.container,fragment).commit();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(getActivity(),"无法打开蓝牙",Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }


    public class clientAcceptThread extends Thread {
        Handler handler;
        public clientAcceptThread(Handler handler){
            this.handler=handler;
            try {
                //获取设备并进行连接
                connectDevice = bluetoothAdapter.getRemoteDevice(adress);
                clientSocket = connectDevice.createRfcommSocketToServiceRecord(BlueToothSearchFragment.MyUUID);
                bluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "clientAcceptThread: isdiscovering"+bluetoothAdapter.isDiscovering());
                clientSocket.connect();
                is = clientSocket.getInputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        @Override
        public void run() {
            Log.d("Result", "run: --------------------run"+isListener);
            while (isListener) {
                    byte[] buffer = new byte[128];
                    int count = 0;
                    try {
                        count = is.read(buffer);
                        Message msg = new Message();
                        msg.obj =new String(buffer,0,count);
                        //String message=msg.obj.toString();
                        msg.what = 1;
                        if(!TextUtils.isEmpty(msg.obj.toString())&&!msg.obj.toString().equals(" ")&&msg.obj!=null&&msg.obj.toString().length()<5){
                            handler.sendMessage(msg);
                        }
                    } catch (IOException e) {
                        isListener = false;
                        Log.d("bluetoothConnect", "run: -------------蓝牙断开链接------------");
                        Message msg=new Message();
                        msg.what=2;
                        handler.sendMessage(msg);
                        e.printStackTrace();
                    }

                }
            try {
                if(is!=null){
                    is.close();
                    clientSocket.close();
                    //isListener = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static String asciiToString(String value)
    {
        StringBuffer sbu = new StringBuffer();
        String[] chars = value.split(",");
        for (int i = 0; i < chars.length; i++) {
            sbu.append((char) Integer.parseInt(chars[i]));
        }
        return sbu.toString();
    }

    @Override
    public void onDestroy() {
        Log.d("ResultShow", "onDestroy: ------------------");
       isListener=false;
        SharedPreferences sp = getActivity().getSharedPreferences("adress", Context.MODE_PRIVATE);
        sp.edit().putString("adress","").commit();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        Log.d("ResultShow", "onStop: ------------------");
        //切换蓝牙模块时停止监听，关闭流
        isListener=false;
        if(is!=null&&clientSocket!=null){
            try {
                is.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        super.onStop();
    }

}
