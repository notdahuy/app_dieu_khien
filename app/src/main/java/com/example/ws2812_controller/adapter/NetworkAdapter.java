// NetworkAdapter.java
package com.example.ws2812_controller.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.model.WifiNetwork;

import java.util.List;

public class NetworkAdapter extends RecyclerView.Adapter<NetworkAdapter.ViewHolder> {

    private List<WifiNetwork> networks;
    private OnNetworkClickListener listener;

    public interface OnNetworkClickListener {
        void onNetworkClick(WifiNetwork network);
    }

    public NetworkAdapter(List<WifiNetwork> networks, OnNetworkClickListener listener) {
        this.networks = networks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_network, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WifiNetwork network = networks.get(position);

        holder.tvSsid.setText(network.ssid);
        holder.tvRssi.setText(network.rssi + " dBm");
        holder.tvAuth.setText(network.auth.replace("Some(", "").replace(")", ""));

        // Signal strength icon
        int strength = network.getSignalStrength();
        if (strength >= 75) {
            holder.ivSignal.setImageResource(R.drawable.ic_wifi_full);
        } else if (strength >= 50) {
            holder.ivSignal.setImageResource(R.drawable.ic_wifi_medium);
        } else {
            holder.ivSignal.setImageResource(R.drawable.ic_wifi_low);
        }

        holder.itemView.setOnClickListener(v -> listener.onNetworkClick(network));
    }

    @Override
    public int getItemCount() {
        return networks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSsid;
        TextView tvRssi;
        TextView tvAuth;
        ImageView ivSignal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSsid = itemView.findViewById(R.id.tvSsid);
            tvRssi = itemView.findViewById(R.id.tvRssi);
            tvAuth = itemView.findViewById(R.id.tvAuth);
            ivSignal = itemView.findViewById(R.id.ivSignal);
        }
    }
}