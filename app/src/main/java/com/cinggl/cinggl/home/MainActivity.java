package com.cinggl.cinggl.home;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.ContentFrameLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.cinggl.cinggl.Constants;
import com.cinggl.cinggl.R;
import com.cinggl.cinggl.adapters.SingleOutAdapter;
import com.cinggl.cinggl.creation.CreatePostActivity;
import com.cinggl.cinggl.market.MarketActivity;
import com.cinggl.cinggl.models.Cinggulan;
import com.cinggl.cinggl.models.Post;
import com.cinggl.cinggl.profile.PersonalProfileActivity;
import com.cinggl.cinggl.profile.ProfileFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    @Bind(R.id.fab)FloatingActionButton mFloatingActionButton;
//    @Bind(R.id.bottomNavigationView)BottomNavigationView mBottomNavigationView;
    @Bind(R.id.singleOutRecyclerView)RecyclerView singleOutRecyclerView;

    private static final String KEY_LAYOUT_POSITION = "layout pooition";
    private Parcelable recyclerViewState;
    //firestore reference
    private CollectionReference cinglesReference;
    private Query randomQuery;
    //firebase auth
    private SingleOutAdapter singleOutAdapter;
    private DocumentSnapshot lastVisible;
    private List<Post> posts = new ArrayList<>();
    private List<String> cinglesIds = new ArrayList<>();
    private LinearLayoutManager layoutManager;
    private int TOTAL_ITEMS = 4;

    private static final int MAX_WIDTH = 200;
    private static final int MAX_HEIGHT = 200;
    private static final int IMAGE_GALLERY_REQUEST = 112;
    private Uri photoUri;
    final FragmentManager fragmentManager = getSupportFragmentManager();
    private static final String TAG = MainActivity.class.getSimpleName();
    final Fragment profileFragment = new ProfileFragment();
    private int mSelectedItem;
    private int orientation;
    private ContentFrameLayout mContent;
    private ProgressDialog mProgressDialog;
    private DatabaseReference usersRef;
    private DocumentReference usersReference;
    private FirebaseAuth firebaseAuth;
    private ImageView mProfileCover;
    private CircleImageView mProfileImageView;
    private TextView mFullNameTextView;
    private TextView mSecondNameTextView;
    private TextView mEmailTextView;

    private Fragment savedState = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        firebaseAuth = FirebaseAuth.getInstance();

        mFloatingActionButton.setOnClickListener(this);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);
        mProfileCover = (ImageView) header.findViewById(R.id.header_cover_image);
        mProfileImageView = (CircleImageView) header.findViewById(R.id.creatorImageView);
        mFullNameTextView = (TextView) header.findViewById(R.id.fullNameTextView);
        mSecondNameTextView = (TextView) header.findViewById(R.id.secondNameTextView);
        mEmailTextView = (TextView) header.findViewById(R.id.emailTextView);

        if (firebaseAuth.getCurrentUser() != null){
            //firestore
            cinglesReference = FirebaseFirestore.getInstance().collection(Constants.POSTS);
            usersReference = FirebaseFirestore.getInstance().collection(Constants.FIREBASE_USERS)
                    .document(firebaseAuth.getCurrentUser().getUid());
            randomQuery = cinglesReference.orderBy("timeStamp", Query.Direction.DESCENDING)
                    .limit(TOTAL_ITEMS);

            randomQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                    if (!documentSnapshots.isEmpty()){
                        Log.d("posts count", documentSnapshots.size() + "");
                    }
                }
            });

            fetchData();
            fetchUserEmail();

        }

        setTheFirstBacthRandomCingles();
        recyclerViewScrolling();
        if (savedInstanceState != null){
            recyclerViewState = savedInstanceState.getParcelable(KEY_LAYOUT_POSITION);
            Log.d("posts saved Instance", "Instance is not null");
            //);
        }else {
            Log.d("Saved Instance", "Instance is completely null");
        }

