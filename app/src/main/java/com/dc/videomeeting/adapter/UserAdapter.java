package com.dc.videomeeting.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dc.videomeeting.databinding.ItemUserBinding;
import com.dc.videomeeting.listner.UserListener;
import com.dc.videomeeting.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<User> userList;
    private UserListener userListener;
    private List<User> selectedUser;

    public UserAdapter(List<User> userList, UserListener userListener) {
        this.userList = userList;
        this.userListener = userListener;
        this.selectedUser = new ArrayList<>();
    }

    public List<User> getSelectedUser() {
        return selectedUser;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setUserData(userList.get(position));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ItemUserBinding binding;
        public ViewHolder(@NonNull ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void setUserData(User user) {
            binding.textFirstChar.setText(user.firstNAme.substring(0, 1));
            binding.name.setText(String.format("%s %s", user.firstNAme, user.lastName));
            binding.email.setText(String.format("%s ", user.email));

            binding.icVideo.setOnClickListener(v -> userListener.initiateVideoMeeting(user));
            binding.icPhone.setOnClickListener(v -> userListener.initiateAudioMeeting(user));

            binding.userContainer.setOnLongClickListener( view -> {
                if (binding.imgSelected.getVisibility() != View.VISIBLE){
                    selectedUser.add(user);
                    binding.imgSelected.setVisibility(View.VISIBLE);
                    binding.icPhone.setVisibility(View.GONE);
                    binding.icVideo.setVisibility(View.GONE);
                    userListener.onMultipleUserAction(true);
                }

                return true;
            });

            binding.userContainer.setOnClickListener(v-> {
                if (binding.imgSelected.getVisibility() == View.VISIBLE){
                    selectedUser.remove(user);
                    binding.imgSelected.setVisibility(View.GONE);
                    binding.icPhone.setVisibility(View.VISIBLE);
                    binding.icVideo.setVisibility(View.VISIBLE);
                    if (selectedUser.size() == 0)
                        userListener.onMultipleUserAction(false);
                }
                else if (selectedUser.size() > 0){
                    selectedUser.add(user);
                    binding.imgSelected.setVisibility(View.VISIBLE);
                    binding.icPhone.setVisibility(View.GONE);
                    binding.icVideo.setVisibility(View.GONE);
                }
            });
        }
    }
}
