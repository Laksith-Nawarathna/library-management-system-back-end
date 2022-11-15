package lk.ijse.dep9.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;

@WebServlet(name = "ReturnServlet", value = "/returns/*")
public class ReturnServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if(request.getPathInfo() != null && !request.getPathInfo().equals("/")){
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }
        if(request.getContentType() == null || request.getContentType().startsWith("application/json")){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON");
            return;
        }
    }
}
