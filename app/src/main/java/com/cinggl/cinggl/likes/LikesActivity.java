package com.cinggl.cinggl.likes;

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
import android.widget.Button;
import android.widget.RelativeLayout;

import com.cinggl.cinggl.App;
import com.cinggl.cinggl.Constants;
import com.cinggl.cinggl.R;
import com.cinggl.cinggl.models.Cinggulan;
import com.cinggl.cinggl.models.Relation;
import com.cinggl.cinggl.viewholders.LikesViewHolder;
import com.cinggl.cinggl.models.Like;
import com.cinggl.cinggl.people.FollowerProfileActivity;
import com.cinggl.cinggl.profile.PersonalProfileActivity;
import com.cinggl.cinggl.viewholders.WhoLikedViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.ObservableSnapshotArray;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import de.hdodenhof.circleimageview.CircleImageView;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static com.cinggl.cinggl.R.id.fullNameTextView;
import static java.lang.System.load;

public class LikesActivity extends AppCompatActivity {
    @Bind(R.id.recentLikesRecyclerView)RecyclerView mRecentLikesRecyclerView;
    @Bind(R.id.emptyLikesRelativeLayout)RelativeLayout mEmptyRelativelayout;

    //firestore
    private CollectionReference relationsReference;
    private CollectionReference usersReference;
    private CollectionReference likesReference;
    private Query likesQuery;
    private FirebaseAuth firebaseAuth;
    private CircleImageView profileImageView;
    private String mPostKey;
    private FirebaseRecyclerAdapter firebaseRecyclerAdapter;
    private FirestoreRecyclerAdapter firestoreRecyclerAdapter;
    private Button followButton;
    private boolean processFollow = false;
    private static final String TAG = LikesActivity.class.getSimpleName();
    private static final String EXTRA_USER_UID = "uid";
    private static final String EXTRA_POST_KEY = "post key";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_likes);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null){
            mPostKey = getIntent().getStringExtra(EXTRA_POST_KEY);
            if(mPostKey == null){
                throw new IllegalArgumentException("pass an EXTRA_POST_KEY");
            }
            //firestore
            relationsReference = FirebaseFirestore.getInstance().collection(Constants.RELATIONS);
            usersReference = FirebaseFirestore.getInstance().collection(Constants.FIREBASE_USERS);
            likesReference = FirebaseFirestore.getInstance().collection(Constants.LIKES);
            postLikes();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

    }


    @Override
    public void onStop(){
        super.onStop();
    }

    public void postLikes(){
        likesQuery = likesReference.document(mPostKey).collection("likes").orderBy("uid");
        FirestoreRecyclerOptions<Like> options = new FirestoreRecyclerOptions.Builder<Like>()
                .setQuery(likesQuery, Like.class)
                .build();

        firestoreRecyclerAdapter = new FirestoreRecyclerAdapter<Like, LikesViewHolder>(options) {

            @Override
            protected void onBindViewHolder(final LikesViewHolder holder, int position, Like model) {
                holder.bindLikes(getSnapshots().getSnapshot(position));
                Like like = getSnapshots().getSnapshot(position).toObject(Like.class);
                final String postKey = like.getPushId();
                final String uid = like.getUid();


                if (mRecentLikesRecyclerView == null){
                    mEmptyRelativelayout.setVisibility(View.VISIBLE);
                }

                //get the profile of the user who just liked
                usersReference.document(uid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {

                        if (e != null) {
                            Log.w(TAG, "Listen error", e);
                            return;
                        }

                        if (documentSnapshot.exists()){
                            final Cinggulan cinggulan = documentSnapshot.toObject(Cinggulan.class);
                            final String profileImage = cinggulan.getProfileImage();
                            final String username = cinggulan.getUsername();
                            final String firstName = cinggulan.getFirstName();
                            final String secondName = cinggulan.getSecondName();


                            holder.usernameTextView.setText(username);
                            holder.fullNameTextView.setText(firstName + " " + secondName);

                            Picasso.with(LikesActivity.this)
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
                                            Picasso.with(LikesActivity.this)
                                                    .load(profileImage)
                                                    .fit()
                                                    .centerCrop()
                                                    .placeholder(R.drawable.profle_image_background)
                                                    .into(holder.profileImageView);


                                        }
                                    });

                        }
                    }
                });

                holder.profileImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (uid.equals(firebaseAuth.getCurrentUser().getUid())){
                            Intent intent = new Intent(LikesActivity.this, PersonalProfileActivity.class);
                            startActivity(intent);
                        }else {
                            Intent intent = new Intent(LikesActivity.this, FollowerProfileActivity.class);
                            intent.putExtra(LikesActivity.EXTRA_USER_UID, uid);
                            startActivity(intent);
                        }
                    }
                });

                relationsReference.document("following").collection(firebaseAuth.getCurrentUser().getUid())
                        .whereEqualTo("uid", uid).addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen error", e);
                            return;
                        }

                        if (documentSnapshots.isEmpty()){
                            holder.followButton.setText("Follow");
                        }else {
                            holder.followButton.setText("Following");
                        }
                    }
                });

                if (uid.equals(firebaseAuth.getCurrentUser().getUid())){
                    holder.followButton.setVisibility(View.GONE);
                }else {
                    holder.followButton.setVisibility(View.VISIBLE);
                    holder.followButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            processFollow = true;
                            relationsReference.document("followers")
                                    .collection(uid).whereEqualTo("uid", firebaseAuth.getCurrentUser().getUid())
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
                                                    relationsReference.document("followers").collection(uid)
                                                            .document(firebaseAuth.getCurrentUser().getUid()).set(follower);
                                                    final Relation following = new Relation();
                                                    following.setUid(uid);
                                                    relationsReference.document("following").collection(firebaseAuth.getCurrentUser().getUid())
                                                            .document(uid).set(following);
                                                    processFollow = false;
                                                    holder.followButton.setText("Following");
                                                }else {
                                                    relationsReference.document("followers").collection(uid)
                                                            .document(firebaseAuth.getCurrentUser().getUid()).delete();
                                                    relationsReference.document("following").collection(firebaseAuth.getCurrentUser().getUid())
                                                            .document(uid).delete();
                                                    processFollow = false;
                                                    holder.followButton.setText("Follow");
                                                }
                                            }
                                        }
                                    });

                        }
                    });
                }

            }

            @Override
            public ObservableSnapshotArray<Like> getSnapshots() {
                return super.getSnapshots();
            }

            @Override
            public LikesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate
                        (R.layout.likes_list_layout, parent, false);
                return new LikesViewHolder(view);

            }

            @Override
            public int getItemCount() {
                return super.getItemCount();
            }
        };

        mRecentLikesRecyclerView.setAdapter(firestoreRecyclerAdapter);
        firestoreRecyclerAdapter.startListening();
        mRecentLikesRecyclerView.setHasFixedSize(false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setAutoMeasureEnabled(true);
        mRecentLikesRecyclerView.setLayoutManager(layoutManager);
    }

}