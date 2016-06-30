package com.hhbgk.webservice.discovery.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.hhbgk.webservice.discovery.R;
import com.hhbgk.webservice.discovery.data.model.PtzNode;
import com.hhbgk.webservice.discovery.onvif.OnvifService;
import com.hhbgk.webservice.discovery.util.Dbug;

import java.util.List;

/**
 * Author: bob
 * Date: 16-6-28 16:48
 * Version: V1
 * Description:
 */
public class PtzFragment extends Fragment implements View.OnClickListener {
    private String tag = getClass().getSimpleName();
    private static List<PtzNode> mPtzNodeList;
    private Button ptzUp, ptzDown, ptzLeft, ptzRight, ptzHome;
    private OnvifService mOnvifService;
    private String serviceUrl;

    public static PtzFragment newInstance(String serviceUrl) {
        Dbug.e("PtzFragment", "service url=" + serviceUrl);
        PtzFragment fragment = new PtzFragment();
        Bundle bundle = new Bundle();
        bundle.putString("service_url", serviceUrl);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ptz_layout, container, false);
        ptzUp = (Button) view.findViewById(R.id.ptz_up);
        ptzUp.setOnClickListener(this);
        ptzDown = (Button) view.findViewById(R.id.ptz_down);
        ptzDown.setOnClickListener(this);
        ptzHome = (Button) view.findViewById(R.id.ptz_home);
        ptzHome.setOnClickListener(this);
        ptzLeft = (Button) view.findViewById(R.id.ptz_left);
        ptzLeft.setOnClickListener(this);
        ptzRight = (Button) view.findViewById(R.id.ptz_right);
        ptzRight.setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mOnvifService = OnvifService.getInstance();
        String url = getArguments().getString("service_url");
        serviceUrl = url;
        Dbug.w(tag, "url=" + url);
        if (!TextUtils.isEmpty(url)) {
            mOnvifService.requestPtzService(url, new OnvifService.OnGetPtzServiceListener() {
                @Override
                public void onSuccess(List<PtzNode> serviceInfoList) {
                    if (serviceInfoList.size() > 0) {
                        Dbug.i(tag, "serviceInfoList size=" + serviceInfoList.size() + ", is home supported="+ serviceInfoList.get(0).isHomeSupported());
                    }
                }

                @Override
                public void onFailure(String message) {

                }
            });
        }
    }

    private final Handler handler = new Handler();

    @Override
    public void onClick(View v) {
        if (v == ptzUp) {
            Dbug.i(tag, "on click= up");
            mOnvifService.requestPtzContinuousMoveUp(serviceUrl, null);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mOnvifService.requestStopPtzContinuousMove(serviceUrl, null);
                }
            }, 3000);
        } else if (ptzDown == v) {
            mOnvifService.requestPtzContinuousMoveDown(serviceUrl, null);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mOnvifService.requestStopPtzContinuousMove(serviceUrl, null);
                }
            }, 3000);
        } else if (v == ptzHome) {
            mOnvifService.requestPtzGotoHomePosition(serviceUrl, null);
        } else if (v == ptzLeft) {
            mOnvifService.requestPtzContinuousMoveLeft(serviceUrl, null);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mOnvifService.requestStopPtzContinuousMove(serviceUrl, null);
                }
            }, 3000);
        } else if (v == ptzRight) {
            mOnvifService.requestPtzContinuousMoveRight(serviceUrl, null);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mOnvifService.requestStopPtzContinuousMove(serviceUrl, null);
                }
            }, 3000);
        }
    }
}
