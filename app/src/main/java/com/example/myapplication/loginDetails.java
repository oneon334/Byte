package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class loginDetails extends AppCompatActivity {

    private static final String TAG ="User Details" ;
    private CircleImageView profileImage;
    private EditText UserName;
    private FirebaseAuth mAuth;
    private Button finishbtn;
    private String Tag="";

    String UserID;
    StorageReference storageReference;
    private FirebaseFirestore firestore;
    private CheckBox radio1,radio2,radio3,radio4,radio5,radio6,radio7;
    private View parentLayout;
    private Boolean USERNAME_ALREADY=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_details);

        parentLayout = findViewById(android.R.id.content);

        radio1=findViewById(R.id.radioButton1);
        radio2=findViewById(R.id.radioButton2);
        radio3=findViewById(R.id.radioButton3);
        radio4=findViewById(R.id.radioButton4);
        radio5=findViewById(R.id.radioButton5);
        radio6=findViewById(R.id.radioButton6);
        radio7=findViewById(R.id.radioButton7);



        mAuth = FirebaseAuth.getInstance();
        profileImage = findViewById(R.id.circleImageView);
        UserName = findViewById(R.id.editTextTextPersonName);
        finishbtn = findViewById(R.id.finishBtn);
        firestore = FirebaseFirestore.getInstance();
        UserID = mAuth.getCurrentUser().getUid();
        storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference profileRef = storageReference.child("users/" + mAuth.getCurrentUser().getUid() + "/profile.jpg");



        profileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).into(profileImage);

            }
        });


     profileImage.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {

            Intent openGallery =new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(openGallery,1000);
         }
     });

     finishbtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {

             selectionListUpdate();
             if(Tag.isEmpty()){
                 Snackbar.make(parentLayout, "Select at least one", Snackbar.LENGTH_SHORT).show();
             }
             else if(UserName.getText().toString().isEmpty()){
                 UserName.setError("Username is empty");
             }
             else if(checkUsername(UserName.getText().toString())){
                 UserName.setError("Username already exist");
             }
             else{
                 FirebaseUser user = mAuth.getCurrentUser();
                 String UID=user.getUid();
                 Map<String, String> map=new HashMap<>();
                 map.put("Username", UserName.getText().toString());
                 map.put("Tag",Tag);
                 firestore.collection("Users").document(UID).set(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                     @Override
                     public void onSuccess(Void aVoid) {
                         Snackbar.make(parentLayout, "Welcome "+UserName.getText().toString(), Snackbar.LENGTH_SHORT).show();
                         Intent i=new Intent( loginDetails.this, HomeActivity.class);
                         startActivity(i);
                     }
                 });
             }
         }
     });



    }
    private boolean checkUsername(final String username) {
        firestore.collection("Users").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                Log.i(TAG, "onSuccess: checkUsername");
                if(queryDocumentSnapshots.isEmpty()){
                    USERNAME_ALREADY=false;
                }else {
                    List<DocumentSnapshot> snapshotList = queryDocumentSnapshots.getDocuments();
                    for (DocumentSnapshot snapshot : snapshotList) {
                        if (snapshot.getString("Username").equals(username)) {
                            Log.i(TAG, "onSuccess: Equal");
                            USERNAME_ALREADY = true;
                            break;
                        }
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                USERNAME_ALREADY=true;
            }
        });
        return USERNAME_ALREADY;
    }

    private void selectionListUpdate() {
        Log.i("selection", "selectionListUpdate: True");
        if(radio1.isChecked()){
            Log.i(TAG, "selectionListUpdate: web");
            Tag=Tag+"Web Development$";
        }
        if(radio2.isChecked()){
            Tag=Tag+"App Development$";
        }
        if(radio3.isChecked()){
            Tag=Tag+"Competitive Programming$";
        }
        if(radio4.isChecked()){
            Tag=Tag+"Politics$";
        }
        if(radio5.isChecked()){
            Tag=Tag+"T.V. Series$";
        }
        if(radio6.isChecked()){
            Tag=Tag+"Automobile$";
        }
        if(radio7.isChecked()){
            Tag=Tag+"Literature$";
        }
        Log.i(TAG, "onClick$"+Tag);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1000 && resultCode==RESULT_OK){

            Uri resultUri=data.getData();
            profileImage.setImageURI(resultUri);
            uploadImageTOFirebase(resultUri);
        }

    }

    private void uploadImageTOFirebase(Uri imageUri) {

            final StorageReference Fileref=storageReference.child("users/"+mAuth.getCurrentUser().getUid()+"/profile.jpg");
            Fileref.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Fileref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Picasso.get().load(uri).into(profileImage);
                        }
                    });


                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                   Toast.makeText(loginDetails.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });

    }
}