package org.sdn.client;
import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sdn.DB.DBFunction;
import org.sdn.DB.VIB;
import org.sdn.dataType.KeyPair;
import org.sdn.dataType.Message;
import org.sdn.dataType.SecurityInfo;
import org.sdn.dataType.Time_NPackets;
import org.sdn.dataType.bandwidth;
import org.sdn.dataType.deploymentStatus;
import org.sdn.dataType.host;
import org.sdn.dataType.hostStatus;
import org.sdn.dataType.msgType;
import org.sdn.system.SecurityInfoDistributor;
import org.sdn.system.hostInformationCollector;
import org.sdn.tcpClient.tcpClient;
import org.sdn.test.tester;

import com.google.gson.Gson;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


public class StatisticClient {
	private BlockingQueue<Message> MsgQueue;
	private BlockingQueue<Message> AggregateQueue;
	private BlockingQueue<Message> bandwidthQueue;
	private DBFunction db=new DBFunction();
	private static final int MAX_X=1000;
	public StatisticClient(){
		MsgQueue=new LinkedBlockingQueue<Message>();
		AggregateQueue=new LinkedBlockingQueue<Message>();
		bandwidthQueue=new LinkedBlockingQueue<Message>();
	}
	public void readMsg() throws Exception{
		Message msg=null;
		while((msg=MsgQueue.take())!=null){
		msgType msgtype=msg.getMsgtype();
		switch(msgtype){
		case Aggregate:
			System.out.println("Got customized Message from Aggregate Thread");
		    AggregateQueue.put(msg);	
		    break;
		case Bandwidth:
			System.out.println("Got customized Message of Bandwidth Thread");
			bandwidthQueue.put(msg);
		    break;
		case AggregateReset:
			 System.out.println("Got Message of AggregateReset");
			 AggregateQueue.put(msg);
		case  BandwidthReset:
			 System.out.println("Got Message of BandwidthReset");
			 bandwidthQueue.put(msg);
		}
		}
	}
    public void run(){
    	    try{
    		clearnUP();
        	hostInformationCollector hostinfoCollector=new hostInformationCollector();
          	hostinfoCollector.hostInfoToDatabase();
          	List<host> hosts=selectAllhosts();
         	deployServerOnHost(hosts);
         	generateKeyPair(hosts);
         	generatelookupTable(hosts);
         	distributeKeyPairAndTable(hosts);
        	String policy=System.getProperty("policy");	
          	System.out.println("read from the configuration file:"+policy);
         	if(policy.equals("random")){
    /***************************test to collect the result of the bandwidth***********************************/
          		 (new bandwidthThread(bandwidthQueue)).start();
          		 Thread.sleep(5000);
          	     tester ts=new tester();
          	     (new bandwidthCollectorThread(ts.getAlltheHost(),3000)).start();
          		 (new checkingThread(MsgQueue,policy)).start();
          		 }
          	else{ 
    	    (new aggregateThread(AggregateQueue)).start();
          	(new bandwidthThread(bandwidthQueue)).start();
          	Thread.sleep(5000);
    	    (new checkingThread(MsgQueue,policy)).start();
    	    readMsg();	
    	    }
         /*	String mode=System.getProperty("mode");
         	if(mode.equals("secure"))
         	  runSecureMode();
         	else if(mode.equals("load balancing")){
         		runLoadbanlancingMode(); 
         	}*/
         	
    	    }catch(Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}  
 
    }
   private void distributeKeyPairAndTable(List<host> hosts) throws InterruptedException {
		// TODO Auto-generated method stub
	    List<Thread> threads=new LinkedList<>();
		for(host h:hosts){
		Thread t= new SecurityInfoDistributor(h);	
		threads.add(t);
		t.start();
		}
		for(Thread t:threads)
			t.join();
	}
private void generatelookupTable(List<host> hosts) throws IOException{
		// TODO Auto-generated method stub
	    String lookupTableDir= System.getProperty("lookupTableDir");
		for(host h:hosts){
			HashMap<Integer,String> lookuptable=new HashMap<Integer,String>();
			
			int index;
			//while(size!=0){
			for(host oh:hosts){
			if(!oh.getIP().equals(h.getIP())){
			index=db.randomNumGeneration(hosts.size());
			while(lookuptable.containsKey(index))
			   index=db.randomNumGeneration(hosts.size());	
			lookuptable.put(index,oh.getIP());
			}
			}
		 DBFunction.gethostSecurityInfo().get(h.getIP()).setLookupTable(lookuptable);
		 writeTofile(lookupTableDir,h.getIP(),lookuptable);	 
		}
	}
private void writeTofile(String lookupTabledir,String ip, HashMap<Integer, String> lookuptable) throws IOException {
	// TODO Auto-generated method stub
	String lookupFileName=lookupTabledir+"/"+ip;
	File fout=new File(lookupFileName);
	FileOutputStream fos=new FileOutputStream(fout);
	BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(fos));
	for(Map.Entry<Integer,String> entry: lookuptable.entrySet()){
		String line=entry.getKey()+" "+entry.getValue();
		bw.write(line);
		bw.newLine();
	}
	bw.close();
}
private void generateKeyPair(List<host> hosts) {
		// TODO Auto-generated method stub
	   //generate key value pairs
	    LinkedList<Integer> values=generateRandomNumbers(hosts.size()*2);
		for(host h:hosts){
			int x=values.getFirst();
			int y=values.getLast();
			//System.out.println("x="+x+" y="+y);
			KeyPair kp=new KeyPair(x,y);
			SecurityInfo s=new SecurityInfo();
			s.setKp(kp);
			DBFunction.gethostSecurityInfo().put(h.getIP(),s);
			values.removeFirst();
			values.removeLast();
			} 	
		
	}
