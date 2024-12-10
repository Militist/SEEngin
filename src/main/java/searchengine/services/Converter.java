package searchengine.services;

import searchengine.config.Site;
import searchengine.model.SiteEntity;

public class Converter {

    public static SiteEntity toSiteEntity(Site site) {
        if (site == null) {
            throw new IllegalArgumentException("Site cannot be null");
        }

        SiteEntity siteEntity = new SiteEntity();

        String url = site.getUrl();
        if (url == null || !isValidUrl(url)) {
            System.err.println("Malformed URL detected: " + url);
            throw new IllegalArgumentException("Malformed URL: " + url);
        }

        siteEntity.setUrl(url);
        siteEntity.setName(site.getName());

        return siteEntity;
    }

    public static Site toSite(SiteEntity siteEntity) {


        if (siteEntity == null) {
            throw new IllegalArgumentException("SiteEntity cannot be null");
        }
        Site site = new Site();
        site.setUrl(siteEntity.getUrl());
        site.setName(siteEntity.getName());
        return site;

    }

    private static boolean isValidUrl(String url) {
        // Простейшая проверка URL, можно использовать более сложную
        return url.startsWith("http") || url.startsWith("https");
    }

}
