package com.andeqa.andeqa.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andeqa.andeqa.Constants;
import com.andeqa.andeqa.R;
import com.andeqa.andeqa.comments.CommentsActivity;
import com.andeqa.andeqa.impressions.ImpressionTracker;
import com.andeqa.andeqa.models.Andeqan;
import com.andeqa.andeqa.models.CollectionPost;
import com.andeqa.andeqa.models.Like;
import com.andeqa.andeqa.models.Post;
import com.andeqa.andeqa.models.Timeline;
import com.andeqa.andeqa.player.Player;
import com.andeqa.andeqa.profile.ProfileActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
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
import com.google.firebase.firestore.QuerySnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.WeakHashMap;

public class HomePostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ImpressionTracker.VisibilityTrackerListener{
    private static final String TAG =  PostsAdapter.class.getSimpleName();
    private Context mContext;
    private static final String EXTRA_POST_ID = "post id";
    private static final String COLLECTION_ID = "collection id";
    private static final String VIDEO = "video";
    private static final String TYPE = "type";
    private static final String EXTRA_USER_UID =  "uid";

    private static final String SOURCE = PostsAdapter.class.getSimpleName();

    private boolean processLikes = false;
    private boolean processDislikes = false;
    private boolean processWallet = false;
    private boolean processAmount = false;
    private boolean processCredit = false;
    private static final double DEFAULT_PRICE = 1.5;
    private static final double GOLDEN_RATIO = 1.618;
    private static final String POST_HEIGHT = "height";
    private static final String POST_WIDTH = "width";
    private static final int MAX_WIDTH = 200;
    private static final int MAX_HEIGHT = 200;
    private static final int VIDEO_POST = 1;
    private static final int NORMAL_IMAGE_POST = 2;
    private static final int IMAGE_POST = 3;
    private static final int LIMIT = 10;
    //firestore reference
    private CollectionReference collectionsPosts;
    private com.google.firebase.firestore.Query commentsCountQuery;
    private CollectionReference usersReference;
    private CollectionReference commentsReference;
    private CollectionReference likesReference;
    private DatabaseReference impressionReference;
    private CollectionReference timelineCollection;
    //firebase
    private DatabaseReference databaseReference;
    //firebase auth
    private FirebaseAuth firebaseAuth;
    public boolean showOnClick = true;
    private ConstraintSet constraintSet;
    private Player player;
    private long startTime;
    private long stopTime;
    private long duration;
    DecimalFormat formatter = new DecimalFormat("0.000");
    private List<Post> posts = new ArrayList<>();
    //impression tracking
    private ImpressionTracker impressionTracker;
    private final WeakHashMap<View, Integer> mViewPositionMap = new WeakHashMap<>();

    public HomePostsAdapter(Activity activity) {
        this.mContext = activity;
        impressionTracker = new ImpressionTracker(activity);
        initReferences();
    }

    public void setRandomPosts(List<Post> posts){
        this.posts = posts;
    }


    @Override
    public void onVisibilityChanged(List<View> visibleViews, List<View> invisibleViews) {


    }

