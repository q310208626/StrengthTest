package com.lsj.hdmi.strengthtest;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.lsj.hdmi.strengthtest.view.BlueToothSearchFragment;
import com.lsj.hdmi.strengthtest.view.ResultShowFragment;

public class MainActivity extends AppCompatActivity {
    Fragment resultFragment=new ResultShowFragment();
    Fragment bluetoothFragment=new BlueToothSearchFragment();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setHomeButtonEnabled(true);
        FragmentManager fm=getFragmentManager();
        FragmentTransaction fragmentTransaction=fm.beginTransaction();
        if (resultFragment==null){
            resultFragment=new ResultShowFragment();
        }
        fragmentTransaction.add(R.id.container,resultFragment).commit();

    }

    @Override
    protected void onDestroy() {
        SharedPreferences sp =getSharedPreferences("adress", Context.MODE_PRIVATE);
        sp.edit().putString("adress",null).commit();
        sp.edit().putBoolean("connectDevice",false).commit();
        super.onDestroy();
    }
}
