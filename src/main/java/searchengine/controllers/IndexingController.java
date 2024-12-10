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
        Map<String, Object> response = new HashMap<>();

        ForkJoinPool.commonPool().submit(() -> {
            try {
                indexingService.indexAllSites();
                response.put("result", true);
                response.put("message", "Индексация завершена успешно");
                System.out.println("Метод был запущен");
            } catch (Exception e) {
                response.put("result", false);
                response.put("error", "Ошибка во время индексации: " + e.getMessage());
                System.err.println("Ошибка во время индексации: " + e.getMessage());
                e.printStackTrace();
            } finally {
                indexingInProgress.set(false);
            }
        });

        return ResponseEntity.ok(Map.of("result", true, "message", "Индексация запущена"));

    }
}