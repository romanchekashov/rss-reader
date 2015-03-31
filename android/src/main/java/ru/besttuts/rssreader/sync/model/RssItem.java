package ru.besttuts.rssreader.sync.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Created by roman on 28.03.2015.
 */
@Root(name = "item")
public class RssItem {
    @Element
    public String guid;

    @Element
    public String title;

    @Element
    public String link;

    @Element
    public String description;

    @Element
    public String pubDate;

    @Element(required = false)
    public Enclosure enclosure;
}
