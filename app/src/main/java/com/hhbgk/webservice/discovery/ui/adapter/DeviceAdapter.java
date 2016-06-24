package com.hhbgk.webservice.discovery.ui.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hhbgk.webservice.discovery.R;
import com.hhbgk.webservice.discovery.data.model.CameraInfo;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private List<CameraInfo> mData;// = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;
    public DeviceAdapter() {

    }
    public void setData(List<CameraInfo> data) {
        mData = data;
        notifyDataSetChanged();
    }

    @Override
    public DeviceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Log.i("DeviceAdapter", "mData.get(position).getName()=" + mData.get(position).getName());
        CameraInfo ipCamera = mData.get(position);
        holder.name.setText(ipCamera.getName());
        holder.location.setText(ipCamera.getLocation());
        holder.baseUrl.setText(ipCamera.getUri());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(v, position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mData != null && mData.size() > 0) {
            return mData.size();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView location;
        private TextView baseUrl;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.device_name);
            location = (TextView) itemView.findViewById(R.id.device_location);
            baseUrl = (TextView) itemView.findViewById(R.id.device_base_url);
            /*CardView cardView = (CardView) itemView.findViewById(R.id.card_view);
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(v, );
                    }
                }
            });*/
        }
    }
    /**
     * Interface definition for a callback to be invoked when an item in this
     * AdapterView has been clicked.
     */
    public interface OnItemClickListener {

        /**
         * Callback method to be invoked when an item in this AdapterView has
         * been clicked.
         * <p>
         * Implementers can call getItemAtPosition(position) if they need
         * to access the data associated with the selected item.
         *
         * @param view The view within the AdapterView that was clicked (this
         *            will be a view provided by the adapter)
         * @param position The position of the view in the adapter.
         */
        void onItemClick(View view, int position);
    }

    /**
     * Register a callback to be invoked when an item in this AdapterView has
     * been clicked.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnItemClickListener(@Nullable OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }
}
