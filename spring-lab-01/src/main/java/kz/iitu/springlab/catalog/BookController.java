package kz.iitu.springlab.catalog;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public List<Book> findAll(
            @RequestParam(required = false) String author) {

        return service.findAll(author);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> findById(@PathVariable long id) {

        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<Book> create(@RequestBody Book book) {

        Book created = service.create(new Book(null, book.title(), book.author(), book.year()));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> update(
            @PathVariable long id,
            @RequestBody Book book) {

        service.findById(id);

        Book updated = service.create(
                new Book(id, book.title(), book.author(), book.year())
        );

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {

        if (service.delete(id)) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }
}
