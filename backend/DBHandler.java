import java.sql.*;

public class DBHandler {
    private static final String URL = "jdbc:mysql://localhost:3306/student_portal";
    private static final String USER = "root"; // MySQL username
    private static final String PASS = "";     // MySQL password

    public static boolean insertLeave(String name, String roll, String fromDate, String toDate, String reason) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASS);
            String query = "INSERT INTO leave_requests (student_name, roll_no, from_date, to_date, reason) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, name);
            pst.setString(2, roll);
            pst.setString(3, fromDate);
            pst.setString(4, toDate);
            pst.setString(5, reason);

            int rows = pst.executeUpdate();
            con.close();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
