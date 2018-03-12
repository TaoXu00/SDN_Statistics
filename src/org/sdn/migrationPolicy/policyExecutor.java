package org.sdn.migrationPolicy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.DatatypeConverter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sdn.DB.DBFunction;
import org.sdn.client.StatisticClient;
import org.sdn.client.aggregateThread;
import org.sdn.dataType.KeyPair;
import org.sdn.dataType.deploymentStatus;
import org.sdn.dataType.host;
import org.sdn.dataType.hostStatus;
import org.sdn.dataType.link;
import org.sdn.tcpClient.tcpClient;

public class policyExecutor {
	public static final int ONEM=1000000;
	private DBFunction db=new DBFunction(); 
	private tcpClient tclient=new tcpClient();
	private Connection conn=db.newConnection();
	private WebTarget target;
	
	public policyExecutor(){
		 Client client=ClientBuilder.newClient();
         target=client.target(db.getBaseURI());
	}  
	public void excutePolicy(String policy,host srchost,String srcSwitch) throws Exception{
		switch(policy){
		case "random":
			excuteRandomPolicy();
			break;
		case "bandwidth":
			excuteBandwidthPolicy(srchost);
			break;
		case "shortest path":
           excuteShortestPathPolicy(srchost,srcSwitch);
           break;
		
		}
	}
	
	/*private void excuteBandwidthPolicy(host srchost) throws SQLException {
		/***************previous version*********************
	   // TODO Auto-generated method stub
		//select one port that has the smallest bandwidth
		System.out.println("now i am excute Bandwidth policy");
		List<host> freehosts=db.getDeploymentStatus().getFreeHosts();
		List<String> hostIps=new LinkedList<String>();
		Statement stt=conn.createStatement();
		boolean findSrcHost=false;
		host dsthost = null;
		while(!findSrcHost){
	    String sql="SELECT switchDPID,port FROM StatisticsBandwidth WHERE bitsPerSecondRx+bitsPerSecondTx =( SELECT min(bitsPerSecondRx+bitsPerSecondTx) FROM StatisticsBandwidth)";
	    ResultSet rs=stt.executeQuery(spublic static final int 1M=1000000;ql);   
	    while(rs.next()){
	    	String port=rs.getString(1)+"_"+rs.getString(2);
	    	System.out.println("get the miniest port is "+ port);
	    	String dsthostIP=formPortToHostIP(port);
	    	for(host h:freehosts){
	    		if(h.getIP().equals(dsthostIP)){
		    		findSrcHost=true;
		    		dsthost=h;
		    		break;
		    	}
	     	}
	    	
	    }
	    } 
		tclient.sendMigrationCommand(srchost,dsthost);	
	}*/
	
