package ru.besttuts.rssreader.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.HashSet;

import ru.besttuts.rssreader.Constants;
import ru.besttuts.rssreader.R;
import ru.besttuts.rssreader.provider.RssContract;

import static ru.besttuts.rssreader.ui.RssFragment.COLUMN_DESCRIPTION;
import static ru.besttuts.rssreader.ui.RssFragment.COLUMN_ID;
import static ru.besttuts.rssreader.ui.RssFragment.COLUMN_IMG_SRC;
import static ru.besttuts.rssreader.ui.RssFragment.COLUMN_IS_READ;
import static ru.besttuts.rssreader.ui.RssFragment.COLUMN_LINK;
import static ru.besttuts.rssreader.ui.RssFragment.COLUMN_PUBLISHED;
import static ru.besttuts.rssreader.ui.RssFragment.COLUMN_RSS_PROVIDER;
import static ru.besttuts.rssreader.ui.RssFragment.COLUMN_TITLE;
import static ru.besttuts.rssreader.ui.RssFragment.PROJECTION;
import static ru.besttuts.rssreader.ui.RssFragment.ViewHolder;
import static ru.besttuts.rssreader.util.LogUtils.makeLogTag;
import static ru.besttuts.rssreader.util.LogUtils.LOGD;

/**
 * Created by roman on 31.03.2015.
 */
public class CursorRssAdapter extends RecyclerView.Adapter<ViewHolder> {

    private static final String TAG = makeLogTag(CursorRssAdapter.class);

    private Cursor mCursor = new MatrixCursor(PROJECTION);
    private HashSet<Integer> expandedIds = new HashSet<>();
    private int positionToLoadMore;
    private final int POSITION_OFFSET = 5;
    private RssFragment mFragment;
    private boolean hasDataToLoad = true;

    // Provide a suitable constructor (depends on the kind of dataset)
    public CursorRssAdapter(RssFragment fragment) {
        mFragment = fragment;
    }

    public void changeCursor(Cursor newCursor) {
        hasDataToLoad = true;
        if (0 == newCursor.getCount()) {
            hasDataToLoad = false;
            return;
        }

        mCursor = newCursor;

        RssFragment.currentLoadedItems = mCursor.getCount();
        positionToLoadMore = mCursor.getCount() - POSITION_OFFSET;

        LOGD(TAG, String.format("appendData: mCursor.getCount() = %d", mCursor.getCount()));
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_rss_item, parent, false);

        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v, new ViewHolder.IMyViewHolderClicks() {
            @Override
            public void onLinkClick(TextView link) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.getText().toString()));
                mFragment.startActivity(browserIntent);
            }

            @Override
            public void onClick(ViewHolder viewHolder, View caller, int position) {
                mCursor.moveToPosition(position);
                int id = mCursor.getInt(COLUMN_ID);
                boolean isRead = 0 < mCursor.getInt(COLUMN_IS_READ);
                if (!expandedIds.contains(id)) {
                    expandedIds.add(id);

                    viewHolder.description.setVisibility(View.VISIBLE);
                    viewHolder.link.setVisibility(View.VISIBLE);
                    if (!isRead) {
                        Uri uri = RssContract.Entry.CONTENT_URI.buildUpon()
                                .appendPath(String.valueOf(id)).build();

                        ContentValues values = new ContentValues(0);
                        values.put(RssContract.Entry.COLUMN_NAME_IS_READ, Constants.RSS_ITEM_READ);
                        mFragment.getActivity().getContentResolver().update(uri, values, null, null);
                    }
                } else {
                    expandedIds.remove(id);

                    viewHolder.description.setVisibility(View.GONE);
                    viewHolder.link.setVisibility(View.GONE);
                    if (!isRead) {
                        viewHolder.title.setTypeface(Typeface.DEFAULT);
                    }
                }
            }
        });
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        LOGD(TAG, "onBindViewHolder: position = " + position);

        mCursor.moveToPosition(position);

        holder.position = position;
        holder.id = mCursor.getInt(COLUMN_ID);
        holder.rssProvider.setText(Constants.RssProvider.RSS_PROVIDER_ARRAY[mCursor.getInt(COLUMN_RSS_PROVIDER)]);
        holder.title.setText(mCursor.getString(COLUMN_TITLE));
        holder.description.setText(mCursor.getString(COLUMN_DESCRIPTION));
        holder.link.setText(mCursor.getString(COLUMN_LINK));
        holder.date.setText(mFragment.dateFormat.format(mCursor.getLong(COLUMN_PUBLISHED)));

        if (0 < mCursor.getInt(COLUMN_IS_READ)) {
            holder.title.setTypeface(Typeface.DEFAULT);
        } else {
            holder.title.setTypeface(Typeface.DEFAULT_BOLD);
        }

        String image = mCursor.getString(COLUMN_IMG_SRC);
        if (!TextUtils.isEmpty(image)) {
            holder.mainImage.setVisibility(View.VISIBLE);
            Picasso.with(mFragment.getActivity()).load(image).into(holder.mainImage);
        } else {
            holder.mainImage.setVisibility(View.GONE);
        }

        if (expandedIds.contains(mCursor.getInt(COLUMN_ID))) {
            holder.description.setVisibility(View.VISIBLE);
            holder.link.setVisibility(View.VISIBLE);
        } else {
            holder.description.setVisibility(View.GONE);
            holder.link.setVisibility(View.GONE);
        }

        if (hasDataToLoad && positionToLoadMore == position) {
//                Cursor c = getActivity().getContentResolver().query(RssContract.Entry.CONTENT_URI,
//                        PROJECTION,
//                        RssContract.Entry.COLUMN_NAME_PUBLISHED + " < ?",
//                        new String[]{String.valueOf(prevTime)},
//                        RssContract.Entry._ID + " desc limit " + DEFAULT_RSS_COUNT_PER_REQUEST);
//                appendData(c);
//
//                c.close();
//                Bundle bundle = new Bundle();
//                bundle.putLong(ARG_PREV_TIME, prevTime);
//                bundle.putInt(ARG_CURRENT_LOADED_ITEMS, currentLoadedItems);
            RssFragment.currentLoadedItems += RssFragment.DEFAULT_RSS_COUNT_PER_REQUEST;
            mFragment.getLoaderManager().restartLoader(0, null, mFragment);

//                LOGD(TAG, String.format("mRecyclerView.onBindViewHolder: position(%d)", position));
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

}
