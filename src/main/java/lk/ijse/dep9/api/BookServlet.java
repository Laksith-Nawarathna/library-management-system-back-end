package lk.ijse.dep9.api;

import jakarta.annotation.Resource;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.ijse.dep9.api.util.HttpServlet2;
import lk.ijse.dep9.dto.BookDTO;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name = "BookServlet", value = "/books/*", loadOnStartup = 0)
public class BookServlet extends HttpServlet2 {

    @Resource(lookup = "java:comp/env/jdbc/lms")
    private DataSource pool;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getPathInfo() == null || request.getPathInfo().equals("/")) {
            String query = request.getParameter("q");
            String size = request.getParameter("size");
            String page = request.getParameter("page");

            if (query != null && size != null && page != null) {
                if (!size.matches("\\d+") || !page.matches("\\d+")) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid page or size");
                } else {
                    searchPaginatedBooks(query, Integer.parseInt(size), Integer.parseInt(page), response);
                }
            } else if (query != null) {
                searchBooks(query, response);
            } else if (size != null && page != null) {
                if (!size.matches("\\d+") || !page.matches("\\d+")) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid page or size");
                } else {
                    loadAllPaginatedBooks(Integer.parseInt(size), Integer.parseInt(page), response);
                }
            } else {
                loadAllBooks(response);
            }
        } else {
            Matcher matcher = Pattern.compile("^/([0-9][0-9\\\\-]*[0-9])/?$").matcher(request.getPathInfo());
            if (matcher.matches()) {
                getBookDetails(matcher.group(1), response);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            }
        }
    }

    private void loadAllBooks(HttpServletResponse response) throws IOException {
        try (Connection connection = pool.getConnection()) {
            Statement stm = connection.createStatement();
            String sql = "SELECT * FROM book";
            ResultSet rst = stm.executeQuery(sql);
            ArrayList<BookDTO> books = new ArrayList<>();

            while (rst.next()) {
                String isbn = rst.getString("isbn");
                String title = rst.getString("title");
                String author = rst.getString("author");
                int copies = rst.getInt("copies");
                BookDTO bookDTO = new BookDTO(isbn, title, author, copies);
                books.add(bookDTO);
            }

            response.setContentType("application/json");
            JsonbBuilder.create().toJson(books, response.getWriter());
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to fetch books");
        }
    }

    private void loadAllPaginatedBooks(int size, int page, HttpServletResponse response) throws IOException {
        try (Connection connection = pool.getConnection()) {
            Statement stmCount = connection.createStatement();
            String sql = "SELECT COUNT(isbn) FROM book";
            ResultSet rst1 = stmCount.executeQuery(sql);
            rst1.next();
            int totalBooks = rst1.getInt(1);
            response.addIntHeader("X-Total-Count", totalBooks);

            PreparedStatement stm = connection.prepareStatement("SELECT * FROM book LIMIT ? OFFSET ?");
            stm.setInt(1, size);
            stm.setInt(2, (page - 1) * size);
            ResultSet rst2 = stm.executeQuery();

            ArrayList<BookDTO> books = new ArrayList<>();

            while (rst2.next()) {
                String isbn = rst2.getString("isbn");
                String title = rst2.getString("title");
                String author = rst2.getString("author");
                int copies = rst2.getInt("copies");
                books.add(new BookDTO(isbn, title, author, copies));
            }

            response.setContentType("application/json");
            JsonbBuilder.create().toJson(books, response.getWriter());
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to fetch books");
        }
    }

    private void searchBooks(String query, HttpServletResponse response) throws IOException {
        try (Connection connection = pool.getConnection()) {
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM book WHERE isbn LIKE ? OR title LIKE ? OR author LIKE ?");
            query = "%" + query + "%";
            stm.setString(1, query);
            stm.setString(2, query);
            stm.setString(3, query);
            ResultSet rst = stm.executeQuery();
            ArrayList<BookDTO> books = new ArrayList<>();

            while (rst.next()) {
                String isbn = rst.getString("isbn");
                String title = rst.getString("title");
                String author = rst.getString("author");
                int copies = rst.getInt("copies");
                books.add(new BookDTO(isbn, title, author, copies));
            }

            response.setContentType("application/json");
            JsonbBuilder.create().toJson(books, response.getWriter());
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to fetch books");
        }
    }

    private void searchPaginatedBooks(String query, int size, int page, HttpServletResponse response) throws IOException {
        try (Connection connection = pool.getConnection()) {
            PreparedStatement stmCount = connection.prepareStatement("SELECT COUNT(isbn) FROM book WHERE isbn LIKE ? OR title LIKE ? OR author LIKE ?");
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM book WHERE isbn LIKE ? OR title LIKE ? OR author LIKE ? LIMIT ? OFFSET ?");

            query = "%" + query + "%";
            stmCount.setString(1, query);
            stmCount.setString(2, query);
            stmCount.setString(3, query);
            ResultSet rst1 = stmCount.executeQuery();
            rst1.next();
            int totalBooks = rst1.getInt(1);
            response.addIntHeader("X-Total-Count", totalBooks);

            stm.setString(1, query);
            stm.setString(2, query);
            stm.setString(3, query);
            stm.setInt(4, size);
            stm.setInt(5, (page - 1) * size);
            ResultSet rst2 = stm.executeQuery();

            ArrayList<BookDTO> books = new ArrayList<>();

            while (rst2.next()) {
                String isbn = rst2.getString("isbn");
                String title = rst2.getString("title");
                String author = rst2.getString("author");
                int copies = rst2.getInt("copies");
                books.add(new BookDTO(isbn, title, author, copies));
            }

            response.setContentType("application/json");
            JsonbBuilder.create().toJson(books, response.getWriter());
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to fetch books");
        }
    }

    private void getBookDetails(String isbn, HttpServletResponse response) throws IOException {
        try (Connection connection = pool.getConnection()) {
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM book WHERE isbn = ?");
            stm.setString(1, isbn);
            ResultSet rst = stm.executeQuery();

            if (rst.next()) {
                String title = rst.getString("title");
                String author = rst.getString("author");
                int copies = rst.getInt("copies");
                BookDTO bookDTO = new BookDTO(isbn, title, author, copies);
                response.setContentType("application/json");
                JsonbBuilder.create().toJson(bookDTO, response.getWriter());
            }
            else{
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid book isbn");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to fetch the book details");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().println("books doPost()");
    }

    @Override
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().println("books doPatch()");
    }
}
