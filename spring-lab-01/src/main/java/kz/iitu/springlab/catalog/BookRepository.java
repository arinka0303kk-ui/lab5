package kz.iitu.springlab.catalog;

import kz.iitu.springlab.web.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    @EntityGraph(value = "Book.author")
    List<Book> findByGenre(Genre genre);

    @EntityGraph(value = "Book.author")
    List<Book> findByTitleContainingIgnoreCase(String fragment);

    List<Book> findByYearBetweenOrderByYearDesc(int from, int to);
    List<Book> findTop3ByGenreOrderByYearDesc(Genre genre);
    long countByAuthorName(String name);

    @Query("""
            select b from Book b
            where b.genre = :genre and b.year >= :year
            order by b.title
            """)
    List<Book> search(@Param("genre") Genre genre, @Param("year") int year);

    @Modifying(clearAutomatically = true)
    @Query("update Book b set b.archived = true where b.year < :year")
    int archiveOlderThan(@Param("year") int year);

    @Query("select b from Book b join fetch b.author")
    List<Book> findAllWithAuthor();

    @Override
    @EntityGraph(value = "Book.author")
    Page<Book> findAll(Pageable pageable);

    @EntityGraph(value = "Book.author")
    Page<Book> findByGenre(Genre genre, Pageable pageable);

    @EntityGraph(value = "Book.author")
    @Query("select b from Book b where lower(b.author.name) = lower(:author)")
    Page<Book> findPageByAuthor(@Param("author") String author, Pageable pageable);

    @EntityGraph(value = "Book.author")
    @Query("select b from Book b where b.genre = :genre and lower(b.author.name) = lower(:author)")
    Page<Book> findPageByGenreAndAuthor(@Param("genre") Genre genre, @Param("author") String author, Pageable pageable);

    @EntityGraph(value = "Book.author")
    Slice<Book> findAllBy(Pageable pageable);
}
