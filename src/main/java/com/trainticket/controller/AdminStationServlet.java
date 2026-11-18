package com.trainticket.controller;

import com.trainticket.model.User;

import com.trainticket.model.Station;
import java.io.IOException;
import java.net.URLEncoder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/admin/station")
public class AdminStationServlet extends HttpServlet {
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
            

            boolean success = catalog.addStation(station);
            
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
                "/admin/station?error=Error+adding+station:+" + "Please+check+the+fields+and+try+again");
        }
    }
    
    private void handleEditDelete(HttpServletRequest request, HttpServletResponse response, String action) 
            throws IOException {
        try {

            String message = "";
            boolean success = false;
            
            if ("edit".equals(action)) {
                Station station = new Station();
                station.setStationId(Integer.parseInt(request.getParameter("stationId")));
                station.setStationCode(request.getParameter("stationCode"));
                station.setStationName(request.getParameter("stationName"));
                station.setCity(request.getParameter("city"));
                station.setState(request.getParameter("state"));
                
                success = catalog.updateStation(station);
                message = success ? "Station+updated+successfully" : "Failed+to+update+station";
                
            } else if ("delete".equals(action)) {
                int stationId = Integer.parseInt(request.getParameter("stationId"));
                success = catalog.deleteStation(stationId);
                message = success ? "Station+archived+successfully" : "Failed+to+delete+station";
            }
            
            response.sendRedirect(request.getContextPath() + 
                "/admin/station?" + (success ? "success" : "error") + "=" + message);
            
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + 
                "/admin/station?error=Operation+failed:+" + "Please+check+the+fields+and+try+again");
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
            

            request.setAttribute("stations", catalog.getAllStations());
            request.getRequestDispatcher("/admin/manageStations.jsp").forward(request, response);
        } catch (Exception e) {
            request.setAttribute("error", "Unable to load stations. Please try again.");
            request.getRequestDispatcher("/admin/manageStations.jsp").forward(request, response);
        }
    }
}