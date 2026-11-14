package com.trainticket.api;

import com.trainticket.dao.PasswordResetDAO;
import com.trainticket.service.EmailDeliveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.SQLException;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
public class PasswordResetApi {
    private final EmailDeliveryService mail;
    public PasswordResetApi(EmailDeliveryService mail){this.mail=mail;}
    public record Request(@NotBlank @Size(max=100) String username){}
    public record Complete(@NotBlank @Size(min=64,max=64) String token,@NotNull @Size(min=12,max=128) String newPassword){}
    @PostMapping("/api/auth/password-reset/request") public Map<String,String> request(@Valid @RequestBody Request input) throws SQLException {
        if(mail.configured()) {
            var reset=new PasswordResetDAO().request(input.username());
            if(reset!=null)try{mail.reset(reset.email(),reset.token());}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Password-reset email could not be delivered");}
        }
        return Map.of("message","If this account exists and email delivery is configured, a reset link will be sent. Check your inbox and spam folder. You can also use a saved recovery code.");
    }
    @PostMapping("/api/auth/password-reset/complete") public Map<String,String> complete(@Valid @RequestBody Complete input,HttpServletRequest request) throws SQLException {
        if(!new PasswordResetDAO().complete(input.token(),input.newPassword()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reset link is invalid or expired. Request another link.");
        if(request.getSession(false)!=null)request.getSession(false).invalidate();return Map.of("message","Password reset. Login with your new password.");
    }
    @GetMapping("/api/admin/email-status") public Map<String,Object> status(){return Map.of("configured",mail.configured(),"provider",mail.provider());}
}
