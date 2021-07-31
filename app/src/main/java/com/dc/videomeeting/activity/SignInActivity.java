package com.dc.videomeeting.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.dc.videomeeting.databinding.ActivitySignInBinding;
import com.dc.videomeeting.utilities.Constants;
import com.dc.videomeeting.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";
    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }

        binding.textSignUp.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));

        binding.btnSignIn.setOnClickListener(view -> {
            if (binding.inputEmail.getText().toString().trim().isEmpty())
                Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show();
            else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches())
                Toast.makeText(this, "Enter Valid Email", Toast.LENGTH_SHORT).show();
            else if (binding.inputPassword.getText().toString().trim().isEmpty())
                Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show();
            else
                signIn();
        });
    }

    private void signIn() {
        binding.btnSignIn.setVisibility(View.INVISIBLE);
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString().trim())
                .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString().trim())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_FIRST_NAME, documentSnapshot.getString(Constants.KEY_FIRST_NAME));
                        preferenceManager.putString(Constants.KEY_LAST_NAME, documentSnapshot.getString(Constants.KEY_LAST_NAME));
                        preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                    else {
                        Toast.makeText(SignInActivity.this, "Unable to Sign In", Toast.LENGTH_SHORT).show();
                        binding.btnSignIn.setVisibility(View.VISIBLE);
                        binding.progressBar.setVisibility(View.INVISIBLE);
                    }
                });
    }
}