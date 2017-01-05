package com.hhbgk.webservice.discovery.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.hhbgk.webservice.discovery.R;
import com.hhbgk.webservice.discovery.data.model.CameraInfo;
import com.hhbgk.webservice.discovery.data.model.ServiceInfo;
import com.hhbgk.webservice.discovery.onvif.OnvifService;
import com.hhbgk.webservice.discovery.ui.adapter.DeviceAdapter;

import java.util.ArrayList;
import java.util.List;

public class DeviceList extends Fragment {
    private String tag = getClass().getSimpleName();
    private DeviceAdapter mDeviceAdapter;
    private RecyclerView mRecyclerView;
    private final List<CameraInfo> mDevices = new ArrayList<>();
    private ImageButton mSearchBtn;
    private OnvifService mOnvifService;

    public static DeviceList newInstance() {
        return new DeviceList();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.device_list, container, false);
        mSearchBtn = (ImageButton) rootView.findViewById(R.id.search_button);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.device_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mDeviceAdapter = new DeviceAdapter();
        mRecyclerView.setAdapter(mDeviceAdapter);
        mOnvifService = OnvifService.getInstance();
        mDeviceAdapter.setOnItemClickListener(new DeviceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Log.i(tag, "position=" + position + ", " + mDevices.get(position).getUri());

                mOnvifService.requestServices(mDevices.get(position).getUri(), new OnvifService.OnGetServicesListener() {
                    @Override
                    public void onSuccess(List<ServiceInfo> serviceInfoList) {
                        Log.e(tag, "onSuccess: service size=" + serviceInfoList.size());
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.container, ServiceList.newInstance(serviceInfoList))
                                .commit();
                    }

                    @Override
                    public void onFailure(String message) {

                    }
                });
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });

        mSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnvifService.probeDeviceService(new OnvifService.OnProbeListener() {
                    @Override
                    public void onSuccess(CameraInfo object) {
                        //Log.i(tag, "1111 CameraInfo=" + object.toString());
                        mDevices.clear();
                        mDevices.add(object);
                        mDeviceAdapter.setData(mDevices);
                    }

                    @Override
                    public void onFailure(String message) {
                        Log.e(tag, "on failure: thread id==" + Thread.currentThread().getId());
                    }
                });
            }
        });
        return rootView;
    }
}
