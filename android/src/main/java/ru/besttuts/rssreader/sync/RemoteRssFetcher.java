package ru.besttuts.rssreader.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import retrofit.RestAdapter;
import retrofit.converter.SimpleXMLConverter;
import ru.besttuts.rssreader.Constants;
import ru.besttuts.rssreader.sync.model.RssGazetaRu;
import ru.besttuts.rssreader.sync.model.RssItem;
import ru.besttuts.rssreader.sync.model.RssLentaRu;
import ru.besttuts.rssreader.provider.RssContract;

import static ru.besttuts.rssreader.util.LogUtils.LOGD;
import static ru.besttuts.rssreader.util.LogUtils.makeLogTag;

/**
 * Created by roman on 28.03.2015.
 */
public class RemoteRssFetcher {

    private static final String TAG = makeLogTag(RemoteRssFetcher.class);

    private Context mContext = null;

    public RemoteRssFetcher(Context context) {
        mContext = context;
    }

    // Sat, 28 Mar 2015 09:30:00 +0300
    private SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN, Locale.US);

    private ContentValues transform(RssItem item) throws ParseException {
        ContentValues values = new ContentValues();
        values.put(RssContract.Entry.COLUMN_NAME_GUID, item.guid);
        values.put(RssContract.Entry.COLUMN_NAME_TITLE, item.title);
        values.put(RssContract.Entry.COLUMN_NAME_DESCRIPTION, item.description);
        values.put(RssContract.Entry.COLUMN_NAME_LINK, item.link);
        values.put(RssContract.Entry.COLUMN_NAME_PUBLISHED, dateFormat.parse(item.pubDate).getTime());
        values.put(RssContract.Entry.COLUMN_NAME_IS_READ, Constants.RSS_ITEM_NOT_READ);

        if (null != item.enclosure) {
            values.put(RssContract.Entry.COLUMN_NAME_IMG_SRC, item.enclosure.url);
        }

        if (item instanceof RssLentaRu.LentaRuRssItem) {
            values.put(RssContract.Entry.COLUMN_NAME_RSS_PROVIDER, Constants.RssProvider.LENTA_RU);
        } else {
            values.put(RssContract.Entry.COLUMN_NAME_RSS_PROVIDER, Constants.RssProvider.GAZETA_RU);
        }

        return values;
    }

    public void doRemoteSync(ContentResolver contentResolver, SyncResult syncResult) throws RemoteException, OperationApplicationException {
        LOGD(TAG, String.format("doRemoteSync: %s, %s", contentResolver, syncResult));

        long start = System.currentTimeMillis();

//        RssLentaRu rssLentaRu = fetchLentaRu();
//        RssGazetaRu rssGazetaRu = fetchGazetaRu();
//
//        LOGD(TAG, String.format("fetchLentaRu: %s, size = %d", rssLentaRu, rssLentaRu.channel.items.size()));
//        LOGD(TAG, String.format("Fetching took %d ms", System.currentTimeMillis() - start));
//
//        List<RssLentaRu.LentaRuRssItem> rssLentaItems = rssLentaRu.channel.items;
//        List<RssGazetaRu.GazetaRuRssItem> rssGazetaItems = rssGazetaRu.channel.items;

        ExecutorService exec = Executors.newFixedThreadPool(2);
        final CountDownLatch latch = new CountDownLatch(2);

        Future<List<RssLentaRu.LentaRuRssItem>> rssLentaItemsFuture = exec.submit(new Callable<List<RssLentaRu.LentaRuRssItem>>() {
            @Override
            public List<RssLentaRu.LentaRuRssItem> call() throws Exception {
                RssLentaRu rssLentaRu = fetchLentaRu();
                latch.countDown();
                LOGD(TAG, String.format("fetchLentaRu: %s, size = %d", rssLentaRu, rssLentaRu.channel.items.size()));
                return rssLentaRu.channel.items;
            }
        });
        Future<List<RssGazetaRu.GazetaRuRssItem>> rssGazetaItemsFuture = exec.submit(new Callable<List<RssGazetaRu.GazetaRuRssItem>>() {
            @Override
            public List<RssGazetaRu.GazetaRuRssItem> call() throws Exception {
                RssGazetaRu rssGazetaRu = fetchGazetaRu();
                latch.countDown();
                LOGD(TAG, String.format("fetchGazetaRu: %s, size = %d", rssGazetaRu, rssGazetaRu.channel.items.size()));
                return rssGazetaRu.channel.items;
            }
        });

        List<RssLentaRu.LentaRuRssItem> rssLentaItems = new ArrayList<>();
        List<RssGazetaRu.GazetaRuRssItem> rssGazetaItems = new ArrayList<>();

        try {
            latch.await();
            rssLentaItems = rssLentaItemsFuture.get();
            rssGazetaItems = rssGazetaItemsFuture.get();

            LOGD(TAG, String.format("Fetching took %d ms", System.currentTimeMillis() - start));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        int rssLentaItemsLength = rssLentaItems.size();
        int rssGazetaItemsLength = rssGazetaItems.size();

        // Get list of all items
        LOGD(TAG, "Fetching local entries for merge");
        Uri uri = RssContract.Entry.CONTENT_URI; // Get all entries
        Cursor c = contentResolver.query(uri, new String[]{RssContract.Entry.COLUMN_NAME_GUID, RssContract.Entry._ID, RssContract.Entry.COLUMN_NAME_PUBLISHED},
                null, null,
                RssContract.Entry._ID + " desc limit " + (rssLentaItemsLength + rssGazetaItemsLength));
        assert c != null;
        LOGD(TAG, "Found " + c.getCount() + " local entries. Computing merge solution...");
        LOGD(TAG, String.format("Fetching local entries took %d ms", System.currentTimeMillis() - start));

        HashSet<String> existingEntries = new HashSet<>();
        while (c.moveToNext()) {
            existingEntries.add(c.getString(0));
//            LOGD(TAG, String.format("Entry: id = %d, date = %d, guid = %s", c.getInt(1), c.getLong(2), c.getString(0)));
        }
        c.close();
        LOGD(TAG, String.format("Filling HashSet with local entries took %d ms", System.currentTimeMillis() - start));

        List<ContentValues> list = new ArrayList<>(rssLentaItemsLength + rssGazetaItemsLength);

        int lentaIndex = 0;
        int gazetaIndex = 0;
        ContentValues lentaRuRssItem = null, gazetaRuRssItem = null;
        long lentaPubTime = 0, gazetaPubTime = 0;

        while (lentaIndex != rssLentaItemsLength || gazetaIndex != rssGazetaItemsLength) {
            try {

                if (null == lentaRuRssItem) {
                    lentaRuRssItem = transform(rssLentaItems.get(lentaIndex));
                    lentaPubTime = (long) lentaRuRssItem.get(RssContract.Entry.COLUMN_NAME_PUBLISHED);
                }
                if (null == gazetaRuRssItem) {
                    gazetaRuRssItem = transform(rssGazetaItems.get(gazetaIndex));
                    gazetaPubTime = (long) gazetaRuRssItem.get(RssContract.Entry.COLUMN_NAME_PUBLISHED);
                }


                if(lentaPubTime >= gazetaPubTime) {
                    if (!existingEntries.contains(lentaRuRssItem.getAsString(RssContract.Entry.COLUMN_NAME_GUID))) {
                        list.add(lentaRuRssItem);
                    }
                    lentaIndex++;
                    if(lentaIndex != rssLentaItemsLength) {
                        lentaRuRssItem = null;
                    } else {
                        for (; gazetaIndex < rssGazetaItemsLength; gazetaIndex++) {
                            gazetaRuRssItem = transform(rssGazetaItems.get(gazetaIndex));
                            if (!existingEntries.contains(gazetaRuRssItem.getAsString(RssContract.Entry.COLUMN_NAME_GUID))) {
                                list.add(gazetaRuRssItem);
                            }
                        }
                    }
                } else {
                    if (!existingEntries.contains(gazetaRuRssItem.getAsString(RssContract.Entry.COLUMN_NAME_GUID))) {
                        list.add(gazetaRuRssItem);
                    }
                    gazetaIndex++;
                    if (gazetaIndex != rssGazetaItemsLength) {
                        gazetaRuRssItem = null;
                    } else {
                        for (; lentaIndex < rssLentaItemsLength; lentaIndex++) {
                            lentaRuRssItem = transform(rssLentaItems.get(lentaIndex));
                            if (!existingEntries.contains(lentaRuRssItem.getAsString(RssContract.Entry.COLUMN_NAME_GUID))) {
                                list.add(lentaRuRssItem);
                            }
                        }
                    }
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        // Add new items
        for (int i = list.size() - 1; i >= 0; i--) {
            LOGD(TAG, "Scheduling insert: entry_guid=" + list.get(i).get(RssContract.Entry.COLUMN_NAME_GUID));

            batch.add(ContentProviderOperation.newInsert(RssContract.Entry.CONTENT_URI)
                    .withValues(list.get(i)).build());

            syncResult.stats.numInserts++;
        }
        LOGD(TAG, "Merge solution ready. Applying batch update");
        contentResolver.applyBatch(RssContract.CONTENT_AUTHORITY, batch);
        contentResolver.notifyChange(
                RssContract.Entry.CONTENT_URI, // URI where data was modified
                null,                           // No local observer
                false);                         // IMPORTANT: Do not sync to network
        // This sample doesn't support uploads, but if *your* code does, make sure you set
        // syncToNetwork=false in the line above to prevent duplicate syncs.

    }

    public RssLentaRu fetchLentaRu() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("http://lenta.ru")
                .setConverter(new SimpleXMLConverter())
                .build();

        RssRetrofitService apiService = restAdapter.create(RssRetrofitService.class);
        return apiService.getLentaRuRss();
    }

    public RssGazetaRu fetchGazetaRu() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("http://www.gazeta.ru")
                .setConverter(new SimpleXMLConverter())
                .build();

        RssRetrofitService apiService = restAdapter.create(RssRetrofitService.class);
        return apiService.getGazetaRuRss();
    }

}
