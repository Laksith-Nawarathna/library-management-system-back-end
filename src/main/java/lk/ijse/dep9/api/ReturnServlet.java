package lk.ijse.dep9.api;

import jakarta.annotation.Resource;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import lk.ijse.dep9.api.util.HttpServlet2;
import lk.ijse.dep9.dto.ReturnDTO;
import lk.ijse.dep9.dto.ReturnItemDTO;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@WebServlet(name = "ReturnServlet", value = "/returns/*")
public class ReturnServlet extends HttpServlet2 {

    @Resource(lookup = "java:comp/env/jdbc/lms")
    private DataSource pool;
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

    private void addReturnItems(ReturnDTO returnDTO, HttpServletResponse response) throws IOException {
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

        /*  Business Validation  */
        try(Connection connection = pool.getConnection()) {
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM issue_item\n" +
                    "INNER JOIN issue_note `in` on issue_item.issue_id = `in`.id\n" +
                    "WHERE member_id = ? AND issue_id = ? AND isbn = ?");
            stm.setString(1, returnDTO.getMemberId());

            PreparedStatement stm2 = connection.prepareStatement("SELECT * FROM `return` WHERE isbn = ? AND issue_id = ?");

            PreparedStatement stm3 = connection.prepareStatement("INSERT INTO `return` (date, issue_id, isbn) VALUES (?, ?, ?)");

            try {
                connection.setAutoCommit(false);
                for (ReturnItemDTO returnItem : returnItems) {
                    stm.setInt(2, returnItem.getIssueNoteId());
                    stm.setString(3, returnItem.getIsbn());

                    stm2.setInt(1, returnItem.getIssueNoteId());
                    stm2.setString(2, returnItem.getIsbn());

                    if (!stm.executeQuery().next()){
                        throw new JsonbException(
                                String.format("Either one of these %s, $s, %s doesn't exist or this return item is not belonged to this member",
                                        returnDTO.getMemberId(), returnItem.getIssueNoteId(), returnItem.getIsbn()));
                    }

                    if (stm2.executeQuery().next()){
                        throw new JsonbException("This " + returnItem.getIsbn() + " have been already returned");
                    }

                    stm3.setDate(1, Date.valueOf(LocalDate.now()));
                    stm3.setInt(2, returnItem.getIssueNoteId());
                    stm3.setString(3, returnItem.getIsbn());

                    if (stm3.executeUpdate() != 1){
                        throw new SQLException("Failed to insert a return item");
                    }
                }
                connection.commit();
                response.setStatus(HttpServletResponse.SC_CREATED);
            }catch (Throwable t){
                connection.rollback();
                if (t instanceof JsonbException) throw t;
                t.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to return item");
            }finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to return items");
        }

    }
}
