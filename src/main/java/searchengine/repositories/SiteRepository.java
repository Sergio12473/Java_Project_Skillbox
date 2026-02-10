package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexStatus;
import searchengine.model.Site;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {
    Optional<Site> findByUrlLikeIgnoreCase(String url);
    Optional<Site> findByUrlIgnoreCase(String url);


    @Query("update Site s set s.status = ?1, s.statusTime = ?2, s.lastError = ?3 where s.id = ?4")
    void updateStatusAndStatusTimeAndLastErrorById(IndexStatus status, LocalDateTime statusTime, String lastError, Integer id);

    @Override
    void deleteById(Long aLong);
}