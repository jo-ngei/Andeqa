package com.cinggl.cinggl.people;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cinggl.cinggl.App;
import com.cinggl.cinggl.Constants;
import com.cinggl.cinggl.R;
import com.cinggl.cinggl.models.Relation;
import com.cinggl.cinggl.viewholders.PeopleViewHolder;
import com.cinggl.cinggl.models.Cinggulan;
import com.cinggl.cinggl.profile.PersonalProfileActivity;
import com.firebase.ui.common.ChangeEventType;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.ObservableSnapshotArray;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

public class FollowingActivity extends AppCompatActivity {
    //firestore references
    private CollectionReference relationsReference;
    private CollectionReference usersReference;
    private Query followingQuery;
    //adapters
    private FirestoreRecyclerAdapter firestoreRecyclerAdapter;
    //firebase auth
    private FirebaseAuth firebaseAuth;
    private String mUid;
    private boolean processFollow = false;
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


        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser()!= null){
            mUid = getIntent().getStringExtra(EXTRA_USER_UID);
            if(mUid == null){
                throw new IllegalArgumentException("pass an EXTRA_USER_UID");
            }
            Log.d("passed uid", mUid);

            relationsReference = FirebaseFirestore.getInstance().collection(Constants.RELATIONS);
            usersReference = FirebaseFirestore.getInstance().collection(Constants.FIREBASE_USERS);
            followingQuery = relationsReference.document("following").collection(mUid);

            retrieveFollowing();
            firestoreRecyclerAdapter.startListening();
        }
    }

    private void retrieveFollowing(){
        followingQuery.orderBy("uid");
        FirestoreRecyclerOptions<Relation> options = new FirestoreRecyclerOptions.Builder<Relation>()
                .setQuery(followingQuery, Relation.class)
                .build();

        firestoreRecyclerAdapter = new FirestoreRecyclerAdapter<Relation, PeopleViewHolder>
                (options) {
            @Override
            protected void onBindViewHolder(final PeopleViewHolder holder, int position, Relation model) {
                //postKey is the uid of the cinggulan user is following
                final String postKey = getSnapshots().get(position).getUid();
                holder.bindPeople(model);
                Log.d("following postKey", postKey);

                //post key and uid are same
                usersReference.document(postKey).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen error", e);
                            return;
                        }

                        if (documentSnapshot.exists()){
                            Cinggulan cinggulan =  documentSnapshot.toObject(Cinggulan.class);
                            final String profileImage = cinggulan.getProfileImage();
                            final String firstName = cinggulan.getFirstName();
                            final String secondName = cinggulan.getSecondName();
                            final String username = cinggulan.getUsername();
                            final String uid = cinggulan.getUid();
                            Log.d("following uid fa", uid);

                            holder.usernameTextView.setText(username);
                            holder.fullNameTextView.setText(firstName + " " + secondName);
                            Picasso.with(FollowingActivity.this)
                                    .load(profileImage)
                                    .fit()
                                    .centerCrop()
                                    .placeholder(R.drawable.profle_image_background)
                                    .networkPolicy(NetworkPolicy.OFFLINE)
                                    .into(holder.profileImageView, new Callback() {
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
                                                    .into(holder.profileImageView);


                                        }
                                    });

                            //lauch user profile
                            holder.profileImageView.setOnClickListener(new View.OnClickListener() {
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

                            //show if following or not
                            relationsReference.document("following").collection(mUid)
                                    .whereEqualTo("uid", postKey)
                                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                        @Override
                                        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                                            if (documentSnapshots.isEmpty()){
                                                holder.followButton.setText("Unfollow");
                                            }else {
                                                holder.followButton.setText("Following");
                                            }
                                        }
                                    });

                            //follow or unfollow
                            if (uid.equals(firebaseAuth.getCurrentUser().getUid())){
                                holder.followButton.setVisibility(View.GONE);
                            }else {
                                holder.followButton.setVisibility(View.VISIBLE);
                                holder.followButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        processFollow = true;
                                        relationsReference.document("followers")
                                                .collection(postKey)
                                                .whereEqualTo("uid", firebaseAuth.getCurrentUser().getUid())
                                                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {


                                                        if (e != null) {
                                                            Log.w(TAG, "Listen error", e);
                                                            return;
                                                        }

                                                        if (processFollow){
                                                            if (documentSnapshots.isEmpty()){
                                                                //set followers and following
                                                                Relation follower = new Relation();
                                                                follower.setUid(firebaseAuth.getCurrentUser().getUid());
                                                                relationsReference.document("followers").collection(postKey)
                                                                        .document(firebaseAuth.getCurrentUser().getUid()).set(follower);
                                                                final Relation following = new Relation();
                                                                following.setUid(postKey);
                                                                relationsReference.document("following").collection(firebaseAuth.getCurrentUser().getUid())
                                                                        .document(postKey).set(following);
                                                                processFollow = false;
                                                                holder.followButton.setText("Following");
                                                            }else {
                                                                relationsReference.document("followers").collection(postKey)
                                                                        .document(firebaseAuth.getCurrentUser().getUid()).delete();
                                                                relationsReference.document("following").collection(firebaseAuth.getCurrentUser().getUid())
                                                                        .document(postKey).delete();
                                                                processFollow = false;
                                                                holder.followButton.setText("Unfollow");
                                                            }
                                                        }
                                                    }
                                                });

                                    }
                                });

                            }
                        }



                    }
                });



            }

            @Override
            public PeopleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.poeple_list, parent, false);
                return new PeopleViewHolder(view);
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
            }

            @Override
            public void onError(FirebaseFirestoreException e) {
                super.onError(e);
            }

            @Override
            public void onChildChanged(ChangeEventType type, DocumentSnapshot snapshot, int newIndex, int oldIndex) {
                super.onChildChanged(type, snapshot, newIndex, oldIndex);
            }

            @Override
            public int getItemCount() {
                return super.getItemCount();
            }

            @Override
            public Relation getItem(int position) {
                return super.getItem(position);
            }

            @Override
            public ObservableSnapshotArray<Relation> getSnapshots() {
                return super.getSnapshots();
            }
        };
        mFollowingRecyclerView.setAdapter(firestoreRecyclerAdapter);
        mFollowingRecyclerView.setHasFixedSize(false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        layoutManager.onSaveInstanceState();
        layoutManager.setAutoMeasureEnabled(true);
        mFollowingRecyclerView.setLayoutManager(layoutManager);
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
    public void onDestroy() {
        super.onDestroy();
        firestoreRecyclerAdapter.stopListening();
    }
}