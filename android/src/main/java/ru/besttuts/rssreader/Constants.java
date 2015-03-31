package ru.besttuts.rssreader;

/**
 * Created by roman on 28.03.2015.
 */
public class Constants {

    public interface Config {
        // Is this an internal dogfood build?
        public static final boolean IS_DOGFOOD_BUILD = false;
    }

    public static final String DATE_FORMAT_PATTERN = "EEE, d MMM yyyy HH:mm:ss Z";

    public static final int RSS_ITEM_NOT_READ = 0;
    public static final int RSS_ITEM_READ = 1;

    public interface RssProvider {
        public static final String[] RSS_PROVIDER_ARRAY = new String[]{"lenta.ru", "gazeta.ru"};
        public static final int LENTA_RU = 0;
        public static final int GAZETA_RU = 1;
    }

}