	private void excuteRandomPolicy() {
		try {
		// TODO Auto-generated method stub
		String mode=System.getProperty("secureMode");
     	if(mode.equals("no"))
     		excuteRandomPolicyNoSecure();
     	else if(mode.equals("Digital_Fountain")||mode.equals("Shamir")){
			excuteRandomPolicySecure(mode);
			}
     	}catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 	
     	
	}
	private void excuteRandomPolicyNoSecure() {
		System.out.println("now i am excute Random policy");
		deploymentStatus dStatus=DBFunction.getDeploymentStatus();
		Map<host,hostStatus> busyhostStatus=DBFunction.getDeploymentStatus().getBusyHosts();
		List<host> freeHosts=DBFunction.getDeploymentStatus().getFreeHosts();
		List<host> busyHosts=new LinkedList<host>();
		busyHosts.addAll(busyhostStatus.keySet());
		int srclength=busyHosts.size();
		int dstlength=freeHosts.size();
		int src=db.randomNumGeneration(srclength);
		int dst=db.randomNumGeneration(dstlength);
		host srchost=busyHosts.get(src);
		host desthost=freeHosts.get(dst);
	    String srcIp=srchost.getIP();
		String dstIp=desthost.getIP();
		//while(!(srcIp.equals("10.0.0.2")&&dstIp.equals("10.0.0.3"))){
		while(!(srcIp.equals("10.0.0.2"))){
		src=db.randomNumGeneration(srclength);
        dst=db.randomNumGeneration(dstlength);
        
		srchost=busyHosts.get(src);
        desthost=freeHosts.get(dst);
		srcIp=srchost.getIP();
		dstIp=desthost.getIP();	
		}
		System.out.println("Migration Source:"+srchost.getName()+" IP:"+srcIp);
		System.out.println("Migration Destination:"+desthost.getName()+" IP:"+dstIp);
		tclient.sendMigrationCommand(srchost,desthost);	
	}
	public void excuteRandomPolicySecure(String mode) throws Exception {
		// TODO Auto-generated method stub
		DBFunction db=new DBFunction();
		host sourceHost=selectRandomSourceHost();
		while(!sourceHost.getIP().equals("10.0.0.2")){
			sourceHost=selectRandomSourceHost();
			System.out.println(sourceHost.getIP());
		}
		boolean valid=false;
		int randomNumber = 0;
		int index=0;
		while(!valid){
	    randomNumber=db.randomNumGeneration(Integer.MAX_VALUE/2);
	    int N=DBFunction.gethostSecurityInfo().size();
	    index=applyHashFunction(sourceHost,randomNumber,N);
	    valid=validateIndex(sourceHost,index);
		}
		//System.out.println("source host:"+sourceHost.getIP()+" index:"+index);
		StatisticClient sclient=new StatisticClient();
		List<host> hosts=sclient.selectAllhosts();
		HashMap<String,Integer> lantencyTable=generateLantencyTable(sourceHost,hosts);
	    tcpClient client=new tcpClient();
	    String keyNumber=System.getProperty("keyNumber");
	    //to do find the dstHost
	    String dstIP=DBFunction.gethostSecurityInfo().get(sourceHost.getIP()).getLookupTable().get(index);
	    //System.out.println("dstHost index:"+index+ "  dstIP:"+dstIP);
	    List<host> freeHosts=DBFunction.getDeploymentStatus().getFreeHosts();
	    host dstHost = null;
	    for(host h:freeHosts){
	    	System.out.println("free IP:"+h.getIP());
	    	if(dstIP.equals(h.getIP())){
	    	//if(dstIP.equals("10.0.0.5")){
	    		dstHost=h;
	    		 System.out.println("dstHost index:"+index+ "  dstIP:"+dstHost.getIP());
	    	    break;
	    	}
	    }
	    
	    client.sendMigrationCommandSecure(mode,sourceHost,dstHost,randomNumber,lantencyTable,keyNumber);
	 
	}
	private HashMap<String, Integer> generateLantencyTable(host sourceHost, List<host> hosts) throws Exception {
		// TODO Auto-generated method stub
		HashMap<String,Integer> lantencyTable=new HashMap<String,Integer>();
		String srcSwitch=getswitchDPIDPort(sourceHost.getAttachPoint()).split("_")[0];
		
		for(host h:hosts){
			if(!h.getIP().equals(sourceHost.getIP())){
				//System.out.println("============"+h.getIP()+" "+sourceHost.getIP());
				String dstSwitch=getswitchDPIDPort(h.getAttachPoint()).split("_")[0];
				//System.out.println("============"+srcSwitch+" "+dstSwitch);
				int latency=queryFastPath(srcSwitch,dstSwitch);
				lantencyTable.put(h.getIP(),latency);
			}		
		}
		
		return lantencyTable;
	}
	private boolean validateIndex(host sourceHost,int index) {
		// TODO Auto-generated method stub
		HashMap<Integer,String> lookupTable=DBFunction.gethostSecurityInfo().get(sourceHost.getIP()).getLookupTable();
		List<String> freeHosts=DBFunction.getDeploymentStatus().getFreeHosts().stream().map(h->h.getIP()).collect(Collectors.toList());
	/*	for(Map.Entry<Integer,String> entry:lookupTable.entrySet()){
			System.out.println("entry table");
			System.out.print(entry.getKey()+" "+entry.getValue());
		}*/
		if(!lookupTable.containsKey(index))
		   return false;
		else if(!freeHosts.contains(lookupTable.get(index)))
		   return false;
		else if(!lookupTable.get(index).equals("10.0.0.4"))
		   return false;
		else 
		   return true;
	}
	private int applyHashFunction(host sourceHost, int randomNumber,int N) throws InvalidKeyException, NoSuchAlgorithmException {
		// TODO Auto-generated method stub
		KeyPair kp=DBFunction.gethostSecurityInfo().get(sourceHost.getIP()).getKp();
		String x=Integer.toString(kp.getX());
		String y=Integer.toString(kp.getY());
		String data=x.concat(y);
		String key=Integer.toString(randomNumber);
		String res=generateHMacSHA256(key,data);
		String num1=res.replaceAll("[^0-9]","");
		String num;
		//System.out.println("Hash num:"+num1+" length:"+num1.length());
		if(num1.length()>9)
			num=num1.substring(0,9);
		else
			num=num1;
		//System.out.println("num:"+num);
		int index;
	/*	if(num.length()==0)
			index=0;*/
		index=Integer.parseInt(num)%N;
		return index;
	}
	public String generateHMacSHA256(String key, String data)
			throws InvalidKeyException, NoSuchAlgorithmException {
		Mac hMacSHA256 = Mac.getInstance("HmacSHA256");
		byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
		final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");
		hMacSHA256.init(secretKey);
		byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
		byte[] res = hMacSHA256.doFinal(dataBytes);
		return DatatypeConverter.printBase64Binary(res);
	}
	private host selectRandomSourceHost() {
		// TODO Auto-generated method stub
		deploymentStatus dStatus=DBFunction.getDeploymentStatus();
		Map<host,hostStatus> busyhostStatus=DBFunction.getDeploymentStatus().getBusyHosts();
		List<host> busyHosts=new LinkedList<host>();
		busyHosts.addAll(busyhostStatus.keySet());
		int index=db.randomNumGeneration(busyHosts.size());
	    host h=busyHosts.get(index);
		return h;
	}
	
	
	private void excuteBandwidthPolicy(host srchost) throws SQLException, JSONException {
	// TODO Auto-generated method stub
    String srcAttachPointPort=null;
    String dstAttachPointPort=null;
    Map<host,Integer> freeHostsPathbandwidth=new HashMap<host,Integer>();
	//first find the attachPoint of the srchost	 
	srcAttachPointPort=srchost.getAttachPoint();
	System.out.println("now i am excute Bandwidth policy");
	List<host> freehosts=DBFunction.getDeploymentStatus().getFreeHosts();
	host dsthost = null;
	Map<host,Integer> availablebandwidth=new HashMap<host,Integer>();
	for(host h:freehosts){
		System.out.println("freeHost:"+h.getName());
		dstAttachPointPort=h.getAttachPoint();
		//constract path perform the floodlight API
		List<String> path=getPath(srcAttachPointPort,dstAttachPointPort,srchost.getName(),h.getName());
	/*	System.out.println("path list:");
		for(String node:path)
			System.out.println(node+" ");*/
		int bandwidth=getPathbandwidth(path);
		freeHostsPathbandwidth.put(h,bandwidth);
 	}
	//here need to compute the available bandwidth
	for(Map.Entry<host, Integer> entry:freeHostsPathbandwidth.entrySet()){
		String attachpoint=entry.getKey().getAttachPoint();
		String switchDPIDPort=getswitchDPIDPort(attachpoint);
		String parts[]=switchDPIDPort.split("_");
		String switchDPID=parts[0];
		String port=parts[1];
		//get the value from the database and compute the available 
	    Statement sst=conn.createStatement();
	    String sql="SELECT bitsPerSecondTx FROM StatisticsBandwidth WHERE switchDPID='"+switchDPID+"' AND port='"+port+"'";
	    ResultSet rs=sst.executeQuery(sql);
	    if(rs.next()){
	    	int bwConsumption=rs.getInt(1);
	    	int bwAvailable=entry.getValue()*ONEM-bwConsumption;
	    //	System.out.println("["+entry.getKey().getName()+"]"+":"+bwAvailable);
	    	availablebandwidth.put(entry.getKey(),bwAvailable);
	    }
	}
	Map<host,Integer> sortByPathBandwidth=sortByValueBandwidth(availablebandwidth);
	Map.Entry<host, Integer> entry=sortByPathBandwidth.entrySet().iterator().next();
	dsthost=entry.getKey();
	//getOneFreeHostFromSwitch(shortestSwitch);
	tclient.sendMigrationCommand(srchost,dsthost);	
}
	private void excuteShortestPathPolicy(host srchost,String srcSwitch) throws Exception {
		// TODO Auto-generated method stub
		 Map<String, LinkedList<host>>freeSwitchHosts=selectfreeSwitches();
		 Map<String,Integer> pathLatency=new HashMap<String,Integer>();
		 Set<String> freeSwitches=freeSwitchHosts.keySet();
		 for(String dstSwitch:freeSwitches){
			//send the request to floodlight ask for the shortest path
			int latency=queryFastPath(srcSwitch,dstSwitch);
			pathLatency.put(dstSwitch,latency);
		}
		Map<String,Integer> sortedPath=sortByValueShortestPath(pathLatency);
		Map.Entry<String, Integer> entry=sortedPath.entrySet().iterator().next();
		String shortestSwitch=entry.getKey();
		//getOneFreeHostFromSwitch(shortestSwitch);
		host desthost=freeSwitchHosts.get(shortestSwitch).getFirst();
		tclient.sendMigrationCommand(srchost,desthost);			
	}
	private int getPathbandwidth(List<String> path) {
		// TODO Auto-generated method stub
		int minBandwidth=Integer.MAX_VALUE;
		int bandwidth;
		Map<String,Integer> linkbandwidth=DBFunction.getLinkBandwidth();
		/*System.out.println("linkbandwidth: "+linkbandwidth.size());
		for(String l:linkbandwidth.keySet())
			System.out.println("------------"+l+" bandwidth:"+linkbandwidth.get(l));*/
		
		for(int i=0;i<path.size()-1;i++){
			String Node1=path.get(i);
			String Node2=path.get(i+1);			
			String l=Node1+"-"+Node2;
			if(!linkbandwidth.containsKey(l)){
				String l1=Node2+"-"+Node1;
			    bandwidth=linkbandwidth.get(l1);
			}else
				bandwidth=linkbandwidth.get(l);
			if(bandwidth<minBandwidth)
				minBandwidth=bandwidth;
		}
		
		return minBandwidth;
	}
	private List<String> getPath(String srcAttachPointPort, String dstAttachPointPort,String srcHostname,String dstHostname) throws JSONException, SQLException {
		// TODO Auto-generated method stub
		//TO  DO
		List<String> path=new LinkedList<String>();
		List<String> switchlist=new LinkedList<String>();
		//System.out.println("**************"+srcAttachPointPort+" "+dstAttachPointPort);
		//System.out.println("***************"+getswitchDPIDPort(srcAttachPointPort));
		/*System.out.println( srcAttachPointPort+" "+dstAttachPointPort);
		System.out.println( getswitchDPIDPort(srcAttachPointPort)+" "+getswitchDPIDPort(dstAttachPointPort));*/
		String[] parts1=getswitchDPIDPort(srcAttachPointPort).split("_");
		String[] parts2=getswitchDPIDPort(dstAttachPointPort).split("_");
		String srcdpid=parts1[0];
		String srcport=parts1[1];
		String dstdpid=parts2[0];
		String dstport=parts2[1];
		//System.out.println("++++++"+srcdpid+" "+srcport+" "+dstdpid+" "+dstport);
		Response res;
        res=target.path("routing/path")
        		   .path(srcdpid)
        		   .path(srcport)
        		   .path(dstdpid)
        		   .path(dstport)
        		   .path("json")
        		   .request(MediaType.APPLICATION_JSON)
        		   .get();
        String s=res.readEntity(String.class);
        JSONObject jsonobj=new JSONObject(s);
        JSONArray jsonarray=jsonobj.getJSONArray("results");
        for(int i=0;i<jsonarray.length();i++){
        JSONObject object=jsonarray.getJSONObject(i);
  	    String sw=object.getString("switch");
  	    if(!switchlist.contains(sw))
  	    switchlist.add(sw);       
       }
       path.add(srcHostname);
       for(String sw:switchlist){
    	  // System.out.println(sw);
    	   String node=getswitchName(sw);
    	   path.add(node);
    	 //  System.out.print(" "+node);
       }
       path.add(dstHostname);
       /* for(int i=0;i<jsonarray.length();i++){
              JSONObject object=jsonarray.getJSONObject(i);
        	   String port=object.getString("switch")+"_"+object.getString("port");
        	   System.out.println("Path port list:"+port);
        	   String switchName=getswitchName(port);
        	   path.add(switchName);       
        }*/
        
		return path;
	}
	public String getswitchDPIDPort(String name) throws SQLException{
		String switchDPIDPort = null;
		Statement stt=conn.createStatement();
		String sql="SELECT name FROM mapping WHERE value='"+name+"'";
		ResultSet rs=stt.executeQuery(sql);
		if(rs.next())
			switchDPIDPort=rs.getString(1);	
		return switchDPIDPort;
	}
	private String getswitchName(String sw) throws SQLException {
		// TODO Auto-generated method stub
		String switchName = null;
		Statement stt=conn.createStatement();
		String sql="SELECT value FROM mapping WHERE name LIKE'"+sw+"%'";
		ResultSet rs=stt.executeQuery(sql);
		if(rs.next())
			switchName=rs.getString(1).split("-")[0];				
		return switchName;
	}
	public String findAttachPoint(host srchost) throws SQLException {
		// TODO Auto-generated method stub
		String attachPointDPID=srchost.getAttachPoint();
		if(srchost.getAttachPoint()==null){
			Statement stt=conn.createStatement();
			String sql="SELECT name FROM mapping WHERE value='"+srchost.getName()+"'";
			ResultSet rs=stt.executeQuery(sql);
			if(rs.next())
			   attachPointDPID=rs.getString(1);
			  }
		
		return attachPointDPID;
	}
	
