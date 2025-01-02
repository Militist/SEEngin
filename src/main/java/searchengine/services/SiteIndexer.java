package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class SiteIndexer {

    @Autowired
    private SitesList sitesList;
    private Site site;

    public SiteIndexer() {
        this.sitesList = new SitesList();
        this.site = new Site();
    }

    public void go() {
        List<Site> listSites = sitesList.getSites();
        ExecutorService executorService = Executors.newFixedThreadPool(listSites.size());
        List<Future<Site>> futures = new ArrayList<>();

        for (Site site : listSites) {
            if (site.getUrl() == null || site.getUrl().isEmpty()) {
                System.err.println("Ошибка: URL сайта не может быть null или пустым.");
                continue; // Пропускаем обработку для этого сайта
            }

            IndexingService_01 task = new IndexingService_01(site);
            Future<Site> future = executorService.submit(task);
            futures.add(future);
        }

        List<Site> sites = coll(futures);

        for (Site site : sites) {
            System.out.println(site);
        }

        executorService.shutdown();

    }

    private static List<Site> coll(List<Future<Site>> futures) {
        List<Site> sites = new ArrayList<>();
        for (Future<Site> future : futures) {
            try {
                Site site = future.get(); // Получаем результат выполнения задачи
                sites.add(site); // Добавляем результат в список
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace(); // Обработка исключений
            }
        }
        return sites; // Возвращаем накопленные результаты
    }

}

