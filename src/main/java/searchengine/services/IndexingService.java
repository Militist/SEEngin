package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Service
public class IndexingService {

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    public void indexAllSites() {
        List<SiteEntity> sites = siteRepository.findAll();
        for (SiteEntity site : sites) {
            indexSite(site);
        }
    }

    private void indexSite(SiteEntity site) {
        Queue<String> toVisit = new LinkedList<>();
        toVisit.add(site.getUrl());

        try {
            while (!toVisit.isEmpty()) {
                String currentUrl = toVisit.poll();
                try {
                    Document doc = Jsoup.connect(currentUrl).get();
                    String content = doc.outerHtml();
                    Page page = new Page();
                    page.setId(site.getId());
                    page.setPath(currentUrl.substring(site.getUrl().length()));
                    page.setCode(200);
                    page.setContent(content);
                    pageRepository.save(page);

                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);

                    for (Element link : doc.select("a[href]")) {
                        String absHref = link.attr("abs:href");
                        toVisit.add(absHref);
                    }
                } catch (IOException e) {
                    // Если произошла ошибка, устанавливаем статус FAILED и записываем ошибку
                    site.setType(Status.FAILED);
                    site.setLastError("Ошибка при обработке URL " + currentUrl + ": " + e.getMessage());
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                    return;
                }
            }

            // Обновляем статус сайта на INDEXED по завершении индексации
            site.setType(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now()); // обновляем время статуса, если требуется
            siteRepository.save(site); // Сохраняем изменение статуса в базе данных

        } catch (Exception e) {

            site.setType(Status.FAILED);
            site.setLastError("Общая ошибка: " + e.getMessage());
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }

    }

    private void handleFailure(SiteEntity site, String errorMessage) {

        site.setType(Status.FAILED);
        site.setLastError(errorMessage);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    private void deleteSiteData(int siteId) {
        pageRepository.deleteBySiteId(siteId);
        siteRepository.deleteById(siteId);
    }

}