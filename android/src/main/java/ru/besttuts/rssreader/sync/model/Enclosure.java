package ru.besttuts.rssreader.sync.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Created by roman on 28.03.2015.
 */
@Root(name = "enclosure")
public class Enclosure {
    @Attribute
    public String url;

    @Attribute
    public String type;

    @Attribute(required = false)
    public String length;
}
