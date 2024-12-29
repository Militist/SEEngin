package searchengine.services;

import searchengine.config.Page;
import searchengine.config.Site;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

public class Converter {

    public static SiteEntity toSiteEntity(Site site) {
        if (site == null) {
            throw new IllegalArgumentException("Site cannot be null");
        }

        String url = site.getUrl();
        if (url == null || !isValidUrl(url) || site.getName() == null || site.getName().isEmpty()) {
            throw new IllegalArgumentException("Malformed site data: " + (url != null ? url : "null URL") + ", name: " + site.getName());
        }

        SiteEntity siteEntity = new SiteEntity();
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
        return url.startsWith("http") || url.startsWith("https");
    }

    public static PageEntity toPageEntity(Page page, SiteEntity siteEntity) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setSiteEntity(siteEntity);
        pageEntity.setPath(page.getPath());
        pageEntity.setCode(page.getCode());
        pageEntity.setContent(page.getContent());
        return pageEntity;
    }

}
