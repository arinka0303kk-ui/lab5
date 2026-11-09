package kz.iitu.springlab.catalog;

import jakarta.persistence.EntityManager;
import kz.iitu.springlab.web.Genre;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session.events.log=false"
})
@ActiveProfiles("dev")
@Transactional
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
class Lab08Tests {
    @Autowired BookRepository repository;
    @Autowired BookService service;
    @Autowired EntityManager em;

    private Statistics statistics() {
        return em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    }

    private void reset() { em.flush(); em.clear(); statistics().clear(); }

    @Test
    void associationsAndEntityFields() {
        assertEquals(8, repository.count());
        assertEquals(3L, em.createQuery("select count(a) from Author a", Long.class).getSingleResult());
        assertEquals(4L, em.createQuery("select count(t) from Tag t", Long.class).getSingleResult());
        Author author = em.createQuery("select a from Author a where a.name = 'Isaac Asimov'", Author.class)
                .getSingleResult();
        assertEquals(3, author.getBooks().size());
        Book book = author.getBooks().stream().filter(b -> b.getTitle().equals("I, Robot")).findFirst().orElseThrow();
        assertSame(author, book.getAuthor());
        assertEquals(2, book.getTags().size());
        assertNotNull(book.getCreatedAt());
        long version = book.getVersion();
        book.setTitle("I, Robot (updated)");
        em.flush();
        assertTrue(book.getVersion() > version);
        assertEquals("SCIENCE", em.createNativeQuery("select genre from book where id = :id", String.class)
                .setParameter("id", book.getId()).getSingleResult());
        Book added = new Book("Helper", Genre.SCIENCE, 2020);
        author.addBook(added);
        assertSame(author, added.getAuthor());
        assertTrue(author.getBooks().contains(added));
    }

    @Test
    void derivedAndJpqlQueries() {
        reset();
        assertEquals(3, repository.findByGenre(Genre.SCIENCE).size());
        System.out.println("LAB08 findByGenre rows=3 sql=" + statistics().getPrepareStatementCount());
        reset();
        assertEquals(3, repository.findByTitleContainingIgnoreCase("HARRY").size());
        System.out.println("LAB08 findByTitleContainingIgnoreCase rows=3 sql=" + statistics().getPrepareStatementCount());
        reset();
        var years = repository.findByYearBetweenOrderByYearDesc(1950, 1954);
        assertEquals(3, years.size());
        assertEquals(1954, years.getFirst().getYear());
        System.out.println("LAB08 findByYearBetweenOrderByYearDesc rows=3 sql=" + statistics().getPrepareStatementCount());
        reset();
        var top = repository.findTop3ByGenreOrderByYearDesc(Genre.SCIENCE);
        assertEquals(3, top.size());
        assertEquals(1954, top.getFirst().getYear());
        System.out.println("LAB08 findTop3ByGenreOrderByYearDesc rows=3 sql=" + statistics().getPrepareStatementCount());
        reset();
        assertEquals(3, repository.countByAuthorName("Isaac Asimov"));
        System.out.println("LAB08 countByAuthorName value=3 sql=" + statistics().getPrepareStatementCount());
        reset();
        var found = repository.search(Genre.SCIENCE, 1951);
        assertEquals(2, found.size());
        assertEquals("Foundation", found.getFirst().getTitle());
        System.out.println("LAB08 search rows=2 sql=" + statistics().getPrepareStatementCount());
    }

    @Test
    void bulkUpdateClearsPersistenceContext() {
        Book before = repository.findByTitleContainingIgnoreCase("Foundation").getFirst();
        assertFalse(before.isArchived());
        statistics().clear();
        assertEquals(3, service.archiveOlderThan(1960));
        assertEquals(1, statistics().getPrepareStatementCount());
        System.out.println("LAB08 archiveOlderThan updated=3 sql=" + statistics().getPrepareStatementCount());
        assertFalse(em.contains(before));
        assertTrue(repository.findById(before.getId()).orElseThrow().isArchived());
    }

    @Test
    void nPlusOneAndNamedGraph() {
        reset();
        var plain = em.createQuery("select b from Book b", Book.class).getResultList();
        plain.forEach(b -> {
            System.out.println("LAB08 before author=" + b.getAuthor().getName());
            assertNotNull(b.getAuthor().getName());
        });
        assertEquals(4, statistics().getPrepareStatementCount());
        System.out.println("LAB08 N+1 before=" + statistics().getPrepareStatementCount());
        reset();
        var fetched = repository.findAllWithAuthor();
        fetched.forEach(b -> {
            System.out.println("LAB08 after author=" + b.getAuthor().getName());
            assertNotNull(b.getAuthor().getName());
        });
        assertEquals(8, fetched.size());
        assertEquals(1, statistics().getPrepareStatementCount());
        System.out.println("LAB08 N+1 after=" + statistics().getPrepareStatementCount());
        reset();
        repository.findByGenre(Genre.SCIENCE).forEach(b -> assertTrue(em.getEntityManagerFactory()
                .getPersistenceUnitUtil().isLoaded(b, "author")));
        assertEquals(1, statistics().getPrepareStatementCount());
        reset();
        repository.findByTitleContainingIgnoreCase("Harry").forEach(b -> assertTrue(em.getEntityManagerFactory()
                .getPersistenceUnitUtil().isLoaded(b, "author")));
        assertEquals(1, statistics().getPrepareStatementCount());
    }

    @Test
    void pageAndSlice() {
        reset();
        var request = PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "year"));
        var page = repository.findAll(request);
        assertEquals(8, page.getTotalElements());
        assertEquals(3, page.getContent().size());
        assertEquals(1998, page.getContent().getFirst().getYear());
        assertEquals(2, statistics().getPrepareStatementCount());
        System.out.println("LAB08 Page sql=" + statistics().getPrepareStatementCount());
        reset();
        var slice = repository.findAllBy(request);
        assertEquals(3, slice.getNumberOfElements());
        assertTrue(slice.hasNext());
        assertEquals(1, statistics().getPrepareStatementCount());
        System.out.println("LAB08 Slice sql=" + statistics().getPrepareStatementCount());
        assertEquals(3, repository.findByGenre(Genre.SCIENCE, PageRequest.of(0, 2)).getTotalElements());
        assertEquals(3, service.find(null, "isaac asimov", PageRequest.of(0, 2)).getTotalElements());
        assertEquals(3, service.find(Genre.SCIENCE, "Isaac Asimov", PageRequest.of(0, 2)).getTotalElements());
    }

    @Test
    void existingCrudWorksWithDatabaseAndDto() {
        var created = service.create("Test book", "Test author", 2020, Genre.HISTORY);
        assertNotNull(created.id());
        assertEquals("Test author", service.findById(created.id()).author());
        var updated = service.update(created.id(), "Updated book", "Different author", 2021, Genre.FICTION);
        assertEquals("Different author", updated.author());
        assertEquals("Updated book", service.findById(created.id()).title());
        assertTrue(service.delete(created.id()));
        em.flush();
        assertThrows(BookNotFoundException.class, () -> service.findById(created.id()));
    }
}

