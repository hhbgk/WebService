package com.hhbgk.webservice.discovery.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.hhbgk.webservice.discovery.R;
import com.hhbgk.webservice.discovery.data.model.ServiceInfo;
import com.hhbgk.webservice.discovery.data.model.StreamInfo;
import com.hhbgk.webservice.discovery.onvif.OnvifService;
import com.hhbgk.webservice.discovery.ui.activity.PTZActivity;
import com.hhbgk.webservice.discovery.ui.adapter.ServiceAdapter;
import com.hhbgk.webservice.discovery.util.Dbug;

import java.util.List;

public class ServiceList extends Fragment {
    private String tag = getClass().getSimpleName();
    private ServiceAdapter mDeviceAdapter;
    private RecyclerView mRecyclerView;
    private static List<ServiceInfo> mServiceInfoList;
    private OnvifService mOnvifService;

    public static ServiceList newInstance(List<ServiceInfo> serviceInfoList) {
        mServiceInfoList = serviceInfoList;
        ServiceList fragment = new ServiceList();
        /*Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("service_list", (ArrayList<? extends Parcelable>) serviceInfoList);
        fragment.setArguments(bundle);*/
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*if (getArguments() != null) {
            mServiceInfoList = getArguments().getParcelableArrayList("service_list");
            if (mServiceInfoList != null) {
                Log.i(tag, "size======" + mServiceInfoList.size());
            }
        }*/
        mOnvifService = OnvifService.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.service_list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.service_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mDeviceAdapter = new ServiceAdapter();
        mRecyclerView.setAdapter(mDeviceAdapter);
        if (mServiceInfoList != null) {
            mDeviceAdapter.setData(mServiceInfoList);
        }

        mDeviceAdapter.setOnItemClickListener(new ServiceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String serviceURL = mServiceInfoList.get(position).getUri();
                Dbug.i(tag, "position = " + position + ", "+ serviceURL);
                if (mServiceInfoList.get(position).getUri().contains("media_service")){
                    mOnvifService.requestMediaService(mServiceInfoList.get(position).getUri(), new OnvifService.OnGetMediaServiceListener() {
                        @Override
                        public void onSuccess(List<StreamInfo> serviceInfoList) {
                            StreamInfo streamInfo = serviceInfoList.get(0);
                            Log.i(tag, "uri=" + streamInfo.getStreamUri());
                            Toast.makeText(getActivity(), "RTSP:"+streamInfo.getStreamUri(), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(String message) {

                        }
                    });
                } else if (mServiceInfoList.get(position).getUri().contains("ptz_service")) {
                    Intent intent = new Intent(getActivity(), PTZActivity.class);
                    intent.putExtra("service_url", mServiceInfoList.get(position).getUri());
                    startActivity(intent);
                } else if (mServiceInfoList.get(position).getNamespace().contains("image_service")) {

                }
            }
        });
        return view;
    }
}
