package entities;

import java.util.Objects;

import com.alibaba.fastjson.annotation.JSONField;

public final class User {
    @JSONField(name = "userId", ordinal = 1)
    private int user_id;
    @JSONField(name = "userName", ordinal = 2)
    private String user_name;
    @JSONField(name = "password", ordinal = 3)
    private String password;
    @JSONField(name = "email", ordinal = 4)
    private String email;

    public User() {
    }

    public User(int user_id, String user_name, String password, String email) {
        this.user_id = user_id;
        this.user_name = user_name;
        this.password = password;
        this.email = email;
    }

    @Override
    public int hashCode() {
        return Objects.hash(user_name, password, email);
    }

    public int getUserId() {
        return user_id;
    }

    public void setUserId(int user_id) {
        this.user_id = user_id;
    }

    public String getUserName() {
        return user_name;
    }

    public void setUserName(String user_name) {
        this.user_name = user_name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

    