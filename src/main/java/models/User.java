package models;

public class User {
    private String loginName;
    private String rollNo;
    private String email;
    private String mobile;
    private String password; // Should be handled securely, maybe omitted in UI
    private String role;
    private boolean isPending; // true if stored in settings

    public User() {}

    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isPending() { return isPending; }
    public void setPending(boolean pending) { isPending = pending; }
}
