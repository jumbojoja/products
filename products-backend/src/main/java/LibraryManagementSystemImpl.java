import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    private static final Object transaction_lock = new Object();

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        // This method is used to store a book into the database
        // The method will first check if the book already exists in the database, if it does, it will return an ApiResult with a message indicating that the book already exists
        // If the book does not exist, it will insert the book into the database and return an ApiResult with a message indicating that the book has been successfully stored
        try {
            conn.setAutoCommit(false);
            // avoid injection
            String sql_select = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";
            PreparedStatement statement = conn.prepareStatement(sql_select);
            statement.setString(1, book.getCategory());
            statement.setString(2, book.getTitle());
            statement.setString(3, book.getPress());
            statement.setInt(4, book.getPublishYear());
            statement.setString(5, book.getAuthor());
            ResultSet resultSet = statement.executeQuery();
            // check if the book already exists
            if (resultSet.next()) {
                return new ApiResult(false, "already a same book");
            }
            String sql_insert = "INSERT INTO book(category,title,press,publish_year,author,price,stock) VALUES(?,?,?,?,?,?,?)";
            // insert the book and get its id
            PreparedStatement insertStmt = conn.prepareStatement(sql_insert, Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, book.getCategory());
            insertStmt.setString(2, book.getTitle());
            insertStmt.setString(3, book.getPress());
            insertStmt.setInt(4, book.getPublishYear());
            insertStmt.setString(5, book.getAuthor());
            insertStmt.setDouble(6, book.getPrice());
            insertStmt.setInt(7, book.getStock());
            int rows = insertStmt.executeUpdate();
            if (rows == 1) {
                commit(conn);
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    book.setBookId(generatedKeys.getInt(1));
                }
                return new ApiResult(true, "store book success");
            } else {
                conn.rollback();
                return new ApiResult(false, "store book fail");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "store book rollback fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        try {
            // begin to update book stock
            conn.setAutoCommit(false);
            // prepare the sql to select the stock of the book
            String sql_select = "SELECT * FROM book WHERE book_id = ?";
            // prepare the statement
            PreparedStatement statement = conn.prepareStatement(sql_select);
            // set the bookId to the statement
            statement.setInt(1, bookId);
            // execute the query
            ResultSet resultSet = statement.executeQuery();
            // if the book is exist
            if (resultSet.next()) {
                // get the stock of the book
                int stock = resultSet.getInt("stock");
                // the stock is too small
                if (stock + deltaStock < 0) {
                    // return the result
                    return new ApiResult(false, "no so many book stock");
                }
                // update the stock
                stock += deltaStock;
                // prepare the sql to update the stock
                String setSql = "UPDATE book SET stock = ? WHERE book_id = ?";
                // prepare the statement
                PreparedStatement upStatement = conn.prepareStatement(setSql);
                // set the stock to the statement
                upStatement.setInt(1, stock);
                // set the bookId to the statement
                upStatement.setInt(2, bookId);
                // execute the update
                int rows = upStatement.executeUpdate();
                // if the update is successful
                if (rows == 1) {
                    // commit the transaction
                    commit(conn);
                    // return the result
                    return new ApiResult(true, "update book stock success");
                } else {
                    // return the result
                    return new ApiResult(false, "fail to update book stock");
                }
            } else {
                return new ApiResult(false, "book_id not exist");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "incBookStock fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        try {
            //begin add books
            conn.setAutoCommit(false);
            //String to insert books
            String sql_ins = "INSERT INTO book(category,title,press,publish_year,author,price,stock) VALUES(?,?,?,?,?,?,?)";
            //prepare the sql statement
            PreparedStatement statement = conn.prepareStatement(sql_ins, Statement.RETURN_GENERATED_KEYS);
            // add batch
            for (Book book : books) {
                statement.setString(1, book.getCategory());
                statement.setString(2, book.getTitle());
                statement.setString(3, book.getPress());
                statement.setInt(4, book.getPublishYear());
                statement.setString(5, book.getAuthor());
                statement.setDouble(6, book.getPrice());
                statement.setInt(7, book.getStock());
                statement.addBatch();
            }
            //execute the batch
            int[] rows = statement.executeBatch();
            for (int i : rows) {
                // statement fail
                if (i == 0) {
                    rollback(conn);
                    return new ApiResult(false, "fail to execute batch (insert list of books)");
                }
            }
            //get the generated keys
            ResultSet keySet = statement.getGeneratedKeys();
            int i = 0;
            while (keySet.next()) {
                books.get(i).setBookId(keySet.getInt(1));
                i++;
            }
            //commit the transaction
            commit(conn);
            //return the result
            return new ApiResult(true, "success to store list of books");
        } catch (Exception e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "fail to insert a list of books");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        try {
            // Preconditions: book should not exist, or has been borrowed
            conn.setAutoCommit(false);
            // Check if the book exists
            String check_exist = "SELECT * FROM book WHERE book_id = ?";
            PreparedStatement statement = conn.prepareStatement(check_exist);
            statement.setInt(1, bookId);
            ResultSet resultSet = statement.executeQuery();
            // If the book does not exist, return failure
            if (!resultSet.next()) {
                return new ApiResult(false, "book not exist");
            }
            String check_brrow = "SELECT * FROM borrow WHERE book_id = ? AND return_time = 0";
            PreparedStatement statement_b = conn.prepareStatement(check_brrow);
            statement_b.setInt(1, bookId);
            ResultSet resultSet_b = statement_b.executeQuery();
            // If the book has been borrowed, return failure
            if (resultSet_b.next()) {
                return new ApiResult(false, "book is borrowed");
            }
            // Delete the book
            String sql_del = "DELETE FROM book WHERE book_id = ?";
            PreparedStatement statement_del = conn.prepareStatement(sql_del);
            statement_del.setInt(1, bookId);
            int ret = statement_del.executeUpdate();
            // If the deletion is successful, commit the transaction
            if (ret == 1) {
                commit(conn);
                return new ApiResult(true, "success to remove book");
            }
            // If the deletion is unsuccessful, return failure
            return new ApiResult(false, "fail to remove book");
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "fail to remove book");
        } finally{
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            // check if book exists
            String check_exist = "SELECT * FROM book WHERE book_id = ?";
            PreparedStatement statement_exist = conn.prepareStatement(check_exist);
            statement_exist.setInt(1, book.getBookId());
            ResultSet resultSet = statement_exist.executeQuery();
            if (!resultSet.next()) {
                return new ApiResult(false, "book does not exist");
            }
            // modify book info
            String modify_book = "UPDATE book SET ";
            int count = 0;
            if(book.getCategory() != null) {
                modify_book += "category = ?";
                count++;
            }
            if(book.getTitle() != null) {
                modify_book += (count > 0 ? "," : "") + "title = ?";
                count++;
            }
            if(book.getPress() != null) {
                modify_book += (count > 0 ? "," : "") + "press = ?";
                count++;
            }
            if(book.getPublishYear() != -1) {
                modify_book += (count >0 ? "," : "") + "publish_year = ?";
                count++;
            }
            if(book.getAuthor() != null) {
                modify_book += (count >0 ? "," : "") + "author = ?";
                count++;
            }
            if(book.getPrice() != -1) {
                modify_book += (count > 0 ? "," : "") + "price = ?";
                count++;
            }
            if(count == 0) {
                return new ApiResult(false,"no update is needed");
            }
            modify_book += " WHERE book_id = ?";
            PreparedStatement statement_mod = conn.prepareStatement(modify_book);
            int mod_id = 1;
            if(book.getCategory() != null) {
                statement_mod.setString(mod_id, book.getCategory());
                mod_id++;
            }
            if(book.getTitle() != null) {
                statement_mod.setString(mod_id, book.getTitle());
                mod_id++;
            }
            if(book.getPress() != null) {
                statement_mod.setString(mod_id, book.getPress());
                mod_id++;
            }
            if(book.getPublishYear() != -1) {
                statement_mod.setInt(mod_id, book.getPublishYear());
                mod_id++;
            }
            if(book.getAuthor() != null) {
                statement_mod.setString(mod_id, book.getAuthor());
                mod_id++;
            }
            if(book.getPrice() != -1) {
                statement_mod.setDouble(mod_id, book.getPrice());
                mod_id++;
            }
            statement_mod.setInt(mod_id, book.getBookId());
            int row = statement_mod.executeUpdate();
            if (row == 1) {
                commit(conn);
                return new ApiResult(true, "success to modify book info");
            }
            return new ApiResult(false, "fail to modify book info");
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "fail to modify book info");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            // trick
            String sql_query = "SELECT * FROM book WHERE 1=1 ";
            List<Object> parameter = new ArrayList<>();
            if (conditions.getCategory() != null) {
                sql_query += " AND category = ?";
                parameter.add(conditions.getCategory());
            }
            if (conditions.getTitle() != null) {
                sql_query += " AND title LIKE ?";
                parameter.add("%" + conditions.getTitle() + "%");
            }
            if (conditions.getPress() != null) {
                sql_query += " AND press LIKE ?";
                parameter.add("%" + conditions.getPress() + "%");
            }
            if (conditions.getMinPublishYear() != null) {
                sql_query += " AND publish_year >= ?";
                parameter.add(conditions.getMinPublishYear());
            }
            if (conditions.getMaxPublishYear() != null) {
                sql_query += " AND publish_year <= ?";
                parameter.add(conditions.getMaxPublishYear());
            }
            if (conditions.getAuthor() != null) {
                sql_query += " AND author LIKE ?";
                parameter.add("%" + conditions.getAuthor() + "%");
            }
            if (conditions.getMinPrice() != null) {
                sql_query += " AND price >= ?";
                parameter.add(conditions.getMinPrice());
            }
            if (conditions.getMaxPrice() != null) {
                sql_query += " AND price <= ?";
                parameter.add(conditions.getMaxPrice());
            }
            boolean sort_flag = false;
            if (conditions.getSortBy() != null) {
                sort_flag = true;
                sql_query += " ORDER BY " + conditions.getSortBy();
            }
            if (conditions.getSortBy() != null && conditions.getSortOrder() != null) {
                sql_query += ' ' + conditions.getSortOrder().getValue();
            }
            if (sort_flag) {
                sql_query += " ,book_id ASC";
            } else {
                // default sort by PK 
                sql_query += " ORDER BY book_id ASC";
            }
            PreparedStatement statement_query = conn.prepareStatement(sql_query);
            for (int i = 0; i < parameter.size(); i++) {
                // fill parameter
                statement_query.setObject(i+1, parameter.get(i));
            }
            ResultSet resultSet = statement_query.executeQuery();

            commit(conn);

            List<Book> bookList = new ArrayList<>();
            while (resultSet.next()) {
                Book abook = new Book();
                abook.setBookId(resultSet.getInt("book_id"));
                abook.setCategory(resultSet.getString("category"));
                abook.setTitle(resultSet.getString("title"));
                abook.setPress(resultSet.getString("press"));
                abook.setPublishYear(resultSet.getInt("publish_year"));
                abook.setAuthor(resultSet.getString("author"));
                abook.setPrice(resultSet.getDouble("price"));
                abook.setStock(resultSet.getInt("stock"));
                bookList.add(abook);
            }
            BookQueryResults bookQueryResults = new BookQueryResults(bookList);
            return new ApiResult(true, bookQueryResults);
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "query book fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ee){
                ee.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        // cover multiple thread
       Connection conn = connector.getConn();
        try {
            synchronized (transaction_lock) {
                conn.setAutoCommit(false);
                // check if there is not-return book
                String sql_check = "SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND return_time = 0 AND borrow_time != 0";
                PreparedStatement statement_check = conn.prepareStatement(sql_check);
                statement_check.setInt(1, borrow.getCardId());
                statement_check.setInt(2, borrow.getBookId());
                ResultSet resultSet_check = statement_check.executeQuery();
                if (resultSet_check.next()) {
                    return new ApiResult(false, "you have not return the book");
                }
                // check card
                String sql_card = "SELECT * FROM card WHERE card_id = ?";
                PreparedStatement statement_card = conn.prepareStatement(sql_card);
                statement_card.setInt(1, borrow.getCardId());
                ResultSet resultSet_card = statement_card.executeQuery();
                if (!resultSet_card.next()) {
                    return new ApiResult(false, "card not exist");
                }
                // check book
                String sql_book = "SELECT * FROM book WHERE book_id = ? AND stock > 0";
                PreparedStatement statement_book = conn.prepareStatement(sql_book);
                statement_book.setInt(1, borrow.getBookId());
                ResultSet resultSet_book = statement_book.executeQuery();
                if (resultSet_book.next()) {
                    String sql_update_book = "UPDATE book SET stock = stock - 1 WHERE book_id = ?";
                    PreparedStatement statement_update_book = conn.prepareStatement(sql_update_book);
                    statement_update_book.setInt(1, borrow.getBookId());
                    int row = statement_update_book.executeUpdate();
                    if (row != 1) {
                        return new ApiResult(false, "borrow book fail");
                    }
                    String sql_insert_borrow = "INSERT INTO borrow (card_id, book_id, borrow_time) VALUES (?, ?, ?)";
                    PreparedStatement statement_insert_borrow = conn.prepareStatement(sql_insert_borrow);
                    statement_insert_borrow.setInt(1, borrow.getCardId());
                    statement_insert_borrow.setInt(2, borrow.getBookId());
                    statement_insert_borrow.setLong(3, borrow.getBorrowTime());
                    int row_update = statement_insert_borrow.executeUpdate();
                    if (row_update == 1) {
                        commit(conn);
                        return new ApiResult(true, "borrow book success");
                    } else {
                        return new ApiResult(false, "borrow book fail");
                    }
                } else {
                    return new ApiResult(false, "book not exist or out of stock");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "borrow book fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ee){
                ee.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            // Preparing the SQL query to check if the book is available to be returned
            String sql_check_book = "SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND return_time = 0 AND borrow_time < ?";
            PreparedStatement statement_check_book = conn.prepareStatement(sql_check_book);
            statement_check_book.setInt(1, borrow.getCardId());
            statement_check_book.setInt(2, borrow.getBookId());
            statement_check_book.setLong(3, borrow.getReturnTime());
            // Executing the query
            ResultSet result_check_book = statement_check_book.executeQuery();
            // Checking if the book is available to be returned
            if (result_check_book.next()) {
                // Updating the borrow table with the return time
                String sql_update_borrow = "UPDATE borrow SET return_time = ? WHERE card_id = ? AND book_id = ? AND return_time = 0";
                PreparedStatement statement_update_borrow = conn.prepareStatement(sql_update_borrow);
                statement_update_borrow.setLong(1, borrow.getReturnTime());
                statement_update_borrow.setInt(2, borrow.getCardId());
                statement_update_borrow.setInt(3, borrow.getBookId());
                // Executing the update
                int row_update = statement_update_borrow.executeUpdate();
                // Checking if the update was successful
                if (row_update == 1) {
                    // Updating the book table with an additional stock
                    String sql_update_book = "UPDATE book SET stock = stock + 1 WHERE book_id = ?";
                    PreparedStatement statement_update_book = conn.prepareStatement(sql_update_book);
                    statement_update_book.setInt(1, borrow.getBookId());
                    // Executing the update
                    int row_update_book = statement_update_book.executeUpdate();
                    // Checking if the update was successful
                    if (row_update_book == 1) {
                    // Committing the transaction
                        commit(conn);
                        return new ApiResult(true, "return book success");
                    } else {
                    // Rolling back the transaction
                        rollback(conn);
                        return new ApiResult(false, "return book fail");
                    }
                } else {
                    rollback(conn);
                    return new ApiResult(false, "return book fail");
                }
            } else {
                rollback(conn);
                return new ApiResult(false, "return book fail");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "return book fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ee){
                ee.printStackTrace();   
            }
        }
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        try {
            // Set auto commit to false
            conn.setAutoCommit(false);
            // Create an array list to store the borrow items
            List<BorrowHistories.Item> borrow_items = new ArrayList<>();
            // Create a SQL query to get the borrow history of a card
            String sql_check_history = "SELECT * FROM borrow WHERE card_id = ? ORDER BY borrow_time DESC";
            // Create a prepared statement from the SQL query
            PreparedStatement statement_check_history = conn.prepareStatement(sql_check_history);
            // Set the card id in the prepared statement
            statement_check_history.setInt(1, cardId);
            // Execute the query and get the result set
            ResultSet resultSet_check_history = statement_check_history.executeQuery();
            // Loop through the result set
            while (resultSet_check_history.next()) {
                // Get the book id of the borrow book
                int book_id = resultSet_check_history.getInt("book_id");
                // Create a new book object
                Book borrow_book = new Book();
                // Create a SQL query to get the book info
                String sql_get_book = "SELECT * FROM book WHERE book_id = ?";
                // Create a prepared statement from the SQL query
                PreparedStatement statement_get_book = conn.prepareStatement(sql_get_book);
                // Set the book id in the prepared statement
                statement_get_book.setInt(1, book_id);
                // Execute the query and get the result set
                ResultSet resultSet_get_book = statement_get_book.executeQuery();
                // Check if the result set has a book
                if (resultSet_get_book.next()) {
                    // Set the book info
                    borrow_book.setBookId(resultSet_get_book.getInt("book_id"));
                    borrow_book.setCategory(resultSet_get_book.getString("category"));
                    borrow_book.setTitle(resultSet_get_book.getString("title"));
                    borrow_book.setPress(resultSet_get_book.getString("press"));
                    borrow_book.setPublishYear(resultSet_get_book.getInt("publish_year"));
                    borrow_book.setAuthor(resultSet_get_book.getString("author"));
                    borrow_book.setPrice(resultSet_get_book.getDouble("price"));
                }
                // Create a new borrow object
                Borrow borrow = new Borrow();
                // Set the book id and card id in the borrow object
                borrow.setBookId(book_id);
                borrow.setCardId(cardId);
                // Set the borrow time and return time in the borrow object
                borrow.setBorrowTime(resultSet_check_history.getLong("borrow_time"));
                borrow.setReturnTime(resultSet_check_history.getLong("return_time"));
                // Add the borrow item to the array list
                borrow_items.add(new BorrowHistories.Item(cardId, borrow_book, borrow));
            }
            // Commit the transaction
            commit(conn);
            // Return the borrow history
            return new ApiResult(true, "success", new BorrowHistories(borrow_items));
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "show borrow history fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ee){
                ee.printStackTrace();   
            }
        }
    }

    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        try {
            //begin setting the auto commit mode of the connection to false
            conn.setAutoCommit(false);
            //prepare the SQL query to check if the card already exists
            String sql_check_card = "SELECT * FROM card WHERE name = ? AND department = ? AND type = ?";
            //create a prepared statement to execute the query
            PreparedStatement statement_check_card = conn.prepareStatement(sql_check_card);
            //set the values of the query
            statement_check_card.setString(1, card.getName());
            statement_check_card.setString(2, card.getDepartment());
            statement_check_card.setString(3, card.getType().getStr());
            //execute the query and get the result set
            ResultSet resultSet_check_card = statement_check_card.executeQuery();
            //if the result set has a next row, the card already exists
            if (resultSet_check_card.next()) {
            //return an ApiResult indicating the card already exists
            return new ApiResult(false, "card already exists");
            //otherwise, prepare the SQL query to insert the card
            } else {
                String sql_insert_card = "INSERT INTO card (name, department, type) VALUES (?, ?, ?)";
                //create a prepared statement to execute the query, with the RETURN_GENERATED_KEYS attribute to get the generated keys
                PreparedStatement statement_insert_card = conn.prepareStatement(sql_insert_card, Statement.RETURN_GENERATED_KEYS);
                //set the values of the query
                statement_insert_card.setString(1, card.getName());
                statement_insert_card.setString(2, card.getDepartment());
                statement_insert_card.setString(3, card.getType().getStr());
                //execute the query and get the row count
                int row_insert_card = statement_insert_card.executeUpdate();
                //if the row count is greater than 0, the card was inserted successfully
                if (row_insert_card > 0) {
                    //get the result set of the generated keys
                    ResultSet generatedKeys = statement_insert_card.getGeneratedKeys();
                    //if the result set has a next row, the card's id was generated
                    if (generatedKeys.next()) {
                        //set the card's id
                        card.setCardId(generatedKeys.getInt(1));
                    }
                    //commit the transaction
                    commit(conn);
                    //return an ApiResult indicating the success
                    return new ApiResult(true, "success");
                //otherwise, rollback the transaction and return an ApiResult indicating the failure
                } else {
                    rollback(conn);
                    return new ApiResult(false, "register card fail");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "register card fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ee){
                ee.printStackTrace();   
            }
        }
    }

    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        try {
            // Set the auto commit mode of the connection to false
            conn.setAutoCommit(false);
            // Prepare the SQL query to check if the user with the given card ID has borrowed any books
            String aql_check_book = "SELECT * FROM borrow WHERE card_id = ? AND return_time = 0 ";
            // Create a prepared statement with the given SQL query
            PreparedStatement statement_check_book = conn.prepareStatement(aql_check_book);
            // Set the card ID as a parameter for the prepared statement
            statement_check_book.setInt(1, cardId);
            // Execute the query and get the result set
            ResultSet resultSet_check_book = statement_check_book.executeQuery();
            // Check if the result set has any results
            if (resultSet_check_book.next()) {
            // Return an error message if the user has borrowed a book
                return new ApiResult(false, "card has borrowed books");
            }
            // Prepare the SQL query to check if the user with the given card ID exists
            String sql_check_card = "SELECT * FROM card WHERE card_id = ?";
            // Create a prepared statement with the given SQL query
            PreparedStatement statement_check_card = conn.prepareStatement(sql_check_card);
            // Set the card ID as a parameter for the prepared statement
            statement_check_card.setInt(1, cardId);
            // Execute the query and get the result set
            ResultSet resultSet_check_card = statement_check_card.executeQuery();
            // Check if the result set has any results
            if (resultSet_check_card.next()) {
                // Delete the user from the database
                String sql_delete_card = "DELETE FROM card WHERE card_id = ?";
                // Create a prepared statement with the given SQL query
                PreparedStatement statement_delete_card = conn.prepareStatement(sql_delete_card);
                // Set the card ID as a parameter for the prepared statement
                statement_delete_card.setInt(1, cardId);
                // Execute the query and get the number of rows affected
                int row_delete_card = statement_delete_card.executeUpdate();
                // Check if the number of rows affected is equal to 1
                if (row_delete_card == 1) {
                // Commit the transaction
                    commit(conn);
                // Return a success message
                    return new ApiResult(true, "remove book success");
                } else {
                // Rollback the transaction
                    rollback(conn);
                // Return an error message
                    return new ApiResult(false, "remove book fail");
                }
            } else {
                // Return an error message if the user does not exist
                return new ApiResult(false, "card not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "remove card fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ee){
                ee.printStackTrace();   
            }
        }
    }

    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        try {
            // Set auto commit to false
            conn.setAutoCommit(false);
            // Create a new list of cards
            List<Card> cardList =new ArrayList<>();
            // Prepare the SQL query to show cards
            String sql_show_cards = "SELECT * FROM card ORDER BY card_id";
            // Create a prepared statement from the SQL query
            PreparedStatement statement_show_cards = conn.prepareStatement(sql_show_cards);
            // Execute the query and get the result set
            ResultSet resultSet_show_cards = statement_show_cards.executeQuery();
            // Loop through the result set and add each card to the list
            while (resultSet_show_cards.next()) {
                Card card = new Card();
                card.setCardId(resultSet_show_cards.getInt("card_id"));
                card.setName(resultSet_show_cards.getString("name"));
                card.setDepartment(resultSet_show_cards.getString("department"));
                card.setType(Card.CardType.values(resultSet_show_cards.getString("type")));
                cardList.add(card);
            }
            // Commit the transaction
            commit(conn);
            // Return the result
            return new ApiResult(true, "show cards success", new CardList(cardList));
        } catch(SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "show cards fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch(SQLException ee) {
                ee.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult ModifyCard(Card card) {
        Connection conn = connector.getConn();
        try {
            // Set the auto commit mode of the connection to false
            conn.setAutoCommit(false);
            // Prepare the SQL query to check if the user with the given card ID
            String aql_check_book = "SELECT * FROM card WHERE card_id = ? ";
            // Create a prepared statement with the given SQL query
            PreparedStatement statement_check_book = conn.prepareStatement(aql_check_book);
            // Set the card ID as a parameter for the prepared statement
            statement_check_book.setInt(1, card.getCardId());
            // Execute the query and get the result set
            ResultSet resultSet_check_book = statement_check_book.executeQuery();
            // Check if the result set has any results
            if (!resultSet_check_book.next()) {
            // Return an error message if the user has borrowed a book
                return new ApiResult(false, "card not exist");
            }
            // Prepare the SQL query to check if the user with the given card ID exists
            String update_card = "UPDATE card SET name = ? , department = ? , type = ?  WHERE card_id = ? ";
            // Create a prepared statement with the given SQL query
            PreparedStatement statement_update_card = conn.prepareStatement(update_card);
            //set the values of the query
            statement_update_card.setString(1, card.getName());
            statement_update_card.setString(2, card.getDepartment());
            statement_update_card.setString(3, card.getType().getStr());
            statement_update_card.setInt(4, card.getCardId());
            // Execute the query and get the result set
            int rows = statement_update_card.executeUpdate();
            // Check if the result set has any results
            if (rows > 0) {
                commit(conn);
                //return an ApiResult indicating the success
                return new ApiResult(true, "success");
            } else {
                //otherwise, rollback the transaction and return an ApiResult indicating the failure
                rollback(conn);
                return new ApiResult(false, "modify card fail");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "modify card fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ee){
                ee.printStackTrace();   
            }
        }
    }

    @Override
    public ApiResult showBooks() {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            String check_book = "SELECT * FROM book";
            PreparedStatement statement = conn.prepareStatement(check_book);
            ResultSet result = statement.executeQuery();
            /* if (!result.next()) {
                return new ApiResult(false, "no book exist");
            } */
            List<Book> bookList = new ArrayList<>();
            while (result.next()) {
                Book abook = new Book();
                abook.setBookId(result.getInt("book_id"));
                abook.setCategory(result.getString("category"));
                abook.setTitle(result.getString("title"));
                abook.setPress(result.getString("press"));
                abook.setPublishYear(result.getInt("publish_year"));
                abook.setAuthor(result.getString("author"));
                abook.setPrice(result.getDouble("price"));
                abook.setStock(result.getInt("stock"));
                bookList.add(abook);
            }
            BookQueryResults bookQueryResults = new BookQueryResults(bookList);
            return new ApiResult(true, bookQueryResults);
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "modify card fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ee){
                ee.printStackTrace();   
            }
        }
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
