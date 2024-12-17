package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;

public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    @Modifying
    @Transactional
    @Query("DELETE FROM PageEntity p WHERE p.siteEntity.id = :siteId")
    void deleteBySiteId(@Param("siteId")int siteId);
}