    private void initReferences(){
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null){
            usersReference = FirebaseFirestore.getInstance().collection(Constants.FIREBASE_USERS);
            timelineCollection = FirebaseFirestore.getInstance().collection(Constants.TIMELINE);
            //firebase
            databaseReference = FirebaseDatabase.getInstance().getReference(Constants.RANDOM_PUSH_ID);
            //document reference
            commentsReference  = FirebaseFirestore.getInstance().collection(Constants.COMMENTS);
            commentsCountQuery= commentsReference;
            likesReference = FirebaseFirestore.getInstance().collection(Constants.LIKES);
            //firebase database references
            impressionReference = FirebaseDatabase.getInstance().getReference(Constants.VIEWS);
            impressionReference.keepSynced(true);
        }

    }

    @Override
    public int getItemCount() {
        return posts.size();
    }


    @Override
    public int getItemViewType(int position) {
        Post post = posts.get(position);
        final String type = post.getType();
        if (type.equals("single_video_post") || type.equals("collection_video_post")){
            return VIDEO_POST;
        }else {
            return IMAGE_POST;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIDEO_POST:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_video_post, parent, false);
                return new VideoPostViewHolder(view);
            case IMAGE_POST:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_image_post, parent, false);
                return new PhotoPostViewHolder(view);
        }
        return null;
    }


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        Post post = posts.get(position);
        final String type = post.getType();

        if (type.equals("single_video_post") || type.equals("collection_video_post")){
//            populateVideo((VideoPostViewHolder)holder, position);
        }else {
            populateConstrainedImage((PhotoPostViewHolder)holder, position);
        }


    }



    @Override
    public long getItemId(int position) {
        Post post = posts.get(position);
        long time = (int) post.getTime();
        return time;
    }


    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(hasStableIds);
    }

    private void populateVideo(final VideoPostViewHolder holder, final int position){
        final Post post = posts.get(position);
        final String postId = post.getPost_id();
        final String uid = post.getUser_id();
        final String collectionId = post.getCollection_id();
        final String type = post.getType();

        //firestore references
        player = new Player(mContext.getApplicationContext(), holder.postVideoView);
        player.addMedia(post.getUrl());
        holder.playImageView.setVisibility(View.VISIBLE);
        holder.playImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.postVideoView.getPlayer() == null){
                    holder.postVideoView.getPlayer().setPlayWhenReady(true);
                    holder.puaseImageView.setVisibility(View.VISIBLE);
                    holder.playImageView.setVisibility(View.GONE);
                }else {
                    player.releasePlayer();
                    holder.postVideoView.getPlayer().setPlayWhenReady(true);
                    holder.puaseImageView.setVisibility(View.VISIBLE);
                    holder.playImageView.setVisibility(View.GONE);
                }
            }
        });

        holder.puaseImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.postVideoView.getPlayer().setPlayWhenReady(false);
                holder.puaseImageView.setVisibility(View.GONE);
                holder.playImageView.setVisibility(View.VISIBLE);

            }
        });

        if (!TextUtils.isEmpty(post.getTitle())){
            holder.bottomLinearLayout.setVisibility(View.VISIBLE);
            holder.titleTextView.setText(post.getTitle());
            holder.titleRelativeLayout.setVisibility(View.VISIBLE);
        }else {
            holder.titleTextView.setText("");
            holder.titleRelativeLayout.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(post.getDescription())){
            //prevent collection note from overlapping other layouts
            final String [] strings = post.getDescription().split("");
            final int size = strings.length;
            if (size <= 50){
                holder.bottomLinearLayout.setVisibility(View.VISIBLE);
                holder.descriptionRelativeLayout.setVisibility(View.VISIBLE);
                holder.descriptionTextView.setText(post.getDescription());
            }else{
                holder.bottomLinearLayout.setVisibility(View.VISIBLE);
                holder.descriptionRelativeLayout.setVisibility(View.VISIBLE);
                final String boldMore = "...";
                final String boldLess = "";
                String normalText = post.getDescription().substring(0, 49);
                holder.descriptionTextView.setText(normalText + boldMore);
                holder.descriptionRelativeLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (showOnClick){
                            String normalText = post.getDescription();
                            holder.descriptionTextView.setText(normalText + boldLess);
                            showOnClick = false;
                        }else {
                            String normalText = post.getDescription().substring(0, 49);
                            holder.descriptionTextView.setText(normalText + boldMore);
                            showOnClick = true;
                        }
                    }
                });
            }
        }else {
            holder.descriptionTextView.setText("");
            holder.descriptionRelativeLayout.setVisibility(View.GONE);
        }

        holder.mCommentsLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =  new Intent(mContext, CommentsActivity.class);
                intent.putExtra(HomePostsAdapter.EXTRA_POST_ID, postId);
                intent.putExtra(HomePostsAdapter.COLLECTION_ID, collectionId);
                intent.putExtra(HomePostsAdapter.TYPE, type);
                mContext.startActivity(intent);
            }
        });

        holder.postRelativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =  new Intent(mContext, VideoDetailActivity.class);
                intent.putExtra(HomePostsAdapter.EXTRA_POST_ID, postId);
                intent.putExtra(HomePostsAdapter.COLLECTION_ID, collectionId);
                intent.putExtra(HomePostsAdapter.EXTRA_USER_UID, uid);
                intent.putExtra(HomePostsAdapter.TYPE, type);
                mContext.startActivity(intent);
            }
        });

        holder.profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, ProfileActivity.class);
                intent.putExtra(HomePostsAdapter.EXTRA_USER_UID, uid);
                mContext.startActivity(intent);
            }
        });

        usersReference.document(uid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen error", e);
                    return;
                }

                if (documentSnapshot.exists()){
                    final Andeqan andeqan = documentSnapshot.toObject(Andeqan.class);
                    holder.usernameTextView.setText(andeqan.getUsername());
                    Glide.with(mContext.getApplicationContext())
                            .load(andeqan.getProfile_image())
                            .apply(new RequestOptions()
                                    .placeholder(R.drawable.ic_user)
                                    .diskCacheStrategy(DiskCacheStrategy.DATA))
                            .into(holder.profileImageView);
                }
            }
        });

        //get the number of commments in a single
        commentsReference.document("post_ids").collection(postId)
                .orderBy("comment_id").whereEqualTo("post_id", postId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                        if (e != null) {
                            Log.w(TAG, "Listen error", e);
                            return;
                        }

                        if (!documentSnapshots.isEmpty()){
                            final int commentsCount = documentSnapshots.size();
                            holder.commentsCountTextView.setText(commentsCount + "");
                        }else {
                            holder.commentsCountTextView.setText("0");
                        }
                    }
                });


        likesReference.document(postId).collection("likes")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                        if (e != null) {
                            Log.w(TAG, "Listen error", e);
                            return;
                        }

                        if (!documentSnapshots.isEmpty()){
                            holder.likesCountTextView.setText(documentSnapshots.size() + " ");
                        }else {
                            holder.likesCountTextView.setText("0");
                        }

                    }
                });

        likesReference.document(postId).collection("dislikes")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                        if (e != null) {
                            Log.w(TAG, "Listen error", e);
                            return;
                        }

                        if (!documentSnapshots.isEmpty()){
                            holder.dislikeCountTextView.setText(documentSnapshots.size() + " ");
                        }else {
                            holder.dislikeCountTextView.setText("0");
                        }

                    }
                });


        //color the like image view if the user has liked
        likesReference.document(postId).collection("likes")
                .whereEqualTo("user_id", firebaseAuth.getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                        if (e != null) {
                            Log.w(TAG, "Listen error", e);
                            return;
                        }

                        if (documentSnapshots.isEmpty()){
                            //change the like image view backgroud color
                            holder.likesImageView.setColorFilter(Color.BLACK);
                        }else {
                            //changed the like image view background color to show user has liked
                            holder.likesImageView.setColorFilter(Color.RED);
                        }

                    }
                });


