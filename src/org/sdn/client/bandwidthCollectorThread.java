package org.sdn.client;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;


import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sdn.DB.DBFunction;
import org.sdn.dataType.bandwidth;
import com.mysql.jdbc.DatabaseMetaData;

public class bandwidthCollectorThread extends Thread {
	private WebTarget target;
	private DBFunction db=new DBFunction();
	private Connection conn=db.newConnection(); 
	private int frequency;
	private String prefix="temp_bandwidth_";
	private bandwidth[] portlist;
	public bandwidthCollectorThread(bandwidth[] portlist,int freq) throws Exception{
     try{
     Client client=ClientBuilder.newClient();
     target=client.target(db.getBaseURI());
     this.portlist=portlist;
     this.frequency=freq;
     }catch(Exception e){
    	 throw new Exception(e);
     }
    }
	public void getBandwidthPerSwitchPerPortCustomrizationMode(){
  	  try{
  	    	for(bandwidth port:portlist){ 
  	    			String dpid=port.getDpid();
  	    			String portNum=port.getPort();
  	    			Response res;
  	    	        res=target.path("statistics/bandwidth")
  	    	        		   .path(dpid)
  	    	        		   .path(portNum)
  	    	        		   .path("json")
  	    	        		   .request(MediaType.APPLICATION_JSON)
  	    	        		   .get();
  	    	        
  	    	        String s=res.readEntity(String.class);
  	    	        JSONArray jsonarray=new JSONArray(s);
  	    	        if(!jsonarray.get(0).equals(null)){
  	       	        	//System.out.println(dpid+" "+port+" length:"+jsonarray.length());
  	           	        //System.out.println(jsonarray.get(0)); 
  	    	        JSONObject jobj=(JSONObject) jsonarray.get(0);
  	    	        long bitsPerSecondsRx=jobj.getLong("bits-per-second-rx");
  	    	        long BitsPerSecondTx=jobj.getLong("bits-per-second-tx");
  	                String time=db.getCurrentTime();
  	            	String table=prefix+dpid.replace(":","")+"P"+portNum;
  	            	checkIfTableExist(table);
  	            	Statement stt=  conn.createStatement();
  				    stt.execute("INSERT INTO "+table+"(Time,bitsPerSecondRX,bitsPerSecondTx) VALUES"+
  					    "('"+time+"',"+bitsPerSecondsRx+","+BitsPerSecondTx+")" );
  	    	        }
  	    	     }
  			 }catch(Exception e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			    }
  }	    	
	
	public void run(){
		try {		
			while(true){
				 getBandwidthPerSwitchPerPortCustomrizationMode();
				// System.out.println(frequency);
				 Thread.sleep(frequency);			
			  }
			 				
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void checkIfTableExist(String table) throws Exception{
		 DatabaseMetaData meta=(DatabaseMetaData) conn.getMetaData();
		 ResultSet res=meta.getTables(null,null,table,new String[]{"TABLE"});
		 if(!res.next()){ //create a new table
			 Statement stmt=conn.createStatement();
			 String sql="CREATE TABLE "+table+
					     " (id INT not NULL auto_increment,"+
					     " Time CHAR(50),"+
					     " bitsPerSecondRX BIGINT,"+
					     " bitsPerSecondTx BIGINT,"+
			             " PRIMARY KEY(id))";
			stmt.executeUpdate(sql);
			System.out.println("Created table "+table +"in database");
		 }
	 }
	 

}
