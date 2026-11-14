package com.trainticket.config;

import com.trainticket.model.User;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfiguration {
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // CsrfFilter validates the existing JSP tokens and JSON header.
            .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.FORWARD,DispatcherType.ERROR).permitAll()
                .requestMatchers("/admin/**","/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**","/api/bookings/**","/api/journeys/**").authenticated()
                .requestMatchers("/api/account/**").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req,res,e) -> {
                    if (req.getRequestURI().startsWith(req.getContextPath()+"/api/")) {
                        res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"error\":\"Authentication required\"}");
                    } else { res.sendRedirect(req.getContextPath()+"/login.jsp"); }
                })
                .accessDeniedHandler((req,res,e) -> res.sendError(403)))
            .addFilterBefore(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)
                        throws ServletException,IOException {
                    HttpSession session=req.getSession(false);
                    User user=session==null?null:(User)session.getAttribute("user");
                    if(user!=null) {
                        try {
                            if(new com.trainticket.dao.RecoveryDAO().version(user.getUserId())!=user.getCredentialVersion()) {
                                session.invalidate();user=null;
                            }
                        }catch(java.sql.SQLException e){res.sendError(503,"Account verification unavailable");return;}
                    }
                    if (user!=null) {
                        String role="admin".equals(user.getRole())?"ROLE_ADMIN":"ROLE_USER";
                        SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user.getUserId(),null,List.of(new SimpleGrantedAuthority(role))));
                    }
                    chain.doFilter(req,res);
                }
            },AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
