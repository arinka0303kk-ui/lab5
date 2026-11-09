package kz.iitu.springlab.catalog;

import jakarta.persistence.*;

@Entity
@Table(name = "tag")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;

    protected Tag() { }
    public Tag(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
}
