package com.trainticket.controller;

import com.trainticket.dao.JourneyDAO;

import java.io.IOException;
import java.sql.Date;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/user/searchJourney")
public class SearchJourneyServlet extends HttpServlet {
    private com.trainticket.service.CatalogService catalog;
    @Override public void init() throws ServletException {
        catalog = org.springframework.web.context.support.WebApplicationContextUtils
            .getRequiredWebApplicationContext(getServletContext())
            .getBean(com.trainticket.service.CatalogService.class);
    }

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

        
        request.setAttribute("journeys", journeyDAO.searchJourneys(departureStationId, arrivalStationId, travelDate));
        request.setAttribute("departureStation", catalog.getStationById(departureStationId));
        request.setAttribute("arrivalStation", catalog.getStationById(arrivalStationId));
        request.setAttribute("travelDate", travelDate);
        
        request.getRequestDispatcher("/user/journeyResults.jsp").forward(request, response);
    }
}