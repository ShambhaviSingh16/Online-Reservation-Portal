package com.trainticket.controller;

import com.trainticket.dao.UserDAO;
import com.trainticket.model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        
        UserDAO userDAO = new UserDAO();
        User user = userDAO.authenticate(username, password);
        
        if (user != null) {
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) { oldSession.invalidate(); }
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            
            if (user.getRole().equals("admin")) {
                response.sendRedirect("admin/adminDashboard.jsp");
            } else {
                response.sendRedirect("user/userDashboard.jsp");
            }
        } else {
            request.setAttribute("error", "Invalid username or password");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("login.jsp");
    }
}