  /*  private String getOneFreeHostFromSwitch(String shortestSwitch) throws Exception {
		// TODO Auto-generated method stub
	   Statement stt=conn.createStatement();
	   String sql="SELECT value FROM mapping WHERE name LIKE '"+shortestSwitch+"%'";
	   ResultSet rs=stt.executeQuery(sql); 
	   List<host> freehosts=db.getDeploymentStatus().getFreeHosts();
	   //here i select the first one of the 
	   while(rs.next()){
		   String port=rs.getString(1);
		   sql="SELECT destIP FROM connections WHERE source='"+port+"'";
		   rs=stt.executeQuery(sql);
		   if(rs.next()){
			   String destIP=rs.getString(1);
			//   for
		    }
	   }	   
	}*/
	private Map<host,Integer> sortByValueBandwidth(Map<host,Integer> unsortMap){
    	List<Map.Entry<host, Integer>> list= new LinkedList<Map.Entry<host,Integer>>(unsortMap.entrySet());
    	Collections.sort(list,new Comparator<Map.Entry<host,Integer>>(){
    		public int compare(Map.Entry<host,Integer> o1, Map.Entry<host, Integer>o2){
    			return (o2.getValue()).compareTo(o1.getValue());
    		}
    	});
    	Map<host,Integer> sortedMap=new LinkedHashMap<host,Integer>();
    	for(Map.Entry<host, Integer> entry:list){
    		sortedMap.put(entry.getKey(),entry.getValue());
    		System.out.println("*****after sorted available bandwidth:"+entry.getKey().getName()+" "+entry.getValue());
    	}
    	return sortedMap;
    }
	private Map<String,Integer> sortByValueShortestPath(Map<String,Integer> unsortMap){
    	List<Map.Entry<String, Integer>> list= new LinkedList<Map.Entry<String,Integer>>(unsortMap.entrySet());
    	Collections.sort(list,new Comparator<Map.Entry<String,Integer>>(){
    		public int compare(Map.Entry<String,Integer> o1, Map.Entry<String, Integer>o2){
    			return (o1.getValue()).compareTo(o2.getValue());
    		}
    	});
    	Map<String,Integer> sortedMap=new LinkedHashMap<String,Integer>();
    	for(Map.Entry<String, Integer> entry:list){
    		sortedMap.put(entry.getKey(),entry.getValue());
    		System.out.println("*****after sorted latency:"+entry.getKey()+" "+entry.getValue());
    	}
    	return sortedMap;
    }
	
  
	private int queryFastPath(String srcSwitch, String dstSwitch) throws JSONException {
		// TODO Auto-generated method stub
		 Response res;
	     res=target.path("routing/paths/fast")
	    		   .path(srcSwitch)
	    		   .path(dstSwitch)
	    		   .path("1/json")
	        	   .request(MediaType.APPLICATION_JSON)
	        	    .get();
	     String s=res.readEntity(String.class); 
	    JSONObject jsonobj=new JSONObject(s);
	    JSONArray pathlist=(JSONArray) jsonobj.get("results");
	    JSONObject path=pathlist.getJSONObject(0);
	    int latency=path.getInt("latency");  
	    return latency;
	}
	
