package com.dc.videomeeting.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dc.videomeeting.R;
import com.dc.videomeeting.databinding.ActivityOutgoingInvitationBinding;
import com.dc.videomeeting.model.User;
import com.dc.videomeeting.network.ApiClient;
import com.dc.videomeeting.network.ApiService;
import com.dc.videomeeting.utilities.Constants;
import com.dc.videomeeting.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutgoingInvitationActivity extends AppCompatActivity {

    private ActivityOutgoingInvitationBinding binding;
    private PreferenceManager preferenceManager;
    private String inviterToken = null;
    private String meetingRoom = null;
    private String meetingType = null;

    private int rejectionCount =0;
    private int totalReceiver =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOutgoingInvitationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        meetingType = getIntent().getStringExtra("type");
        if (meetingType != null){
            if (meetingType.equals("video")){
                binding.imageMeetingType.setImageResource(R.drawable.ic_videocam);
            }
            else if (meetingType.equals("audio")){
                binding.imageMeetingType.setImageResource(R.drawable.ic_phone);
            }
        }

        User user = (User) getIntent().getSerializableExtra("user");
        if (user != null){
            binding.textFirstChar.setText(user.firstNAme.substring(0, 1));
            binding.textUserName.setText(String.format("%s %s", user.firstNAme, user.lastName));
            binding.textUserEmail.setText(user.email);
        }

        binding.imageStopInvitation.setOnClickListener(v -> {
            if(getIntent().getBooleanExtra("isMultiple", false)){
                Type type = new TypeToken<ArrayList<User>>(){}.getType();
                ArrayList<User> receivers = new Gson().fromJson(getIntent().getStringExtra("selectedUsers"), type);
                cancelInvitation(null, receivers);
            }
            else {
                if (user != null){
                    cancelInvitation(user.token, null);
                }
            }

        });

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null){
                inviterToken = task.getResult().getToken();

                if (meetingType != null){
                    if(getIntent().getBooleanExtra("isMultiple", false)){
                        Type type = new TypeToken<ArrayList<User>>(){}.getType();
                        ArrayList<User> receivers = new Gson().fromJson(getIntent().getStringExtra("selectedUsers"), type);
                        if (receivers != null ){
                            totalReceiver = receivers.size();
                        }
                        initiateMeeting(meetingType, null, receivers);
                    }
                    else{
                        if (user != null){
                            totalReceiver = 1;
                            initiateMeeting(meetingType, user.token, null);
                        }
                    }
                }


            }
        });


    }

    private void initiateMeeting(String meetingType, String receiverToken, ArrayList<User> receivers){
        try {

            JSONArray token = new JSONArray();

            if (receiverToken != null)
                token.put(receiverToken);
            if (receivers != null && receivers.size() > 0){
                StringBuilder userNames = new StringBuilder();
                for (int i = 0; i< receivers.size(); i++){
                    token.put(receivers.get(i).token);
                    userNames.append(receivers.get(i).firstNAme).append(" ").append(receivers.get(i).lastName).append("\n");
                }
                binding.textFirstChar.setVisibility(View.GONE);
                binding.textUserEmail.setVisibility(View.GONE);
                binding.textUserName.setText(userNames.toString());
            }

            token.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
            data.put(Constants.KEY_FIRST_NAME, preferenceManager.getString(Constants.KEY_FIRST_NAME));
            data.put(Constants.KEY_LAST_NAME, preferenceManager.getString(Constants.KEY_LAST_NAME));
            data.put(Constants.KEY_EMAIL, preferenceManager.getString(Constants.KEY_EMAIL));
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, inviterToken);

            meetingRoom =
                    preferenceManager.getString(Constants.KEY_USER_ID)+ "_"+
                            UUID.randomUUID().toString().substring(0, 5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, token);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION);

        } catch (Exception e) {
            Log.d("out", e.getMessage());
            Toast.makeText(OutgoingInvitationActivity.this,e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void sendRemoteMessage(String remoteMessageBody, String type){
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
                Constants.getRemoteMessageHeader(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()){
                    if (type.equals(Constants.REMOTE_MSG_INVITATION)){
                        Toast.makeText(OutgoingInvitationActivity.this, "Invitation sent successfully", Toast.LENGTH_SHORT).show();
                    }
                    else if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)){
                        Toast.makeText(OutgoingInvitationActivity.this, "Invitation Cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                else {
                    Toast.makeText(OutgoingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(OutgoingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void cancelInvitation(String receiverToken , ArrayList<User> receivers){
        try {

            JSONArray token = new JSONArray();

            if (receiverToken != null)
                token.put(receiverToken);

            if (receiverToken != null && receivers.size() > 0){

                for (User user : receivers){
                    token.put(user.token);
                }
            }

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, Constants.REMOTE_MSG_INVITATION_CANCELLED);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, token);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE);

        } catch (Exception e) {
            Toast.makeText(OutgoingInvitationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null){
                if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){
                    try {

                        URL serverURL = new URL("https://meet.jit.si");

                        JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                        builder.setServerURL(serverURL);
                        builder.setWelcomePageEnabled(false);
                        builder.setRoom(meetingRoom);
                        if (meetingType.equals("audio")){
                            builder.setVideoMuted(true);
                        }

                        JitsiMeetActivity.launch(OutgoingInvitationActivity.this, builder.build());
                        finish();


                    } catch (Exception e) {
                        Log.d("out", e.getMessage());
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                else if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)){
                    rejectionCount +=1;
                    if (rejectionCount == totalReceiver){
                        Toast.makeText(context, "Invitation Rejected", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }
}