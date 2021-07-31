package com.dc.videomeeting.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

import com.dc.videomeeting.R;
import com.dc.videomeeting.databinding.ActivityIncommingInvitaionBinding;
import com.dc.videomeeting.network.ApiClient;
import com.dc.videomeeting.network.ApiService;
import com.dc.videomeeting.utilities.Constants;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingInvitationActivity extends AppCompatActivity {

    private ActivityIncommingInvitaionBinding binding;
    private String meetingType =null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIncommingInvitaionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        if (meetingType != null){
            if (meetingType.equals("video")){
                binding.imageMeetingType.setImageResource(R.drawable.ic_videocam);
            }
            else if (meetingType.equals("audio")){
                binding.imageMeetingType.setImageResource(R.drawable.ic_phone);
            }
        }

        String firstName = getIntent().getStringExtra(Constants.KEY_FIRST_NAME);
        if (firstName != null){
            binding.textFirstChar.setText(firstName.substring(0, 1));
        }

        String LastName = getIntent().getStringExtra(Constants.KEY_LAST_NAME);
        if (firstName != null && LastName != null){
            binding.textUserName.setText(String.format("%s %s", firstName, LastName));
        }

        String email = getIntent().getStringExtra(Constants.KEY_EMAIL);
        if (email != null){
            binding.textUserEmail.setText(email);
        }

        binding.imageAccept.setOnClickListener(view -> {
            sendInvitationResponse(
                    Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                    getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
            );
        });

        binding.imageReject.setOnClickListener(view -> {
            sendInvitationResponse(
                    Constants.REMOTE_MSG_INVITATION_REJECTED,
                    getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
            );
        });
    }

    private void sendInvitationResponse(String type, String receiverToken){
        try {

            JSONArray token = new JSONArray();
            token.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, type);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, token);

            sendRemoteMessage(body.toString(), type);

        } catch (Exception e) {
            Toast.makeText(IncomingInvitationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){

                        try {

                            URL serverURL = new URL("https://meet.jit.si");

                            JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                            builder.setServerURL(serverURL);
                            builder.setWelcomePageEnabled(false);
                            builder.setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM));

                            if (meetingType.equals("audio")){
                                builder.setVideoMuted(true);
                            }

                            JitsiMeetActivity.launch(IncomingInvitationActivity.this,builder.build());
                            finish();


                        } catch (Exception e) {
                            Toast.makeText(IncomingInvitationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }

                    }
                    else {
                        Toast.makeText(IncomingInvitationActivity.this, "Invitation Rejected", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                else {
                    Toast.makeText(IncomingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(IncomingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null){
                if (type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)){
                    Toast.makeText(context, "Invitation Cancelled", Toast.LENGTH_SHORT).show();
                    finish();
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