package com.example.admin.ghr;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AmbulanceSettingActivity extends AppCompatActivity {

    private String userId;
    private EditText mNameField,mPhoneField, mAmbulanceNoField, mAmbulanceHospitalField;
    private Button mConfirm,mBack;
    private FirebaseAuth mAuth;
    private String mName,mPhone,mAmbulanceNo,mAmbulanceHospital;
    private ImageView mProfileImage;
    private Uri resultUri;
    private String mProfileImageUrl;
    private DatabaseReference mAmbulanceDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ambulance_setting);

        mProfileImage=(ImageView)findViewById(R.id.ambulance_profileImage);
        mNameField=(EditText)findViewById(R.id.ambulance_Name);
        mPhoneField=(EditText)findViewById(R.id.ambulance_Phone);
        mBack=(Button)findViewById(R.id.back);
        mConfirm=(Button)findViewById(R.id.ambulance_confirm);
        mAmbulanceNoField=(EditText)findViewById(R.id.ambulance_no);
        mAmbulanceHospitalField=(EditText)findViewById(R.id.ambulance_hospital);
        mAuth= FirebaseAuth.getInstance();
        userId=mAuth.getCurrentUser().getUid();
        mAmbulanceDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child("Ambulance").child(userId);

        getUserInfo();

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveUserInformation();
            }
        });
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                return;
            }
        });

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });
    }

    private void getUserInfo() {

        mAmbulanceDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map=(Map<String,Object>)dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mName=map.get("name").toString();
                        mNameField.setText(mName);
                    }
                    if(map.get("phone")!=null){
                        mPhone=map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    }
                    if(map.get("ambulance no.")!=null){
                        mAmbulanceNo=map.get("ambulance no.").toString();
                        mAmbulanceNoField.setText(mAmbulanceNo);
                    }
                    if(map.get("hospital")!=null){
                        mAmbulanceHospital=map.get("hospital").toString();
                        mAmbulanceHospitalField.setText(mAmbulanceHospital);
                    }
                    if(map.get("profileImageUrl")!=null){
                        mProfileImageUrl=map.get("profileImageUrl").toString();
                        Glide.with(getApplicationContext()).load(mProfileImageUrl).into(mProfileImage);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInformation() {

        mName=mNameField.getText().toString();
        mPhone=mPhoneField.getText().toString();
        mAmbulanceNo= mAmbulanceNoField.getText().toString();
        mAmbulanceHospital= mAmbulanceHospitalField.getText().toString();
        mAmbulanceDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child("Ambulance").child(userId);
        Map userInfo=new HashMap();
        userInfo.put("name",mName);
        userInfo.put("phone",mPhone);
        userInfo.put("ambulance no.",mAmbulanceNo);
        userInfo.put("hospital",mAmbulanceHospital);
        mAmbulanceDatabase.updateChildren(userInfo);

        if(resultUri!=null){
            StorageReference filePath= FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
            Bitmap bitmap=null;
            try {
                bitmap= MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20,baos);
            byte [] data= baos.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl=taskSnapshot.getDownloadUrl();

                    Map newImage=new HashMap();
                    newImage.put("profileImageUrl",downloadUrl.toString());
                    mAmbulanceDatabase.updateChildren(newImage);

                    finish();
                    return;
                }
            });
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                }
            });
        }else {
            finish();
        }

        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1 && resultCode== Activity.RESULT_OK){
            final Uri imageUri= data.getData();
            resultUri=imageUri;
            mProfileImage.setImageURI(resultUri);
        }
    }
}
