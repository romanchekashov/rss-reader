package ru.besttuts.rssreader.sync;

import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.RemoteException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import ru.besttuts.rssreader.BuildConfig;
import ru.besttuts.rssreader.MainActivity;
import ru.besttuts.rssreader.sync.model.RssGazetaRu;
import ru.besttuts.rssreader.sync.model.RssItem;
import ru.besttuts.rssreader.sync.model.RssLentaRu;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by roman on 28.03.2015.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class RemoteRssFetcherTest {

    RemoteRssFetcher fetcher;
    ContentResolver contentResolver;

    @Before
    public void setUp() throws Exception {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
        fetcher = new RemoteRssFetcher(activity);
        contentResolver = activity.getContentResolver();
    }

    @Test
    public void testFetchLentaRu() {
        RssLentaRu rss = fetcher.fetchLentaRu();

        assertThat(rss).isNotNull();

        assertThat(rss.channel.items).isNotEmpty();

    }

    @Test
    public void testFetchGazetaRu() {
        RssGazetaRu rss = fetcher.fetchGazetaRu();

        assertThat(rss).isNotNull();

        assertThat(rss.channel.items).isNotEmpty();

        for (RssItem item: rss.channel.items) {
            System.out.println(item.guid);
        }
    }

    @Test
    public void testDoRemoteSync() throws RemoteException, OperationApplicationException {

        fetcher.doRemoteSync(contentResolver, new SyncResult());

    }
}
