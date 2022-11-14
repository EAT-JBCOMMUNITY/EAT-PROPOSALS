package com.example.firebasegsocapp.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.firebasegsocapp.R;
import com.example.firebasegsocapp.domain.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import org.jetbrains.annotations.NotNull;

import static com.example.firebasegsocapp.activity.MainActivity.FIREBASE_FIRESTORE;
import static com.example.firebasegsocapp.activity.MainActivity.getFirebaseAuth;

public class CreateAccountActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private TextView txtViewCancelCreation;
    private TextInputEditText edtTextCreationUsername;
    private TextInputEditText edtTextCreationEmail;
    private TextInputEditText edtTextCreationPassword;
    private TextInputEditText edtTextConfirmedPassword;
    private TextView txtViewAccCreationShowPassword;
    private TextView txtViewShowConfirmedPassword;
    private Button buttonCreation;
    private TextView txtViewCreationAltMethod;
    private ProgressDialog progressDialog;


    private FirebaseAuth getAuth(){
        return firebaseAuth;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        init();
        setListeners();
    }

    @Override
    public void onBackPressed() {
        cancelActivity();
    }

    private void init() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        firebaseAuth = getFirebaseAuth();
        txtViewCancelCreation = findViewById(R.id.txtViewCancelCreation);
        edtTextCreationUsername = findViewById(R.id.edtTextAccCreationUsername);
        edtTextCreationEmail = findViewById(R.id.edtTextAccCreationEmail);
        edtTextCreationPassword = findViewById(R.id.edtTextAccCreationPassword);
        edtTextConfirmedPassword = findViewById(R.id.edtTextConfirmedPassword);
        txtViewAccCreationShowPassword = findViewById(R.id.txtViewAccCreationShowPassword);
        txtViewShowConfirmedPassword = findViewById(R.id.txtViewShowConfirmedPassword);
        buttonCreation = findViewById(R.id.buttonCreation);
        txtViewCreationAltMethod = findViewById(R.id.txtViewCreationAltMethod);
    }

    private void setListeners() {
        txtViewCancelCreation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelActivity();
            }
        });

        txtViewAccCreationShowPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txtViewAccCreationShowPassword.getText().equals("SHOW")){
                    edtTextCreationPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    txtViewAccCreationShowPassword.setText("HIDE");
                    return;
                }
                edtTextCreationPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                txtViewAccCreationShowPassword.setText("SHOW");
            }
        });

        txtViewShowConfirmedPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txtViewShowConfirmedPassword.getText().equals("SHOW")){
                    edtTextConfirmedPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    txtViewShowConfirmedPassword.setText("HIDE");
                    return;
                }
                edtTextConfirmedPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                txtViewShowConfirmedPassword.setText("SHOW");
            }
        });

        buttonCreation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtTextCreationUsername.getText().toString();
                String email = edtTextCreationEmail.getText().toString();
                String password = edtTextCreationPassword.getText().toString();
                String confirmedPassword = edtTextConfirmedPassword.getText().toString();
                if(checkUserInput(username, email, password, confirmedPassword)) {
                    progressDialog.setMessage("Creating account ...");
                    progressDialog.show();
                    createAccount(username, email, password);
                }
            }
        });

        txtViewCreationAltMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("message", "switch_login");
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void cancelActivity(){
        finish();
    }

    private boolean checkUserInput (String username, String email, String password, String confirmedPassword) {
        if(TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            if (TextUtils.isEmpty(username))
                edtTextCreationUsername.setError("Username can not be empty.");
            if (TextUtils.isEmpty(email))
                edtTextCreationEmail.setError("E-mail can not be empty.");
            if (TextUtils.isEmpty(password))
                edtTextCreationPassword.setError("Password can not be empty.");
            return false;
        }
        if(!password.equals(confirmedPassword)) {
            edtTextConfirmedPassword.setError("The given passwords do not match.");
            return false;
        }
        return true;
    }

    private void handleAccCreationErrors(String exception) {
        if(exception.equals("The email address is badly formatted."))
            edtTextCreationEmail.setError(exception);
        if(exception.contains("The email address is already in use")) {
            edtTextCreationEmail.setError(exception);
            txtViewCreationAltMethod.setTextColor(getResources().getColor(R.color.blood_red));
        }
        if(exception.contains("The given password is invalid."))
            edtTextCreationPassword.setError(exception);
    }

    private void createAccount (String username, String email, String password) {
        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder().setDisplayName(username);
        UserProfileChangeRequest changeRequest = builder.build();

        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(CreateAccountActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    getAuth().getCurrentUser().updateProfile(changeRequest).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            storePendingUser(email, username);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull @NotNull Exception e) {
                            getAuth().getCurrentUser().delete();
                            progressDialog.dismiss();
                            Toast.makeText(CreateAccountActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                }
                else {
                    progressDialog.dismiss();
                    handleAccCreationErrors(task.getException().getLocalizedMessage());
                }
            }
        });
    }

    private void storePendingUser(String userEmail, String userFullName){
        FIREBASE_FIRESTORE.collection("pending_users").document(userEmail)
                .set(new User("", userEmail, userFullName))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        sendEmailVerification();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        getAuth().getCurrentUser().delete();
                        progressDialog.dismiss();
                        Toast.makeText(CreateAccountActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendEmailVerification() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(CreateAccountActivity.this);
                            builder.setTitle("E-mail Verification")
                                    .setMessage("An E-mail verification was sent to "+user.getEmail()+". Please verify your E-mail to activate your account.")
                                    .setCancelable(false)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            getFirebaseAuth().signOut();
                                            Intent resultIntent = new Intent();
                                            resultIntent.putExtra("message", "signup_complete");
                                            setResult(Activity.RESULT_OK, resultIntent);
                                            progressDialog.dismiss();
                                            finish();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        getAuth().getCurrentUser().delete();
                        progressDialog.dismiss();
                        Toast.makeText(CreateAccountActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
