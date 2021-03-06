package com.andeqa.andeqa.utils;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

public abstract class EndlessStaggeredScrollListener extends RecyclerView.OnScrollListener {
//    public static String TAG = EndlesssStaggeredRecyclerOnScrollListener.class.getSimpleName();

    /**
     * The total number of items in the dataset after the last load
     */
    private int mPreviousTotal = 0;
    /**
     * True if we are still waiting for the last set of data to load.
     */
    private boolean mLoading = true;
    private StaggeredGridLayoutManager layoutManager;

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        int visibleItemCount = recyclerView.getChildCount();
        int totalItemCount = recyclerView.getLayoutManager().getItemCount();
        layoutManager = ((StaggeredGridLayoutManager) recyclerView.getLayoutManager());
        int [] firstVisibleItem = layoutManager.findFirstVisibleItemPositions(null);

        if (mLoading) {
            if (totalItemCount > mPreviousTotal) {
                mLoading = false;
                mPreviousTotal = totalItemCount;
            }
        }
        int visibleThreshold = 10;
        if (!mLoading && (totalItemCount - visibleItemCount)
                <= (firstVisibleItem[0] + visibleThreshold)) {
            // End has been reached

            onLoadMore();

            mLoading = true;
        }
    }

    public abstract void onLoadMore();
}
