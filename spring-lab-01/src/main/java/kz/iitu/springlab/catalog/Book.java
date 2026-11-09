package kz.iitu.springlab.catalog;

import jakarta.persistence.*;
import kz.iitu.springlab.web.Genre;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "book")
@NamedEntityGraph(name = "Book.author", attributeNodes = @NamedAttributeNode("author"))
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Genre genre;
    private int year;
    private boolean archived;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();
    @Version
    private long version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;
    @ManyToMany
    @JoinTable(name = "book_tag", joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    protected Book() { }

    public Book(String title, Genre genre, int year) {
        this.title = title;
        this.genre = genre;
        this.year = year;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Genre getGenre() { return genre; }
    public int getYear() { return year; }
    public boolean isArchived() { return archived; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    public Author getAuthor() { return author; }
    public Set<Tag> getTags() { return tags; }
    public void setTitle(String title) { this.title = title; }
    public void setGenre(Genre genre) { this.genre = genre; }
    public void setYear(int year) { this.year = year; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public void setAuthor(Author author) { this.author = author; }

    public void addTag(Tag tag) { tags.add(tag); }
}
