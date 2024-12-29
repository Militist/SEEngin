package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.Page;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class CrawTask extends RecursiveAction {

    private final String url;
    private final int depth;
    private final int maxDepth;
    private final Set<String> links;
    private final Set<String> visitedLinks;

    private SiteEntity siteEntity;

    private PageRepository pageRepository;

    public CrawTask(String url, int depth, int maxDepth, Set<String> links, Set<String> visitedLinks, PageRepository pageRepository) {
        this.url = url;
        this.depth = depth;
        this.maxDepth = maxDepth;
        this.links = links;
        this.visitedLinks = visitedLinks;
        this.siteEntity = new SiteEntity();
        this.pageRepository = pageRepository;
    }

    @Override
    protected void compute() {
        if (depth > maxDepth || visitedLinks.contains(url)) {
            return;
        }
        synchronized (visitedLinks) {
            if (visitedLinks.add(url)) {
                processUrl();
            } else {
                System.out.println("Ссылка '" + url + "' уже была посещена.");
            }
        }
    }

    private void processUrl() {
        Document document;

        try {
            document = Jsoup.connect(url)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36")
                    .referrer("http://www.google.com")
                    .get();

            handleResponse(document);

            Thread.sleep(2000); // Ожидаем между запросами

        } catch (IOException e) {
            System.out.println("Access error '" + url + "': " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
        }
    }

    private void handleResponse(Document document) {
        int statusCode = document.connection().response().statusCode();
        String contentType = document.connection().response().contentType();

        if (statusCode != 200 || contentType == null || contentType.contains("text/html")) {
            return;
        }

        Elements linkOnPage = document.select("a[href]");
        for (Element element : linkOnPage) {
            String absUrl = element.attr("abs:href");
            if (!absUrl.isEmpty() && visitedLinks.add(absUrl)) {
                links.add(absUrl);
                processPage(absUrl, statusCode, element);
                invokeAll(createSubTasks());
            }
        }
    }

    private List<CrawTask> createSubTasks() {
        List<CrawTask> subTasks = new ArrayList<>();
        for (String urlLink : links) {
            if (!visitedLinks.contains(urlLink)) {
                subTasks.add(new CrawTask(urlLink, depth + 1, maxDepth, links, visitedLinks, pageRepository));
            }
        }
        return subTasks;
    }

    private void processPage(String absUrl, int statusCode, Element element) {
        Page page = new Page();
        page.setPath(absUrl);
        page.setCode(statusCode);
        page.setContent(element.text());

        System.out.println("Сохраняем страницу: " + page.getPath());

        saveToDatabasePageEntity(page, siteEntity);
    }

    private void saveToDatabasePageEntity(Page page, SiteEntity siteEntity) {

        System.out.println("Вызывается сохранение для страницы: " + page.getPath());

        validatePageEntity(page, siteEntity);
        System.out.println("Сохраняем страницу: " + page.getPath());

        Optional<PageEntity> existingPageEntity = pageRepository.findByUrl(page.getPath());

        if (existingPageEntity.isPresent()) {

            updateExistingPageEntity(existingPageEntity.get(), page);
            System.out.println("Обновлена существующая страница: " + page.getPath());

        } else {
            System.out.println("Сохраняем новую страницу: " + page.getPath());
            createNewPageEntity(page, siteEntity);
        }
    }

    private void validatePageEntity(Page page, SiteEntity siteEntity) {
        if (page == null || siteEntity == null || page.getPath() == null || page.getPath().isEmpty()) {
            throw new IllegalArgumentException("Page, SiteEntity или путь Page не должны быть null или пустыми");
        }
    }

    private void updateExistingPageEntity(PageEntity existingPageEntity, Page page) {
        existingPageEntity.setContent(page.getContent());
        existingPageEntity.setCode(page.getCode());
        pageRepository.save(existingPageEntity);
    }

    private void createNewPageEntity(Page page, SiteEntity siteEntity) {
        PageEntity pageEntity = Converter.toPageEntity(page, siteEntity);
        pageRepository.save(pageEntity);
    }

}
