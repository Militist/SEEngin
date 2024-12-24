package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Page;
import searchengine.model.PageEntity;
import searchengine.repositories.PageRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PageService {

    @Autowired
    private PageRepository pageRepository;

    public boolean doesPageExist(String url) {
        return pageRepository.findByUrl(url).isPresent();
    }

    public List<String> getListPages(String url) {
        SiteCrawler siteCrawler  = new SiteCrawler(3);
        siteCrawler.getPageLinks(url);
        List<Page> result = CrawTask.getAllPagesList();

        Map<String, List<Page>> groupedPages = result
                .stream()
                .collect(Collectors.groupingBy(Page :: getPath));

        return groupedPages.values()
                .stream()
                .filter(pages -> pages.size() > 1)
                .flatMap(List :: stream)
                .map(Page::toString)
                .distinct()
                .collect(Collectors.toList());
    }

}
