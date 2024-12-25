package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import searchengine.repositories.PageRepository;
import searchengine.services.IndexingService;
import searchengine.services.PageService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("api/indexing")
public class IndexingController {

    @Autowired
    private IndexingService indexingService;
    @Autowired
    private PageService pageService;
    @Autowired
    private PageRepository pageRepository;

    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);


    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        if (indexingInProgress.get()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Indexing is already running"
            ));
        }

        indexingInProgress.set(true);
        Map<String, Object> response = new ConcurrentHashMap<>();

        ForkJoinPool.commonPool().submit(() -> {
            try {
                indexingService.indexAllSites();
                response.put("result", true);
                response.put("message", "Indexing completed successfully");
                System.out.println("The method has been launched");
            } catch (Exception e) {
                response.put("result", false);
                response.put("error", "Error during indexing: " + e.getMessage());
                System.err.println("Error during indexing:: " + e.getMessage());
                e.printStackTrace();
            } finally {
                indexingInProgress.set(false);
            }
        });

        return ResponseEntity.ok(Map.of("result", true, "message", "Indexing is running"));

    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        if (!indexingInProgress.get()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Indexing is not running"
            ));
        }

        indexingService.shotDownNow();

        indexingInProgress.set(false);
        Map<String, Object> response = Map.of("result", true);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> addOrUpdatePage(@RequestParam String url) {
        if (!pageService.doesPageExist(url)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Данная страница находится за пределами сайтов, \n" +
                            "указанных в конфигурационном файле\n" + url
            ));
        }
        return ResponseEntity.ok(Map.of("result", true));

    }

    @GetMapping("/identityPages")
    public ResponseEntity<List<String>> identityPages(@RequestParam String url) {
        List<String> duplicatedPages = pageService.getListPages(url);

        if (duplicatedPages.isEmpty()) {
            return ResponseEntity.ok(List.of("No duplicate pages found."));
        }
        return ResponseEntity.ok(duplicatedPages);
    }
}