/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author motamedi
 */
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataBaseManager {

//	public String host = "atossa.cs.uoregon.edu";
//	public String database = "trace_info";// "ruben_twitter"; // twitter_businesses
//	private String username = "motamedi";
//	private String password = "fedelio";
        
        public String host = "atossa.cs.uoregon.edu";
	public String database = "dns_as_map";// "ruben_twitter"; // twitter_businesses
	private String username = "abhijit";
	private String password = "abhijit";
	private static DataBaseManager instance;
	public static Connection conn = null;
	public Statement stmt;

        
        public static Connection getConn() 
        {
            return conn;
        }
        
        
        
	private DataBaseManager() {
		connect();
		getStatement();
		execute("use " + database + ";");
	}

	public static DataBaseManager getInstance() {
		if (instance == null) {
			instance = new DataBaseManager();
		}
		return instance;
	}

	public void connect() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (IllegalAccessException iae) {
			System.out.println(">> illegal access problem during getting an instance of the driver<<");
			iae.printStackTrace();
		} catch (InstantiationException ie) {
			System.out.println(">> instantiation problem during getting an instance of the driver<<");
			ie.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			System.out.println(">> \"class not found\" problem during getting an instance of the driver<<");
			cnfe.printStackTrace();
		}
		conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:mysql://" + host + ":3306/"+ "?useUnicode=true&characterEncoding=utf8&user=" + username + "&password=" + password);
			conn.setAutoCommit(true);
		} catch (SQLException sqle) {
			System.out.println(">> problem during getting the connection <<");
			sqle.printStackTrace();
		}
	}

	public Statement getStatement() {
//        Statement st = null;
		try {
			stmt = conn.createStatement();
		} catch (SQLException sqle) {
			System.out.println(">> problem during getting the statement <<");
			sqle.printStackTrace();
		}
		return stmt;
	}

	public void execute(String query) {
		try {
//		    System.out.println(query);

			this.stmt.executeQuery(query);
		} catch (SQLException sqle) {
			System.out.println(">> problem during the execution <<");
			sqle.printStackTrace();
			System.out.println(query);
		}
	}

	public void executeUpdate(String query) {
		try {
//			System.out.println(query);
			this.stmt.executeUpdate(query);
		} catch (SQLException sqle) {
			System.out.println(">> problem during the execution <<");
			sqle.printStackTrace();
			System.out.println(query);
		}
	}

	public ResultSet executeRS(String query) {
		ResultSet rs = null;
		try {
//			System.out.println(query);
			rs = this.stmt.executeQuery(query);
		} catch (SQLException sqle) {
			System.out.println(">> problem during the execution <<");
			sqle.printStackTrace();
			System.out.println(query);
		}
		return rs;
	}

	public synchronized void checkDatabaseConnection() {
		try {
			if (conn.isClosed()) {
				connect();
				this.stmt = getStatement();
				return;
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

		try {
			if (stmt.isClosed()) {
				stmt = getStatement();
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
	}

	
}
