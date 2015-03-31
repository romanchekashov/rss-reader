package ru.besttuts.rssreader.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import ru.besttuts.rssreader.BuildConfig;
import ru.besttuts.rssreader.Constants;
import ru.besttuts.rssreader.MainActivity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by roman on 28.03.2015.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class RssProviderTest {

    ContentResolver contentResolver;
    SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN, Locale.US);

    String guid = "http://www.gazeta.ru/social/news/2015/03/29/n_7059653.shtml";
    int rssProvider = Constants.RssProvider.GAZETA_RU;
    String title = "Московский ювелирный салон ограбили на миллион рублей";
    String description = "На северо-западе Москвы произошло вооруженное ограбление ювелирного салона сети \"Адамас\", передает агентство городских новостей \"Москва\". \"В ювелирный салон вошли пятеро неизвестных в матерчатых масках с ...";
    String img = null;
    String link = "http://www.gazeta.ru/social/news/2015/03/29/n_7059653.shtml";
    String sPubDate = "Sun, 29 Mar 2015 10:24:55 +0300";
    int isRead = 0;

    @Before
    public void setUp() throws Exception {
        MainActivity activity = Robolectric.setupActivity(MainActivity.class);
        contentResolver = activity.getContentResolver();
    }

    @Test
    public void testEntryContentUriIsSane() {
        assertThat(Uri.parse("content://ru.besttuts.rssreader/entries"))
                .isEqualTo(RssContract.Entry.CONTENT_URI);
    }

    @Test
    public void testCreateRetrieveDelete() throws ParseException {

        long pubDate = dateFormat.parse(sPubDate).getTime();

        // Create
        ContentValues newValues = new ContentValues();
        newValues.put(RssContract.Entry.COLUMN_NAME_GUID, guid);
        newValues.put(RssContract.Entry.COLUMN_NAME_RSS_PROVIDER, rssProvider);
        newValues.put(RssContract.Entry.COLUMN_NAME_TITLE, title);
        newValues.put(RssContract.Entry.COLUMN_NAME_DESCRIPTION, description);
        newValues.put(RssContract.Entry.COLUMN_NAME_IMG_SRC, img);
        newValues.put(RssContract.Entry.COLUMN_NAME_LINK, link);
        newValues.put(RssContract.Entry.COLUMN_NAME_PUBLISHED, pubDate);
        newValues.put(RssContract.Entry.COLUMN_NAME_IS_READ, isRead);
        Uri newUri = contentResolver.insert(RssContract.Entry.CONTENT_URI, newValues);

        // Retrieve
        String[] projection = {
                RssContract.Entry.COLUMN_NAME_GUID,      // 0
                RssContract.Entry.COLUMN_NAME_RSS_PROVIDER,       // 1
                RssContract.Entry.COLUMN_NAME_TITLE, // 2
                RssContract.Entry.COLUMN_NAME_DESCRIPTION, // 3
                RssContract.Entry.COLUMN_NAME_IMG_SRC, // 4
                RssContract.Entry.COLUMN_NAME_LINK, // 5
                RssContract.Entry.COLUMN_NAME_PUBLISHED, // 6
                RssContract.Entry.COLUMN_NAME_IS_READ // 7
        };

        Cursor c = contentResolver.query(newUri, projection, null, null, null);
        assertThat(c.getCount()).isEqualTo(1);
        c.moveToFirst();
        assertThat(c.getString(0)).isEqualTo(guid);
        assertThat(c.getInt(1)).isEqualTo(rssProvider);
        assertThat(c.getString(2)).isEqualTo(title);
        assertThat(c.getString(3)).isEqualTo(description);
        assertThat(c.getString(4)).isEqualTo(img);
        assertThat(c.getString(5)).isEqualTo(link);
        assertThat(c.getLong(6)).isEqualTo(pubDate);
        assertThat(c.getInt(7)).isEqualTo(isRead);

        int deleted = contentResolver.delete(newUri, null, null);
        assertThat(deleted).isEqualTo(1);

    }

}
