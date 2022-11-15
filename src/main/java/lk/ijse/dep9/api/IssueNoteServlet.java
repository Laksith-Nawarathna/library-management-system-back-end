package lk.ijse.dep9.api;

import jakarta.annotation.Resource;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import lk.ijse.dep9.dto.IssueNoteDTO;

import javax.sql.DataSource;
import java.io.IOException;

@WebServlet(name = "IssueNoteServlet", value = "/issue-notes")
public class IssueNoteServlet extends HttpServlet {

    @Resource(lookup = "java:comp/env/jdbc/lms")
    private DataSource pool;
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if(request.getPathInfo() != null && !request.getPathInfo().equals("/")){
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }

        try{
            if(request.getContentType() == null || !request.getContentType().startsWith("application/json")){
                throw new JsonbException("Invalid JSON");
            }

            IssueNoteDTO issueNote = JsonbBuilder.create().fromJson(request.getReader(), IssueNoteDTO.class);
            createIssueNote(issueNote, response);
        }catch (JsonbException e){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON");
        }
    }

    private void createIssueNote(IssueNoteDTO issueNote, HttpServletResponse response){
        if(issueNote.getMemberId() == null || !issueNote.getMemberId().matches("^([A-Fa-f0-9]{8}(-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12})$")){
            throw new JsonbException("Member id is empty or invalid");
        } else if (issueNote.getBooks().isEmpty()) {
            throw new JsonbException("Cannot place an issue note without books");  // handle erd total participation relation between issueNote
        } else if (issueNote.getBooks().stream().anyMatch(isbn -> isbn.matches("([0-9][0-9\\\\-]*[0-9])"))) {
            throw new JsonbException("Invalid ISBN");
        }
    }
}