private LinkedList<Integer> generateRandomNumbers(int size) {
	// TODO Auto-generated method stub
	LinkedList<Integer> values=new LinkedList<Integer>();
	while(size!=0){
	  int v=db.randomNumGeneration(MAX_X);
	  if(!values.contains(v)){
		  values.add(v);
		  size--;
	  }
	}
	return values;
}
/* private void runSecureMode() {
		// TODO Auto-generated method stub
		DBFunction db=new DBFunction();
	    int randomNumber=db.randomNumGeneration(Integer.MAX_VALUE);
	    host sourceHost=selectRandomSourceHost();
	    tcpClient client=new tcpClient();
	    client.sendMigrationCommandSecure(sourceHost, randomNumber);
	}
	
	private host selectRandomSourceHost() {
		// TODO Auto-generated method stub
		List<host> freehostsList=DBFunction.getDeploymentStatus().getFreeHosts();
		int index=db.randomNumGeneration(freehostsList.size());
	    host h=freehostsList.get(index);
		return h;
	}*/
	private void runLoadbanlancingMode(){
    	try{
    	String policy=System.getProperty("policy");	
      	System.out.println("read from the configuration file:"+policy);
      	if(policy.equals("random")){
/***************************test to collect the result of the bandwidth***********************************/
      		 (new bandwidthThread(bandwidthQueue)).start();
      		 Thread.sleep(5000);
      	     tester ts=new tester();
      	     (new bandwidthCollectorThread(ts.getAlltheHost(),3000)).start();
      		 (new checkingThread(MsgQueue,policy)).start();
      		 }
      	else{ 
	    (new aggregateThread(AggregateQueue)).start();
      	(new bandwidthThread(bandwidthQueue)).start();
      	Thread.sleep(5000);
	    (new checkingThread(MsgQueue,policy)).start();
	    readMsg();	
	    }
    	}catch(Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    }
    private void clearnUP() throws Exception {
		// TODO Auto-generated method stub
    	System.out.println("cleaning database....");
		Connection conn=db.newConnection();
		Statement stt=conn.createStatement();
		String sql="DROP TABLE IF EXISTS temp_bandwidth_0000000000000001P1,temp_bandwidth_0000000000000002P3,temp_bandwidth_0000000000000003P1,temp_bandwidth_0000000000000004P1,temp_bandwidth_0000000000000005P1";
		stt.executeUpdate(sql);
		sql="TRUNCATE TABLE connections";
		stt.executeUpdate(sql);
		sql="TRUNCATE TABLE mapping";
		stt.executeUpdate(sql);
		sql="TRUNCATE TABLE StatisticsAggregate";
		stt.executeUpdate(sql);
		sql="TRUNCATE TABLE StatisticsBandwidth";
		stt.executeUpdate(sql);
		conn.close();
	    
		
	/*	System.out.println("cleaning docker....");
		   String password=System.getProperty("password");
		   Runtime r=Runtime.getRuntime();
		   String[] cmd={"/bin/bash","-c","echo "+password+" | sudo -S docker stop $(docker ps -a -q)"};
		   Process p=r.exec(cmd);
		   p.waitFor();
		   Runtime r1=Runtime.getRuntime();
		   String[] cmd1={"/bin/bash","-c","echo "+password+" | sudo -S docker rm $(docker ps -a -q)"};
		   Process p1=r1.exec(cmd1);
		   p1.waitFor();
		  System.out.println("killing iperf....");
		  Runtime r2=Runtime.getRuntime();
		   String[] cmd2={"/bin/bash","-c","echo "+password+" | sudo -S pkill iperf"};
		   Process p2=r2.exec(cmd2);
		   p2.waitFor();*/
	}
	public String readPolicy(){ //read from the system configuration file
		 String policy=System.getProperty("policy");
		 return policy;	
	}
    public void deployServerOnHost(List<host> hosts) throws SQLException{
     String ratioS=System.getProperty("serverdeployRatio");
     double ratio=Double.parseDouble(ratioS);
     List<host> selectedhosts=selectHostRunserverRandomly(hosts,ratio);
     sendStartCommandToSelectedhosts(selectedhosts);
  /*   for(host h:selectedhosts)
    	 System.out.println("host "+h.getIP()+" will run the container");*/
     initializeDepolymentStatus(selectedhosts,hosts);
    }
    public void initializeDepolymentStatus(List<host> selectedhosts,List<host> hosts){
    	deploymentStatus deploymentS=DBFunction.getDeploymentStatus();
        String image=System.getProperty("image") ;
        String container=System.getProperty("container");
    	for(host h:selectedhosts){
    		hostStatus hStatus=new hostStatus();
    		String containerName=h.getIP()+"-"+container;
    		hStatus.getContainers().put(image, containerName);
    		DBFunction.getDeploymentStatus().getBusyHosts().put(h, hStatus);
    	}
    	for(host h:hosts){
    		if(!selectedhosts.contains(h))
    			DBFunction.getDeploymentStatus().getFreeHosts().add(h);
    	}
    }
    
    public void  sendStartCommandToSelectedhosts(List<host> selectedhosts){
    	String image=System.getProperty("image");
    	String container=System.getProperty("container");
    	for(host h:selectedhosts){
    	String IP=h.getIP();
    	tcpClient client=new tcpClient();
    	client.sendStartContainer(IP, image, container);
    	}
    	
    	
    }
    public List<host> selectHostRunserverRandomly(List<host> hosts,double ratio){
    	int total=hosts.size();
    	int N=(int) (total*ratio);
    	ArrayList<Integer> intlist=new ArrayList<Integer>();
    	List<host> Selectedhosts=new LinkedList<host>();
        while(intlist.size()!=N){
        	Random rand=new Random();
        	rand.setSeed(System.currentTimeMillis());
        //	rand.setSeed(Integer.parseInt(System.getProperty("RandomGeneratorSeed")));
        	int n=rand.nextInt(N);
        	if(!intlist.contains(n))
        		intlist.add(n);	
        }
        
        for(Integer i:intlist)
         Selectedhosts.add(hosts.get(i));      
    	return 	Selectedhosts;
    	} 
    public List<host> selectAllhosts() throws SQLException{
    	  List<host> hosts=new LinkedList<host>();
    	  Connection conn=db.newConnection();
    	  Statement sst=conn.createStatement();
    	  String sql="SELECT * FROM mapping WHERE type='host'";
    	  ResultSet rs=sst.executeQuery(sql);
    	  while(rs.next()){
    		  String name=rs.getString("name");
    		  String ip=rs.getString("value");	 
    		  sql="SELECT source FROM connections WHERE destination='"+name+"-eth1'";
    		  Statement sst1=conn.createStatement();
    		  ResultSet rs1=sst1.executeQuery(sql);
    		  if(rs1.next()){
    			 String attachPoint=rs1.getString(1);
    		     host h=new host(name,ip);
    		     h.setAttachPoint(attachPoint);
    		     hosts.add(h);	 
    		  } 
    	  }
    	  return hosts;
    }
}
