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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class VictimSettingActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener  {

    private Spinner mSpinner;
    private String mBlood,userId;
    private EditText mNameField,mPhoneField,mAllergyField;
    private Button mConfirm,mBack;
    private FirebaseAuth mAuth;
    private DatabaseReference mCustomerDatabase;
    private String mName,mPhone,mAllergy;
    private ImageView mProfileImage;
    private Uri resultUri;
    private String mProfileImageUrl;
    ArrayAdapter<CharSequence> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_victim_setting);

        mProfileImage=(ImageView)findViewById(R.id.profileImage);
        mSpinner = (Spinner) findViewById(R.id.bloodgroup);
        adapter = ArrayAdapter.createFromResource(this,
                R.array.blood_group_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(this);

        mNameField=(EditText)findViewById(R.id.victimName);
        mPhoneField=(EditText)findViewById(R.id.victimPhone);
        mAllergyField=(EditText)findViewById(R.id.allergy);
        mBack=(Button)findViewById(R.id.back);
        mConfirm=(Button)findViewById(R.id.confirm);
        mAuth=FirebaseAuth.getInstance();
        userId=mAuth.getCurrentUser().getUid();
        mCustomerDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child("Victim").child(userId);

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



    private void getUserInfo(){
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
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
                    if(map.get("allergy")!=null){
                        mAllergy=map.get("allergy").toString();
                        mAllergyField.setText(mAllergy);
                    }
                    if(map.get("bloodGroup")!=null){
                        mBlood=map.get("bloodGroup").toString();
                        int spinnerPosition = adapter.getPosition(mBlood);
                        mSpinner.setSelection(spinnerPosition);
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
            mAllergy=mAllergyField.getText().toString();

            Map userInfo=new HashMap();
            userInfo.put("name",mName);
            userInfo.put("phone",mPhone);
            userInfo.put("bloodGroup",mBlood);
            userInfo.put("allergy",mAllergy);
            mCustomerDatabase.updateChildren(userInfo);

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
                        mCustomerDatabase.updateChildren(newImage);

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
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        mBlood=adapterView.getItemAtPosition(i).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

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
