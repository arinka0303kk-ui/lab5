package kz.iitu.springlab.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.iitu.springlab.web.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService service;
    public BookController(BookService service) { this.service = service; }

    @GetMapping
    public Page<BookResponse> findAll(@RequestParam(required = false) Genre genre,
                                      @RequestParam(required = false) String author,
                                      @PageableDefault(size = 5, sort = "title") Pageable pageable) {
        return service.find(genre, author, pageable).map(BookResponse::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> findById(@PathVariable long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest book) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(book.title(), book.author(), book.year(), book.genre()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> update(@PathVariable long id, @Valid @RequestBody BookRequest book) {
        return ResponseEntity.ok(service.update(id, book.title(), book.author(), book.year(), book.genre()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    public record BookRequest(@NotBlank @Size(max = 120) String title,
                              @NotBlank String author, int year, Genre genre) { }
}
