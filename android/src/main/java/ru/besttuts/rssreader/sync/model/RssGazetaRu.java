package ru.besttuts.rssreader.sync.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

import ru.besttuts.rssreader.sync.model.RssItem;

/**
 * Created by roman on 28.03.2015.
 */
@Root(name = "rss", strict = false)
public class RssGazetaRu {

    @Element
    public Channel channel;

    @Attribute
    public String version;

    @Root(name = "channel", strict = false)
    public static class Channel {
        @ElementList(name = "item", inline = true)
        public List<GazetaRuRssItem> items;

        @Element
        public String language;

        @Element
        public String title;

        @Element(name="link")
        public String link;

        @Element
        public String description;
    }

    public static class GazetaRuRssItem extends RssItem {

        @Element
        public String author;

    }

}
