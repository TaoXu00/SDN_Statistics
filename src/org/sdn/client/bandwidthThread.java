package org.sdn.client;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sdn.DB.DBFunction;
import org.sdn.dataType.Message;
import org.sdn.dataType.bandwidth;
import org.sdn.dataType.msgType;

import com.mysql.jdbc.DatabaseMetaData;
public class bandwidthThread extends Thread {
	private WebTarget target;
	private DBFunction db=new DBFunction();
	private Connection conn=db.newConnection(); 
	private int frequency;
	private String prefix="temp_bandwidth_";
	private BlockingQueue<Message> bandwidthQueue;
	public bandwidthThread(BlockingQueue<Message> bw) throws Exception{
     try{
     Client client=ClientBuilder.newClient();
     target=client.target(db.getBaseURI());
     this.bandwidthQueue = bw;
     }catch(Exception e){
    	 throw new Exception(e);
     }
    }
    
    public Map<String,LinkedList<String>> getSwitchPortPair() throws JSONException{
	      Response res;
	      res=target.path("core/switch/all/port-desc/json")
	           //.request(MediaType.APPLICATION_XML)
	           .request(MediaType.APPLICATION_JSON)
	           .get();
	        String s=res.readEntity(String.class);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
	        JSONObject jsonobj=new JSONObject(s);
	        Map<String,LinkedList<String>> SwitchPortsPair=new HashMap<String,LinkedList<String>>();
	        Iterator iter=jsonobj.keys();
	        try{
	        while(iter.hasNext()){
	        	String dpid=(String)iter.next();
	        	JSONObject obj1=jsonobj.getJSONObject(dpid);
	        	JSONArray  portlist=(JSONArray) obj1.getJSONArray("port_desc");
	        	LinkedList<String> ports=new LinkedList<String>();
	        	//find the port list which connect to the host
	        	Statement sst=conn.createStatement();
	        	ResultSet rs=sst.executeQuery("SELECT source FROM connections WHERE dstType='"+"host'");
	        	LinkedList<String> portConnectHostlist=new LinkedList<String>();
	        	while(rs.next()){
	        		portConnectHostlist.add(rs.getString(1));
	        	}
	        	for(int i=0;i<portlist.length();i++) {
	        		JSONObject objects=(JSONObject) portlist.get(i); 		
	        		String portNumber=(String) objects.get("port_number");
	        		if(!portNumber.equals("local")){
	        	    String value=objects.getString("name");
	        	   //add port number mapping
	        		String name=dpid+"_"+portNumber;
	        		ResultSet rs1=sst.executeQuery("SELECT * FROM mapping WHERE name='"+name+"' AND value='"+value+"'");
	        		if(!rs1.next()){
	        		sst.execute("INSERT INTO mapping VALUES"+
	        		"('"+name+"','port','"+value+"')"
	        	   );
	        	   }
	        	   if(portConnectHostlist.contains(value))
	        		ports.add(portNumber);
	        		
	        	 }
	        	}
	        	SwitchPortsPair.put(dpid,ports);       	
	         }
	        
	        }catch(Exception e){
	        	e.printStackTrace();
	        }
	         return SwitchPortsPair;  
	    }
    
