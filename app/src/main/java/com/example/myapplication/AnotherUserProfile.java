package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.picasso.transformations.BlurTransformation;

public class AnotherUserProfile extends AppCompatActivity {

    private TextView posts;
    private TextView followers;
    private TextView following;
    private CircleImageView AnotherUserProfileImageView;
    private ImageView CoverPhoto;
    private Button FollowButton;
    private FirebaseFirestore fstore;
    private FirebaseAuth mAuth;
    private StorageReference storageReference;
    private String UserID;
    private String AnotherUserId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_another_user_profile);

        mAuth = FirebaseAuth.getInstance();
        UserID = mAuth.getCurrentUser().getUid();
        fstore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        FollowButton=findViewById(R.id.FollowBtn);

        AnotherUserProfileImageView=findViewById(R.id.anotherProfileImage);
        CoverPhoto=findViewById(R.id.anotherUserCover);

        followers=findViewById(R.id.followers);
        following=findViewById(R.id.following);
        posts=findViewById(R.id.post);

        Intent intent=getIntent();
        AnotherUserId=intent.getStringExtra("SearchUserID");

        StorageReference profileRef = storageReference.child("users/" +AnotherUserId+ "/profile.jpg");
        profileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                //setting profile picture
                Picasso.get().load(uri).into(AnotherUserProfileImageView);
                //setting cover image
                Picasso.get().load(uri).transform(new BlurTransformation(AnotherUserProfile.this,25,3)).into(CoverPhoto);
            }
        });

        updateUserDetails();



       /* CollectionReference collectionReference=fstore.collection("Users").document(UserID).collection("Following");
        final Query query=collectionReference.whereEqualTo("ProfileId",AnotherUserId);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                        FollowButton.setText("FOLLOWING");
                }
            }
        });*/

        fstore.collection("Users").document(UserID).collection("Following")
                .whereEqualTo("ProfileId",AnotherUserId).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(value.isEmpty()){
                    Log.i("status", "onEvent: Do not follow");
                }else{
                    FollowButton.setText("FOLLOWING");
                }
            }
        });


           FollowButton.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {

                   if(FollowButton.getText().toString().equals("FOLLOW")) {

                       Map<String, Object> map = new HashMap();
                       map.put("ProfileId", AnotherUserId);

                       Map<String, Object> map1 = new HashMap();
                       map1.put("ProfileId", UserID);

                       Toast.makeText(AnotherUserProfile.this, "You Start Following", Toast.LENGTH_LONG).show();
                       fstore.collection("Users").document(UserID).collection("Following").add(map);
                       fstore.collection("Users").document(AnotherUserId).collection("Followers").add(map1);
                       fstore.collection("Users").document(UserID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                           @Override
                           public void onSuccess(DocumentSnapshot documentSnapshot) {
                               Double HisFollowing = documentSnapshot.getDouble("Following");
                               fstore.collection("Users").document(UserID).update("Following", HisFollowing + 1);
                           }
                       });
                       fstore.collection("Users").document(AnotherUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                           @Override
                           public void onSuccess(DocumentSnapshot documentSnapshot) {
                               Double AnotherFollower = documentSnapshot.getDouble("Followers");
                               fstore.collection("Users").document(AnotherUserId).update("Followers", AnotherFollower + 1);
                               updateUserDetails();
                           }
                       });

                   }
                   else
                   {


                       fstore.collection("Users").document(UserID).collection("Following")
                               .whereEqualTo("ProfileId",AnotherUserId).addSnapshotListener(new EventListener<QuerySnapshot>() {
                           @Override
                           public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                for(DocumentSnapshot doc: value){
                                    doc.getReference().delete();
                                }
                           }
                       });
                       fstore.collection("Users").document(AnotherUserId).collection("Followers")
                               .whereEqualTo("ProfileId",UserID).addSnapshotListener(new EventListener<QuerySnapshot>() {
                           @Override
                           public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                               for(DocumentSnapshot doc: value){
                                   doc.getReference().delete();
                               }
                           }
                       });


                       fstore.collection("Users").document(UserID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                           @Override
                           public void onSuccess(DocumentSnapshot documentSnapshot) {
                               Double HisFollowing = documentSnapshot.getDouble("Following");
                               fstore.collection("Users").document(UserID).update("Following", HisFollowing -1);
                           }
                       });
                       fstore.collection("Users").document(AnotherUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                           @Override
                           public void onSuccess(DocumentSnapshot documentSnapshot) {
                               Double AnotherFollower = documentSnapshot.getDouble("Followers");
                               fstore.collection("Users").document(AnotherUserId).update("Followers", AnotherFollower -1);
                               updateUserDetails();
                           }
                       });


                      FollowButton.setText("FOLLOW");

                   }

               }
           });


    }

    private void updateUserDetails() {
        fstore.collection("Users").document(AnotherUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {


                // getting value of number of followers,following and posts from database and showing in this activity..

                Double mFollowers=documentSnapshot.getDouble("Followers");
                Double mFollowing=documentSnapshot.getDouble("Following");
                Double mPost=documentSnapshot.getDouble("Post");
                followers.setText(String.format("%.0f", mFollowers));
                following.setText(String.format("%.0f", mFollowing));
                posts.setText(String.format("%.0f", mPost));




            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }


    private void addProfileFollowing() {
        Map<String,Object> map=new HashMap();
        map.put("ProfileId",AnotherUserId);

        Map<String,Object> map1=new HashMap();
        map1.put("ProfileId",UserID);

        fstore.collection("Users").document(UserID).collection("Following").add(map);
        fstore.collection("Users").document(AnotherUserId).collection("Followers").add(map1);
        fstore.collection("Users").document(UserID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                String f2=documentSnapshot.getString("Following");
                int fllwing=Integer.parseInt(f2);
                fllwing=fllwing+1;
                String UpdatedFollowing=Integer.toString(fllwing);
                fstore.collection("Users").document(UserID).update("Following",fllwing);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
            }
        });

        fstore.collection("Users").document(AnotherUserId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                String f3=documentSnapshot.getString("Followers");
                int fllowers=Integer.parseInt(f3);
                 fllowers=fllowers+1;
                String UpdatedFollowers=Integer.toString(fllowers);
                fstore.collection("Users").document(UserID).update("Followers",fllowers);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
            }
        });


        }
}