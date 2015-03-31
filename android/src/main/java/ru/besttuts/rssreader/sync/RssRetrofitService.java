package ru.besttuts.rssreader.sync;

import retrofit.http.GET;
import retrofit.http.Headers;
import ru.besttuts.rssreader.sync.model.RssGazetaRu;
import ru.besttuts.rssreader.sync.model.RssLentaRu;

/**
 * Created by roman on 28.03.2015.
 */
public interface RssRetrofitService {

    @GET("/rss")
    RssLentaRu getLentaRuRss();

    /**
     * Добавил User-Agent от десктопного браузера, иначе происходит редирект на мобильную версию сайта
     * http://m.gazeta.ru/export/rss/lenta.xml - этой страницы не существует!
     *
     * @return RssGazetaRu
     */
    @Headers("User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.101 Safari/537.36")
    @GET("/export/rss/lenta.xml")
    RssGazetaRu getGazetaRuRss();

}
