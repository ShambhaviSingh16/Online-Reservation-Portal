package com.trainticket.api;

import com.trainticket.dao.RecoveryDAO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.SQLException;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
public class RecoveryApi {
    public record Generate(@NotBlank @Size(max=1024) String password){}
    public record Reset(@NotBlank @Size(max=100) String username,@NotBlank @Size(max=64) String recoveryCode,@NotNull @Size(min=12,max=128) String newPassword){}
    @PostMapping("/api/account/recovery-codes")
    public Map<String,Object> generate(@Valid @RequestBody Generate input,HttpServletRequest req) throws SQLException {
        List<String> codes=new RecoveryDAO().generate(AuthApi.user(req.getSession(false)).getUserId(),input.password());
        if(codes.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Current password could not be verified");
        return Map.of("codes",codes,"message","Save these codes privately. They are displayed only once. Previous codes are invalid.");
    }
    @PostMapping("/api/auth/recover")
    public Map<String,String> recover(@Valid @RequestBody Reset input,HttpServletRequest req) throws SQLException {
        if(!new RecoveryDAO().reset(input.username(),input.recoveryCode(),input.newPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Recovery could not be verified. Check your username and saved code.");
        if(req.getSession(false)!=null)req.getSession(false).invalidate();
        return Map.of("message","Password reset. All prior sessions and recovery codes are invalid. Login with your new password.");
    }
}
