package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    @Query("select l from Lemma l where l.site = ?1 and l.lemma = ?2")
    Optional<Lemma> findBySiteAndLemma(Site site, String lemma);

    @Query("select l from Lemma l where l.lemma in ?1 and upper(l.site.url) = upper(?2) order by l.frequency")
    List<Lemma> findByLemmaInAndSite_UrlIgnoreCaseOrderByFrequencyAsc(Collection<String> lemmata, String url);

    @Query("select l from Lemma l where l.lemma in ?1 order by l.frequency ASC")
    List<Lemma> findByLemmaInOrderByFrequencyAsc(Collection<String> lemmata);

    @Query("select count(l) from Lemma l where l.site = ?1")
    long countBySite(Site site);


    void deleteBySite(Site site);
}