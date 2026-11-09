package kz.iitu.springlab.security;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class AdminController {
 private final AppUserRepository users;
 public AdminController(AppUserRepository users){this.users=users;}
 @GetMapping("/admin") public String admin(Model model){
  model.addAttribute("users",users.findAll());return "admin";
 }
}
