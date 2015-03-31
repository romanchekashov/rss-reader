package ru.besttuts.rssreader.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import ru.besttuts.rssreader.Constants;
import ru.besttuts.rssreader.MainActivity;
import ru.besttuts.rssreader.R;
import ru.besttuts.rssreader.model.Entry;
import ru.besttuts.rssreader.provider.RssContract;
import ru.besttuts.rssreader.sync.SyncUtils;

import static ru.besttuts.rssreader.util.LogUtils.LOGD;
import static ru.besttuts.rssreader.util.LogUtils.makeLogTag;

/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 */
public class RssFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = makeLogTag(RssFragment.class);

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private RecyclerView mRecyclerView;
    private CursorRssAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    SimpleDateFormat dateFormat = new SimpleDateFormat();

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_VISIBLE_ITEM_POSITION = "visible_item_position";
    private static final String ARG_CURRENT_LOADED_ITEMS = "current_loaded_items";
    private static final String ARG_PREV_TIME = "prevTime";
    private static final String ARG_WAS_STATE_CHANGE = "wasStateChange";
    public static final int DEFAULT_RSS_COUNT_PER_REQUEST = 40;

    public static int visibleItemPosition;
    public static int currentLoadedItems = DEFAULT_RSS_COUNT_PER_REQUEST;
    public static long prevTime;
    public static boolean wasStateChange;

    /**
     * Projection for querying the content provider.
     */
    static final String[] PROJECTION = new String[]{
            RssContract.Entry._ID,      // 0
            RssContract.Entry.COLUMN_NAME_GUID,      // 1
            RssContract.Entry.COLUMN_NAME_RSS_PROVIDER,       // 2
            RssContract.Entry.COLUMN_NAME_TITLE, // 3
            RssContract.Entry.COLUMN_NAME_DESCRIPTION, // 4
            RssContract.Entry.COLUMN_NAME_IMG_SRC, // 5
            RssContract.Entry.COLUMN_NAME_LINK, // 6
            RssContract.Entry.COLUMN_NAME_PUBLISHED, // 7
            RssContract.Entry.COLUMN_NAME_IS_READ // 8
    };

    static final int COLUMN_ID = 0;
    static final int COLUMN_GUID = 1;
    static final int COLUMN_RSS_PROVIDER = 2;
    static final int COLUMN_TITLE = 3;
    static final int COLUMN_DESCRIPTION = 4;
    static final int COLUMN_IMG_SRC = 5;
    static final int COLUMN_LINK = 6;
    static final int COLUMN_PUBLISHED = 7;
    static final int COLUMN_IS_READ = 8;

    private OnFragmentInteractionListener mListener;

    public static RssFragment newInstance() {
        return new RssFragment();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RssFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LOGD(TAG, String.format("onCreate: %s", this));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            visibleItemPosition = savedInstanceState.getInt(ARG_VISIBLE_ITEM_POSITION);
            currentLoadedItems = savedInstanceState.getInt(ARG_CURRENT_LOADED_ITEMS);
            prevTime = savedInstanceState.getLong(ARG_PREV_TIME);
            wasStateChange = savedInstanceState.getBoolean(ARG_WAS_STATE_CHANGE);
        }
        View view = inflater.inflate(R.layout.fragment_rss, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);

        mSwipeRefreshLayout.setColorSchemeResources(R.color.theme_primary_deep_orange);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerViewNews);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        ((LinearLayoutManager) mLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new CursorRssAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        // Create account, if needed
        SyncUtils.CreateSyncAccount(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        LOGD(TAG, String.format("onCreateLoader(%d, %s)", i, bundle));
        // We only have one loader, so we can ignore the value of i.
        // (It'll be '0', as set in onCreate().)
        return new CursorLoader(getActivity(),  // Context
                RssContract.Entry.CONTENT_URI, // URI
                PROJECTION,                // Projection
                null,                           // Selection
                null,                           // Selection args
                RssContract.Entry.COLUMN_NAME_PUBLISHED + " desc limit " + currentLoadedItems); // Sort
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LOGD(TAG, String.format("onLoadFinished: %d", null != cursor ? cursor.getCount() : 0));
        if(0 < cursor.getCount()) MainActivity.hideLoading();
        onRefreshComplete(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        LOGD(TAG, String.format("onLoaderReset(%s)", cursorLoader));
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onNewsClick(int position);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                LOGD(TAG, "onRefresh called from SwipeRefreshLayout");
                SyncUtils.TriggerRefresh();
            }
        });

        // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
        if (!mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(true);
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(ARG_VISIBLE_ITEM_POSITION,
                ((LinearLayoutManager) mLayoutManager).findFirstVisibleItemPosition());
        outState.putInt(ARG_CURRENT_LOADED_ITEMS, currentLoadedItems);
        outState.putLong(ARG_PREV_TIME, prevTime);
        outState.putBoolean(ARG_WAS_STATE_CHANGE, true);
    }

    private void onRefreshComplete(Cursor cursor) {

        if (null != cursor && 0 < cursor.getCount()) {
            // Remove all items from the ListAdapter, and then replace them with the new items
            mAdapter.changeCursor(cursor);
            mAdapter.notifyDataSetChanged();
        }

        if (wasStateChange) {
            mLayoutManager.scrollToPosition(visibleItemPosition);
            wasStateChange = false;
        }

        // Stop the refreshing indicator
        mSwipeRefreshLayout.setRefreshing(false);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        public ImageView mainImage;

        public TextView title;
        public TextView rssProvider;
        public TextView date;
        public TextView description;
        public TextView link;
        public boolean isRead;
        public boolean isExpanded;
        public int position;
        public int id;
        public IMyViewHolderClicks mListener;

        public ViewHolder(View v, IMyViewHolderClicks listener) {
            super(v);
            mListener = listener;
            mainImage = (ImageView) v.findViewById(R.id.mainImage);
            title = (TextView) v.findViewById(R.id.tvTitle);
            rssProvider = (TextView) v.findViewById(R.id.tvRssProvider);
            date = (TextView) v.findViewById(R.id.tvDate);
            description = (TextView) v.findViewById(R.id.tvDescription);
            link = (TextView) v.findViewById(R.id.tvLink);

            link.setOnClickListener(this);
            // Is this needed or handled automatically by RecyclerView.ViewHolder?
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v instanceof TextView) {
                mListener.onLinkClick((TextView) v);
            } else {
                mListener.onClick(this, v, position);
            }
        }

        public static interface IMyViewHolderClicks {
            public void onLinkClick(TextView link);

            public void onClick(ViewHolder viewHolder, View caller, int position);
        }
    }

}

