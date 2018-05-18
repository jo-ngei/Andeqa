package com.andeqa.andeqa.creation;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.andeqa.andeqa.Constants;
import com.andeqa.andeqa.R;
import com.andeqa.andeqa.models.Collection;
import com.andeqa.andeqa.models.TransactionDetails;
import com.andeqa.andeqa.profile.ProfieCollectionPostsActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;

public class CreateCollectionActivity extends AppCompatActivity implements View.OnClickListener{
    @Bind(R.id.collectionNameEditText)EditText mCollectionNameEditText;
    @Bind(R.id.collectionNoteEditText)EditText mCollectionNoteEditText;
    @Bind(R.id.doneTextView)TextView mDoneTextView;
    @Bind(R.id.noteCountTextView)TextView mNoteCountTextView;
    @Bind(R.id.nameCountTextView)TextView mNameCountTextView;
    @Bind(R.id.collectionCoverImageView)ImageView mCollectionCoverImageView;
    @Bind(R.id.collectionRelativeLayout)RelativeLayout mCollectionRelativeLayout;
    @Bind(R.id.addCollectionTextView)TextView mAddCollectionTextView;
    @Bind(R.id.addRelativeLayout)RelativeLayout mAddRelativeLayout;


    private Uri photoUri;
    private static final String KEY_IMAGE = "IMAGE FROM GALLERY";
    private static final String TAG = "CreatePostActivity";
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    private static final int DEFAULT_TITLE_LENGTH_LIMIT = 25;
    private static final int DEFAULT_DESCRIPTION_LENGTH_LIMIT = 100;
    private static final int IMAGE_GALLERY_REQUEST = 112;
    private Toolbar toolbar;

    //FIRESTORE
    private FirebaseFirestore firebaseFirestore;
    private CollectionReference postsCollection;
    private CollectionReference collectionOwnersCollection;
    private CollectionReference usersReference;
    private DatabaseReference postReference;
    private CollectionReference collectionCollection;

