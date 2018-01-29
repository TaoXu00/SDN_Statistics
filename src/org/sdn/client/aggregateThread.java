package org.sdn.client;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.sdn.DB.DBFunction;
import org.sdn.dataType.Message;
import org.sdn.dataType.msgType;

import com.mysql.jdbc.DatabaseMetaData;
import com.mysql.jdbc.StringUtils;

public class aggregateThread extends Thread {
	  private WebTarget target;
	  private DBFunction db=new DBFunction();
	  private int frequency;
	  private Connection conn;
	  private String prefix="temp_aggregate_";
	  private BlockingQueue<Message> AggregateQueue;
      public aggregateThread(BlockingQueue<Message> AggregateQueue) throws Exception{
      try{
         Client client=ClientBuilder.newClient();
         target=client.target(db.getBaseURI());
         conn= db.newConnection();
         this.AggregateQueue=AggregateQueue;
      }catch(Exception e){
    	 throw new Exception(e);
      }
     }
	 public void getPacketsAggregatePerSwitch() throws JSONException{
	    	Response res;
	        res=target.path("core/switch/all/aggregate/json")
	        		   .request(MediaType.APPLICATION_JSON)
	        		   .get();
	        String s=res.readEntity(String.class);
	        JSONObject jsonobj=new JSONObject(s);
	        Iterator iter=jsonobj.keys();
	        LinkedList<String> switches=new LinkedList<String>();
	        try{
	        Statement stt=conn.createStatement();
	        while(iter.hasNext()){
	        	String dpid=(String)iter.next();
	        	switches.add("'"+dpid+"'");
	        	JSONObject obj1=(JSONObject) jsonobj.get(dpid);
	        	JSONObject obj2=(JSONObject) obj1.get("aggregate");
	        	long flow_count=obj2.getLong("flow_count");
	        	long packet_count=obj2.getLong("packet_count");
	            long diff=0;
	            //write to data base
	            String time=db.getCurrentTime();      	  
	            ResultSet rs=stt.executeQuery("SELECT packet_count FROM StatisticsAggregate WHERE SwitchDpid='"+dpid+"'");
	            	if(!rs.next()){
	            		diff=-0;
	            		stt.execute("INSERT INTO StatisticsAggregate(switchDPID,Time,flow_count,packet_count,PCDifference) VALUES"+
	 				           "('"+dpid+"','"+time+"',"+flow_count+","+packet_count+","+diff+")");
	            	}
	            	else{
	            		diff=packet_count-rs.getInt(1);
	            		stt.execute("UPDATE StatisticsAggregate SET Time='"+time+"',flow_count="+flow_count+",packet_count="+packet_count+",PCDifference="+diff+" WHERE switchDPID='"+dpid+"'");        		
	                 	}		
	       } 
	          // String sql="DELETE FROM StatisticsAggregate WHERE SwitchDpid not in ("+String.join(",",switches)+")";
	        //   stt.execute(sql);
	      }catch(Exception e){
	    	  e.printStackTrace();
	      }
	        
	    }
	 public void getPacketsAggregatePerSwitchCustomrizationMode(String[] switchIDs) throws Exception{
		 for(String switchID:switchIDs){
			 Response res;
		     res=target.path("core/switch")
		    		   .path(switchID)
		    		   .path("aggregate/json")
		        	   .request(MediaType.APPLICATION_JSON)
		        		.get();
		        String s=res.readEntity(String.class); 
		        JSONObject jsonobj=new JSONObject(s);
		        JSONObject obj1=(JSONObject) jsonobj.get("aggregate");
		        long packet_count=obj1.getLong("packet_count");
		        long flow_count=obj1.getLong("flow_count");
		        String time=db.getCurrentTime();
		        String table=prefix+switchID.replaceAll(":","");
		        checkIfTableExist(table);
		        //inseert the data to the database
		        long diff=0;
		        int maxID=0;
		        
		        Statement stt=conn.createStatement();
		        String selectMaxId="SELECT max(id) from "+table;
		        ResultSet idMax=stt.executeQuery(selectMaxId);
		        if(idMax.next())
		        maxID=idMax.getInt(1);
		        if(maxID==0)
		           diff=0;
		        else{ 
		          String selectLastAggregate="SELECT packet_count FROM "+table+" WHERE id="+maxID;
		          ResultSet rs= stt.executeQuery(selectLastAggregate);
		          if(rs.next())
		        	  diff=packet_count-rs.getInt(1);  
		        }
		        stt.execute("INSERT INTO "+table+"(Time,flow_count,packet_count,PCDifference) VALUES"+
				           "('"+time+"',"+flow_count+","+packet_count+","+diff+")");    
		 }
	 
	 }
	
	  
	 public void runAsdefaultMode(){
		 try {
			int defaultFrequency=Integer.parseInt(System.getProperty("aggregateDefaultFrequency"));
			setFrequency(defaultFrequency);
			getPacketsAggregatePerSwitch();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	 }
	 public void setFrequency(int freq){
		 frequency=freq;	 
	 }
	 public void checkIfTableExist(String table) throws SQLException{
		
		 DatabaseMetaData meta=(DatabaseMetaData) conn.getMetaData();
		 ResultSet res=meta.getTables(null,null,table,new String[]{"TABLE"});
		 if(!res.next()){ //create a new table
			 Statement stmt=conn.createStatement();
			 String sql="CREATE TABLE "+table+
					     " (id INT not NULL auto_increment,"+
					     " Time DATETIME,"+
					     " flow_count INT,"+
					     " packet_count INT,"+
					     " PCDifference INT,"+
			             " PRIMARY KEY(id))";
			stmt.executeUpdate(sql);
			System.out.println("Created table "+table +"in database");
			
		 }
	 }
	 
	 public void runAsCustomrizationMode(String[] switches,int freq) throws Exception{
		 //create temp table to store the statistics for each switches
		 setFrequency(freq);
		 getPacketsAggregatePerSwitchCustomrizationMode(switches);
		 
	 }
	
	  public void run(){
		  try {
			int customrizationMode=0;
			String[] switchIDs = null;
			while(true){
				Message msg=AggregateQueue.poll();
				if(msg==null&&customrizationMode==0){					
					runAsdefaultMode();
					//System.out.println("now in aggregate default mode");
				}else if(msg==null&&customrizationMode==1){
              //  System.out.println("still in Aggregate customrizationMode");
				runAsCustomrizationMode(switchIDs,frequency);
				}else{
					msgType msgtype=msg.getMsgtype();
					switch(msgtype){
					case Aggregate:
						customrizationMode=1;
						switchIDs=msg.getDpid();
			            frequency=msg.getFrequency();
			            runAsCustomrizationMode(switchIDs,frequency);
			         //   System.out.println("now start Aggregate customrizationMode,set frequency "+frequency+" ms");
			            break;
					case AggregateReset:
						customrizationMode=0;
						db.deleteTemptable(conn, prefix);
						runAsdefaultMode();	
					//	System.out.println("now reset to Aggregate default mode");
						break;
					}
				}
				Thread.sleep(frequency);
			}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  
			}
	
		
	}
	


