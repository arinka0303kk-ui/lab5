package kz.iitu.springlab.security;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
@Entity @Table(name="app_user")
public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=60) private String username;
 @Column(name="password_hash",nullable=false,length=100) private String passwordHash;
 @Column(nullable=false) private boolean enabled;
 @ElementCollection(fetch=FetchType.EAGER)
 @CollectionTable(name="user_role",joinColumns=@JoinColumn(name="user_id"))
 @Column(name="role",length=20,nullable=false) private Set<String> roles=new HashSet<>();
 protected AppUser() { }
 public Long getId(){return id;}
 public String getUsername(){return username;}
 public String getPasswordHash(){return passwordHash;}
 public boolean isEnabled(){return enabled;}
 public Set<String> getRoles(){return roles;}
}