    private static final String COLLECTION_ID = "collection id";
    private static final String EXTRA_USER_UID = "uid";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_collection);
        ButterKnife.bind(this);

        mDoneTextView.setOnClickListener(this);
        mAddRelativeLayout.setOnClickListener(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
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

            //initialize firestore
            firebaseFirestore =  FirebaseFirestore.getInstance();
            //get the reference to posts(collection reference)
            postsCollection = firebaseFirestore.collection(Constants.USER_COLLECTIONS);
            collectionOwnersCollection = firebaseFirestore.collection(Constants.COLLECTION_OWNERS);
            usersReference = FirebaseFirestore.getInstance().collection(Constants.FIREBASE_USERS);
            collectionCollection = FirebaseFirestore.getInstance().collection(Constants.USER_COLLECTIONS);

            //firebase
            postReference = FirebaseDatabase.getInstance().getReference(Constants.USER_COLLECTIONS);

            //textwatchers
            mCollectionNameEditText.setFilters(new InputFilter[]{new InputFilter
                    .LengthFilter(DEFAULT_TITLE_LENGTH_LIMIT)});
            mCollectionNoteEditText.setFilters(new  InputFilter[]{new InputFilter
                    .LengthFilter(DEFAULT_DESCRIPTION_LENGTH_LIMIT)});
            textWatchers();
            uploadingToFirebaseDialog();

            //permission
           int version = Build.VERSION.SDK_INT;
           if (version > Build.VERSION_CODES.LOLLIPOP_MR1){
               if (!checkIfAlreadyHavePermission()){
                   requestForSpecificPermission();
               }
           }

        }
    }

    private void textWatchers(){
        //TITLE TEXT WATCHER
        mCollectionNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int count = DEFAULT_TITLE_LENGTH_LIMIT - editable.length();
                mNameCountTextView.setText(Integer.toString(count));

                if (count == 0){
                    mNameCountTextView.setTextColor(Color.RED);
                }else if (count <= 25){
                    mNameCountTextView.setTextColor(Color.BLACK);
                }else{
                    //do nothing
                }

            }
        });

        //DESCRIPTION TEXT WATCHER
        mCollectionNoteEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int count = DEFAULT_DESCRIPTION_LENGTH_LIMIT- editable.length();
                mNoteCountTextView.setText(Integer.toString(count));

                if (count == 0){
                    mNoteCountTextView.setTextColor(Color.RED);
                }else if (count <= 100){
                    mNoteCountTextView.setTextColor(Color.BLACK);
                }else{
                    //do nothing
                }

            }
        });

    }

    private boolean checkIfAlreadyHavePermission(){
        int result = ContextCompat.checkSelfPermission(this,  Manifest.permission.GET_ACCOUNTS);
        if (result == PackageManager.PERMISSION_GRANTED){
            return true;
        }else {
            return false;
        }
    }

    private void requestForSpecificPermission(){
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, 101);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 101:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //granted
                }else {
                    // not granted
                }
                break;
                default:
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void uploadingToFirebaseDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating your collection");
        progressDialog.setCancelable(false);
    }

    private void  createCollection(){

        if (!TextUtils.isEmpty(mCollectionNameEditText.getText().toString())){
            progressDialog.show();
            final DatabaseReference collectionRef= postReference.push();
            final String collectionId = collectionRef.getKey();

            collectionCollection.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot documentSnapshots) {
                    final Collection collection = new Collection();
                    final long timeStamp = new Date().getTime();
                    //set the single ownership
                    TransactionDetails transactionDetails = new TransactionDetails();
                    transactionDetails.setPost_id(collectionId);
                    transactionDetails.setUser_id(firebaseAuth.getCurrentUser().getUid());
                    transactionDetails.setTime(timeStamp);
                    transactionDetails.setType("owner");
                    transactionDetails.setAmount(0.0);
                    transactionDetails.setWallet_balance(0.0);
                    collectionOwnersCollection.document(collectionId).set(transactionDetails);

                    if (photoUri != null){
                        StorageReference storageReference = FirebaseStorage
                                .getInstance().getReference()
                                .child(Constants.USER_COLLECTIONS)
                                .child(collectionId);

                        UploadTask uploadTask = storageReference.putFile(photoUri);
                        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                final Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                CollectionReference cl = postsCollection;
                                cl.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot documentSnapshots) {

                                        final int count = documentSnapshots.getDocuments().size();
                                        //save the collection
                                        collection.setType("collection");
                                        collection.setName(mCollectionNameEditText.getText().toString().trim());
                                        collection.setNote(mCollectionNoteEditText.getText().toString().trim());
                                        collection.setNumber(count + 1);
                                        collection.setUser_id(firebaseAuth.getCurrentUser().getUid());
                                        collection.setCollection_id(collectionId);
                                        collection.setTime(timeStamp);
                                        collection.setImage(downloadUrl.toString());
                                        collectionCollection.document(collectionId).set(collection);

                                        mCollectionNameEditText.setText("");
                                        mCollectionNoteEditText.setText("");


                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(CreateCollectionActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                                progressDialog.setMessage("Creating your collection" + " " + ((int) progress) + "%...");
                                if (progress == 100.0){
                                    progressDialog.dismiss();
                                    //reset input fields

                                    Intent intent = new Intent(CreateCollectionActivity.this, ProfieCollectionPostsActivity.class);
                                    intent.putExtra(CreateCollectionActivity.COLLECTION_ID, collectionId);
                                    intent.putExtra(CreateCollectionActivity.EXTRA_USER_UID, firebaseAuth.getCurrentUser().getUid());
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        });

                    }else {
                        final int count = documentSnapshots.getDocuments().size();
                        //save the collection
                        collection.setType("collection");
                        collection.setName(mCollectionNameEditText.getText().toString().trim());
                        collection.setNote(mCollectionNoteEditText.getText().toString().trim());
                        collection.setNumber(count + 1);
                        collection.setUser_id(firebaseAuth.getCurrentUser().getUid());
                        collection.setCollection_id(collectionId);
                        collection.setTime(timeStamp);
                        collectionCollection.document(collectionId).set(collection);

                        progressDialog.dismiss();
                        mCollectionNameEditText.setText("");
                        mCollectionNoteEditText.setText("");


                        Intent intent = new Intent(CreateCollectionActivity.this, ProfieCollectionPostsActivity.class);
                        intent.putExtra(CreateCollectionActivity.COLLECTION_ID, collectionId);
                        intent.putExtra(CreateCollectionActivity.EXTRA_USER_UID, firebaseAuth.getCurrentUser().getUid());
                        startActivity(intent);
                        finish();
                    }

                }
            });


        }else {
            Toast.makeText(CreateCollectionActivity.this, "Your collection does'nt have a name",
                    Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == IMAGE_GALLERY_REQUEST && data != null){
                photoUri = data.getData();
                if (photoUri != null){
                    mAddRelativeLayout.setVisibility(View.GONE);
                    mCollectionRelativeLayout.setVisibility(View.VISIBLE);
                    Picasso.with(this)
                            .load(photoUri)
                            .into(mCollectionCoverImageView,
                                    new Callback.EmptyCallback(){
                                @Override
                                public void onSuccess(){

                                }
                            });

                }
            }else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }



    @Override
    public void onClick(View v){

        if (v == mDoneTextView){
            createCollection();
        }

        if (v == mAddRelativeLayout){
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, IMAGE_GALLERY_REQUEST);
        }
    }

}


