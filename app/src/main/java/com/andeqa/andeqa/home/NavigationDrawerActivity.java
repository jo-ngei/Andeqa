package com.andeqa.andeqa.home;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.andeqa.andeqa.Constants;
import com.andeqa.andeqa.R;
import com.andeqa.andeqa.collections.CollectionsFragment;
import com.andeqa.andeqa.creation.CreateCollectionActivity;
import com.andeqa.andeqa.creation.CreatePostActivity;
import com.andeqa.andeqa.explore.ExploreFragment;
import com.andeqa.andeqa.explore.SellingFragment;
import com.andeqa.andeqa.message.MessagingActivity;
import com.andeqa.andeqa.models.Andeqan;
import com.andeqa.andeqa.profile.ProfileActivity;
import com.andeqa.andeqa.settings.SettingsActivity;
import com.andeqa.andeqa.timeline.TimelineFragment;
import com.andeqa.andeqa.utils.BottomNavigationViewBehavior;
import com.andeqa.andeqa.utils.BottomNavigationViewHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class NavigationDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        View.OnClickListener{
    @Bind(R.id.bottomNavigationView)BottomNavigationView mBottomNavigationView;
    @Bind(R.id.fab)FloatingActionButton mFloatingActionButton;
    @Bind(R.id.container)FrameLayout fragmentContainer;

    private static final int MAX_WIDTH = 200;
    private static final int MAX_HEIGHT = 200;
    private static final int IMAGE_GALLERY_REQUEST = 112;
    private static final String EXTRA_USER_UID = "uid";
    private Uri photoUri;
    private static final String TAG = NavigationDrawerActivity.class.getSimpleName();
    private int mSelectedItem;
    private CollectionReference usersReference;
    private CollectionReference timelineCollection;
    private Query timelineQuery;
    private CollectionReference messagingCollection;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private ImageView mProfileCover;
    private CircleImageView mProfileImageView;
    private TextView mFullNameTextView;
    private TextView mEmailTextView;
    final FragmentManager fragmentManager = getSupportFragmentManager();
    final Fragment homeFragment = new HomeFragment();
    final Fragment marketFragment = new ExploreFragment();
    final Fragment collectionFragment = new CollectionsFragment();
    final Fragment timelineFragment = new TimelineFragment();

    private TextView notificationTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        //initialize firebase authentication
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
        mProfileCover = (ImageView) header.findViewById(R.id.profileCoverImageView);
        mProfileImageView = (CircleImageView) header.findViewById(R.id.profileImageView);
        mFullNameTextView = (TextView) header.findViewById(R.id.fullNameTextView);
        mEmailTextView = (TextView) header.findViewById(R.id.emailTextView);


        if (firebaseAuth.getCurrentUser() != null){
            //firestore
            usersReference = FirebaseFirestore.getInstance().collection(Constants.FIREBASE_USERS);
            timelineCollection = FirebaseFirestore.getInstance().collection(Constants.TIMELINE);
            timelineCollection = FirebaseFirestore.getInstance().collection(Constants.TIMELINE);
            timelineQuery = timelineCollection.document(firebaseAuth.getCurrentUser().getUid())
                    .collection("activities").orderBy("time", Query.Direction.ASCENDING)
                    .whereEqualTo("status", "un_read");
            messagingCollection = FirebaseFirestore.getInstance().collection(Constants.MESSAGES);


        }
        //bottom navigation
        BottomNavigationViewHelper.disableShiftMode(mBottomNavigationView);
        mBottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView
                .OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectFragment(item);
                return true;
            }
        });

        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams)
                mBottomNavigationView.getLayoutParams();
        layoutParams.setBehavior(new BottomNavigationViewBehavior());

        MenuItem selectedItem;
        selectedItem = mBottomNavigationView.getMenu().getItem(0);
        selectFragment(selectedItem);


    }

    @Override
    public void onStart() {
        super.onStart();
        fetchData();
        fetchUserEmail();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        if (id == R.id.action_chats){
            Intent intent = new Intent(this, MessagingActivity.class);
            startActivity(intent);
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
        usersReference.document(firebaseAuth.getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {

                if (e != null) {
                    Log.w(TAG, "Listen error", e);
                    return;
                }

                if (documentSnapshot.exists()){
                    final Andeqan cinggulan = documentSnapshot.toObject(Andeqan.class);
                    String firstName = cinggulan.getFirst_name();
                    String secondName = cinggulan.getSecond_name();
                    final String profileImage = cinggulan.getProfile_image();
                    final String profileCover = cinggulan.getProfile_cover();

                    mFullNameTextView.setText(firstName + " " + secondName);

                    Picasso.with(NavigationDrawerActivity.this)
                            .load(profileImage)
                            .resize(MAX_WIDTH, MAX_HEIGHT)
                            .onlyScaleDown()
                            .centerCrop()
                            .placeholder(R.drawable.ic_user)
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .into(mProfileImageView, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError() {
                                    Picasso.with(NavigationDrawerActivity.this)
                                            .load(profileImage)
                                            .resize(MAX_WIDTH, MAX_HEIGHT)
                                            .onlyScaleDown()
                                            .centerCrop()
                                            .placeholder(R.drawable.ic_user)
                                            .into(mProfileImageView);

                                }
                            });

                    Picasso.with(NavigationDrawerActivity.this)
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
                                    Picasso.with(NavigationDrawerActivity.this)
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

        if (id == R.id.action_profile){
            Intent intent = new Intent(NavigationDrawerActivity.this, ProfileActivity.class);
            intent.putExtra(NavigationDrawerActivity.EXTRA_USER_UID, firebaseAuth.getCurrentUser().getUid());
            startActivity(intent);
        }

        if (id == R.id.action_settings){
            Intent intent = new Intent(NavigationDrawerActivity.this, SettingsActivity.class);
            startActivity(intent);
        }


        if (id == R.id.action_about){
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://andeqa.com"));
            startActivity(intent);
        }

        if (id == R.id.action_share_andeqa){
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "Hey! Check out Andeqa, the mobile app where you buy and sell beautiful photos with your social value" +
                            " at: https://play.google.com/store/apps/details?id=com.andeqa.andeqa");
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
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"andeqa@andeqa.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Query from android app");
            intent.putExtra(Intent.EXTRA_TEXT, body);
            this.startActivity(Intent.createChooser(intent, this.getString(R.string.choose_email_client)));
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private void updateToolbarText(CharSequence text){
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(text);
        }
    }



    private void selectFragment(MenuItem item){
        //initialize each corresponding fragment
        switch (item.getItemId()){
            case R.id.action_home:
                FragmentTransaction ft = fragmentManager.beginTransaction();
                ft.replace(R.id.container, homeFragment);
                ft.commit();
                break;
            case R.id.action_collection:
                FragmentTransaction collectionTransaction = fragmentManager.beginTransaction();
                 collectionTransaction.replace(R.id.container, collectionFragment).commit();
                break;
            case R.id.action_market:
                FragmentTransaction profileTransaction = fragmentManager.beginTransaction();
                profileTransaction.replace(R.id.container, marketFragment).commit();
                break;
            case R.id.action_timeline:
                FragmentTransaction timelineTransaction = fragmentManager.beginTransaction();
                timelineTransaction.replace(R.id.container, timelineFragment).commit();
                break;

        }

        //update selected item
        mSelectedItem = item.getItemId();

        updateToolbarText(item.getTitle());

        //uncheck the other items
        for(int i = 0; i < mBottomNavigationView.getMenu().size(); i++){
            MenuItem menuItem = mBottomNavigationView.getMenu().getItem(i);
            menuItem.setChecked(menuItem.getItemId() ==item.getItemId());
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == IMAGE_GALLERY_REQUEST && data != null){
                photoUri = data.getData();
                if (photoUri != null){
                    Intent intent = new Intent(NavigationDrawerActivity.this, CreatePostActivity.class);
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

        if (v == mFloatingActionButton){
            Intent intent = new Intent(NavigationDrawerActivity.this, CreateCollectionActivity.class);
            startActivity(intent);
        }

    }

}
