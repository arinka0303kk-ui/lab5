package kz.iitu.springlab.security;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfiguration {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(10);}
 // Variant 9: command-line clients supply Authorization on EVERY request.
 // No cookie session is accepted; use HTTPS outside this localhost exercise.
 @Bean @Order(1)
 SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
  http.securityMatcher("/api/**")
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .securityContext(c->c.securityContextRepository(new NullSecurityContextRepository()))
   .requestCache(c->c.disable()).csrf(c->c.disable())
   .authorizeHttpRequests(a->a
    .requestMatchers(HttpMethod.GET,"/api/books","/api/books/**").hasAnyRole("USER","ADMIN")
    .requestMatchers("/api/books","/api/books/**").hasRole("ADMIN")
    .anyRequest().denyAll())
   .httpBasic(Customizer.withDefaults());
  return http.build();
 }
 @Bean @Order(2)
 SecurityFilterChain webChain(HttpSecurity http) throws Exception {
  http.authorizeHttpRequests(a->a
   .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
   .requestMatchers("/","/index.html","/login","/css/**").permitAll()
   .requestMatchers("/books/new").hasRole("ADMIN")
   .requestMatchers(HttpMethod.GET,"/books","/books/**").hasAnyRole("USER","ADMIN")
   .requestMatchers("/books","/books/**","/admin","/admin/**").hasRole("ADMIN")
   .anyRequest().denyAll())
   .formLogin(f->f.defaultSuccessUrl("/books",true).permitAll())
   .logout(l->l.logoutSuccessUrl("/?logout"));
  return http.build();
 }
}

