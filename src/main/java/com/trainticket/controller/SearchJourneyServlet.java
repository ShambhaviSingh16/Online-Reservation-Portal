package com.trainticket.controller;

import com.trainticket.dao.JourneyDAO;
import com.trainticket.dao.StationDAO;
import java.io.IOException;
import java.sql.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/user/searchJourney")
public class SearchJourneyServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        
        int departureStationId = Integer.parseInt(request.getParameter("departureStation"));
        int arrivalStationId = Integer.parseInt(request.getParameter("arrivalStation"));
        Date travelDate = Date.valueOf(request.getParameter("travelDate"));
        
        JourneyDAO journeyDAO = new JourneyDAO();
        StationDAO stationDAO = new StationDAO();
        
        request.setAttribute("journeys", journeyDAO.searchJourneys(departureStationId, arrivalStationId, travelDate));
        request.setAttribute("departureStation", stationDAO.getStationById(departureStationId));
        request.setAttribute("arrivalStation", stationDAO.getStationById(arrivalStationId));
        request.setAttribute("travelDate", travelDate);
        
        request.getRequestDispatcher("/user/journeyResults.jsp").forward(request, response);
    }
}