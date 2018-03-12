package org.sdn.DB;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import javax.ws.rs.core.UriBuilder;

import org.sdn.ServerInfoLog.MsgLog;
import org.sdn.dataType.KeyPair;
import org.sdn.dataType.SecurityInfo;
import org.sdn.dataType.deploymentStatus;
import org.sdn.dataType.link;

import com.mysql.jdbc.DatabaseMetaData;
import org.sdn.ServerInfoLog.MsgLog;
public class DBFunction {
  public  static MsgLog msglog=new MsgLog();
  private static deploymentStatus deploymentS=new deploymentStatus();
  private static Map<String,Integer>linkBandwidth=new HashMap<String,Integer>();
  private static Map<String,SecurityInfo> hostSecurityInfo=new HashMap<String,SecurityInfo>();
  private static int Session_ID=0;
  public static Map<String,Integer> getLinkBandwidth(){
	  return linkBandwidth;
  }
 /* private static Map<link,Integer>linkBandwidth=new HashMap<link,Integer>();
  public static Map<link,Integer> getLinkBandwidth(){
	  return linkBandwidth;
  }*/
  public static int getSessionID(){
	  Session_ID+=1;
	  return Session_ID;
  }
  public static Map<String,SecurityInfo> gethostSecurityInfo(){
	  return hostSecurityInfo;
  }
  public static deploymentStatus getDeploymentStatus(){
	  return deploymentS;
  }
  public Connection newConnection(){
	  Connection conn=null;
	  try {
			Class.forName("com.mysql.jdbc.Driver");
			conn= DriverManager.getConnection("jdbc:mysql://localhost:3306/SDN","root","");	
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return  conn;
  }
  public void deleteTemptable(Connection conn,String prefix) throws SQLException {
		// TODO Auto-generated method stub
		DatabaseMetaData md=(DatabaseMetaData) conn .getMetaData();
		ResultSet rs=md.getTables(null, null,"%",null);
		while(rs.next()){
			String table=rs.getString(3);
			if(table.startsWith(prefix)){
				Statement stmt=conn.createStatement();
				String sql="DROP TABLE "+table;
				stmt.executeUpdate(sql);
				System.out.println("table deleted in the database");
			}				
		}
	}
  public String getCurrentTime(){
 	    long unixTime=System.currentTimeMillis();
      Date date=new Date(unixTime);
      SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      sdf.setTimeZone(TimeZone.getTimeZone("UTC-6"));
      String time=sdf.format(date);
      return time;
      }
	public static URI getBaseURI(){
		String url="http://localhost:8081/wm/";  
	    return UriBuilder.fromUri(url).build();
	}
	public int randomNumGeneration(int length) {
		// TODO Auto-generated method stub
		Random rand=new Random(System.nanoTime()%100000);
	//	rand.setSeed(System.currentTimeMillis());
		int n=rand.nextInt(length);
		return n;
	}
    public static String getCurTime(){
        long unixTime=System.currentTimeMillis();
        Date date=new Date(unixTime);
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC-6"));
        String time=sdf.format(date);
        return time;
    }
	public static void writeTolog(String s) {
		System.out.println(s);
		try{
		System.out.println(s);
		String time=getCurTime();
		msglog.write("["+time+"]"+s+"\n");
		}
		catch(Exception e){
		 e.printStackTrace();
		}

	}
}
