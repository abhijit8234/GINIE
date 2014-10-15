

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author abhijit
 *
 */
public class Try {
	public static void main(String[] args) throws SQLException, IOException, InterruptedException
	{
		String[] arr= new String[10];
		ArrayList<String> str = new ArrayList();
		str.add("hello");
		str.add("hi");
		arr[0]= "asdf";
		arr[1]= "adfgdf";
		arr[0] = str.remove(0);
		
		System.out.println(arr[0]);
		System.out.println(str.get(0));
	}
}