//
//        //bottom navigation
//        BottomNavigationViewHelper.disableShiftMode(mBottomNavigationView);
//        mBottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView
//                .OnNavigationItemSelectedListener() {
//            @Override
//            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//                selectFragment(item);
//                return true;
//            }
//        });
//
//        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams)
//                mBottomNavigationView.getLayoutParams();
//        layoutParams.setBehavior(new BottomNavigationViewBehavior());
//
//        MenuItem selectedItem;
//        selectedItem = mBottomNavigationView.getMenu().getItem(0);
//        selectFragment(selectedItem);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_slide_right){
            Intent intent = new Intent(MainActivity.this, BestPostsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);

        }

        return super.onOptionsItemSelected(item);
    }

    private void fetchUserEmail(){
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = firebaseUser.getUid();

        mEmailTextView.setText(firebaseUser.getEmail());
    }


    private void fetchData(){
        //database references
        usersReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {

                if (e != null) {
                    Log.w(TAG, "Listen error", e);
                    return;
                }

                if (documentSnapshot.exists()){
                    final Cinggulan cinggulan = documentSnapshot.toObject(Cinggulan.class);
                    String firstName = cinggulan.getFirstName();
                    String secondName = cinggulan.getSecondName();
                    final String profileImage = cinggulan.getProfileImage();
                    final String profileCover = cinggulan.getProfileCover();

                    mFullNameTextView.setText(firstName + " " + secondName);

                    Picasso.with(MainActivity.this)
                            .load(profileImage)
                            .resize(MAX_WIDTH, MAX_HEIGHT)
                            .onlyScaleDown()
                            .centerCrop()
                            .placeholder(R.drawable.profle_image_background)
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .into(mProfileImageView, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError() {
                                    Picasso.with(MainActivity.this)
                                            .load(profileImage)
                                            .resize(MAX_WIDTH, MAX_HEIGHT)
                                            .onlyScaleDown()
                                            .centerCrop()
                                            .placeholder(R.drawable.profle_image_background)
                                            .into(mProfileImageView);

                                }
                            });

                    Picasso.with(MainActivity.this)
                            .load(profileCover)
                            .fit()
                            .centerCrop()
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .into(mProfileCover, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError() {
                                    Picasso.with(MainActivity.this)
                                            .load(profileCover)
                                            .fit()
                                            .centerCrop()
                                            .into(mProfileCover);


                                }
                            });

                }
            }
        });

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_home){
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
        }

        if (id == R.id.action_profile){
            Intent intent = new Intent(MainActivity.this, PersonalProfileActivity.class);
            startActivity(intent);
        }

        if (id == R.id.action_ifair){
            Intent intent = new Intent(MainActivity.this, MarketActivity.class);
            startActivity(intent);
        }

//        if (id == R.id.action_about){
//            Intent intent = new Intent(Intent.ACTION_VIEW,
//                    Uri.parse("https://johnmutuku628.wixsite.com/cinggl"));
//            startActivity(intent);
//        }

        if (id == R.id.action_share_cinggl){
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "Hey check out Cinggl and show your world to the world" +
                            " at: https://play.google.com/store/apps/details?id=com.cinggl.cinggl");
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        }


        if (id == R.id.action_send_feedback){
            String body = null;
            try {
                body = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
                body = "\n\n-----------------------------\nPlease don't remove this information\n Device OS: Android \n Device OS version: " +
                        Build.VERSION.RELEASE + "\n App Version: " + body + "\n Device Brand: " + Build.BRAND +
                        "\n Device Model: " + Build.MODEL + "\n Device Manufacturer: " + Build.MANUFACTURER;
            } catch (PackageManager.NameNotFoundException e) {
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"johnngei2@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Query from android app");
            intent.putExtra(Intent.EXTRA_TEXT, body);
            this.startActivity(Intent.createChooser(intent, this.getString(R.string.choose_email_client)));
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private void setTheFirstBacthRandomCingles(){
        randomQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                if (e != null) {
                    Log.w(TAG, "Listen error", e);
                    return;
                }

                for (DocumentChange change : documentSnapshots.getDocumentChanges()) {
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
                    onDataChanged();
                }

            }
        });

        //initilize the recycler view and set posts
        singleOutAdapter = new SingleOutAdapter(this);
        singleOutRecyclerView.setAdapter(singleOutAdapter);
        singleOutRecyclerView.setHasFixedSize(false);
        layoutManager = new LinearLayoutManager(this);
        singleOutRecyclerView.setLayoutManager(layoutManager);
    }

    private void setNextRandomCingles(){
        cinglesReference.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(final QuerySnapshot creditsSnapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen error", e);
                    return;
                }

                if (creditsSnapshots.isEmpty()){
                }else {
                    randomQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(final QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                            if (e != null) {
                                Log.w(TAG, "Listen error", e);
                                return;
                            }

                            if (!documentSnapshots.isEmpty()){
                                //get the last visible document(cingle)
                                lastVisible = documentSnapshots.getDocuments()
                                        .get(documentSnapshots.size() - 1);

                                //query starting from last retrived cingle
                                final Query nextBestCinglesQuery = cinglesReference.orderBy("timeStamp", Query.Direction.DESCENDING)
                                        .startAfter(lastVisible);
                                //retrive more cingles if present
                                nextBestCinglesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                                    @Override
                                    public void onEvent(final QuerySnapshot snapshots, FirebaseFirestoreException e) {
                                        if (e != null) {
                                            Log.w(TAG, "Listen error", e);
                                            return;
                                        }

                                        Log.d("remaining posts", snapshots.size() + "");

                                        //retrieve cingles depending on the remaining size of the list
                                        if (!snapshots.isEmpty()){
                                            final long lastSize = snapshots.size();
                                            if (lastSize < TOTAL_ITEMS){
                                                nextBestCinglesQuery.limit(lastSize);
                                            }else {
                                                nextBestCinglesQuery.limit(TOTAL_ITEMS);
                                            }


                                            //make sure that the size of snapshot equals item count
                                            if (singleOutAdapter.getItemCount() == creditsSnapshots.size()){
                                            }else if (singleOutAdapter.getItemCount() < creditsSnapshots.size()){
                                                for (DocumentChange change : snapshots.getDocumentChanges()) {
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
                                                    onDataChanged();
                                                }
                                            }else {
                                            }


                                        }


                                    }
                                });
                            }

                        }
                    });
                }

            }
        });

    }

    private void recyclerViewScrolling(){
        singleOutRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!recyclerView.canScrollVertically(-1)) {
                    onScrolledToTop();
                } else if (!recyclerView.canScrollVertically(1)) {
                    onScrolledToBottom();
                } else if (dy < 0) {
                    onScrolledUp();
                } else if (dy > 0) {
                    onScrolledDown();
                }
            }
        });
    }

    public void onScrolledUp() {}

    public void onScrolledDown() {

    }

    public void onScrolledToTop() {

    }

    public void onScrolledToBottom() {
        setNextRandomCingles();
    }

    public void delay(){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

            }
        },3000);
    }

    public void onDocumentAdded(DocumentChange change) {
        Post post = change.getDocument().toObject(Post.class);
        cinglesIds.add(change.getDocument().getId());
        posts.add(post);
        singleOutAdapter.setRandomPosts(posts);
        singleOutAdapter.getItemCount();
        singleOutAdapter.notifyItemInserted(posts.size());

    }

    private void onDocumentModified(DocumentChange change) {
        Post post = change.getDocument().toObject(Post.class);
        if (change.getOldIndex() == change.getNewIndex()) {
            // Item changed but remained in same position
            cinglesIds.add(change.getDocument().getId());
            posts.set(change.getNewIndex(), post);
            singleOutAdapter.notifyItemChanged(change.getOldIndex());

        } else {
            // Item changed and changed position
            posts.remove(change.getOldIndex());
            posts.add(change.getNewIndex(), post);
            singleOutAdapter.notifyItemMoved(change.getOldIndex(), change.getNewIndex());
        }
    }

    private void onDocumentRemoved(DocumentChange change) {
        String cingle_key = change.getDocument().getId();
        int cingle_index = cinglesIds.indexOf(cingle_key);
        if (cingle_index > -1){
            //remove data from the list
            cinglesIds.remove(change.getDocument().getId());
            singleOutAdapter.removeAt(change.getOldIndex());
            singleOutAdapter.notifyItemRemoved(change.getOldIndex());
            singleOutAdapter.getItemCount();
        }else {
            Log.v(TAG, "onDocumentRemoved:" + cingle_key);
        }

    }

    private void onError(FirebaseFirestoreException e) {};

    private void onDataChanged() {}


    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();
    }



