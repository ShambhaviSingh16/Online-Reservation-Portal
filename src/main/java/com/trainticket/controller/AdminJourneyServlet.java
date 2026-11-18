package com.trainticket.controller;

import com.trainticket.model.User;
import com.trainticket.dao.JourneyDAO;


import com.trainticket.model.Journey;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/admin/journey")
public class AdminJourneyServlet extends HttpServlet {
    private com.trainticket.service.CatalogService catalog;
    @Override public void init() throws ServletException {
        catalog = org.springframework.web.context.support.WebApplicationContextUtils
            .getRequiredWebApplicationContext(getServletContext())
            .getBean(com.trainticket.service.CatalogService.class);
    }

    
    private static final DateTimeFormatter DATETIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

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
            handleEditDelete(request, response, action);
            return;
        }
        
        // Handle add journey
        try {
            // Get parameters
            int trainId = Integer.parseInt(request.getParameter("trainId"));
            int departureStationId = Integer.parseInt(request.getParameter("departureStationId"));
            int arrivalStationId = Integer.parseInt(request.getParameter("arrivalStationId"));
            double fare = Double.parseDouble(request.getParameter("fare"));
            int availableSeats = Integer.parseInt(request.getParameter("availableSeats"));
            
            // Parse and validate datetime values
            LocalDateTime departureLdt = parseDateTime(request.getParameter("departureTime"));
            LocalDateTime arrivalLdt = parseDateTime(request.getParameter("arrivalTime"));
            
            if (arrivalLdt.isBefore(departureLdt)) {
                throw new IllegalArgumentException("Arrival time must be after departure time");
            }
            
            // Convert to Timestamp
            Timestamp departureTime = Timestamp.valueOf(departureLdt);
            Timestamp arrivalTime = Timestamp.valueOf(arrivalLdt);
            
            // Create and save journey
            Journey journey = new Journey();
            journey.setTrainId(trainId);
            journey.setDepartureStationId(departureStationId);
            journey.setArrivalStationId(arrivalStationId);
            journey.setDepartureTime(departureTime);
            journey.setArrivalTime(arrivalTime);
            journey.setFare(fare);
            journey.setAvailableSeats(availableSeats);
            
            JourneyDAO journeyDAO = new JourneyDAO();
            boolean success = journeyDAO.addJourney(journey);
            
            if (success) {
                response.sendRedirect(request.getContextPath() + "/admin/journey?success=Journey+added+successfully");
            } else {
                response.sendRedirect(request.getContextPath() + 
                    "/admin/journey?error=Journey+already+exists&trainId=" + trainId +
                    "&departureStationId=" + departureStationId +
                    "&arrivalStationId=" + arrivalStationId +
                    "&departureTime=" + URLEncoder.encode(request.getParameter("departureTime"), "UTF-8") +
                    "&arrivalTime=" + URLEncoder.encode(request.getParameter("arrivalTime"), "UTF-8") +
                    "&fare=" + fare + "&availableSeats=" + availableSeats);
            }
            
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + 
                "/admin/journey?error=Invalid+number+format:+" + "Please+check+the+fields+and+try+again");
        } catch (IllegalArgumentException e) {
            response.sendRedirect(request.getContextPath() + 
                "/admin/journey?error=" + "Please+check+the+fields+and+try+again");
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + 
                "/admin/journey?error=System+error:+" + "Please+check+the+fields+and+try+again");
        }
    }
    
    private void handleEditDelete(HttpServletRequest request, HttpServletResponse response, String action) 
            throws IOException {
        try {
            JourneyDAO journeyDAO = new JourneyDAO();
            String message = "";
            boolean success = false;
            
            if ("edit".equals(action)) {
                Journey journey = new Journey();
                journey.setJourneyId(Integer.parseInt(request.getParameter("journeyId")));
                journey.setTrainId(Integer.parseInt(request.getParameter("trainId")));
                journey.setDepartureStationId(Integer.parseInt(request.getParameter("departureStationId")));
                journey.setArrivalStationId(Integer.parseInt(request.getParameter("arrivalStationId")));
                journey.setDepartureTime(Timestamp.valueOf(LocalDateTime.parse(
                    request.getParameter("departureTime"), DATETIME_FORMATTER)));
                journey.setArrivalTime(Timestamp.valueOf(LocalDateTime.parse(
                    request.getParameter("arrivalTime"), DATETIME_FORMATTER)));
                journey.setFare(Double.parseDouble(request.getParameter("fare")));
                journey.setAvailableSeats(Integer.parseInt(request.getParameter("availableSeats")));
                
                success = journeyDAO.updateJourney(journey);
                message = success ? "Journey+updated+successfully" : "Failed+to+update+journey";
                
            } else if ("delete".equals(action)) {
                int journeyId = Integer.parseInt(request.getParameter("journeyId"));
                success = journeyDAO.deleteJourney(journeyId);
                message = success ? "Journey+deleted+successfully" : "Failed+to+delete+journey";
            }
            
            response.sendRedirect(request.getContextPath() + 
                "/admin/journey?" + (success ? "success" : "error") + "=" + message);
            
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + 
                "/admin/journey?error=Operation+failed:+" + "Please+check+the+fields+and+try+again");
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
                request.setAttribute("trainId", request.getParameter("trainId"));
                request.setAttribute("departureStationId", request.getParameter("departureStationId"));
                request.setAttribute("arrivalStationId", request.getParameter("arrivalStationId"));
                request.setAttribute("departureTime", request.getParameter("departureTime"));
                request.setAttribute("arrivalTime", request.getParameter("arrivalTime"));
                request.setAttribute("fare", request.getParameter("fare"));
                request.setAttribute("availableSeats", request.getParameter("availableSeats"));
            }
            
            JourneyDAO journeyDAO = new JourneyDAO();


            
            request.setAttribute("journeys", journeyDAO.getAllJourneys());
            request.setAttribute("trains", catalog.getAllTrains());
            request.setAttribute("stations", catalog.getAllStations());
            
            request.getRequestDispatcher("/admin/manageJourneys.jsp").forward(request, response);
        } catch (Exception e) {
            request.setAttribute("error", "Error loading data: " + e.getMessage());
            request.getRequestDispatcher("/admin/manageJourneys.jsp").forward(request, response);
        }
    }
    
    private LocalDateTime parseDateTime(String datetimeStr) throws IllegalArgumentException {
        if (datetimeStr == null || datetimeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Datetime cannot be empty");
        }
        try {
            return LocalDateTime.parse(datetimeStr, DATETIME_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid datetime format. Use YYYY-MM-DDTHH:MM");
        }
    }
}