//      color the like image view if the user has dislikes
        likesReference.document(postId).collection("dislikes")
                .whereEqualTo("user_id", firebaseAuth.getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                        if (e != null) {
                            Log.w(TAG, "Listen error", e);
                            return;
                        }

                        if (documentSnapshots.isEmpty()){
                            //changed the dislike image view background color to show user has not disliked
                            holder.dislikeImageView.setColorFilter(Color.BLACK);

                        }else {
                            //changed the dislike image view background color to show user has disliked
                            holder.dislikeImageView.setColorFilter(Color.RED);

                        }

                    }
                });


        holder.dislikeLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processDislikes = true;
                processWallet = true;
                likesReference.document(postId).collection("dislikes")
                        .whereEqualTo("user_id", firebaseAuth.getCurrentUser().getUid())
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                                if (e != null) {
                                    Log.w(TAG, "Listen error", e);
                                    return;
                                }


                                if (processDislikes){
                                    if (documentSnapshots.isEmpty()){
                                        Like like = new Like();
                                        like.setUser_id(firebaseAuth.getCurrentUser().getUid());
                                        likesReference.document(postId).collection("dislikes")
                                                .document(firebaseAuth.getCurrentUser().getUid()).set(like);
                                        processDislikes = false;
                                        holder.dislikeImageView.setColorFilter(Color.RED);

                                    }else {
                                        likesReference.document(postId).collection("dislikes")
                                                .document(firebaseAuth.getCurrentUser().getUid()).delete();
                                        processDislikes = false;
                                        holder.dislikeImageView.setColorFilter(Color.BLACK);

                                    }
                                }

                            }
                        });
            }
        });


        holder.likesRelativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processLikes = true;
                processCredit = true;
                likesReference.document(postId).collection("likes")
                        .whereEqualTo("user_id", firebaseAuth.getCurrentUser().getUid())
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                                if (e != null) {
                                    Log.w(TAG, "Listen error", e);
                                    return;
                                }


                                if (processLikes){
                                    if (documentSnapshots.isEmpty()){
                                        final Like like = new Like();
                                        like.setUser_id(firebaseAuth.getCurrentUser().getUid());
                                        likesReference.document(postId).collection("likes")
                                                .document(firebaseAuth.getCurrentUser().getUid()).set(like);


                                        timelineCollection.document(uid).collection("activities")
                                                .whereEqualTo("post_id", postId)
                                                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                                                        if (e != null) {
                                                            Log.w(TAG, "Listen error", e);
                                                            return;
                                                        }


                                                        if (documentSnapshots.isEmpty()){
                                                            Log.d("timeline is empty", postId);
                                                            final Timeline timeline = new Timeline();
                                                            final long time = new Date().getTime();

                                                            final String activityId = databaseReference.push().getKey();
                                                            timeline.setPost_id(postId);
                                                            timeline.setTime(time);
                                                            timeline.setUser_id(firebaseAuth.getCurrentUser().getUid());
                                                            timeline.setType("like");
                                                            timeline.setActivity_id(activityId);
                                                            timeline.setStatus("un_read");
                                                            timeline.setReceiver_id(uid);

                                                            if (uid.equals(firebaseAuth.getCurrentUser().getUid())){
                                                                //do nothing
                                                            }else {
                                                                timelineCollection.document(uid).collection("activities")
                                                                        .document(postId).set(timeline);
                                                            }
                                                        }
                                                    }
                                                });

                                        processLikes = false;
                                        holder.likesImageView.setColorFilter(Color.RED);

                                    }else {
                                        likesReference.document(postId).collection("likes")
                                                .document(firebaseAuth.getCurrentUser().getUid()).delete();
                                        processLikes = false;
                                        holder.likesImageView.setColorFilter(Color.BLACK);

                                    }
                                }

                            }
                        });
            }
        });


    }

    private void populateConstrainedImage(final PhotoPostViewHolder holder, final int position){
        final Post post = posts.get(position);
        final String postId = post.getPost_id();
        final String uid = post.getUser_id();
        final String collectionId = post.getCollection_id();
        final String type = post.getType();

        if (post.getHeight() != null && post.getWidth() != null){
            final float width = (float) Integer.parseInt(post.getWidth());
            final float height = (float) Integer.parseInt(post.getHeight());
            float ratio = height/width;

            constraintSet = new ConstraintSet();
            constraintSet.clone(holder.postConstraintLayout);
            constraintSet.setDimensionRatio(holder.postImageView.getId(), "H," + ratio);
            holder.postImageView.setImageResource(R.drawable.post_placeholder);
            constraintSet.applyTo(holder.postConstraintLayout);

        }else {
            constraintSet = new ConstraintSet();
            constraintSet.clone(holder.postConstraintLayout);
            constraintSet.setDimensionRatio(holder.postImageView.getId(), "H," + 1);
            holder.postImageView.setImageResource(R.drawable.post_placeholder);
            constraintSet.applyTo(holder.postConstraintLayout);

        }

        //calculate view visibility and add visible views to impression tracker
        mViewPositionMap.put(holder.itemView, position);
        impressionTracker.addView(holder.itemView, 100, postId);

        if (post.getUrl() == null){
            //firebase firestore references
            if (type.equals("single")|| type.equals("single_image_post")){
                collectionsPosts = FirebaseFirestore.getInstance().collection(Constants.COLLECTIONS_POSTS)
                        .document("singles").collection(collectionId);
            }else{
                collectionsPosts = FirebaseFirestore.getInstance().collection(Constants.COLLECTIONS_POSTS)
                        .document("collections").collection(collectionId);
            }
            collectionsPosts.document(postId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w(TAG, "Listen error", e);
                        return;
                    }

                    if (documentSnapshot.exists()){
                        final CollectionPost collectionPost = documentSnapshot.toObject(CollectionPost.class);
                        //set the image on the image view
                        Glide.with(mContext.getApplicationContext())
                                .load(collectionPost.getImage())
                                .apply(new RequestOptions()
                                        .placeholder(R.drawable.post_placeholder)
                                        .diskCacheStrategy(DiskCacheStrategy.DATA))
                                .into(holder.postImageView);

                        if (!TextUtils.isEmpty(collectionPost.getTitle())){
                            holder.captionLinearLayout.setVisibility(View.VISIBLE);
                            holder.titleTextView.setText(collectionPost.getTitle());
                            holder.titleRelativeLayout.setVisibility(View.VISIBLE);
                        }else {
                            holder.titleRelativeLayout.setVisibility(View.GONE);
                        }

                        if (!TextUtils.isEmpty(collectionPost.getDescription())){
                            //prevent collection note from overlapping other layouts
                            final String [] strings = collectionPost.getDescription().split("");
                            final int size = strings.length;
                            if (size <= 50){
                                holder.captionLinearLayout.setVisibility(View.VISIBLE);
                                holder.descriptionRelativeLayout.setVisibility(View.VISIBLE);
                                holder.descriptionTextView.setText(collectionPost.getDescription());
                            }else{
                                holder.captionLinearLayout.setVisibility(View.VISIBLE);
                                holder.descriptionRelativeLayout.setVisibility(View.VISIBLE);
                                final String boldMore = "...";
                                String normalText = collectionPost.getDescription().substring(0, 49);
                                holder.descriptionTextView.setText(normalText + boldMore);
                            }
                        }else {
                            holder.captionLinearLayout.setVisibility(View.GONE);
                        }
                    }else {
                        //post does not exist
                    }
                }
            });
        }else {
            Glide.with(mContext.getApplicationContext())
                    .load(post.getUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.post_placeholder)
                            .diskCacheStrategy(DiskCacheStrategy.DATA))
                    .into(holder.postImageView);

            if (!TextUtils.isEmpty(post.getTitle())){
                holder.captionLinearLayout.setVisibility(View.VISIBLE);
                holder.titleTextView.setText(post.getTitle());
                holder.titleRelativeLayout.setVisibility(View.VISIBLE);
            }else {
                holder.titleRelativeLayout.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(post.getDescription())){
                //prevent collection note from overlapping other layouts
                final String [] strings = post.getDescription().split("");
                final int size = strings.length;
                if (size <= 50){
                    holder.captionLinearLayout.setVisibility(View.VISIBLE);
                    holder.descriptionRelativeLayout.setVisibility(View.VISIBLE);
                    holder.descriptionTextView.setText(post.getDescription());
                }else{
                    holder.captionLinearLayout.setVisibility(View.VISIBLE);
                    holder.descriptionRelativeLayout.setVisibility(View.VISIBLE);
                    final String boldMore = "...";
                    String normalText = post.getDescription().substring(0, 49);
                    holder.descriptionTextView.setText(normalText + boldMore);
                }
            }else {
                holder.captionLinearLayout.setVisibility(View.GONE);
            }
        }

        holder.mCommentsLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =  new Intent(mContext, CommentsActivity.class);
                intent.putExtra(HomePostsAdapter.EXTRA_POST_ID, postId);
                intent.putExtra(HomePostsAdapter.COLLECTION_ID, collectionId);
                intent.putExtra(HomePostsAdapter.TYPE, type);
                mContext.startActivity(intent);
            }
        });

        if (post.getWidth() != null && post.getHeight() != null){
            holder.postImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent =  new Intent(mContext, PostDetailActivity.class);
                    intent.putExtra(HomePostsAdapter.EXTRA_POST_ID, postId);
                    intent.putExtra(HomePostsAdapter.COLLECTION_ID, collectionId);
                    intent.putExtra(HomePostsAdapter.EXTRA_USER_UID, uid);
                    intent.putExtra(HomePostsAdapter.TYPE, type);
                    intent.putExtra(HomePostsAdapter.POST_HEIGHT, post.getHeight());
                    intent.putExtra(HomePostsAdapter.POST_WIDTH, post.getWidth());
                    mContext.startActivity(intent);
                }
            });
        }else {
            holder.postImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent =  new Intent(mContext, PostDetailActivity.class);
                    intent.putExtra(HomePostsAdapter.EXTRA_POST_ID, postId);
                    intent.putExtra(HomePostsAdapter.COLLECTION_ID, collectionId);
                    intent.putExtra(HomePostsAdapter.EXTRA_USER_UID, uid);
                    intent.putExtra(HomePostsAdapter.TYPE, type);
                    mContext.startActivity(intent);
                }
            });

        }

        holder.profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, ProfileActivity.class);
                intent.putExtra(HomePostsAdapter.EXTRA_USER_UID, uid);
                mContext.startActivity(intent);
            }
        });


