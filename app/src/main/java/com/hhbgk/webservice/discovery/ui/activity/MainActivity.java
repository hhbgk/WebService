package com.hhbgk.webservice.discovery.ui.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.hhbgk.webservice.discovery.R;
import com.hhbgk.webservice.discovery.ui.fragment.DeviceList;
import com.hhbgk.webservice.discovery.ui.fragment.ServiceList;

public class MainActivity extends FragmentActivity {
    private String tag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, DeviceList.newInstance())
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().findFragmentById(R.id.container) instanceof ServiceList) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, DeviceList.newInstance())
                    .commit();
        } else {
            super.onBackPressed();
        }
    }
}
