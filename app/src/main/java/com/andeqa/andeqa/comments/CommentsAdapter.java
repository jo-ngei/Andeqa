package com.andeqa.andeqa.comments;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andeqa.andeqa.Constants;
import com.andeqa.andeqa.R;
import com.andeqa.andeqa.models.Andeqan;
import com.andeqa.andeqa.models.Comment;
import com.andeqa.andeqa.models.Relation;
import com.andeqa.andeqa.models.Timeline;
import com.andeqa.andeqa.profile.ProfileActivity;
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
 * Created by J.EL on 3/23/2018.
 */

public class CommentsAdapter extends RecyclerView.Adapter<CommentViewHolder> {
    private Context mContext;
    private FirebaseAuth firebaseAuth;
    private String mPostId;
    //firebase
    private DatabaseReference databaseReference;
    //firestore
    private CollectionReference usersCollection;
    private CollectionReference relationsCollection;
    private CollectionReference timelineCollection;
    private static final String COLLECTION_ID = "collection id";
    private static final String EXTRA_POST_ID = "post id";
    private static final String EXTRA_USER_UID = "uid";
    private static final String TAG = CommentsAdapter.class.getSimpleName();
    private boolean processFollow = false;
    private List<DocumentSnapshot> documentSnapshots = new ArrayList<>();



    public CommentsAdapter(Context mContext) {
        this.mContext = mContext;
    }

    protected void setPostComments(List<DocumentSnapshot> mSnapshots){
        this.documentSnapshots = mSnapshots;
        notifyDataSetChanged();
    }


    public DocumentSnapshot getSnapshot(int index) {
        return documentSnapshots.get(index);
    }


    @Override
    public int getItemCount() {
        return documentSnapshots.size();
    }

    @Override
    public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comments_layout_list, parent, false);
        return new CommentViewHolder(view);

    }

    @Override
    public void onBindViewHolder(final CommentViewHolder holder, int position) {
        Comment comment = getSnapshot(position).toObject(Comment.class);
        final String pushId = comment.getPushId();
        final String uid = comment.getUid();

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null){
            //firestore
            usersCollection = FirebaseFirestore.getInstance().collection(Constants.FIREBASE_USERS);
            relationsCollection = FirebaseFirestore.getInstance().collection(Constants.RELATIONS);
            timelineCollection = FirebaseFirestore.getInstance().collection(Constants.TIMELINE);
            //firebase
            databaseReference = FirebaseDatabase.getInstance().getReference(Constants.RANDOM_PUSH_ID);

        }

        //set the comment
        holder.mCommentTextView.setText(comment.getCommentText());

        holder.profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, ProfileActivity.class);
                intent.putExtra(CommentsAdapter.EXTRA_USER_UID, uid);
                mContext.startActivity(intent);
            }
        });

        //get the profile of the user wh just commented
        usersCollection.document(uid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
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

                    holder.usernameTextView.setText(username);
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


        relationsCollection.document("following").collection(firebaseAuth.getCurrentUser().getUid())
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
                    relationsCollection.document("followers")
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
                                            relationsCollection.document("followers").collection(uid)
                                                    .document(firebaseAuth.getCurrentUser().getUid()).set(follower)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Timeline timeline = new Timeline();
                                                            final long time = new Date().getTime();

                                                            final String postId = databaseReference.push().getKey();

                                                            timeline.setPushId(postId);
                                                            timeline.setTime(time);
                                                            timeline.setUid(firebaseAuth.getCurrentUser().getUid());
                                                            timeline.setType("followers");
                                                            timeline.setPostId(postId);
                                                            timelineCollection.document(uid).collection("timeline")
                                                                    .document(firebaseAuth.getCurrentUser().getUid())
                                                                    .set(timeline);
                                                        }
                                                    });
                                            final Relation following = new Relation();
                                            following.setUid(uid);
                                            relationsCollection.document("following").collection(firebaseAuth.getCurrentUser().getUid())
                                                    .document(uid).set(following);
                                            processFollow = false;
                                            holder.followButton.setText("Following");
                                        }else {
                                            relationsCollection.document("followers").collection(uid)
                                                    .document(firebaseAuth.getCurrentUser().getUid()).delete();
                                            relationsCollection.document("following").collection(firebaseAuth.getCurrentUser().getUid())
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