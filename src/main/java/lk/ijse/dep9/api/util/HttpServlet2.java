package lk.ijse.dep9.api.util;

import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.ijse.dep9.dto.ResponseStatusDTO;
import lk.ijse.dep9.exception.ResponseStatusException;

import java.io.IOException;
import java.util.Date;

public class HttpServlet2 extends HttpServlet {

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       try{
           if (req.getMethod().equalsIgnoreCase("PATCH")){
               doPatch(req, resp);;
           }else{
               super.service(req, resp);
           }
       }catch (Throwable t){

           ResponseStatusException r = t instanceof ResponseStatusException ?
                   (ResponseStatusException) t : null;

           if (r == null || r.getStatus() >= 500){  // >500 server side errors
               t.printStackTrace();
           }
           ResponseStatusDTO statusDTO = new ResponseStatusDTO( r == null ? 500: r.getStatus(),
                   t.getMessage(),
                   req.getRequestURI(),
                   new Date().getTime());
           resp.setContentType("application/json");
           resp.setStatus(statusDTO.getStatus());
           JsonbBuilder.create().toJson(statusDTO, resp.getWriter());
       }
    }
}
