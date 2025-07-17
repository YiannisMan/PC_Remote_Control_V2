package com.example.lab_control_v2.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab_control_v2.R;
import com.example.lab_control_v2.model.CardModel;

import java.util.ArrayList;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private Context context;
    private ArrayList<CardModel> cardModelArrayList;
    private OnCardCheckedListener checkedListener;

    public interface OnCardCheckedListener {
        void onCardCheckedChanged(int position, boolean isChecked);
    }

    public CardAdapter(Context context, ArrayList<CardModel> cardModelArrayList, OnCardCheckedListener listener) {
        this.context = context;
        this.cardModelArrayList = cardModelArrayList;
        this.checkedListener = listener;
    }

    public CardAdapter(Context context, ArrayList<CardModel> cardModelArrayList) {
        this.context = context;
        this.cardModelArrayList = cardModelArrayList;
        this.checkedListener = null;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardModel model = cardModelArrayList.get(position);
        holder.hostnameTv.setText(model.getHostname());

        if (model.getOs() != null && !model.getOs().isEmpty())
        {
            holder.osTv.setText(model.getOs());
        }
        else
        {
            holder.osTv.setText("N/A");
        }
        holder.statusTv.setText(model.getStatus());

        String currentStatus = model.getStatus().toLowerCase();
        if (currentStatus.equals("online")) {
            holder.statusTv.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else if (currentStatus.contains("error") || currentStatus.equals("offline") || currentStatus.equals("timeout") || currentStatus.equals("no response") || currentStatus.equals("empty response")) {
            holder.statusTv.setTextColor(Color.parseColor("#F44336")); // Red
        } else if (currentStatus.equals("άγνωστη") || currentStatus.equals("status")) {
            holder.statusTv.setTextColor(Color.GRAY);
        } else {
            holder.statusTv.setTextColor(Color.parseColor("#FF9800")); // Orange
        }
        holder.osTv.setTextColor(Color.DKGRAY);


        holder.selectCb.setOnCheckedChangeListener(null);
        holder.selectCb.setChecked(model.isSelected());
        holder.selectCb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (checkedListener != null)
            {
                if(holder.getAdapterPosition() != RecyclerView.NO_POSITION)
                {
                    checkedListener.onCardCheckedChanged(holder.getAdapterPosition(), isChecked);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardModelArrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView hostnameTv, osTv, statusTv;
        private CheckBox selectCb;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            hostnameTv = itemView.findViewById(R.id.idCode);
            osTv = itemView.findViewById(R.id.idType);
            statusTv = itemView.findViewById(R.id.idStatus);
            selectCb = itemView.findViewById(R.id.idCheckBox);
        }
    }
}