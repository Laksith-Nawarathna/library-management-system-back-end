package lk.ijse.dep9.api;

import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import lk.ijse.dep9.dto.ReturnDTO;
import lk.ijse.dep9.dto.ReturnItemDTO;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@WebServlet(name = "ReturnServlet", value = "/returns/*")
public class ReturnServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if(request.getPathInfo() != null && !request.getPathInfo().equals("/")){
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }
       try{
           if(request.getContentType() == null || request.getContentType().startsWith("application/json")){
               response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON");
               return;
           }

           ReturnDTO returnDTO = JsonbBuilder.create().fromJson(request.getReader(), ReturnDTO.class);

           addReturnItems(returnDTO, response);
       }catch (JsonbException e){
           response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
       }
    }

    private void addReturnItems(ReturnDTO returnDTO, HttpServletResponse response){
        /* Data Validation */
        if (returnDTO.getMemberId() == null || !returnDTO.getMemberId().matches("^([A-Fa-f0-9]{8}(-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12})$")){
            throw new JsonbException("Member id is empty or invalid");
        } else if (returnDTO.getReturnItems().isEmpty()) {
            throw new JsonbException("No return items have been found");
        } else if (returnDTO.getReturnItems().stream().anyMatch(Objects::isNull)) {  // dto -> dto == null
            throw new JsonbException("Null items have been found in the list");
        } else if (returnDTO.getReturnItems().stream().anyMatch(item ->
            item.getIssueNoteId() == null || item.getIsbn() == null || !item.getIsbn().matches("([0-9][0-9\\\\-]*[0-9])"))) {
            throw new JsonbException("Some items are invalid");
        }
        Set<ReturnItemDTO> returnItems = returnDTO.getReturnItems().stream().collect(Collectors.toSet());



    }
}
