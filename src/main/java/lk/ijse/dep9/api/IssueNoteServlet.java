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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

//    Data validation
    private void createIssueNote(IssueNoteDTO issueNote, HttpServletResponse response) throws IOException {
        if(issueNote.getMemberId() == null || !issueNote.getMemberId().matches("^([A-Fa-f0-9]{8}(-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12})$")){
            throw new JsonbException("Member id is empty or invalid");
        } else if (issueNote.getBooks().isEmpty()) {
            throw new JsonbException("Cannot place an issue note without books");  // handle erd total participation relation between issueNote
        } else if (issueNote.getBooks().size() > 3) {
            throw new JsonbException("Cannot issue more than 3 books");
        } else if (issueNote.getBooks().stream().anyMatch(isbn -> isbn == null || isbn.matches("^(\\d[\\d\\\\-]*\\d)$"))) {
            throw new JsonbException("Invalid ISBN in the books list");
        }

//      Business validation
        try(Connection connection = pool.getConnection()) {
            PreparedStatement stmMemberExist = connection.prepareStatement("SELECT * FROM member WHERE id = ?");
            stmMemberExist.setString(1, issueNote.getMemberId());
            if(!stmMemberExist.executeQuery().next()){
                throw new JsonbException("Member does not exist");
            }

            PreparedStatement stm = connection.prepareStatement("SELECT b.title, b.copies, COUNT(r.isbn) AS issued_book_count ,((b.copies - COUNT(r.isbn) > 0)) AS availability FROM book b\n" +
                    "    LEFT OUTER JOIN issue_item ii ON b.isbn = ii.isbn\n" +
                    "    LEFT OUTER JOIN `return` r ON NOT(ii.issue_id = r.issue_id and ii.isbn = r.isbn)\n" +
                    "    WHERE b.isbn = ? GROUP BY b.isbn");

            for (String isbn : issueNote.getBooks()) {
                stm.setString(1, isbn);
                ResultSet rst = stm.executeQuery();
                if(!rst.next()) throw new JsonbException("Book does not exist");
                if (!rst.getBoolean("availability")){
                    throw new JsonbException(isbn + "is not available at the moment");
                }
            }

            PreparedStatement stmAvailable = connection.prepareStatement("SELECT m.name, 3 - COUNT(r.issue_id) AS available FROM issue_note\n" +
                    "    INNER JOIN issue_item ii on issue_note.id = ii.issue_id\n" +
                    "    INNER JOIN `return` r on not(ii.issue_id = r.issue_id and ii.isbn = r.isbn)\n" +
                    "    RIGHT OUTER JOIN member m on issue_note.member_id = m.id\n" +
                    "    WHERE m.id = ? \n" +
                    "    GROUP BY m.id");

            stmAvailable.setString(1, issueNote.getMemberId());
            ResultSet rst = stmAvailable.executeQuery();
            rst.next();
            int available = rst.getInt("available");
            if (issueNote.getBooks().size() > available){
                throw new JsonbException("Member can borrow only " + available + " books");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to place the issue note");
        }
    }
}
