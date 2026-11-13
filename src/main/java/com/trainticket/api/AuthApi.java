package com.trainticket.api;

import com.trainticket.dao.UserDAO;
import com.trainticket.model.User;
import com.trainticket.filter.CsrfFilter;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth")
public class AuthApi {
    public record Login(@NotBlank @Size(max=100) String username,@NotBlank @Size(max=1024) String password) { }
    public record Registration(@NotBlank @Pattern(regexp="[A-Za-z0-9_.-]{3,100}") String username,
        @NotBlank @Size(min=12,max=128) String password,@NotBlank @Email @Size(max=255) String email,
        @NotBlank @Size(max=200) String fullName) { }
    public record Account(int userId,String username,String fullName,String role) { }
    static Account account(User user) { return new Account(user.getUserId(),user.getUsername(),user.getFullName(),user.getRole()); }
    public static User user(HttpSession session) {
        User user=session==null?null:(User)session.getAttribute("user");
        if(user==null) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required"); }
        return user;
    }
    @GetMapping("/csrf") public Map<String,String> csrf(HttpServletRequest request) {
        return Map.of("token",CsrfFilter.ensureToken(request.getSession()),"headerName","X-CSRF-Token");
    }
    @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Login body,HttpServletRequest request) {
        User user=new UserDAO().authenticate(body.username(),body.password());
        if(user==null) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials"); }
        HttpSession old=request.getSession(false); if(old!=null) { old.invalidate(); }
        HttpSession session=request.getSession(true); session.setAttribute("user",user);
        return Map.of("user",account(user),"csrfToken",CsrfFilter.ensureToken(session));
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> register(@Valid @RequestBody Registration body) {
        User user=new User(); user.setUsername(body.username()); user.setPassword(body.password());
        user.setEmail(body.email()); user.setFullName(body.fullName());
        if(!new UserDAO().registerUser(user)) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Registration unavailable or account already exists"); }
        return Map.of("message","Registration successful");
    }
    @GetMapping("/me") public Account me(HttpServletRequest request) { return account(user(request.getSession(false))); }
    @PostMapping("/logout") public Map<String,String> logout(HttpServletRequest request) {
        HttpSession session=request.getSession(false); if(session!=null) { session.invalidate(); }
        return Map.of("message","Logged out");
    }
}
