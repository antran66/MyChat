package com.antran.mychat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button UpdateAccountSettings;
    private EditText userName, userStatus;
    private CircleImageView userProfileImage;

    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;

    private static final int GalleryPick = 1;
    private StorageReference UserProfileImagesRef;
    private ProgressDialog loadingBar;
    private String downloadImageUrl;

    private Toolbar SettingToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UserProfileImagesRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        InitilizeFields();

        userName.setVisibility(View.INVISIBLE);

        UpdateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSettings();
            }
        });


        RetrieveUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/");
                startActivityForResult(galleryIntent, GalleryPick);
            }
        });
    }



    private void InitilizeFields() {
        UpdateAccountSettings = (Button)findViewById(R.id.update_settings_button);
        userName=(EditText)findViewById(R.id.set_user_name);
        userStatus=(EditText)findViewById(R.id.set_profile_status);
        userProfileImage=(CircleImageView) findViewById(R.id.set_profile_image);
        loadingBar = new ProgressDialog(this);

        SettingToolBar = (Toolbar)findViewById(R.id.settings_toolbar);
        setSupportActionBar(SettingToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Setting");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==GalleryPick && resultCode==RESULT_OK && data!=null){
            Uri imageUri = data.getData();

            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK){
                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait your profile image is uplating...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                Uri resultUri = result.getUri();

                //filePath ...jpg
                final StorageReference filePath = UserProfileImagesRef.child(currentUserID + ".jpg");
                UploadTask uploadTask = filePath.putFile(resultUri);

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        downloadImageUrl = filePath.getDownloadUrl().toString();
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "Profile image uploaded successfully.",Toast.LENGTH_SHORT).show();
                            downloadImageUrl = task.getResult().toString();

                            RootRef.child("Users").child(currentUserID).child("image")
                                    .setValue(downloadImageUrl)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                Toast.makeText(SettingsActivity.this,"Image save in database, Successfuly...",Toast.LENGTH_SHORT).show();

                                                loadingBar.dismiss();}
                                            else{
                                                String message = task.getException().toString();
                                                Toast.makeText(SettingsActivity.this,"Error: "+message,Toast.LENGTH_SHORT).show();

                                                loadingBar.dismiss();
                                            }
                                        }
                                    });
                        } else {
                            String message = task.getException().toString();

                            Toast.makeText(SettingsActivity.this, "Error: "+message,Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });
            }
        }
    }

    private void UpdateSettings() {
        String setUserName = userName.getText().toString().trim();
        String setStatus = userStatus.getText().toString().trim();

        if(TextUtils.isEmpty(setUserName)){
            Toast.makeText(this, "Plase write your user name first...",Toast.LENGTH_SHORT).show();

        }
        if(TextUtils.isEmpty(setStatus)){
            Toast.makeText(this, "Plase write your status first...",Toast.LENGTH_SHORT).show();

        }else{
            HashMap<String, Object> profileMap = new HashMap<>();
                profileMap.put("uid", currentUserID);
                profileMap.put("name", setUserName);
                profileMap.put("status", setStatus);
                profileMap.put("image", downloadImageUrl);
            RootRef.child("Users").child(currentUserID).setValue(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                SendUserToMainActivity();
                                Toast.makeText(SettingsActivity.this, "Profile updated successfully...",Toast.LENGTH_SHORT).show();

                            }else {
                                String message = task.getException().toString();
                                Toast.makeText(SettingsActivity.this, "Error: "+message,Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }



    private void RetrieveUserInfo() {
        RootRef.child("Users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name")) && (dataSnapshot.hasChild("image"))){
                            String retrieveUserName = dataSnapshot.child("name").getValue().toString();
                            String retrieveStatus = dataSnapshot.child("status").getValue().toString();
                            String retrieveProfileImage = dataSnapshot.child("image").getValue().toString();

                            userName.setText(retrieveUserName);
                            userStatus.setText(retrieveStatus);
                            Picasso.get().load(retrieveProfileImage).into(userProfileImage);
                        }
                        else if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name"))){
                            String retrieveUserName = dataSnapshot.child("name").getValue().toString();
                            String retrieveStatus = dataSnapshot.child("status").getValue().toString();

                            userName.setText(retrieveUserName);
                            userStatus.setText(retrieveStatus);
                        }
                        else{
                            userName.setVisibility(View.VISIBLE);
                            Toast.makeText(SettingsActivity.this, "Please set & update your profile information", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }


    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
