package com.cinggl.cinggl.relations;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.EventLog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cinggl.cinggl.Constants;
import com.cinggl.cinggl.R;
import com.cinggl.cinggl.adapters.PeopleViewHolder;
import com.cinggl.cinggl.models.Cingulan;
import com.cinggl.cinggl.profile.PersonalProfileActivity;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class FollowingActivity extends AppCompatActivity {
    private DatabaseReference followingRef;
    private DatabaseReference relationsRef;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference usernameRef;
    private Query followingQueary;
    private TextView firstNameTextView;
    private TextView secondNameTextView;
    private CircleImageView profileImageView;
    private Button followButton;
    private String mUid;
    private boolean processFollow = false;
    private FirebaseRecyclerAdapter firebaseRecyclerAdapter;
    private static final String TAG = FollowersFragment.class.getSimpleName();
    private static final String EXTRA_USER_UID = "uid";

    @Bind(R.id.followingRecyclerView)RecyclerView mFollowingRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //ON NAVIGATING BACK
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mUid = getIntent().getStringExtra(EXTRA_USER_UID);
        if(mUid == null){
            throw new IllegalArgumentException("pass an EXTRA_USER_UID");
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        usernameRef = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_USERS);
        followingRef = FirebaseDatabase.getInstance().getReference(Constants.RELATIONS);

        usernameRef.keepSynced(true);

        retrieveFollowing();
    }

    public void retrieveFollowing(){
        relationsRef = FirebaseDatabase.getInstance().getReference(Constants.RELATIONS).child("following")
                .child(mUid);
        relationsRef.keepSynced(true);


        //Retrive any child that has the curent user uid
        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Cingulan, PeopleViewHolder>
                (Cingulan.class, R.layout.followers_list, PeopleViewHolder.class, relationsRef){
            @Override
            protected void populateViewHolder(final PeopleViewHolder viewHolder, final Cingulan model, int position) {
                DatabaseReference userRef = getRef(position);
                final String postKey = userRef.getKey();
                viewHolder.bindPeople(model);

                relationsRef.child(postKey).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String uid = (String) dataSnapshot.child("uid").getValue();

                        try {
                            usernameRef.child(uid).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    final String profileImage = (String) dataSnapshot.child("profileImage").getValue();
                                    final String firstName = (String) dataSnapshot.child("firstName").getValue();
                                    final String secondName = (String) dataSnapshot.child("secondName").getValue();

                                    try {
                                        viewHolder.firstNameTextView.setText(firstName);
                                        viewHolder.secondNameTextView.setText(secondName);

                                        Picasso.with(FollowingActivity.this)
                                                .load(profileImage)
                                                .fit()
                                                .centerCrop()
                                                .placeholder(R.drawable.profle_image_background)
                                                .networkPolicy(NetworkPolicy.OFFLINE)
                                                .into(viewHolder.profileImageView, new Callback() {
                                                    @Override
                                                    public void onSuccess() {

                                                    }

                                                    @Override
                                                    public void onError() {
                                                        Picasso.with(FollowingActivity.this)
                                                                .load(profileImage)
                                                                .fit()
                                                                .centerCrop()
                                                                .placeholder(R.drawable.profle_image_background)
                                                                .into(viewHolder.profileImageView);


                                                    }
                                                });
                                    }catch (Exception e){

                                    }

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }catch (Exception e){

                        }

                        viewHolder.profileImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (uid.equals(firebaseAuth.getCurrentUser().getUid())){
                                    Intent intent = new Intent(FollowingActivity.this, PersonalProfileActivity.class);
                                    startActivity(intent);
                                }else {
                                    Intent intent = new Intent(FollowingActivity.this, FollowerProfileActivity.class);
                                    intent.putExtra(FollowingActivity.EXTRA_USER_UID, uid);
                                    startActivity(intent);
                                }
                            }
                        });

                        if (uid.equals(firebaseAuth.getCurrentUser().getUid())){
                            viewHolder.followButton.setVisibility(View.GONE);
                        }else {
                            viewHolder.followButton.setVisibility(View.VISIBLE);

                            viewHolder.followButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    processFollow = true;
                                    followingRef.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            if (processFollow){
                                                if (dataSnapshot.child("followers").child(postKey).hasChild(firebaseAuth.getCurrentUser().getUid())){

                                                    //remove the uid from the person followed
                                                    followingRef.child("followers").child(postKey).child(firebaseAuth.getCurrentUser().getUid())
                                                            .removeValue();

                                                    //remove the person uid is following from the uid
                                                    followingRef.child("following").child(firebaseAuth.getCurrentUser().getUid()).child(postKey)
                                                            .removeValue();

                                                    processFollow = false;
                                                    onFollow(false);
                                                    //set the text on the button to follow if the user in not yet following;
//                                        followButton.setText("FOLLOW");

                                                }else {
                                                    try {
                                                        //add uid to the uid of the person followed
                                                        followingRef.child("followers").child(postKey).child(firebaseAuth.getCurrentUser().getUid())
                                                                .child("uid").setValue(firebaseAuth.getCurrentUser().getUid());

                                                        //add uid of the person followed to the uid that is folowing
                                                        followingRef.child("following").child(firebaseAuth.getCurrentUser().getUid()).child(postKey)
                                                                .child("uid").setValue(postKey);

                                                        processFollow = false;
                                                        onFollow(false);

                                                        //set text on the button to following;
                                                        viewHolder.followButton.setText("Following");

                                                    }catch (Exception e){

                                                    }

                                                }

                                            }

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }
                            });

                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                followingRef.child("following").child(postKey)
                        .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.hasChild(firebaseAuth.getCurrentUser().getUid())){
                            viewHolder.followButton.setText("Unfollow");
                        }else {
                            viewHolder.followButton.setText("Follow");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        };

        mFollowingRecyclerView.setAdapter(firebaseRecyclerAdapter);
        mFollowingRecyclerView.setHasFixedSize(false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        layoutManager.onSaveInstanceState();
        layoutManager.setAutoMeasureEnabled(true);
        mFollowingRecyclerView.setLayoutManager(layoutManager);
    }

    private void onFollow(final boolean increament){
        relationsRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() != null){
                    int value = mutableData.getValue(Integer.class);
                    if(increament){
                        value++;
                    }else{
                        value--;
                    }
                    mutableData.setValue(value);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
                Log.d(TAG, "followTransaction:onComplete" + databaseError);

            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        firebaseRecyclerAdapter.cleanup();
    }
}