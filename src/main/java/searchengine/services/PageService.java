package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.PageEntity;
import searchengine.repositories.PageRepository;

import java.util.List;

@Service
public class PageService {

    private final PageRepository pageRepository;

    @Autowired
    public PageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    public List<PageEntity> getAllPages() {
        return pageRepository.findAll();
    }
}
