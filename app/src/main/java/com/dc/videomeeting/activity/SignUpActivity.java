package com.dc.videomeeting.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.dc.videomeeting.databinding.ActivitySignUpBinding;
import com.dc.videomeeting.utilities.Constants;
import com.dc.videomeeting.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        binding.textSignIn.setOnClickListener(view -> onBackPressed());
        binding.arrowBack.setOnClickListener(view -> onBackPressed());

        binding.btnSignUp.setOnClickListener(view -> {

            if (binding.inputFirstName.getText().toString().trim().isEmpty())
                Toast.makeText(this, "Enter first name", Toast.LENGTH_SHORT).show();
            else if (binding.inputLastName.getText().toString().trim().isEmpty())
                Toast.makeText(this, "Enter last name", Toast.LENGTH_SHORT).show();
            else if (binding.inputEmail.getText().toString().trim().isEmpty())
                Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show();
            else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches())
                Toast.makeText(this, "Enter Valid Email", Toast.LENGTH_SHORT).show();
            else if (binding.inputPassword.getText().toString().trim().isEmpty())
                Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show();
            else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty())
                Toast.makeText(this, "Confirm your password", Toast.LENGTH_SHORT).show();
            else if (!binding.inputPassword.getText().toString().trim().equals(binding.inputConfirmPassword.getText().toString().trim()))
                Toast.makeText(this, "Password & Confirm password must be same", Toast.LENGTH_SHORT).show();
            else
                signUp();
        });
    }

    private void signUp() {
        binding.btnSignUp.setVisibility(View.INVISIBLE);
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Create a new user with a first and last name
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_FIRST_NAME, binding.inputFirstName.getText().toString());
        user.put(Constants.KEY_LAST_NAME, binding.inputLastName.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString());

// Add a new document with a generated ID
        db.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());

                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                        preferenceManager.putString(Constants.KEY_FIRST_NAME, binding.inputFirstName.getText().toString());
                        preferenceManager.putString(Constants.KEY_LAST_NAME, binding.inputLastName.getText().toString());
                        preferenceManager.putString(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Error adding User ", e);
                        Toast.makeText(SignUpActivity.this, "Unable to Sign UP", Toast.LENGTH_SHORT).show();

                        binding.btnSignUp.setVisibility(View.VISIBLE);
                        binding.progressBar.setVisibility(View.INVISIBLE);
                    }
                });
    }
}