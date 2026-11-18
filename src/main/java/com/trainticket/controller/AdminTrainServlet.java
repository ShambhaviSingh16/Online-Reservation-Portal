package com.trainticket.controller;
import com.trainticket.model.User;

import com.trainticket.model.Train;
import java.io.IOException;
import java.net.URLEncoder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/admin/train")
public class AdminTrainServlet extends HttpServlet {
    private com.trainticket.service.CatalogService catalog;
    @Override public void init() throws ServletException {
        catalog = org.springframework.web.context.support.WebApplicationContextUtils
            .getRequiredWebApplicationContext(getServletContext())
            .getBean(com.trainticket.service.CatalogService.class);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
    	HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null || 
            !((User)session.getAttribute("user")).getRole().equals("admin")) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        
 String action = request.getParameter("action");
        
        // Handle edit/delete actions
        if ("edit".equals(action) || "delete".equals(action)) {
            handleEditDelete(request, response, action, session);
            return;
        }
        
        // Handle add train
        try {
            Train train = new Train();
            train.setTrainNumber(request.getParameter("trainNumber"));
            train.setTrainName(request.getParameter("trainName"));
            train.setTotalSeats(Integer.parseInt(request.getParameter("totalSeats")));
            

            boolean success = catalog.addTrain(train, ((User)session.getAttribute("user")).getUserId());
            
            if (success) {
                response.sendRedirect(request.getContextPath() + "/admin/train?success=Train+added+successfully");
            } else {
                response.sendRedirect(request.getContextPath() + 
                    "/admin/train?error=Train+number+already+exists&trainNumber=" + 
                    URLEncoder.encode(train.getTrainNumber(), "UTF-8") +
                    "&trainName=" + URLEncoder.encode(train.getTrainName(), "UTF-8") +
                    "&totalSeats=" + train.getTotalSeats());
            }
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + 
                "/admin/train?error=Error+adding+train:+" + "Please+check+the+fields+and+try+again");
        }
    }
    
    private void handleEditDelete(HttpServletRequest request, HttpServletResponse response, 
            String action, HttpSession session) throws IOException {
        try {

            String message = "";
            boolean success = false;
            
            if ("edit".equals(action)) {
                Train train = new Train();
                train.setTrainId(Integer.parseInt(request.getParameter("trainId")));
                train.setTrainNumber(request.getParameter("trainNumber"));
                train.setTrainName(request.getParameter("trainName"));
                train.setTotalSeats(Integer.parseInt(request.getParameter("totalSeats")));
                
                success = catalog.updateTrain(train);
                message = success ? "Train+updated+successfully" : "Failed+to+update+train";
                
            } else if ("delete".equals(action)) {
                int trainId = Integer.parseInt(request.getParameter("trainId"));
                success = catalog.deleteTrain(trainId);
                message = success ? "Train+archived+successfully" : "Failed+to+delete+train";
            }
            
            response.sendRedirect(request.getContextPath() + 
                "/admin/train?" + (success ? "success" : "error") + "=" + message);
            
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + 
                "/admin/train?error=Operation+failed:+" + "Please+check+the+fields+and+try+again");
        }
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null || 
            !((User)session.getAttribute("user")).getRole().equals("admin")) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        
        try {
            // Handle success/error messages from redirect
            if (request.getParameter("success") != null) {
                request.setAttribute("success", request.getParameter("success"));
            }
            if (request.getParameter("error") != null) {
                request.setAttribute("error", request.getParameter("error"));
                // Preserve form data for error display
                request.setAttribute("trainNumber", request.getParameter("trainNumber"));
                request.setAttribute("trainName", request.getParameter("trainName"));
                request.setAttribute("totalSeats", request.getParameter("totalSeats"));
            }
            

            request.setAttribute("trains", catalog.getAllTrains());
            request.getRequestDispatcher("/admin/manageTrains.jsp").forward(request, response);
        } catch (Exception e) {
            request.setAttribute("error", "Unable to load trains. Please try again.");
            request.getRequestDispatcher("/admin/manageTrains.jsp").forward(request, response);
        }
    }
}