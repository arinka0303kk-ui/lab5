package kz.iitu.springlab.catalog;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public List<Book> findAll(String author) {
        if (author == null || author.isBlank()) {
            return repository.findAll();
        }

        return repository.findAll()
                .stream()
                .filter(book -> book.author().equalsIgnoreCase(author.trim()))
                .toList();
    }

    public Book findById(long id) {
        return repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book create(Book book) {
        return repository.save(book);
    }

    public boolean delete(long id) {
        return repository.deleteById(id);
    }
}
