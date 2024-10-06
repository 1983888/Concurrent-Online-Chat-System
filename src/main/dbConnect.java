import java.sql.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class dbConnect {
    // Create the Singleton instance mysqlConn
    private static Connection mysqlConn = null;
    static // this block will run only once when class is loaded into memory
    {
        String filePath = "src/main/dbConnect.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Read connection details from the text file
            String url = br.readLine().trim();
            String dbName = br.readLine().trim();
            String user = br.readLine().trim();
            String password = br.readLine().trim();

            Class.forName("com.mysql.cj.jdbc.Driver");

            mysqlConn = DriverManager.getConnection(url+dbName, user, password);

            System.out.println("MySQL Db Connection is successful");
        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
    public static Connection getMysqlConnection() {
        return mysqlConn;
    }
}