	public void getBandwidthPerSwitchPerPort() throws JSONException{
		 try{
		       Map<String,LinkedList<String>> SwitchPortPairMap=getSwitchPortPair();
		       Statement stt=  conn.createStatement();
		       String sql="TRUNCATE StatisticsBandwidth";
		       stt.executeUpdate(sql); 
		    	for(String dpid:SwitchPortPairMap.keySet()){
		    		List<String> ports=SwitchPortPairMap.get(dpid);
		    		for(String port:ports){ 
		    			Response res;
		    	        res=target.path("statistics/bandwidth")
		    	        		   .path(dpid)
		    	        		   .path(port)
		    	        		   .path("json")
		    	        		   .request(MediaType.APPLICATION_JSON)
		    	        		   .get();	       
		    	        String s=res.readEntity(String.class);
		    	        JSONArray jsonarray=new JSONArray(s);
		    	        if(!jsonarray.get(0).equals(null)){
		    	        JSONObject jobj=(JSONObject) jsonarray.get(0);
		    	        long bitsPerSecondsRx=jobj.getLong("bits-per-second-rx");
		    	        long BitsPerSecondTx=jobj.getLong("bits-per-second-tx");
		    	       // int linkSpeedBitsPerSecond=jobj.getInt("link-speed-bits-per-second");
		    	       // int linkSpeedBitsPerSecond=getlinkBandwidth(dpid,port);
		                String time=db.getCurrentTime();
		                stt.execute("INSERT INTO StatisticsBandwidth VALUES"+
		    				    "('"+dpid+"','"+port+"','"+time+"',"+bitsPerSecondsRx+","+BitsPerSecondTx+")" );
						/*ResultSet rs=stt.executeQuery("SELECT * FROM StatisticsBandwidth WHERE switchDPID='"+dpid+ "' AND port='"+port+"'");
						
						if(!rs.next())
							stt.execute("INSERT INTO StatisticsBandwidth VALUES"+
						    "('"+dpid+"','"+port+"','"+time+"',"+linkSpeedBitsPerSecond+","+bitsPerSecondsRx+","+BitsPerSecondTx+")" );
						else 
							stt.execute("UPDATE StatisticsBandwidth SET Time='"+time+"',"+"linkSpeedBitsPerSecond="+linkSpeedBitsPerSecond+",bitsPerSecondRX="+bitsPerSecondsRx+",bitsPerSecondTx="+BitsPerSecondTx+" WHERE switchDPID='"+dpid+ "' AND port='"+port+"'");
		               */
		    	        
		    	         }
		    	      }
		    	    } 
		          }catch (Exception e) {
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
					     " bitsPerSecondRX INT,"+
					     " bitsPerSecondTx INT,"+
			             " PRIMARY KEY(id))";
			stmt.executeUpdate(sql);
		//	System.out.println("Created table "+table +"in database");
			
		 }
	 }
    public void getBandwidthPerSwitchPerPortCustomrizationMode(bandwidth[] portlist){
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
    	    	        if(jsonarray.get(0)!=null);
    	    	        JSONObject jobj=(JSONObject) jsonarray.get(0);
    	    	        int bitsPerSecondsRx=jobj.getInt("bits-per-second-rx");
    	    	        int BitsPerSecondTx=jobj.getInt("bits-per-second-tx");
    	                String time=db.getCurrentTime();
    	            	String table=prefix+dpid.replace(":","")+"P"+portNum;
    	            	checkIfTableExist(table);
    	            	Statement stt=  conn.createStatement();
    				    stt.execute("INSERT INTO "+table+"(Time,bitsPerSecondRX,bitsPerSecondTx) VALUES"+
    					    "('"+time+"',"+bitsPerSecondsRx+","+BitsPerSecondTx+")" );
    	    	     }
    			 }catch(Exception e) {
   				// TODO Auto-generated catch block
   				e.printStackTrace();
   			    }
    }	    	
    
    
	 public void setFrequency(int freq){
		    frequency=freq;	 
	 }
	public void runAsdefaultMode() throws Exception{
		     int frequency=Integer.parseInt(System.getProperty("bandwidthDefaultFrequency"));
			 setFrequency(frequency);
			 getBandwidthPerSwitchPerPort();
	}
	public void runBandwidthAsCustomrizationMode(bandwidth[] portlist,int freq){
		this.frequency=freq;
		getBandwidthPerSwitchPerPortCustomrizationMode(portlist);
	}
	public bandwidth[] generatePortList(){
		bandwidth port1=new bandwidth();
		port1.setDpid("00:00:00:00:00:00:00:01");
		port1.setPort("1");
		bandwidth port2=new bandwidth();
		port2.setDpid("00:00:00:00:00:00:00:02");
		port2.setPort("1");
		bandwidth[] portlist={port1,port2};
		return portlist;
		
	}
	public void run(){
		try {
			int customrizationMode=0;
			bandwidth[] portlists = null;
			while(true){
			  Message msg=bandwidthQueue.poll();
			  if(msg==null&&customrizationMode==0){
		        runAsdefaultMode();
		        System.out.println("now in bandwidth default mode");
			  }else if(msg==null&&customrizationMode==1){
			 //  System.out.println("still in bandwidth customrizationMode");
			   runBandwidthAsCustomrizationMode(portlists,frequency);
			  }else{
				  msgType msgtype=msg.getMsgtype();
				  switch(msgtype){
				  case Bandwidth:
					  customrizationMode=1;
					  portlists=msg.getPortlist();
					  frequency=msg.getFrequency();
					  System.out.println("now start bandwidth customrizationMode,set frequency "+frequency+" ms");
					  runBandwidthAsCustomrizationMode(portlists,frequency);  
					  break;
				  case BandwidthReset:
					  customrizationMode=0;
					  db.deleteTemptable(conn, prefix);
					  runAsdefaultMode();
					  System.out.println("now reset to bandwidth default mode");
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