	/*private void excuteRandomPolicy() throws SQLException {
		// TODO Auto-generated method stub
		//randomly select source and destination host
		//first read all the host in the table,and then use random function to select source and destination
	ArrayList<host> hosts=new ArrayList<host>();
	Connection conn=db.newConnection();
	Statement sst=conn.createStatement();
	String sql="SELECT name,value FROM mapping  WHERE type='host'";
	ResultSet rs=sst.executeQuery(sql);
	while(rs.next()){
	 String host=rs.getString("name");
	 String ip=rs.getString("value");
	 host h=new host(host,ip);
	 hosts.add(h);
	}
	int length=hosts.size();
	int src=randomNumGeneration(length);
	int dst=randomNumGeneration(length);
	while(src==dst){
    dst=randomNumGeneration(length);
    }
	host srchost=hosts.get(src);
	host desthost=hosts.get(dst);
	String srcIp=srchost.getIP();
	String dstIp=desthost.getIP();
	String srcIp="10.0.0.1";
	String dstIp="10.0.0.2";
	System.out.println("Migration Source:"+srchost.getName()+" IP:"+srcIp);
	System.out.println("Migration Destination:"+desthost.getName()+" IP:"+dstIp);
	tclient.sendMigrationCommand(srcIp,dstIp);pathLatency=
	} */
	
