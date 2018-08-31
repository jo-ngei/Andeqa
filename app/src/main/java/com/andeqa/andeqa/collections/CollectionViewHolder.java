package com.andeqa.andeqa.collections;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.andeqa.andeqa.R;
import com.andeqa.andeqa.utils.ProportionalImageView;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by J.EL on 2/28/2018.
 */

public class CollectionViewHolder extends RecyclerView.ViewHolder {

    View mView;
    Context mContext;
    public ImageView mCollectionCoverImageView;
    public CircleImageView profileImageView;
    public TextView usernameTextView;
    public Button followButton;
    public TextView mCollectionNameTextView;
    public TextView mCollectionsNoteTextView;
    public LinearLayout mCollectionsLinearLayout;
    public LinearLayout collectionDetailsLinearLayout;
    public TextView followingCountTextView;


    public CollectionViewHolder(View itemView) {
        super(itemView);
        mContext = itemView.getContext();
        mView = itemView;
        mCollectionCoverImageView = (ImageView) mView.findViewById(R.id.collectionCoverImageView);
        profileImageView = (CircleImageView) mView.findViewById(R.id.profileImageView);
        mCollectionNameTextView = (TextView) mView.findViewById(R.id.collectionNameTextView);
        mCollectionsNoteTextView = (TextView) mView.findViewById(R.id.collectionsNoteTextView);
        mCollectionsLinearLayout = (LinearLayout) mView.findViewById(R.id.collectionLinearLayout);
        followButton = (Button) mView.findViewById(R.id.followButton);
        usernameTextView = (TextView) mView.findViewById(R.id.usernameTextView);
        collectionDetailsLinearLayout = (LinearLayout) mView.findViewById(R.id.collectionDetailsLinearLayout);
        followingCountTextView  = (TextView) mView.findViewById(R.id.followingCountTextView);


    }


}

