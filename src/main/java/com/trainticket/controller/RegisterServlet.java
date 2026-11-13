package com.trainticket.controller;

import com.trainticket.dao.UserDAO;
import com.trainticket.model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String fullName = request.getParameter("fullName");
        if (password == null || password.length() < 12 || password.length() > 128) {
            request.setAttribute("error", "Choose a password between 12 and 128 characters.");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,100}")
                || email == null || email.length() > 255 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || fullName == null || fullName.isBlank() || fullName.length() > 200) {
            request.setAttribute("error", "Enter a valid username, email and full name. Usernames need 3-100 letters, digits, dots, underscores or hyphens.");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setFullName(fullName);
        
        UserDAO userDAO = new UserDAO();
        boolean success = userDAO.registerUser(user);
        
        if (success) {
            request.setAttribute("success", "Registration successful. Please login.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        } else {
            request.setAttribute("error", "Registration failed. Username or email may already exist.");
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("register.jsp");
    }
}
