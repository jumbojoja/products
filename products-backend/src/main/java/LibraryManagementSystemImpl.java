import entities.User;
import entities.Goods;
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
    public ApiResult adduser(String user_name, String password, String email) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            // avoid injection
            String sql_select = "SELECT * FROM user WHERE user_name = ?";
            PreparedStatement statement = conn.prepareStatement(sql_select);
            statement.setString(1, user_name);
            ResultSet resultSet = statement.executeQuery();
            // check if the user already exists
            if (resultSet.next()) {
                return new ApiResult(false, "already a same user");
            }
            String sql_insert = "INSERT INTO user(user_name,password,email) VALUES(?,?,?)";
            // insert the book and get its id
            PreparedStatement insertStmt = conn.prepareStatement(sql_insert, Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, user_name);
            insertStmt.setString(2, password);
            insertStmt.setString(3, email);
            int rows = insertStmt.executeUpdate();
            if (rows == 1) {
                commit(conn);
                return new ApiResult(true, "register success");
            } else {
                conn.rollback();
                return new ApiResult(false, "register fail");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "register rollback fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult checkuser(String user_name, String password, String email) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            // avoid injection
            String sql_select = "SELECT * FROM user WHERE user_name = ? AND password = ?";
            PreparedStatement statement = conn.prepareStatement(sql_select);
            statement.setString(1, user_name);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            // check if the user exists
            if (resultSet.next()) {
                int user_id = resultSet.getInt("user_id");
                User user = new User();
                user.setUserId(user_id);
                user.setUserName(resultSet.getString("user_name"));
                user.setPassword(resultSet.getString("password"));
                user.setEmail(resultSet.getString("email"));
                commit(conn);
                return new ApiResult(true, "login success", user);
            }else {
                conn.rollback();
                return new ApiResult(false, "login fail");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "login rollback fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult addgoods(String sku_id, String goods_name, String goods_link, String img_url, double price, String platform) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            String sql_insert = "INSERT INTO historygoods(sku_id, goods_name, goods_link, img_url, price, platform) " +
                "VALUES(?, ?, ?, ?, ?, ?) ";
            PreparedStatement insert_statement = conn.prepareStatement(sql_insert);
            insert_statement.setString(1, sku_id);
            insert_statement.setString(2, goods_name);
            insert_statement.setString(3, goods_link);
            insert_statement.setString(4, img_url);
            insert_statement.setDouble(5, price);
            insert_statement.setString(6, platform);

            int rows1 = insert_statement.executeUpdate();
            commit(conn);

            // 使用 ON DUPLICATE KEY UPDATE 插入或更新商品信息
            String sql_insert_update = "INSERT INTO goods(sku_id, goods_name, goods_link, img_url, price, platform) " +
                "VALUES(?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "goods_name = VALUES(goods_name), " +
                "goods_link = VALUES(goods_link), " +
                "img_url = VALUES(img_url), " +
                "price = VALUES(price), " +
                "platform = VALUES(platform)";
            PreparedStatement statement = conn.prepareStatement(sql_insert_update);
            statement.setString(1, sku_id);
            statement.setString(2, goods_name);
            statement.setString(3, goods_link);
            statement.setString(4, img_url);
            statement.setDouble(5, price);
            statement.setString(6, platform);

            int rows = statement.executeUpdate();

            if (rows == 1 && rows1 == 1) {
                commit(conn);
                return new ApiResult(true, "add goods success");
            } else {
                conn.rollback();
                return new ApiResult(false, "add goods fail");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "rollback fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult searchgoods(String sku_id) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            String sql_select = "SELECT * FROM historygoods WHERE sku_id = ?";
            PreparedStatement statement = conn.prepareStatement(sql_select);
            statement.setString(1, sku_id);

            ResultSet resultSet = statement.executeQuery();
            commit(conn);

            List<Goods> goodsList = new ArrayList<>();
            while (resultSet.next()) {
                Goods agoods = new Goods();
                agoods.setGoodsId(resultSet.getInt("goods_id"));
                agoods.setSkuId(resultSet.getString("sku_id"));
                agoods.setGoodsName(resultSet.getString("goods_name"));
                agoods.setGoodsLink(resultSet.getString("goods_link"));
                agoods.setImgUrl(resultSet.getString("img_url"));
                agoods.setPrice(resultSet.getDouble("price"));
                agoods.setPlatform(resultSet.getString("platform"));
                goodsList.add(agoods);
            }
            GoodsResults goodsResults = new GoodsResults(goodsList);
            return new ApiResult(true, goodsResults);

        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "searchgoods fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult addcollect(String sku_id, int user_id) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false); 

            // 1. 检查用户是否存在
            String sql_select_user = "SELECT * FROM user WHERE user_id = ?";
            PreparedStatement userStmt = conn.prepareStatement(sql_select_user);
            userStmt.setInt(1, user_id);
            ResultSet userResult = userStmt.executeQuery();
            if (!userResult.next()) {
                return new ApiResult(false, "User not found");
            }

            // 2. 检查商品是否存在
            String sql_select_goods = "SELECT * FROM goods WHERE sku_id = ?";
            PreparedStatement goodsStmt = conn.prepareStatement(sql_select_goods);
            goodsStmt.setString(1, sku_id);
            ResultSet goodsResult = goodsStmt.executeQuery();
            if (!goodsResult.next()) {
                return new ApiResult(false, "Goods not found");
            }

            // 获取商品信息
            String goods_name = goodsResult.getString("goods_name");
            String goods_link = goodsResult.getString("goods_link");
            String img_url = goodsResult.getString("img_url");
            double price = goodsResult.getDouble("price");
            String platform = goodsResult.getString("platform");

            // 3. 将商品添加到收藏表 collectgoods
            String sql_insert_collect = "INSERT INTO collectgoods(goods_id, user_id, sku_id, goods_name, goods_link, img_url, price, platform) " +
                                        "VALUES((SELECT goods_id FROM goods WHERE sku_id = ?), ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(sql_insert_collect);
            insertStmt.setString(1, sku_id);
            insertStmt.setInt(2, user_id);
            insertStmt.setString(3, sku_id);   // sku_id
            insertStmt.setString(4, goods_name);
            insertStmt.setString(5, goods_link);
            insertStmt.setString(6, img_url);
            insertStmt.setDouble(7, price);
            insertStmt.setString(8, platform);

            int rows = insertStmt.executeUpdate();
            
            // 检查是否插入成功
            if (rows == 1) {
                conn.commit();
                return new ApiResult(true, "Item added to collection successfully");
            } else {
                conn.rollback();
                return new ApiResult(false, "Failed to add item to collection");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "Database error occurred");
        } finally {
            try {
                conn.setAutoCommit(true);  // 恢复自动提交
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ApiResult showCollects(int user_id) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            String sql_select = "SELECT * FROM collectgoods WHERE user_id = ?";
            PreparedStatement statement = conn.prepareStatement(sql_select);
            statement.setInt(1, user_id);
            ResultSet resultSet = statement.executeQuery();
            commit(conn);

            List<Goods> goodsList = new ArrayList<>();
            while (resultSet.next()) {
                Goods agoods = new Goods();
                agoods.setGoodsId(resultSet.getInt("goods_id"));
                agoods.setSkuId(resultSet.getString("sku_id"));
                agoods.setGoodsName(resultSet.getString("goods_name"));
                agoods.setGoodsLink(resultSet.getString("goods_link"));
                agoods.setImgUrl(resultSet.getString("img_url"));
                agoods.setPrice(resultSet.getDouble("price"));
                agoods.setPlatform(resultSet.getString("platform"));
                goodsList.add(agoods);
            }
            GoodsResults goodsResults = new GoodsResults(goodsList);
            return new ApiResult(true, goodsResults);

        } catch (SQLException e) {
            e.printStackTrace();
            rollback(conn);
            return new ApiResult(false, "searchgoods fail");
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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
