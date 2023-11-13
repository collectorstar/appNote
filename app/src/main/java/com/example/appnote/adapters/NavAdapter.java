package com.example.appnote.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appnote.R;
import com.example.appnote.activities.LoginActivity;
import com.example.appnote.activities.MainActivity;
import com.example.appnote.entities.Nav;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class NavAdapter extends RecyclerView.Adapter<NavAdapter.NavViewHolder> {

    private List<Nav> list;
    private Context context;
    private MyClickListener myClickListener;

    public NavAdapter(Context context,MyClickListener myClickListener) {
        this.context = context;
        //nav items
        this.list = new ArrayList<>();
        this.list.add(new Nav("Account Info"));
        this.list.add(new Nav("Change Password"));
        this.list.add(new Nav("Logout"));

        this.myClickListener = myClickListener;
    }

    @NonNull
    @Override
    public NavViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NavViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_nav, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NavViewHolder holder, int position) {
        holder.setNav(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class NavViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutNav;
        ImageView ivIcon;
        TextView tvName;

        public NavViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            layoutNav = itemView.findViewById(R.id.layoutNav);
            itemView.setOnClickListener(view -> myClickListener.onItemClick(getAdapterPosition(),tvName.getText().toString()));
        }

        void setNav(Nav item) {
            tvName.setText(item.getName());
            switch (item.getName()) {
                case "Account Info":
                    ivIcon.setImageResource(R.drawable.ic_info_nav);
                    break;
                case "Change Password":
                    ivIcon.setImageResource(R.drawable.ic_change_circle);
                    break;
                case "Logout":
                    ivIcon.setImageResource(R.drawable.ic_logout);
                    break;
            }
        }
    }

    public interface MyClickListener{
        void onItemClick(int position,String itemName);
    }
}
