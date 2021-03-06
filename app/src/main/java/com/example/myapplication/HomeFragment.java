package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment implements com.example.myapplication.latestAdapter.SelectedItem, trendViewPagerAdapter.SelectedPager {

   private RecyclerView recyclerView, upVoteRecycler;
   private List<modelLatest> item_list, upVote_list;
   private com.example.myapplication.latestAdapter latestAdapter, upVoteAdapter;
   private FirebaseFirestore firestore;
   private FirebaseAuth mAuth;
    private ImageView postSearchBtn;
   private List<String> Tag;
   private ViewPager2 trendViewPager;
   private List<trendViewPagerModel> pagerModels;
   private trendViewPagerAdapter pagerAdapter;
   private Query query;
   private NestedScrollView scrollHome;
   private static Integer LAST_VISIBLE=1;
   private static Integer IS_LOADING=0;
    private DocumentSnapshot lastVisible;

   //loading dialog box
   private AlertDialog.Builder builder;
   private AlertDialog show;

   private ProgressBar loadPost;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root=inflater.inflate(R.layout.fragment_home,container,false);


        trendViewPager=root.findViewById(R.id.trendViewPager);

        scrollHome=root.findViewById(R.id.scrollHome);
        loadPost=root.findViewById(R.id.loadPost);

        upVoteRecycler=root.findViewById(R.id.upvoteView);
        upVoteRecycler.setHasFixedSize(true);
        upVoteRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        upVote_list=new ArrayList<>();
        upVoteAdapter=new latestAdapter(upVote_list,this);
        upVoteRecycler.setAdapter(upVoteAdapter);
        upVoteRecycler.setNestedScrollingEnabled(false);

        //Loading Dialog Box
        builder=new AlertDialog.Builder(getContext());
        builder.setView(R.layout.loading_dailog);
        builder.setCancelable(true);
        show = builder.show();
        show.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        pagerModels=new ArrayList<>();

        recyclerView=root.findViewById(R.id.latestView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        postSearchBtn=root.findViewById(R.id.postSearchbtn);
        postSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(getActivity(), PostSearch.class);
                startActivity(i);
            }
        });

        mAuth=FirebaseAuth.getInstance();
        firestore=FirebaseFirestore.getInstance();

        item_list=new ArrayList<>();

        latestAdapter= new latestAdapter(item_list,this);
        latestAdapter.setHasStableIds(true);
        recyclerView.setAdapter(latestAdapter);

        recyclerView.setHasFixedSize(true);

        Tag=new ArrayList<>();

        firestore.collection("Users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                Tag= (List<String>) documentSnapshot.get("Tag");
                setLatestPost();
                setupViewPager();
                setUpVotePost();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("getTagFailed", "onFailure: "+e.getMessage());
            }
        });

        recyclerView.setNestedScrollingEnabled(false);


        return root;

    }


    private void setUpVotePost() {
        firestore.collection("Post")
                .orderBy("UpVote", Query.Direction.DESCENDING)
                .whereIn("tag",Tag)
                .limit(2).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                for (QueryDocumentSnapshot doc : value) {

                    Log.i("dataRecieveCHeck", "onEvent:" + value.size());
                    if(Tag.contains(doc.getString("tag"))) {
                        modelLatest set = doc.toObject(modelLatest.class);
                        upVote_list.add(set);
                        upVoteAdapter.notifyDataSetChanged();
                    }
                }

            }
        });
    }

    private void setupViewPager() {
        Date date=new Date();
        Calendar calendar=Calendar.getInstance();
        calendar.set(date.getYear(),date.getMonth(),date.getDay(),00,00,0);
        Log.i("Date", "setupViewPager: "+date);

        Query query=firestore.collection("Post")
                .orderBy("trend", Query.Direction.DESCENDING)
                .whereIn("tag",Tag);

            query.limit(6).addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                    for (QueryDocumentSnapshot doc : value) {

                        Log.i("dataRecieveCHeck", "onEvent:" + value.size());
                        //if(Tag.contains(doc.getString("tag"))) {
                            trendViewPagerModel set = doc.toObject(trendViewPagerModel.class);
                            pagerModels.add(set);
                            pagerAdapter.notifyDataSetChanged();
                        //}
                    }
                }
            });
            Log.i("ViewPager", "setupViewPager: "+pagerModels.size());
        //View pager basic settings
        pagerAdapter=new trendViewPagerAdapter(pagerModels,this);
        trendViewPager.setAdapter(pagerAdapter);
        trendViewPager.setPadding(0,0,0,0);
        trendViewPager.setClipToPadding(false);
        trendViewPager.setClipChildren(false);
        trendViewPager.setOffscreenPageLimit(4);

    }

    private void setLatestPost() {
        LAST_VISIBLE=1;
        IS_LOADING=1;
        query=firestore.collection("Post")
                .orderBy("time", Query.Direction.DESCENDING)
                .whereIn("tag",Tag)
                .limit(10);
        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if(queryDocumentSnapshots.isEmpty()){
                    Log.i("LatestPost", "onSuccess: Empty");
                }else{
                    //item_list.clear();
                    List<DocumentSnapshot> snapshotList=queryDocumentSnapshots.getDocuments();
                    for(DocumentSnapshot snapshot:snapshotList){
                        item_list.add(snapshot.toObject(modelLatest.class));
                    }
                    latestAdapter.notifyDataSetChanged();
                    lastVisible = snapshotList.get(snapshotList.size() -1);
                    show.dismiss();
                    if(snapshotList.size()<10){
                        LAST_VISIBLE=0;
                    }
                    IS_LOADING=0;
                    scrollHome.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                        @Override
                        public void onScrollChanged()
                        {
                            View view = (View)scrollHome.getChildAt(scrollHome.getChildCount() - 1);

                            int diff = (view.getBottom() - (scrollHome.getHeight() + scrollHome
                                    .getScrollY()));

                            if (diff == 0 && LAST_VISIBLE==1 && IS_LOADING==0) {
                                loadPost.setVisibility(View.VISIBLE);
                                loadLatestPost();
                            }
                        }
                    });
                }
            }
        });


    }

    private void loadLatestPost(){
        IS_LOADING=1;
        Query nextquery=firestore.collection("Post")
                .orderBy("time", Query.Direction.DESCENDING)
                .whereIn("tag",Tag)
                .startAfter(lastVisible)
                .limit(10);
        nextquery.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if(queryDocumentSnapshots.isEmpty()){
                    Log.i("LatestPost", "onSuccess: Empty");
                    loadPost.setVisibility(View.GONE);
                }else if(item_list.size()%10==0) {
                    List<DocumentSnapshot> snapshots = queryDocumentSnapshots.getDocuments();
                    for(DocumentSnapshot doc: snapshots){
                        item_list.add(doc.toObject(modelLatest.class));
                    }
                    loadPost.setVisibility(View.GONE);
                    latestAdapter.notifyDataSetChanged();
                    lastVisible=snapshots.get(snapshots.size()-1);
                    IS_LOADING=0;
                    if(snapshots.size()<10){
                        Log.i("LatestPost", "onScrollChanged: limit reached");
                        LAST_VISIBLE=0;
                    }
                    scrollHome.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                        @Override
                        public void onScrollChanged()
                        {
                            View view = (View)scrollHome.getChildAt(scrollHome.getChildCount() - 1);

                            int diff = (view.getBottom() - (scrollHome.getHeight() + scrollHome
                                    .getScrollY()));

                            if (diff == 0 && LAST_VISIBLE==1 && IS_LOADING==0) {
                                Log.i("LatestPost", "onScrollChanged: More");
                                loadPost.setVisibility(View.VISIBLE);
                                loadLatestPost();
                            }
                        }
                    });
                }
            }
        });
    }



    @Override
    public void selectedItem(modelLatest model_latest) {
        Intent i=new Intent(getActivity(),ViewPost.class);
        i.putExtra("PostId",model_latest.ID);
        startActivity(i);
    }

    public void selectedpager(trendViewPagerModel model) {
        Intent i=new Intent(getActivity(),ViewPost.class);
        i.putExtra("PostId",model.getID());
        startActivity(i);
    }
}
