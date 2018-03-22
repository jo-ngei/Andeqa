package com.andeqa.andeqa.market;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andeqa.andeqa.Constants;
import com.andeqa.andeqa.R;
import com.andeqa.andeqa.firestore.FirestoreAdapter;
import com.andeqa.andeqa.home.PostDetailActivity;
import com.andeqa.andeqa.models.Andeqan;
import com.andeqa.andeqa.models.CollectionPost;
import com.andeqa.andeqa.models.Post;
import com.andeqa.andeqa.models.PostSale;
import com.andeqa.andeqa.models.Credit;
import com.andeqa.andeqa.models.TransactionDetails;
import com.andeqa.andeqa.people.FollowerProfileActivity;
import com.andeqa.andeqa.profile.PersonalProfileActivity;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by J.EL on 11/13/2017.
 */

public class SellingAdapter extends FirestoreAdapter<PostSellingViewHolder> {
    private static final String TAG = SellingAdapter.class.getSimpleName();
    private Context mContext;
    private List<PostSale> postSales = new ArrayList<>();
    //firestore
    private CollectionReference collectionsCollection;
    private CollectionReference postsCollection;
    private CollectionReference ifairReference;
    private CollectionReference usersReference;
    private CollectionReference creditsReference;
    private CollectionReference cingleWalletReference;
    private CollectionReference ownerReference;
    private CollectionReference senseCreditReference;
    private com.google.firebase.firestore.Query sellingQuery;
    //firebase
    private DatabaseReference cinglesRef;
    //adapters
    private FirestoreRecyclerAdapter firestoreRecyclerAdapter;
    private FirebaseAuth firebaseAuth;
    private static final String KEY_LAYOUT_POSITION = "layout pooition";
    private static final String EXTRA_POST_KEY = "post key";
    private static final String EXTRA_USER_UID = "uid";
    private static final String COLLECTION_ID = "collection id";
    private static final int MAX_WIDTH = 200;
    private static final int MAX_HEIGHT = 200;


