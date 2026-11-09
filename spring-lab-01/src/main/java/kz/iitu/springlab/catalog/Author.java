package kz.iitu.springlab.catalog;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "author")
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String country;
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    protected Author() { }
    public Author(String name, String country) {
        this.name = name;
        this.country = country;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public List<Book> getBooks() { return books; }
    public void addBook(Book book) {
        books.add(book);
        book.setAuthor(this);
    }
}
