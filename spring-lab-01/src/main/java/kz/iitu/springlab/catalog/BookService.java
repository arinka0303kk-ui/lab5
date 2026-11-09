package kz.iitu.springlab.catalog;

import jakarta.persistence.EntityManager;
import org.springframework.security.access.prepost.PreAuthorize;
import kz.iitu.springlab.web.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookService {
    private final BookRepository repository;
    private final EntityManager entityManager;

    public BookService(BookRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public List<BookResponse> findAll(String author) {
        return repository.findAllWithAuthor().stream()
                .filter(b -> author == null || author.isBlank()
                        || b.getAuthor().getName().equalsIgnoreCase(author.trim()))
                .map(BookResponse::from).toList();
    }

    public Page<Book> find(Genre genre, String author, Pageable pageable) {
        Page<Book> books;
        if (author != null && !author.isBlank()) {
            books = genre == null
                    ? repository.findPageByAuthor(author.trim(), pageable)
                    : repository.findPageByGenreAndAuthor(genre, author.trim(), pageable);
        } else {
            books = genre == null ? repository.findAll(pageable) : repository.findByGenre(genre, pageable);
        }
        return books;
    }

    public BookResponse findById(long id) { return BookResponse.from(requireBook(id)); }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public BookResponse create(String title, String author, int year, Genre genre) {
        Book book = new Book(title, genre == null ? Genre.SCIENCE : genre, year);
        findOrCreateAuthor(author).addBook(book);
        return BookResponse.from(repository.save(book));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public BookResponse update(long id, String title, String author, int year, Genre genre) {
        Book book = requireBook(id);
        book.setTitle(title);
        book.setYear(year);
        if (genre != null) book.setGenre(genre);
        entityManager.flush();
        if (book.getAuthor() == null || !book.getAuthor().getName().equalsIgnoreCase(author.trim())) {
            Author newAuthor = findOrCreateAuthor(author);
            // A bulk reassignment avoids orphanRemoval deleting a reparented book.
            entityManager.createQuery("update Book b set b.author = :author, b.version = b.version + 1 where b.id = :id")
                    .setParameter("author", newAuthor).setParameter("id", id).executeUpdate();
            entityManager.clear();
            book = requireBook(id);
        }
        return BookResponse.from(book);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public boolean delete(long id) {
        return repository.findById(id).map(book -> {
            repository.delete(book);
            return true;
        }).orElse(false);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public int archiveOlderThan(int year) { return repository.archiveOlderThan(year); }

    private Book requireBook(long id) {
        return repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    private Author findOrCreateAuthor(String name) {
        return entityManager.createQuery("select a from Author a where lower(a.name) = lower(:name)", Author.class)
                .setParameter("name", name.trim()).getResultStream().findFirst().orElseGet(() -> {
                    Author author = new Author(name.trim(), null);
                    entityManager.persist(author);
                    return author;
                });
    }
}