    public SellingAdapter(Query query, Context mContext) {
        super(query);
        this.mContext = mContext;
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    @Override
    public PostSellingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.posts_market_layout, parent, false);
        return new PostSellingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final PostSellingViewHolder holder, int position) {
        final PostSale postSale = getSnapshot(position).toObject(PostSale.class);
        holder.bindIfairCingle(postSale);
        final String postKey = postSale.getPushId();
        final String uid = postSale.getUid();
        final double salePrice = postSale.getSalePrice();

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null){
            senseCreditReference = FirebaseFirestore.getInstance().collection(Constants.SENSECREDITS);
            postsCollection = FirebaseFirestore.getInstance().collection(Constants.POSTS);
            collectionsCollection = FirebaseFirestore.getInstance().collection(Constants.COLLECTIONS);
            ifairReference = FirebaseFirestore.getInstance().collection(Constants.SELLING);
            cingleWalletReference = FirebaseFirestore.getInstance().collection(Constants.POST_WALLET);
            ownerReference = FirebaseFirestore.getInstance().collection(Constants.POST_OWNERS);
            usersReference = FirebaseFirestore.getInstance().collection(Constants.FIREBASE_USERS);
            sellingQuery = ifairReference.orderBy("randomNumber").limit(10);

        }
        DecimalFormat formatter =  new DecimalFormat("0.00000000");
        holder.cingleSalePriceTextView.setText("SC" + " " + formatter.format(salePrice));

        //get the current date
        DateFormat simpleDateFormat = new SimpleDateFormat("d");
        String date = simpleDateFormat.format(new Date());

        if (date.endsWith("1") && !date.endsWith("11"))
            simpleDateFormat = new SimpleDateFormat("d'st' MMM, yyyy");
        else if (date.endsWith("2") && !date.endsWith("12"))
            simpleDateFormat = new SimpleDateFormat("d'nd' MMM, yyyy");
        else if (date.endsWith("3") && !date.endsWith("13"))
            simpleDateFormat = new SimpleDateFormat("d'rd' MMM, yyyy");
        else
            simpleDateFormat = new SimpleDateFormat("d'th' MMM, yyyy");

        holder.datePostedTextView.setText(simpleDateFormat.format(postSale.getTime()));


        postsCollection.document(postKey).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen error", e);
                    return;
                }

                if (documentSnapshot.exists()){
                    final Post post = documentSnapshot.toObject(Post.class);
                    final String collectionId = post.getCollectionId();

                    holder.cingleTradeMethodTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent =  new Intent(mContext, PostDetailActivity.class);
                            intent.putExtra(SellingAdapter.EXTRA_POST_KEY, postKey);
                            intent.putExtra(SellingAdapter.COLLECTION_ID, collectionId);
                            mContext.startActivity(intent);
                        }
                    });

                    holder.postImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(mContext, PostDetailActivity.class);
                            intent.putExtra(SellingAdapter.EXTRA_POST_KEY, postKey);
                            intent.putExtra(SellingAdapter.COLLECTION_ID, collectionId);
                            mContext.startActivity(intent);
                        }
                    });

                    collectionsCollection.document("collection_posts").collection(collectionId)
                            .document(postKey).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                            if (e != null) {
                                Log.w(TAG, "Listen error", e);
                                return;
                            }

                            if (documentSnapshot.exists()){
                                final CollectionPost collectionPost = documentSnapshot.toObject(CollectionPost.class);

                                Picasso.with(mContext)
                                        .load(collectionPost.getImage())
                                        .networkPolicy(NetworkPolicy.OFFLINE)
                                        .into(holder.postImageView, new Callback() {
                                            @Override
                                            public void onSuccess() {

                                            }

                                            @Override
                                            public void onError() {
                                                Picasso.with(mContext)
                                                        .load(collectionPost.getImage())
                                                        .into(holder.postImageView, new Callback() {
                                                            @Override
                                                            public void onSuccess() {

                                                            }

                                                            @Override
                                                            public void onError() {
                                                                Log.v("Picasso", "Could not fetch image");
                                                            }
                                                        });


                                            }
                                        });



                            }
                        }
                    });


                }
            }
        });


        holder.cingleTradeMethodTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =  new Intent(mContext, PostDetailActivity.class);
                intent.putExtra(SellingAdapter.EXTRA_POST_KEY, postKey);
                mContext.startActivity(intent);
            }
        });

        holder.postImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, PostDetailActivity.class);
                intent.putExtra(SellingAdapter.EXTRA_POST_KEY, postKey);
                mContext.startActivity(intent);
            }
        });

        holder.ownerImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uid.equals(firebaseAuth.getCurrentUser().getUid())){
                    Intent intent = new Intent(mContext, PersonalProfileActivity.class);
                    intent.putExtra(SellingAdapter.EXTRA_USER_UID, uid);
                    mContext.startActivity(intent);

                }else {
                    Intent intent = new Intent(mContext, FollowerProfileActivity.class);
                    intent.putExtra(SellingAdapter.EXTRA_USER_UID, uid);
                    mContext.startActivity(intent);
                }
            }
        });

        holder.creatorImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uid.equals(firebaseAuth.getCurrentUser().getUid())){
                    Intent intent = new Intent(mContext, PersonalProfileActivity.class);
                    intent.putExtra(SellingAdapter.EXTRA_USER_UID, uid);
                    mContext.startActivity(intent);

                }else {
                    Intent intent = new Intent(mContext, FollowerProfileActivity.class);
                    intent.putExtra(SellingAdapter.EXTRA_USER_UID, uid);
                    mContext.startActivity(intent);
                }
            }
        });

        if (uid.equals(firebaseAuth.getCurrentUser().getUid())){
            holder.unlistPostTextView.setVisibility(View.VISIBLE);
            holder.unlistPostTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle bundle = new Bundle();
                    bundle.putString(SellingAdapter.EXTRA_POST_KEY, postKey);
                    FragmentManager fragmenManager = ((AppCompatActivity)mContext).getSupportFragmentManager();
                    DialogMarketPostSettings dialogMarketPostSettings = DialogMarketPostSettings.newInstance("post settings");
                    dialogMarketPostSettings.setArguments(bundle);
                    dialogMarketPostSettings.show(fragmenManager, "market post settings fragment");
                }
            });

        }else {
            holder.unlistPostTextView.setVisibility(View.GONE);
        }

        holder.buyPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString(SellingAdapter.EXTRA_POST_KEY, postKey);
                FragmentManager fragmenManager = ((AppCompatActivity)mContext).getSupportFragmentManager();
                DialogSendCredits dialogSendCredits = DialogSendCredits.newInstance("sens credits");
                dialogSendCredits.setArguments(bundle);
                dialogSendCredits.show(fragmenManager, "send credits fragment");
            }
        });


        ifairReference.document(postKey).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {

                if (e != null) {
                    Log.w(TAG, "Listen error", e);
                    return;
                }

                if (documentSnapshot.exists()){
                    holder.cingleTradeMethodTextView.setText("@Selling");
                }else {
                    holder.cingleTradeMethodTextView.setText("@NotOnSale");

                }

            }
        });

        senseCreditReference.document(postKey).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {

                if (e != null) {
                    Log.w(TAG, "Listen error", e);
                    return;
                }

                if (documentSnapshot.exists()){
                    final Credit credit = documentSnapshot.toObject(Credit.class);
                    DecimalFormat formatter = new DecimalFormat("0.00000000");
                    holder.cingleSenseCreditsTextView.setText("SC" + " " + formatter
                            .format(credit.getAmount()));
                }else {
                    holder.cingleSenseCreditsTextView.setText("SC 0.00000000");
                }
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
                    Andeqan cinggulan = documentSnapshot.toObject(Andeqan.class);
                    final String profileImage = cinggulan.getProfileImage();
                    final String username = cinggulan.getUsername();
                    holder.usernameTextView.setText(username);
                    Picasso.with(mContext)
                            .load(profileImage)
                            .resize(MAX_WIDTH, MAX_HEIGHT)
                            .onlyScaleDown()
                            .centerCrop()
                            .placeholder(R.drawable.profle_image_background)
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .into(holder.creatorImageView, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError() {
                                    Picasso.with(mContext)
                                            .load(profileImage)
                                            .resize(MAX_WIDTH, MAX_HEIGHT)
                                            .onlyScaleDown()
                                            .centerCrop()
                                            .placeholder(R.drawable.profle_image_background)
                                            .into(holder.creatorImageView);
                                }
                            });
                }
            }
        });

        ownerReference.document(postKey).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen error", e);
                    return;
                }

                if (documentSnapshot.exists()){
                    TransactionDetails transactionDetails = documentSnapshot.toObject(TransactionDetails.class);
                    final String ownerUid = transactionDetails.getUid();
                    Log.d("owner uid", ownerUid);

                    if (firebaseAuth.getCurrentUser().getUid().equals(ownerUid)){
                        holder.buyPostButton.setVisibility(View.GONE);
                    }else {
                        holder.buyPostButton.setVisibility(View.VISIBLE);
                    }

                    usersReference.document(ownerUid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {

                            if (e != null) {
                                Log.w(TAG, "Listen error", e);
                                return;
                            }

                            if (documentSnapshot.exists()){
                                Andeqan cinggulan = documentSnapshot.toObject(Andeqan.class);
                                final String profileImage = cinggulan.getProfileImage();
                                final String username = cinggulan.getUsername();
                                holder.cingleOwnerTextView.setText(username);
                                Picasso.with(mContext)
                                        .load(profileImage)
                                        .resize(MAX_WIDTH, MAX_HEIGHT)
                                        .onlyScaleDown()
                                        .centerCrop()
                                        .placeholder(R.drawable.profle_image_background)
                                        .networkPolicy(NetworkPolicy.OFFLINE)
                                        .into(holder.ownerImageView, new Callback() {
                                            @Override
                                            public void onSuccess() {

                                            }

                                            @Override
                                            public void onError() {
                                                Picasso.with(mContext)
                                                        .load(profileImage)
                                                        .resize(MAX_WIDTH, MAX_HEIGHT)
                                                        .onlyScaleDown()
                                                        .centerCrop()
                                                        .placeholder(R.drawable.profle_image_background)
                                                        .into(holder.ownerImageView);
                                            }
                                        });
                            }
                        }
                    });
                }
            }
        });


    }

}