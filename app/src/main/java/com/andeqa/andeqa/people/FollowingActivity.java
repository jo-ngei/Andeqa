package com.andeqa.andeqa.people;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.andeqa.andeqa.Constants;
import com.andeqa.andeqa.R;
import com.andeqa.andeqa.models.Andeqan;
import com.andeqa.andeqa.models.Relation;
import com.andeqa.andeqa.utils.EndlessLinearRecyclerViewOnScrollListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

public class FollowingActivity extends AppCompatActivity {
    @Bind(R.id.followingRecyclerView)RecyclerView followingRecyclerView;
    @Bind(R.id.toolbar)Toolbar toolbar;
    //firestore
    private CollectionReference peopleCollection;
    private CollectionReference usersCollection;
    private Query usersQuery;
    private CollectionReference followersCollection;
    private CollectionReference timelineCollection;
    private Query followersQuery;
    //firebase
    private DatabaseReference databaseReference;
    //firebase auth
    private FirebaseAuth firebaseAuth;
    private boolean processFollow = false;
    private static final String TAG = FollowingActivity.class.getSimpleName();
    private static final String EXTRA_USER_UID = "uid";
    private String mUid;
    private FollowingAdapter followingAdapter;
    private static final int TOTAL_ITEMS = 30;

    private List<String> peopleids = new ArrayList<>();
    private List<DocumentSnapshot> documentSnapshots = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_arrow);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        firebaseAuth = FirebaseAuth.getInstance();
        mUid = getIntent().getStringExtra(EXTRA_USER_UID);

        if (firebaseAuth.getCurrentUser()!= null){
            usersCollection = FirebaseFirestore.getInstance().collection(Constants.FIREBASE_USERS);
            usersQuery = usersCollection;
            followersCollection = FirebaseFirestore.getInstance().collection(Constants.PEOPLE);
            followersQuery = followersCollection.document("following")
                    .collection(mUid).orderBy("time");
            timelineCollection = FirebaseFirestore.getInstance().collection(Constants.TIMELINE);
            databaseReference = FirebaseDatabase.getInstance().getReference(Constants.RANDOM_PUSH_ID);

            followingRecyclerView.addOnScrollListener(new EndlessLinearRecyclerViewOnScrollListener() {
                @Override
                public void onLoadMore() {
                    setNextFollowing();
                }
            });
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        documentSnapshots.clear();
        getFollowing();
        setRecyclerView();

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected void onDocumentAdded(DocumentChange change) {
        peopleids.add(change.getDocument().getId());
        documentSnapshots.add(change.getDocument());
        followingAdapter.setPeople(documentSnapshots);
        followingAdapter.notifyItemInserted(documentSnapshots.size() -1);
        followingAdapter.getItemCount();

    }

    protected void onDocumentModified(DocumentChange change) {
        if (change.getOldIndex() == change.getNewIndex()) {
            // Item changed but remained in same position
            documentSnapshots.set(change.getOldIndex(), change.getDocument());
            followingAdapter.notifyItemChanged(change.getOldIndex());
        } else {
            // Item changed and changed position
            documentSnapshots.remove(change.getOldIndex());
            documentSnapshots.add(change.getNewIndex(), change.getDocument());
            followingAdapter.notifyItemRangeChanged(0, documentSnapshots.size());
        }
    }

    protected void onDocumentRemoved(DocumentChange change) {
        try {
            documentSnapshots.remove(change.getOldIndex());
            followingAdapter.notifyItemRemoved(change.getOldIndex());
            followingAdapter.notifyItemRangeChanged(0, documentSnapshots.size());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setRecyclerView(){
        followingAdapter = new FollowingAdapter(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        followingRecyclerView.setAdapter(followingAdapter);
        followingRecyclerView.setHasFixedSize(false);
        followingRecyclerView.setLayoutManager(linearLayoutManager);
    }


    private void getFollowing() {
        followersQuery.limit(TOTAL_ITEMS)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot documentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen error", e);
                            return;
                        }

                        if (!documentSnapshots.isEmpty()){
                            //retrieve the first bacth of documentSnapshots
                            for (final DocumentChange change : documentSnapshots.getDocumentChanges()) {
                                switch (change.getType()) {
                                    case ADDED:
                                        onDocumentAdded(change);
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

    private void setNextFollowing(){
        // Get the last visible document
        final int snapshotSize = followingAdapter.getItemCount();

        if (snapshotSize == 0){
        }else {
            DocumentSnapshot lastVisible = followingAdapter.getSnapshot(snapshotSize - 1);

            //retrieve the first bacth of documentSnapshots
            Query nextSellingQuery =  followersCollection.document("following")
                    .collection(mUid).orderBy("time").startAfter(lastVisible)
                    .limit(TOTAL_ITEMS);

            nextSellingQuery.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot documentSnapshots) {
                    if (!documentSnapshots.isEmpty()){
                        //retrieve the first bacth of documentSnapshots
                        for (final DocumentChange change : documentSnapshots.getDocumentChanges()) {
                            switch (change.getType()) {
                                case ADDED:
                                    onDocumentAdded(change);
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
}
