package com.dc.videomeeting.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dc.videomeeting.R;
import com.dc.videomeeting.adapter.UserAdapter;
import com.dc.videomeeting.databinding.ActivityMainBinding;
import com.dc.videomeeting.databinding.ActivitySignUpBinding;
import com.dc.videomeeting.listner.UserListener;
import com.dc.videomeeting.model.User;
import com.dc.videomeeting.utilities.Constants;
import com.dc.videomeeting.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements UserListener {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<User> userList;
    private UserAdapter userAdapter;

    private int REQUEST_CODE_BATTERY_OPTIMIZATION = 1;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        binding.txtUserName.setText(String.format(
                "%s %s",
                preferenceManager.getString(Constants.KEY_FIRST_NAME),
                preferenceManager.getString(Constants.KEY_LAST_NAME)
        ));

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task ->{
            if (task.isSuccessful() && task.getResult() != null){
                sendFCMTokenToDatabase(task.getResult().getToken());
            }
        });

        binding.txtSignOut.setOnClickListener(view -> signOut());


        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList, this);
        binding.recyclerUserList.setAdapter(userAdapter);

        binding.swipeRefreshLayout.setOnRefreshListener(this::getUsers);

        getUsers();
        checkForBatteryOptimization();
    }

    private void getUsers() {
        binding.swipeRefreshLayout.setRefreshing(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        String myUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                        if (task.isSuccessful() && task.getResult() != null){
                            userList.clear();
                            for (QueryDocumentSnapshot documentSnapshots : task.getResult()){
                                if (!myUserId.equals(documentSnapshots.getId())){
                                   User user = new User();
                                   user.firstNAme = documentSnapshots.getString(Constants.KEY_FIRST_NAME);
                                    user.lastName = documentSnapshots.getString(Constants.KEY_LAST_NAME);
                                    user.email = documentSnapshots.getString(Constants.KEY_EMAIL);
                                    user.token = documentSnapshots.getString(Constants.KEY_FCM_TOKEN);
                                    userList.add(user);
                                }
                            }

                            if (userList.size() >0){
                                userAdapter.notifyDataSetChanged();
                            }
                            else {
                                binding.errorMsg.setText(" No user available");
                                binding.errorMsg.setVisibility(View.VISIBLE);
                            }
                        }
                        else {
                            binding.errorMsg.setText(" No user available");
                            binding.errorMsg.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void signOut() {
        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));

        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(aVoid -> {
                    preferenceManager.clearPreference();
                    startActivity(new Intent(MainActivity.this, SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.toString());
                    Toast.makeText(MainActivity.this, "Unable to Sign Out"+ e, Toast.LENGTH_SHORT).show();
                });
    }

    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));

        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.toString());
                    Toast.makeText(MainActivity.this, "Token updated Failed "+ e, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void initiateVideoMeeting(User user) {

        if (user.token == null || user.token.trim().isEmpty()){
            Toast.makeText(this,
                    user.firstNAme +" "+user.lastName +" is not available for meeting",
                    Toast.LENGTH_SHORT).show();
        }
        else{
           Intent intent = new Intent(this, OutgoingInvitationActivity.class);
           intent.putExtra("user", user);
           intent.putExtra("type", "video");
           startActivity(intent);
        }
    }

    @Override
    public void initiateAudioMeeting(User user) {
        if (user.token == null || user.token.trim().isEmpty()){
            Toast.makeText(this,
                    user.firstNAme +" "+user.lastName +" is not available for meeting",
                    Toast.LENGTH_SHORT).show();
        }
        else{
            Intent intent = new Intent(this, OutgoingInvitationActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("type", "audio");
            startActivity(intent);
        }
    }

    @Override
    public void onMultipleUserAction(Boolean isMultipleUserSelected) {
        if (isMultipleUserSelected){
            binding.imageConference.setVisibility(View.VISIBLE);
            binding.imageConference.setOnClickListener(v-> {
                Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
                intent.putExtra("selectedUsers", new Gson().toJson(userAdapter.getSelectedUser()));
                intent.putExtra("type", "video");
                intent.putExtra("isMultiple", true);
                startActivity(intent);
            });
        }
        else
            binding.imageConference.setVisibility(View.GONE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkForBatteryOptimization(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N){
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())){
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Warning");
                builder.setMessage("Battery optimization is enabled. It can interrupt running background service");
                builder.setPositiveButton("Disable", (dialogInterface, i) -> {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION);
                });
                builder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
                builder.create().show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BATTERY_OPTIMIZATION){
            checkForBatteryOptimization();
        }
    }
}