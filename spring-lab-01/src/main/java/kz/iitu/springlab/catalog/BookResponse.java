package kz.iitu.springlab.catalog;

import kz.iitu.springlab.web.Genre;
import java.time.Instant;

public record BookResponse(Long id, String title, String author, int year,
                           Genre genre, boolean archived, Instant createdAt, long version) {
    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(),
                book.getAuthor() == null ? null : book.getAuthor().getName(),
                book.getYear(), book.getGenre(), book.isArchived(),
                book.getCreatedAt(), book.getVersion());
    }
}