	private String formPortToHostIP(String port) throws SQLException{
		ResultSet rs;
		String dst = null;
		Statement stt=conn.createStatement();
		String sql="SELECT value FROM mapping WHERE name='"+port+"'";
		rs=stt.executeQuery(sql);
		if(rs.next()){
		String value=rs.getString(1);
		sql="SELECT destIP FROM connections WHERE source='"+value+"'";
		rs=stt.executeQuery(sql);
		if(rs.next()){
			dst=rs.getString(1);
		 }		
		}
		return dst;
	}
	private  Map<String, LinkedList<host>> selectfreeSwitches() throws SQLException {
		// TODO Auto-generated method stub
	  String sql;
	  ResultSet rs;
	  List<host> freeHosts=db.getDeploymentStatus().getFreeHosts();
	  Statement stt=conn.createStatement();
	/*  List<String> freeSwitchports=new LinkedList<String>();
	  List<String> freeSwitches=new LinkedList<String>();
	  for(host h:freeHosts){
		  String ip=h.getIP();
		  sql="SELECT source FROM connections WHERE destIP='"+"ip'";
		  rs=stt.executeQuery(sql); 
		  if(rs.next())
			  freeSwitchports.add(rs.getString(1));  
	  }
	  for(String switchport:freeSwitchports){
		  sql="SELECT name FROM mapping WHERE value='"+switchport+"'";
		  rs=stt.executeQuery(sql);
		  if(rs.next()){
			  String dpidPort=rs.getString(1);
			  String[] parts=dpidPort.split("_");
			  freeSwitches.add(parts[0]);		  
			  }	  
	  }
	  return freeSwitches;*/
	  //the key is switch DPID,and the value is the host list which connected with the switch
	  Map<String, LinkedList<host>> freeSwitchHostMap=new HashMap<String,LinkedList<host>>();
	  for(host h:freeHosts){
		  String hostip=h.getIP();
		  sql="SELECT source FROM connections WHERE destIP='"+hostip+"'";
		  rs=stt.executeQuery(sql); 
		  if(rs.next()){
			  String switchport=rs.getString(1);
			  sql="SELECT name FROM mapping WHERE value='"+switchport+"'";
		      rs=stt.executeQuery(sql);
		      if(rs.next()){
			  String dpidPort=rs.getString(1);
			  String[] parts=dpidPort.split("_");
			  String freeSwitch=parts[0];
			  if(freeSwitchHostMap.containsKey(freeSwitch))
				  freeSwitchHostMap.get(freeSwitch).add(h);
			  else{
				  LinkedList<host> hosts=new LinkedList<host>();
				  hosts.add(h);
				  freeSwitchHostMap.put(freeSwitch,hosts);
			  }
			 }	  
		  }
	   }
	  return freeSwitchHostMap;
	}
	  
	
}