//        //calculate the generated points from the compiled time
//        impressionReference.child("compiled_views").child(postId)
//                .addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()){
//                    holder.mCreditsLinearLayout.setVisibility(View.VISIBLE);
//                    ViewDuration impression = dataSnapshot.getValue(ViewDuration.class);
//                    final long compiledDuration = impression.getCompiled_duration();
//                    Log.d("compiled duration", compiledDuration + "");
//                    //get seconds in milliseconds
//                    final long durationInSeconds = compiledDuration / 1000;
//                    //get the points generate
//                    final double points = durationInSeconds * 0.000001;
//                    DecimalFormat formatter = new DecimalFormat("0.0000");
//                    final String pts = formatter.format(points);
//                    holder.senseCreditsTextView.setText(pts + " points");
//
//                }else {
//                    holder.mCreditsLinearLayout.setVisibility(View.VISIBLE);
//                    final double points = 0.00;
//                    DecimalFormat formatter = new DecimalFormat("0.00");
//                    final String pts = formatter.format(points);
//                    holder.senseCreditsTextView.setText(pts + " points");
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });

        impressionReference.child("post_views").child(postId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            final long size = dataSnapshot.getChildrenCount();
                            int childrenCount = (int) size;
                            holder.viewsCountTextView.setText(childrenCount + "");
                        }else {
                            holder.viewsCountTextView.setText("0");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


        usersReference.document(uid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen error", e);
                    return;
                }

                if (documentSnapshot.exists()){
                    final Andeqan andeqan = documentSnapshot.toObject(Andeqan.class);
                    holder.usernameTextView.setText(andeqan.getUsername());
                    Glide.with(mContext.getApplicationContext())
                            .load(andeqan.getProfile_image())
                            .apply(new RequestOptions()
                                    .placeholder(R.drawable.ic_user)
                                    .diskCacheStrategy(DiskCacheStrategy.DATA))
                            .into(holder.profileImageView);
                }
            }
        });

        //get the number of commments in a single
        commentsReference.document("post_ids").collection(postId)
                .orderBy("comment_id").whereEqualTo("post_id", postId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                        if (e != null) {
                            Log.w(TAG, "Listen error", e);
                            return;
                        }

                        if (!documentSnapshots.isEmpty()){
                            final int commentsCount = documentSnapshots.size();
                            holder.commentsCountTextView.setText(commentsCount + "");
                        }else {
                            holder.commentsCountTextView.setText("0");
                        }
                    }
                });


    }

    //region listeners
    private static double roundCredits(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}