//
//    @Override
//    public void onBackPressed() {
//        MenuItem defaulItem = mBottomNavigationView.getMenu().getItem(0);
//        if(mSelectedItem != defaulItem.getItemId()){
//            selectFragment(defaulItem);
//        }else {
//            super.onBackPressed();
//        }
//    }
//
//    private void updateToolbarText(CharSequence text){
//        ActionBar actionBar = getSupportActionBar();
//        if(actionBar != null){
//            actionBar.setTitle(text);
//        }
//    }

//    private void selectFragment(MenuItem item){
//        //initialize each corresponding fragment
//        switch (item.getItemId()){
//            case R.id.action_home:
//                FragmentTransaction ft = fragmentManager.beginTransaction();
//                ft.replace(R.id.home_container, homeFragment);
//                ft.commit();
//                break;
//            case R.id.action_timeline:
//                FragmentTransaction timelineTransaction = fragmentManager.beginTransaction();
//                timelineTransaction.replace(R.id.home_container, timelineFragment).commit();
//                break;
//
//            case R.id.action_profile:
//                FragmentTransaction profileTransaction = fragmentManager.beginTransaction();
//                profileTransaction.replace(R.id.home_container, profileFragment).commit();
//                break;
//        }
//
//        //update selected item
//        mSelectedItem = item.getItemId();
//
//        updateToolbarText(item.getTitle());
//
//        //uncheck the other items
//        for(int i = 0; i < mBottomNavigationView.getMenu().size(); i++){
//            MenuItem menuItem = mBottomNavigationView.getMenu().getItem(i);
//            menuItem.setChecked(menuItem.getItemId() ==item.getItemId());
//        }
//    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == IMAGE_GALLERY_REQUEST && data != null){
                photoUri = data.getData();
                if (photoUri != null){
                    Intent intent = new Intent(MainActivity.this, CreatePostActivity.class);
                    intent.putExtra("photoUri", photoUri.toString());
                    startActivity(intent);
                }
            }else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }


    @Override
    public void onClick(View v){
        if(v == mFloatingActionButton){
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, IMAGE_GALLERY_REQUEST);

        }
    }

}
