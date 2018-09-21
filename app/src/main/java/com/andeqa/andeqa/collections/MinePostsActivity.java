package com.andeqa.andeqa.collections;

import android.content.Intent;
import android.os.Parcelable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andeqa.andeqa.Constants;
import com.andeqa.andeqa.R;
import com.andeqa.andeqa.creation.CreateCollectionPostActivity;
import com.andeqa.andeqa.models.Collection;
import com.andeqa.andeqa.models.CollectionPost;
import com.andeqa.andeqa.models.Relation;
import com.andeqa.andeqa.settings.CollectionSettingsActivity;
import com.andeqa.andeqa.utils.EndlessRecyclerOnScrollListener;
import com.andeqa.andeqa.utils.ItemOffsetDecoration;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MinePostsActivity extends AppCompatActivity implements View.OnClickListener {
    @Bind(R.id.collectionsPostsRecyclerView)RecyclerView mCollectionsPostsRecyclerView;
    @Bind(R.id.createPostButton)FloatingActionButton mCreatePostButton;
    @Bind(R.id.collectionCoverImageView)ImageView mCollectionCoverImageView;
    @Bind(R.id.collectionNoteTextView)TextView mCollectionNoteTextView;
    @Bind(R.id.collapsingToolbar)CollapsingToolbarLayout collapsingToolbarLayout;
    @Bind(R.id.collectionNameTextView)TextView mCollectionNameTextView;
    @Bind(R.id.collectionSettingsRelativeLayout)RelativeLayout mCollectionSettingsRelativeLayout;
    @Bind(R.id.followersCountTextView) TextView mFollowersCountTextView;
    @Bind(R.id.followersTextView)TextView mFollowersTextView;
    @Bind(R.id.postsTextView)TextView mPostsTextView;
    @Bind(R.id.postsCountTextView)TextView mPostsCountTextView;
    @Bind(R.id.followTextView)TextView mFollowTextView;

    private static final String TAG = CollectionPostsActivity.class.getSimpleName();

    //firestore reference
    private CollectionReference postsCollection;
    private CollectionReference collectionCollection;
    private CollectionReference usersCollection;
    private CollectionReference collectionOwnersCollection;
    private CollectionReference collectionsRelations;

    private Query postsQuery;
    //firebase auth
    private FirebaseAuth firebaseAuth;
    //firestore adapters
    private MinePostsAdapters minePostsAdapters;
    private static final String KEY_LAYOUT_POSITION = "layout pooition";
    private Parcelable recyclerViewState;
    private  static final int MAX_WIDTH = 400;
    private static final int MAX_HEIGHT = 400;
    private int TOTAL_ITEMS = 10;
    private StaggeredGridLayoutManager layoutManager;
    private static final String EXTRA_USER_UID = "uid";

    private String mCollectionId;
    private String mUid;
    private String mSource;
    private static final String COLLECTION_ID = "collection id";
    private List<String> mSnapshotsIds = new ArrayList<>();
    private List<DocumentSnapshot> mSnapshots = new ArrayList<>();
    private ItemOffsetDecoration itemOffsetDecoration;
    private boolean processFollow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mine_posts);
        ButterKnife.bind(this);
        //initialize click listener
        mCreatePostButton.setOnClickListener(this);
        mCollectionSettingsRelativeLayout.setOnClickListener(this);

        //FIREBASE AUTH
        firebaseAuth = FirebaseAuth.getInstance();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_arrow);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        collapsingToolbarLayout.setTitle("Posts");

        mCollectionId = getIntent().getStringExtra(COLLECTION_ID);
        mUid = getIntent().getStringExtra(EXTRA_USER_UID);


        if (mUid.equals(firebaseAuth.getCurrentUser().getUid())){
            mCollectionSettingsRelativeLayout.setVisibility(View.VISIBLE);
        }

        postsCollection = FirebaseFirestore.getInstance().collection(Constants.POSTS);
        collectionCollection = FirebaseFirestore.getInstance().collection(Constants.USER_COLLECTIONS);
        collectionsRelations = FirebaseFirestore.getInstance().collection(Constants.COLLECTION_RELATIONS);
        postsQuery = postsCollection.orderBy("time", Query.Direction.ASCENDING);
        usersCollection = FirebaseFirestore.getInstance().collection(Constants.FIREBASE_USERS);
        collectionOwnersCollection = FirebaseFirestore.getInstance().collection(Constants.COLLECTION_OWNERS);

        mCollectionsPostsRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onLoadMore() {
                setNextCollectionPosts();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSnapshots.clear();
        setRecyclerView();
        setCollectionPosts();
        setCollectionsInfo();
        followCollection();
        getCountOfCollectionFollowers();
        getCountOfCollectionsPosts();
        getCountOfFollowers();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCollectionsPostsRecyclerView.removeItemDecoration(itemOffsetDecoration);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.collection_settings, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//
//        return super.onOptionsItemSelected(item);
//    }
//
//    @Override
//    public boolean onPrepareOptionsMenu(final Menu menu) {
//
//
//        return super.onPrepareOptionsMenu(menu);
//    }
//


    private void setRecyclerView(){
        // RecyclerView
        minePostsAdapters = new MinePostsAdapters(MinePostsActivity.this);
        mCollectionsPostsRecyclerView.setAdapter(minePostsAdapters);
        mCollectionsPostsRecyclerView.setHasFixedSize(false);
        layoutManager = new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL);
        itemOffsetDecoration = new ItemOffsetDecoration(this, R.dimen.item_off_set);
        mCollectionsPostsRecyclerView.addItemDecoration(itemOffsetDecoration);
        mCollectionsPostsRecyclerView.setLayoutManager(layoutManager);
        ViewCompat.setNestedScrollingEnabled(mCollectionsPostsRecyclerView,false);
    }

    private void getCountOfFollowers(){
        /**show the number of peopl following collection**/
        collectionsRelations.document("following")
                .collection(mCollectionId).whereEqualTo("following_id",
                firebaseAuth.getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot documentSnapshots,
                                        @Nullable FirebaseFirestoreException e) {

                        if (e != null) {
                            Log.w(TAG, "Listen error", e);
                            return;
                        }

                        if (!documentSnapshots.isEmpty()){
                            int following = documentSnapshots.size();
                            mFollowersCountTextView.setText(following);
                            mFollowersTextView.setText("Following");
                        }else {
                            mFollowersCountTextView.setText("0");
                            mFollowersTextView.setText("Following");
                        }

                    }
                });

    }

    private void getCountOfCollectionFollowers(){
        /**show the number of peopl following collection**/
        collectionsRelations.document("following")
                .collection(mCollectionId).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot documentSnapshots,
                                @Nullable FirebaseFirestoreException e) {

                if (e != null) {
                    Log.w(TAG, "Listen error", e);
                    return;
                }

                if (!documentSnapshots.isEmpty()){
                    int following = documentSnapshots.size();
                    mFollowersCountTextView.setText(following);
                }else {
                    mFollowersCountTextView.setText("0");
                }

            }
        });
    }

    private void getCountOfCollectionsPosts(){
        postsQuery.whereEqualTo("collection_id", mCollectionId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                        if (e != null) {
                            android.util.Log.w(TAG, "Listen error", e);
                            return;
                        }


                        if (!documentSnapshots.isEmpty()){
                            final int count = documentSnapshots.size();
                            mPostsCountTextView.setText(count + "");
                            mPostsTextView.setText("Posts");
                        }else {
                            mPostsCountTextView.setText("0");
                            mPostsTextView.setText("Posts");
                        }

                    }
                });
    }


    private void followCollection(){
        /**follow or un follow collection*/
        if (mUid.equals(firebaseAuth.getCurrentUser().getUid())){
            mFollowTextView.setVisibility(View.GONE);
        }else {
            mFollowTextView.setVisibility(View.VISIBLE);
            mFollowTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    processFollow = true;
                    collectionsRelations.document("following")
                            .collection(firebaseAuth.getCurrentUser().getUid())
                            .whereEqualTo("followed_id", mCollectionId)
                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                                    if (e != null) {
                                        Log.w(TAG, "Listen error", e);
                                        return;
                                    }

                                    if (processFollow){
                                        if (documentSnapshots.isEmpty()){
                                            final Relation following = new Relation();
                                            following.setFollowing_id(firebaseAuth.getCurrentUser().getUid());
                                            following.setFollowed_id(mCollectionId);
                                            following.setType("followed_collection");
                                            following.setTime(System.currentTimeMillis());
                                            collectionsRelations.document("following").collection(firebaseAuth
                                                    .getCurrentUser().getUid()).document(mCollectionId).set(following);

                                            mFollowTextView.setText("FOLLOWING");
                                            processFollow = false;
                                        }else {
                                            collectionsRelations.document("following").collection(firebaseAuth.getCurrentUser().getUid())
                                                    .document(mCollectionId).delete();
                                            mFollowTextView.setText("FOLLOW");
                                            processFollow = false;
                                        }
                                    }
                                }
                            });
                }
            });
        }

    }


    private void setCollectionsInfo(){

        collectionCollection.document(mCollectionId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {

                if (e != null) {
                    android.util.Log.w(TAG, "Listen error", e);
                    return;
                }

                if (documentSnapshot.exists()){
                    Collection collection = documentSnapshot.toObject(Collection.class);
                    final String name = collection.getName();
                    final String note = collection.getNote();
                    final String cover = collection.getImage();
                    final String userId = collection.getUser_id();

                    mCollectionNameTextView.setText(name);
                    mCollectionNoteTextView.setText(note);
                    Glide.with(getApplicationContext())
                            .load(cover)
                            .apply(new RequestOptions()
                                    .diskCacheStrategy(DiskCacheStrategy.DATA))
                            .into(mCollectionCoverImageView);

                }
            }
        });
    }

    private void setCollectionPosts(){
        postsQuery.whereEqualTo("collection_id", mCollectionId).limit(TOTAL_ITEMS)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                        if (e != null) {
                            android.util.Log.w(TAG, "Listen error", e);
                            return;
                        }


                        if (!documentSnapshots.isEmpty()){
                            //retrieve the first bacth of mSnapshots
                            for (final DocumentChange change : documentSnapshots.getDocumentChanges()) {
                                switch (change.getType()) {
                                    case ADDED:
                                        CollectionPost post = change.getDocument().toObject(CollectionPost.class);
                                        final String type = post.getType();
                                        if (!type.equals("collection_video_post") || !type.equals("single_video_post")){
                                            onDocumentAdded(change);

                                        }
                                        break;
                                    case MODIFIED:
                                        onDocumentModified(change);
                                        break;
                                    case REMOVED:
                                        onDocumentRemoved(change);
                                        break;
                                }
                            }
                        }

                    }
                });

    }


    private void setNextCollectionPosts(){
        // Get the last visible document
        final int snapshotSize = minePostsAdapters.getItemCount();
        if (snapshotSize != 0){
            DocumentSnapshot lastVisible = minePostsAdapters.getSnapshot(snapshotSize - 1);

            //retrieve the first bacth of mSnapshots
            Query nextCollectionPostsQuery = postsCollection.orderBy("time", Query.Direction.ASCENDING)
                    .whereEqualTo("collection_id", mCollectionId)
                    .startAfter(lastVisible)
                    .limit(TOTAL_ITEMS);

            nextCollectionPostsQuery.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot documentSnapshots) {
                    if (!documentSnapshots.isEmpty()){
                        //retrieve the first bacth of mSnapshots
                        for (final DocumentChange change : documentSnapshots.getDocumentChanges()) {
                            switch (change.getType()) {
                                case ADDED:
                                    CollectionPost post = change.getDocument().toObject(CollectionPost.class);
                                    final String type = post.getType();
                                    if (!type.equals("collection_video_post") || !type.equals("single_video_post")){
                                        onDocumentAdded(change);

                                    }
                                    break;
                                case MODIFIED:
                                    onDocumentModified(change);
                                    break;
                                case REMOVED:
                                    onDocumentRemoved(change);
                                    break;
                            }
                        }
                    }
                }
            });
        }
    }

    protected void onDocumentAdded(DocumentChange change) {
        mSnapshotsIds.add(change.getDocument().getId());
        mSnapshots.add(change.getDocument());
        minePostsAdapters.setCollectionPosts(mSnapshots);
        minePostsAdapters.notifyItemInserted(mSnapshots.size() -1);
        minePostsAdapters.getItemCount();

    }

    protected void onDocumentModified(DocumentChange change) {
        try {
            if (change.getOldIndex() == change.getNewIndex()) {
                // Item changed but remained in same position
                mSnapshots.set(change.getOldIndex(), change.getDocument());
                minePostsAdapters.notifyItemChanged(change.getOldIndex());
            } else {
                // Item changed and changed position
                mSnapshots.remove(change.getOldIndex());
                mSnapshots.add(change.getNewIndex(), change.getDocument());
                minePostsAdapters.notifyItemRangeChanged(0, mSnapshots.size());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    protected void onDocumentRemoved(DocumentChange change) {
       try {
           mSnapshots.remove(change.getOldIndex());
           minePostsAdapters.notifyItemRemoved(change.getOldIndex());
           minePostsAdapters.notifyItemRangeChanged(0, mSnapshots.size());
       }catch (Exception e){
           e.printStackTrace();
       }
    }


    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onClick(View v){
        if (v == mCreatePostButton){
            Intent intent = new Intent(MinePostsActivity.this, CreateCollectionPostActivity.class);
            intent.putExtra(MinePostsActivity.COLLECTION_ID, mCollectionId);
            startActivity(intent);
            finish();
        }

        if (v == mCollectionSettingsRelativeLayout){
            Intent intent = new Intent(this, CollectionSettingsActivity.class);
            intent.putExtra(MinePostsActivity.COLLECTION_ID, mCollectionId);
            startActivity(intent);
        }

    }

}