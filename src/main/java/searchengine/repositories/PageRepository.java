package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    @Query("delete from Page p where p.id in ?1")
    void deleteByIdIn(Collection<Integer> ids);

    @Query("select p from Page p where p.site = ?1 and upper(p.path) = upper(?2)")
    List<Page> findBySiteAndPathIgnoreCase(Site site, String path);

    @Query("select p from Page p where p.site = ?1")
    List<Page> findBySite(Site site);

    @Query("select count(p) from Page p")
    long countFirstBy();

    @Query("select count(p) from Page p where p.site = ?1")
    long countBySite(Site site);

    @Query("select p from Page p where p.site = ?1 and p.path in ?2 order by p.path")
    List<Page> findBySiteAndPathInOrderByPathAsc(Site site, Collection<String> paths);

    @Transactional
    @Modifying
    @Query("update Page p set p.code = ?1, p.content = ?2 where p.id = ?3")
    void updateCodeAndContentById(Integer code, String content, Integer id);

}