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
                    "error", "Indexing is already running"
            ));
        }

        indexingInProgress.set(true);
        Map<String, Object> response = new HashMap<>();

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
}