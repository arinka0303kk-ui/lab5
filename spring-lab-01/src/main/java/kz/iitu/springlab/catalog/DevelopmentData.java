package kz.iitu.springlab.catalog;

import jakarta.persistence.EntityManager;
import kz.iitu.springlab.web.Genre;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevelopmentData implements CommandLineRunner {
    private final EntityManager entityManager;
    private final BookRepository repository;
    public DevelopmentData(EntityManager entityManager, BookRepository repository) {
        this.entityManager = entityManager;
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (repository.count() != 0) return;
        Tag classic = new Tag("classic");
        Tag fiction = new Tag("fiction");
        Tag bestseller = new Tag("bestseller");
        Tag education = new Tag("education");
        for (Tag tag : new Tag[]{classic, fiction, bestseller, education}) entityManager.persist(tag);
        Author asimov = new Author("Isaac Asimov", "USA");
        add(asimov, "I, Robot", Genre.SCIENCE, 1950, classic, fiction);
        add(asimov, "Foundation", Genre.SCIENCE, 1951, classic);
        add(asimov, "The Caves of Steel", Genre.SCIENCE, 1954, fiction);
        Author harari = new Author("Yuval Noah Harari", "Israel");
        add(harari, "Sapiens", Genre.HISTORY, 2011, bestseller, education);
        add(harari, "Homo Deus", Genre.HISTORY, 2015, education);
        Author rowling = new Author("J. K. Rowling", "United Kingdom");
        add(rowling, "Harry Potter and the Philosopher's Stone", Genre.FANTASY, 1997, bestseller, fiction);
        add(rowling, "Harry Potter and the Chamber of Secrets", Genre.FANTASY, 1998, fiction);
        add(rowling, "Harry Potter and the Prisoner of Azkaban", Genre.FANTASY, 1999, classic);
        for (Author author : new Author[]{asimov, harari, rowling}) entityManager.persist(author);
    }

    private void add(Author author, String title, Genre genre, int year, Tag... tags) {
        Book book = new Book(title, genre, year);
        for (Tag tag : tags) book.addTag(tag);
        author.addBook(book);
    }
}
