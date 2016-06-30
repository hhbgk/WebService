package com.hhbgk.webservice.discovery.ui.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.hhbgk.webservice.discovery.R;
import com.hhbgk.webservice.discovery.ui.fragment.PtzFragment;

public class PTZActivity extends FragmentActivity {
    private String tag = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ptz);
        if (savedInstanceState == null) {
            String ptzServiceUrl = getIntent().getStringExtra("service_url");
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, PtzFragment.newInstance(ptzServiceUrl))
                    .commit();
        }
    }
}
