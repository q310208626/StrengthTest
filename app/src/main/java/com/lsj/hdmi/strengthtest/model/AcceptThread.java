package com.lsj.hdmi.strengthtest.model;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.lsj.hdmi.strengthtest.view.BlueToothSearchFragment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;

/**
 * Created by hdmi on 17-3-10.
 */
public class AcceptThread extends Thread {
    public static String TAG="AcceptThread";
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    public static boolean isListener;
    private OnBluetoothListener onBluetoothListener;

    public AcceptThread(BluetoothAdapter bluetoothAdapter, Handler handler,OnBluetoothListener onBluetoothListener){
            this.onBluetoothListener=onBluetoothListener;
            this.bluetoothAdapter=bluetoothAdapter;
            this.handler=handler;
            isListener=true;
    }

    @Override
    public void run() {
        try {
            while(true){
                serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord("myserver", BlueToothSearchFragment.MyUUID);
                socket=serverSocket.accept();
                Log.d(TAG, "cancle: -----accept-----------");
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
                while(isListener){
                        byte[] buffer=new byte[128];
                        int count=inputStream.read(buffer);
                        Message msg=new Message();
                        msg.obj=new String(buffer,0,count,"utf-8");
                        msg.what=1;
                    if(isListener){
                        handler.sendMessage(msg);
                    }
                    if(!socket.isConnected()){
                        isListener=false;
                        onBluetoothListener.disconnect();
                    }
                }
                socket.close();
                serverSocket.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancle(){
        if(socket!=null&&socket.isConnected()){
            Log.d(TAG, "cancle: -----cancle-----------");
            isListener=false;
        }

    }

    public interface OnBluetoothListener{
        public void disconnect();
    }

    public void sendMessage(final String string){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (socket!=null){
                    try {
                        outputStream=socket.getOutputStream();
                        outputStream.write(string.getBytes("utf-8"));
                        outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }
}
