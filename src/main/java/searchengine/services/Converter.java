package searchengine.services;

import searchengine.config.Site;
import searchengine.model.SiteEntity;

public class Converter {

    public static SiteEntity toSiteEntity(Site site)
    {

        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        return siteEntity;

    }

    public static Site toSite(SiteEntity siteEntity) {

        Site site = new Site();
        site.setUrl(siteEntity.getUrl());
        site.setName(siteEntity.getName());
        return site;

    }

}
