package com.cinggl.cinggl.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cinggl.cinggl.Constants;
import com.cinggl.cinggl.R;
import com.cinggl.cinggl.home.BestCinglesFragment;
import com.cinggl.cinggl.models.Cingle;
import com.cinggl.cinggl.models.CingleSale;
import com.cinggl.cinggl.models.Cingulan;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import static android.media.CamcorderProfile.get;
import static com.cinggl.cinggl.R.id.cingleImageView;

/**
 * Created by J.EL on 9/14/2017.
 */

public class IfairCingleAdapter extends RecyclerView.Adapter<IfairCinglesViewHolder> {
    private Context mContext;
    private static final String EXTRA_POST_KEY = "post key";
    private static final String EXTRA_USER_UID = "uid";
    private DatabaseReference cinglesReference;
    private DatabaseReference usersRef;
    private DatabaseReference ifairReference;
    private DatabaseReference cingleOwnerReference;
    private FirebaseAuth firebaseAuth;
    private boolean processLikes = false;
    private static final double DEFAULT_PRICE = 1.5;
    private static final double GOLDEN_RATIO = 1.618;
    private static final String TAG = IfairCingleAdapter.class.getSimpleName();
    private List<CingleSale> ifairCingles = new ArrayList<>();
    public static final int MAX_WIDTH = 400;
    public static final int MAX_HEIGHT = 400;

    public IfairCingleAdapter(Context mContext) {
        this.mContext = mContext;

    }

    public void setIfairCingles(List<CingleSale> ifairCingles) {
        this.ifairCingles = ifairCingles;
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        ifairCingles.remove(ifairCingles.get(position));
    }


    @Override
    public int getItemCount() {
        return ifairCingles.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public IfairCinglesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ifair_cingles_layout, parent, false );

        return new IfairCinglesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final IfairCinglesViewHolder holder, int position) {
        final CingleSale cingleSale = ifairCingles.get(position);
        final String postKey = ifairCingles.get(position).getPushId();
        Log.d(postKey, "ifair cingles postkey");
//        animate(holder);
        //CALL THE METHOD TO ANIMATE RECYCLER_VIEW
        firebaseAuth = FirebaseAuth.getInstance();

        //DATABASE REFERENCE PATH;
        usersRef = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_USERS);
        cinglesReference = FirebaseDatabase.getInstance()
                .getReference(Constants.FIREBASE_CINGLES);
        ifairReference = FirebaseDatabase.getInstance().getReference(Constants.IFAIR);
        cingleOwnerReference = FirebaseDatabase.getInstance().getReference(Constants.CINGLE_ONWERS);

        usersRef.keepSynced(true);
        cinglesReference.keepSynced(true);
        cingleOwnerReference.keepSynced(true);

        ifairReference.child("Cingle Selling").child(postKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    CingleSale sale = dataSnapshot.getValue(CingleSale.class);
                    final String uid = sale.getUid();
                    final String pushId = sale.getPushId();

                    //retrieve cingle info
                    cinglesReference.child(pushId).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()){
                               final Cingle cingle = dataSnapshot.getValue(Cingle.class);

                                Picasso.with(mContext)
                                        .load(cingle.getCingleImageUrl())
                                        .networkPolicy(NetworkPolicy.OFFLINE)
                                        .into(holder.cingleImageView, new Callback() {
                                            @Override
                                            public void onSuccess() {

                                            }

                                            @Override
                                            public void onError() {
                                                Picasso.with(mContext)
                                                        .load(cingle.getCingleImageUrl())
                                                        .into(holder.cingleImageView);


                                            }
                                        });
                                holder.cingleSenseCreditsTextView.setText(Double.toString(cingle.getSensepoint()));
                                holder.datePostedTextView.setText(cingle.getDatePosted());

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    //retrieve user info
                    usersRef.child(uid).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()){
                                final Cingulan cingulan = new Cingulan();

                                Picasso.with(mContext)
                                        .load(cingulan.getProfileImage())
                                        .networkPolicy(NetworkPolicy.OFFLINE)
                                        .into(holder.profileImageView, new Callback() {
                                            @Override
                                            public void onSuccess() {

                                            }

                                            @Override
                                            public void onError() {
                                                Picasso.with(mContext)
                                                        .load(cingulan.getProfileImage())
                                                        .into(holder.profileImageView);


                                            }
                                        });
                                holder.usernameTextView.setText(cingulan.getUsername());

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //SET THE TRADE METHOD TEXT ACCORDING TO THE TRADE METHOD OF THE CINGLE
        ifairReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final String uid = dataSnapshot.child("Cingle Selling")
                        .child(postKey).child("uid").getValue(String.class);

                //SET CINGLE TRADE METHOD WHEN THERE ARE ALL TRADE METHODS
                if (dataSnapshot.child("Cingle Lacing").hasChild(postKey)){
                    holder.cingleTradeMethodTextView.setText("@CingleLacing");
                }else if (dataSnapshot.child("Cingle Leasing").hasChild(postKey)){
                    holder.cingleTradeMethodTextView.setText("@CingleLeasing");

                }else if (dataSnapshot.child("Cingle Selling").hasChild(postKey)){
                    holder.cingleTradeMethodTextView.setText("@CingleSelling");
                }else if ( dataSnapshot.child("Cingle Backing").hasChild(postKey)){
                    holder.cingleTradeMethodTextView.setText("@CingleBacking");
                }else {
                    holder.cingleTradeMethodTextView.setText("@NotForTrade");
                }


                //HIDE TRADING LAYOUT IF CINGLE IS NOT ON IFAIR
                if (!dataSnapshot.child("Cingle Selling").hasChild(postKey)){
                    holder.cingleTradeMethodTextView.setText("Listed not for sale");
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        //SET THE OWNER OF THE CINGLE
        cingleOwnerReference.child(postKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    final String ownerUid = dataSnapshot.child("owner").getValue(String.class);
                    usersRef.child(ownerUid).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Cingulan cingulan = dataSnapshot.getValue(Cingulan.class);
                            final String username = cingulan.getUsername();
                            final String profileImage = cingulan.getProfileImage();
                            holder.cingleOwnerTextView.setText(username);
                            Picasso.with(mContext)
                                    .load(profileImage)
                                    .fit()
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
                                                    .fit()
                                                    .centerCrop()
                                                    .placeholder(R.drawable.profle_image_background)
                                                    .into(holder.ownerImageView);
                                        }
                                    });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }else {
                    //RETRIEVE THE CREATOR PERSONAL PROFILE DETAIL FOR THE CINGLE
                    cinglesReference.child(postKey).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            final String creatorUid = dataSnapshot.child("uid").getValue(String.class);
                            usersRef.child(creatorUid).addValueEventListener
                                    (new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Cingulan cingulan = dataSnapshot.getValue(Cingulan.class);
                                            final String username = cingulan.getUsername();
                                            final String profileImage = cingulan.getProfileImage();
                                            holder.cingleOwnerTextView.setText(username);
                                            Picasso.with(mContext)
                                                    .load(profileImage)
                                                    .fit()
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
                                                                    .fit()
                                                                    .centerCrop()
                                                                    .placeholder(R.drawable.profle_image_background)
                                                                    .into(holder.ownerImageView);
                                                        }
                                                    });

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

}