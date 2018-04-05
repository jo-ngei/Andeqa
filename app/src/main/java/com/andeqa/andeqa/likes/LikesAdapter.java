package com.andeqa.andeqa.likes;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.andeqa.andeqa.Constants;
import com.andeqa.andeqa.R;
import com.andeqa.andeqa.models.Andeqan;
import com.andeqa.andeqa.models.Like;
import com.andeqa.andeqa.models.Relation;
import com.andeqa.andeqa.models.Timeline;
import com.andeqa.andeqa.profile.ProfileActivity;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by J.EL on 4/5/2018.
 */

public class LikesAdapter extends RecyclerView.Adapter<LikesViewHolder> {
    //context
    private Context mContext;
    //firestore
    private CollectionReference relationsReference;
    private CollectionReference usersReference;
    private CollectionReference timelineCollection;
    //firestore
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private boolean processFollow = false;
    private static final String TAG = LikesActivity.class.getSimpleName();
    private static final String EXTRA_USER_UID = "uid";

    private List<DocumentSnapshot> documentSnapshots = new ArrayList<>();


    public LikesAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void setPostLikes(List<DocumentSnapshot> likes){
        this.documentSnapshots = likes;
        notifyDataSetChanged();
    }

    protected DocumentSnapshot getSnapshot(int index) {
        return documentSnapshots.get(index);
    }



    @Override
    public int getItemCount() {
        return documentSnapshots.size();
    }

    @Override
    public LikesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.likes_list_layout, parent, false);
        return new LikesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final LikesViewHolder holder, int position) {
        Like like = getSnapshot(position).toObject(Like.class);
        final String postKey = like.getPushId();
        final String uid = like.getUid();

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null){

            //firestore
            relationsReference = FirebaseFirestore.getInstance().collection(Constants.RELATIONS);
            usersReference = FirebaseFirestore.getInstance().collection(Constants.FIREBASE_USERS);
            timelineCollection = FirebaseFirestore.getInstance().collection(Constants.TIMELINE);
            databaseReference = FirebaseDatabase.getInstance().getReference(Constants.RANDOM_PUSH_ID);

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
                    final Andeqan cinggulan = documentSnapshot.toObject(Andeqan.class);
                    final String profileImage = cinggulan.getProfileImage();
                    final String username = cinggulan.getUsername();
                    final String firstName = cinggulan.getFirstName();
                    final String secondName = cinggulan.getSecondName();


                    holder.usernameTextView.setText(username);
                    holder.fullNameTextView.setText(firstName + " " + secondName);

                    Picasso.with(mContext)
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
                                    Picasso.with(mContext)
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
                Intent intent = new Intent(mContext, ProfileActivity.class);
                intent.putExtra(LikesAdapter.EXTRA_USER_UID, uid);
                mContext.startActivity(intent);
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
                                                    .document(firebaseAuth.getCurrentUser().getUid()).set(follower)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Timeline timeline = new Timeline();
                                                            final long time = new Date().getTime();

                                                            final String postId = databaseReference.push().getKey();

                                                            timeline.setPostId(postId);
                                                            timeline.setTime(time);
                                                            timeline.setUid(firebaseAuth.getCurrentUser().getUid());
                                                            timeline.setType("followers");
                                                            timeline.setPushId(uid);
                                                            timeline.setStatus("unRead");
                                                            timelineCollection.document(uid).collection("timeline")
                                                                    .document(firebaseAuth.getCurrentUser().getUid())
                                                                    .set(timeline);
                                                        }
                                                    });
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


}
