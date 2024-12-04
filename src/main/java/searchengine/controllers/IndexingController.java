package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import searchengine.services.IndexingService;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("api/indexing")
public class IndexingController {

    @Autowired
    private IndexingService indexingService;

    private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        if (indexingInProgress.get()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Индексация уже запущена"
            ));
        }

        indexingInProgress.set(true);

        ForkJoinPool.commonPool().submit(() -> {
            try {
                indexingService.indexAllSites();
            } catch (Exception e) {

            } finally {
                indexingInProgress.set(false);
            }
        });

        return ResponseEntity.ok(Map.of("result", true));
    }
}
