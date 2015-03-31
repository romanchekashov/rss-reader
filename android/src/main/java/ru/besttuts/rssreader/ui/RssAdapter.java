package ru.besttuts.rssreader.ui;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import ru.besttuts.rssreader.Constants;
import ru.besttuts.rssreader.R;
import ru.besttuts.rssreader.model.Entry;
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

public class RssAdapter extends RecyclerView.Adapter<ViewHolder> {
    private static final String TAG = makeLogTag(RssAdapter.class);

    private List<Entry> mDataset = new ArrayList<>();
    private int positionToLoadMore;
    private final int POSITION_OFFSET = 5;
    private RssFragment mFragment;
    private boolean hasDataToLoad = true;

    // Provide a suitable constructor (depends on the kind of dataset)
    public RssAdapter(RssFragment fragment) {
        mFragment = fragment;
    }

    public void setData(List<Entry> data) {
        mDataset = data;
    }

    public void appendData(List<Entry> data) {
        mDataset.addAll(data);
    }

    public void changeCursor(Cursor newCursor) {
        hasDataToLoad = true;
        if (0 == newCursor.getCount()) {
            hasDataToLoad = false;
            return;
        }
        List<Entry> entries = new ArrayList<>(newCursor.getCount());

        newCursor.moveToFirst();
        do {
            entries.add(transform(newCursor));
        } while (newCursor.moveToNext());

        mDataset = entries;

        RssFragment.currentLoadedItems = mDataset.size();
        positionToLoadMore = RssFragment.currentLoadedItems - POSITION_OFFSET;
    }

    public void appendData(Cursor newCursor) {
        hasDataToLoad = true;
        if (0 == newCursor.getCount()) {
            hasDataToLoad = false;
            return;
        }

        List<Entry> entries = new ArrayList<>(newCursor.getCount());

        newCursor.moveToFirst();
        do {
            entries.add(transform(newCursor));
        } while (newCursor.moveToNext());

        mDataset.addAll(entries);

        RssFragment.currentLoadedItems = mDataset.size();
        positionToLoadMore = RssFragment.currentLoadedItems - POSITION_OFFSET;

        LOGD(TAG, String.format("appendData: mDataset.size = %d", mDataset.size()));
    }

    public void mergeCursor(Cursor newCursor) {
        hasDataToLoad = true;
        if (0 == newCursor.getCount()) {
            hasDataToLoad = false;
            return;
        }

        List<Entry> entries = new ArrayList<>(newCursor.getCount());

        if (mDataset.size() >= newCursor.getCount()) return;

        newCursor.moveToPosition(mDataset.size());
        do {
            entries.add(transform(newCursor));
        } while (newCursor.moveToNext());

        mDataset.addAll(entries);

        RssFragment.currentLoadedItems = mDataset.size();
        positionToLoadMore = RssFragment.currentLoadedItems - POSITION_OFFSET;

        LOGD(TAG, String.format("mergeCursor: mDataset.size = %d", mDataset.size()));
    }

    private Entry transform(Cursor cursor) {
        Entry entry = new Entry();
        entry.id = cursor.getInt(COLUMN_ID);
        entry.rssProvider = Constants.RssProvider.RSS_PROVIDER_ARRAY[cursor.getInt(COLUMN_RSS_PROVIDER)];
        entry.title = cursor.getString(COLUMN_TITLE);
        entry.description = cursor.getString(COLUMN_DESCRIPTION);
        entry.img = cursor.getString(COLUMN_IMG_SRC);
        entry.link = cursor.getString(COLUMN_LINK);
        RssFragment.prevTime = cursor.getLong(COLUMN_PUBLISHED);
        entry.pubDate = mFragment.dateFormat.format(RssFragment.prevTime);
        entry.isRead = 0 < cursor.getInt(COLUMN_IS_READ);
        return entry;
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
                Entry entry = mDataset.get(position);
                entry.isExpanded = !entry.isExpanded;
                if (entry.isExpanded) {
                    viewHolder.description.setVisibility(View.VISIBLE);
                    viewHolder.link.setVisibility(View.VISIBLE);
                    if (!entry.isRead) {
                        Uri uri = RssContract.Entry.CONTENT_URI.buildUpon()
                                .appendPath(String.valueOf(entry.id)).build();

                        ContentValues values = new ContentValues(0);
                        values.put(RssContract.Entry.COLUMN_NAME_IS_READ, Constants.RSS_ITEM_READ);
                        mFragment.getActivity().getContentResolver().update(uri, values, null, null);
                    }
                } else {
                    viewHolder.description.setVisibility(View.GONE);
                    viewHolder.link.setVisibility(View.GONE);
                    if (!entry.isRead) {
                        entry.isRead = true;
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

        Entry entry = mDataset.get(position);
        holder.position = position;
        holder.id = entry.id;
        holder.rssProvider.setText(entry.rssProvider);
        holder.title.setText(entry.title);
        holder.date.setText(entry.pubDate);
        holder.description.setText(entry.description);
        holder.link.setText(entry.link);
        holder.isRead = entry.isRead;

        if (entry.isRead) {
            holder.title.setTypeface(Typeface.DEFAULT);
        } else {
            holder.title.setTypeface(Typeface.DEFAULT_BOLD);
        }

        String image = entry.img;
        if (!TextUtils.isEmpty(image)) {
            holder.mainImage.setVisibility(View.VISIBLE);
            Picasso.with(mFragment.getActivity()).load(image).into(holder.mainImage);
        } else {
            holder.mainImage.setVisibility(View.GONE);
        }

        if (entry.isExpanded) {
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
        return mDataset.size();
    }

}
