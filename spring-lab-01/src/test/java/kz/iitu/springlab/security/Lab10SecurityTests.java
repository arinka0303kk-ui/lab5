package kz.iitu.springlab.security;
import kz.iitu.springlab.catalog.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:lab10tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR","spring.datasource.username=sa","spring.datasource.password="})
@ActiveProfiles("lab10")
class Lab10SecurityTests {
 @Autowired BookService books;
 @Autowired AppUserRepository users;
 @Autowired PasswordEncoder encoder;
 @Test void databaseUsersHaveRealBcryptHashesAndAuthorities(){
  var u=users.findByUsername("user").orElseThrow();
  assertEquals(60,u.getPasswordHash().length());
  assertTrue(encoder.matches("user123",u.getPasswordHash()));
  assertFalse(encoder.matches("wrong",u.getPasswordHash()));
  assertTrue(users.findByUsername("admin").orElseThrow().getRoles().contains("ADMIN"));
 }
 @Test @WithMockUser(roles="USER") void userCannotCallServiceDirectly(){
  assertThrows(AccessDeniedException.class,()->books.delete(Long.MAX_VALUE));
 }
 @Test @WithMockUser(roles="ADMIN") void adminCanCallServiceDirectly(){
  assertFalse(books.delete(Long.MAX_VALUE));
 }
 @Test void bcryptSaltChangesButBothHashesVerify(){
  String a=encoder.encode("user123"),b=encoder.encode("user123");
  assertNotEquals(a,b);assertTrue(encoder.matches("user123",a));assertTrue(encoder.matches("user123",b));
 }
}
