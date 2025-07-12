package com.trainticket.controller;

import com.trainticket.model.User;
import com.trainticket.dao.StationDAO;
import com.trainticket.model.Station;
import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/admin/station")
public class AdminStationServlet extends HttpServlet {
    
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
        
        // Handle add station
        try {
            Station station = new Station();
            station.setStationCode(request.getParameter("stationCode"));
            station.setStationName(request.getParameter("stationName"));
            station.setCity(request.getParameter("city"));
            station.setState(request.getParameter("state"));
            
            StationDAO stationDAO = new StationDAO();
            boolean success = stationDAO.addStation(station);
            
            if (success) {
                response.sendRedirect(request.getContextPath() + "/admin/station?success=Station+added+successfully");
            } else {
                response.sendRedirect(request.getContextPath() + 
                    "/admin/station?error=Station+code+already+exists&stationCode=" + 
                    URLEncoder.encode(station.getStationCode(), "UTF-8") +
                    "&stationName=" + URLEncoder.encode(station.getStationName(), "UTF-8") +
                    "&city=" + URLEncoder.encode(station.getCity(), "UTF-8") +
                    "&state=" + URLEncoder.encode(station.getState(), "UTF-8"));
            }
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + 
                "/admin/station?error=Error+adding+station:+" + URLEncoder.encode(e.getMessage(), "UTF-8"));
        }
    }
    
    private void handleEditDelete(HttpServletRequest request, HttpServletResponse response, String action) 
            throws IOException {
        try {
            StationDAO stationDAO = new StationDAO();
            String message = "";
            boolean success = false;
            
            if ("edit".equals(action)) {
                Station station = new Station();
                station.setStationId(Integer.parseInt(request.getParameter("stationId")));
                station.setStationCode(request.getParameter("stationCode"));
                station.setStationName(request.getParameter("stationName"));
                station.setCity(request.getParameter("city"));
                station.setState(request.getParameter("state"));
                
                success = stationDAO.updateStation(station);
                message = success ? "Station+updated+successfully" : "Failed+to+update+station";
                
            } else if ("delete".equals(action)) {
                int stationId = Integer.parseInt(request.getParameter("stationId"));
                success = stationDAO.deleteStation(stationId);
                message = success ? "Station+deleted+successfully" : "Failed+to+delete+station";
            }
            
            response.sendRedirect(request.getContextPath() + 
                "/admin/station?" + (success ? "success" : "error") + "=" + message);
            
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + 
                "/admin/station?error=Operation+failed:+" + URLEncoder.encode(e.getMessage(), "UTF-8"));
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
                request.setAttribute("stationCode", request.getParameter("stationCode"));
                request.setAttribute("stationName", request.getParameter("stationName"));
                request.setAttribute("city", request.getParameter("city"));
                request.setAttribute("state", request.getParameter("state"));
            }
            
            StationDAO stationDAO = new StationDAO();
            request.setAttribute("stations", stationDAO.getAllStations());
            request.getRequestDispatcher("/admin/manageStations.jsp").forward(request, response);
        } catch (Exception e) {
            request.setAttribute("error", "Error loading stations: " + e.getMessage());
            request.getRequestDispatcher("/admin/manageStations.jsp").forward(request, response);
        }
    }
}