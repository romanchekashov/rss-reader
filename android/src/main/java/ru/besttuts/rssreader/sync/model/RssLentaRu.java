package ru.besttuts.rssreader.sync.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.util.List;

/**
 * Created by roman on 28.03.2015.
 */
@Root(name = "rss", strict = false)
public class RssLentaRu {

    @Element
    public Channel channel;

    @Attribute
    public String version;

    @Root(name = "channel", strict = false)
    public static class Channel {
        @ElementList(name = "item", inline = true)
        public List<LentaRuRssItem> items;

        @Element
        public String language;

        @Element
        public String title;

        @Element(name="link")
        @Path("channel/link")
        public String link;

        @Element
        public String description;
    }

    public static class LentaRuRssItem extends RssItem {

        @Element
        public String category;

    }

}
