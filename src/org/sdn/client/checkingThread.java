package org.sdn.client;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.sdn.DB.DBFunction;
import org.sdn.dataType.Message;
import org.sdn.dataType.bandwidth;
import org.sdn.dataType.host;
import org.sdn.dataType.msgType;
import org.sdn.migrationPolicy.policyExecutor;
import org.sdn.tcpClient.tcpClient;

public class checkingThread extends Thread{
	private BlockingQueue<Message> MsgQueue;
	private String policy;
	private int checkingFrequency;
	private Connection conn;
	private DBFunction db=new DBFunction();
    public checkingThread(BlockingQueue<Message> q,String policy){
    	this.MsgQueue=q;
    	this.policy=policy;
    	this.checkingFrequency=Integer.parseInt(System.getProperty("checkingFrequency"));
    	this.conn=db.newConnection();
    } 
  /*  public void setPolicy(String policy){
    	this.policy=policy;
    }*/
	@Override
	public void run() {
		// TODO Auto-generated method stub
		//the block that where do the analysis
		try {
/********************test*************************************************************************/
		resultsCollector();
		//for random sleep
		Thread.sleep(3000);
		startIperfClient("10.0.0.1","10.0.0.2");
		//for random sleep
		Thread.sleep(3000);
/*************************************************************************************************/
		boolean checking=true;
		  while(checking){ 
/******************policy start execute time********************************************************/
			  long unixPolicyStartTime=System.currentTimeMillis();
			  System.out.println(policy+" "+db.getCurrentTime());
/**************************************************************************************************/
			policyExecutor pExecutor=new policyExecutor();
			if(policy.equals("random")){
				checking=false;
				pExecutor.excutePolicy(policy,null,null);
				}
			else{
		    String selectedswitch=null;
		    host srchost=null;
			selectedswitch = selectMigrationSrcHost();
		    if(selectedswitch!=null){
		   // System.out.println("selected switch "+selectedswitch);
		    srchost= selectSrcHost(selectedswitch);
		    System.out.println("selected srcIP is "+srchost.getIP());
		    checking =false;
			pExecutor.excutePolicy(policy,srchost,selectedswitch);
			//wait for the policy excute successfully;
		    }
			}
				sleep(checkingFrequency);
			}
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	//	}
		//then send the msg
	/*	try {
			produceMsg();
			System.out.println("Aggregate Message produced");
			sleep(30000);
	    	
	        Message msg3=new Message();
	    	msg3.setMsgtype(msgType.BandwidthReset);
	        MsgQueue.put(msg3);
	        System.out.println("BandwidthReset Message produced");
	        
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	private void startIperfClient(String ClientIP,String ServerIP) {
	// TODO Auto-generated method stub
	   tcpClient tclient=new tcpClient();
	   tclient.sendStartIperfClient(ClientIP,ServerIP);
	   System.out.println("*************Iperf Client "+ClientIP+ "Iperf server:"+ServerIP+" started: "+db.getCurrentTime());
}
	public void resultsCollector() throws Exception{
		/**********test store the bandwidth change  the src and dest host start to collect the bandwidth at the same time*************/
		List<host> freehosts=DBFunction.getDeploymentStatus().getFreeHosts();
	    Set<host>  busyhosts=DBFunction.getDeploymentStatus().getBusyHosts().keySet();
	    List<host> hosts=new LinkedList<host>();
	    hosts.addAll(freehosts);
	    hosts.addAll(busyhosts);
	    List<bandwidth> bws=new LinkedList<bandwidth>();
	    for(host h:hosts){
	    String attachpoint=h.getAttachPoint();
	    Statement stt=conn.createStatement();
	    String sql="SELECT name FROM mapping WHERE value='"+attachpoint+"'";
	    ResultSet rs=stt.executeQuery(sql);
	    if(rs.next()){
	    	String[] switchDPIDPort=rs.getString(1).split("_");
	    	String switchDPID=switchDPIDPort[0];
	    	String port=switchDPIDPort[1];
	    	bandwidth bw=new bandwidth();
	    	bw.setDpid(switchDPID);
	    	bw.setPort(port);
	    	bws.add(bw);	
	    }
	    }
	    bandwidth[] portlist=new bandwidth[bws.size()];  
    	bws.toArray(portlist);
    /*	for(bandwidth bw:portlist){
    		System.out.println("++++++++++portlist "+bw.getDpid()+bw.getPort());
    	}*/
    	produceCostimizedBandwidthMsg(portlist);
/************************************************************************************************************************/
	}
    public void produceCostimizedAggregateMsg(List<String> busyHeavySwitchs) throws InterruptedException{
    	int CostomizedAggregateFreq=Integer.parseInt(System.getProperty("aggregateCustomizeFrequency"));
    	String[] dpids=new String[busyHeavySwitchs.size()];
    	busyHeavySwitchs.toArray(dpids);
    	Message msg=new Message();
    	msg.setMsgtype(msgType.Aggregate);
    	msg.setFrequency(CostomizedAggregateFreq);
    	msg.setDpid(dpids);
    	MsgQueue.put(msg);
    }
    public void produceCostimizedBandwidthMsg(bandwidth[] portlist) throws InterruptedException{
    	Message msg2=new Message();
    	msg2.setMsgtype(msgType.Bandwidth);
    	msg2.setFrequency(Integer.parseInt(System.getProperty("bandwidtCustomizeFrequency")));
    	msg2.setPortlist(portlist);
    	MsgQueue.put(msg2);
    }
    public void produceAggregareResetMsg() throws Exception{
    	Message msg=new Message();
    	msg.setMsgtype(msgType.AggregateReset);
        MsgQueue.put(msg);
     //   System.out.println("AggregateReset Message produced");
    }

	private host selectSrcHost(String selectedswitch) throws SQLException {
		// TODO Auto-generated method stub
		String src = null;
		Set<host> busyhosts=db.getDeploymentStatus().getBusyHosts().keySet();
		Statement stt=conn.createStatement();
		String sql="SELECT switchDPID,port,max(bitsPerSecondRx+bitsPerSecondTx) FROM StatisticsBandwidth WHERE switchDPID='"+selectedswitch+"'";
		ResultSet rs=stt.executeQuery(sql);
		host srchost = null;
		if(rs.next()){
			String port=rs.getString(1)+"_"+rs.getString(2);
		//	System.out.println("++++select busy port: "+port);
			sql="SELECT value FROM mapping WHERE name='"+port+"'";
			rs=stt.executeQuery(sql);
			if(rs.next()){
			String value=rs.getString(1);
			sql="SELECT destIP FROM connections WHERE source='"+value+"'";
			rs=stt.executeQuery(sql);
			if(rs.next()){
				src=rs.getString(1);
				for(host h:busyhosts){
					if(h.getIP().equals(src)){
					  srchost=h;
					  break;
				    }
				}	
			  }		
			}
		}
		stt.close();
		return srchost;
	}
	private String selectMigrationSrcHost() throws Exception{
		// TODO Auto-generated method stub
		Set<host> busyhosts=DBFunction.getDeploymentStatus().getBusyHosts().keySet();
		HashMap<String, String> switchs=new HashMap<String,String>();
		List<String> busyswitches=new LinkedList<String>();   //the collection of the busy switch
		String selectedSwitch=null;
		//find the connected switch
		 Statement stt=conn.createStatement();
		 String sql="SELECT t1.destIP,t2.name FROM connections AS t1,mapping as t2 WHERE t1.source=t2.value";
		 ResultSet rs=stt.executeQuery(sql);
		 while(rs.next()){
			String[] parts=rs.getString(2).split("_");
			switchs.put(rs.getString(1),parts[0]);
		 }
		
		 for(host h:busyhosts){
		 String ip=h.getIP();
		 busyswitches.add(switchs.get(ip));
		// System.out.println("busyswitches the switch ID is "+switchs.get(ip));
		}
		/* for(String s:busyswitches)
		 System.out.println("busySwitches:"+s);*/
		//then find the switches which are busy and also having the the aggregate difference greater than the threshold
		List<String> HeavyBusySwitchs=new LinkedList<String>();
        int aggregateThreshold=Integer.parseInt(System.getProperty("aggregateThreshould"));
        int aggregateDifferenceThreshold=Integer.parseInt(System.getProperty("aggregateDifferenceTreshould"));
        boolean existHeavyBusySwitch=false;
        sql="SELECT switchDPID FROM StatisticsAggregate WHERE packet_count >"+aggregateThreshold+" AND PCDifference >"+aggregateDifferenceThreshold;
        rs=stt.executeQuery(sql);
        System.out.println("now i am checking the source host");
     //   System.out.println("the size of busyswitches is "+busyswitches.size());
        while(rs.next()){
        String sID=rs.getString(1);
        if(busyswitches.contains(sID)){
        	HeavyBusySwitchs.add(sID);
        	existHeavyBusySwitch=true;
        	System.out.println("select "+sID+" as HeavyBusySwitch");
        }  
        }
        //enter the customized mode and select one which is continuous high aggregate rite then select only one host,
        //after finishing the analysis, will reset the to default mode.
        if(existHeavyBusySwitch){     	
        	produceCostimizedAggregateMsg(HeavyBusySwitchs);
        	int MonitorTime=Integer.parseInt((System.getProperty("costomizedMornitorTime")));
        	HashMap<String, Float> percentageList=new HashMap<String,Float>();
         	sleep(MonitorTime);
        	//from the database analysis the data of the customized mode for each table
        	String prefix="temp_aggregate_";
        	for(String switchID:HeavyBusySwitchs){
        		int tot=0;
        		int diffGreaterThanZero=0;
        		String table=prefix+switchID.replaceAll(":","");
        		sql="SELECT * FROM "+table;
        	    rs=stt.executeQuery(sql);
        	    while(rs.next()){
        	       if(rs.getInt(4)>0)
        	    	   diffGreaterThanZero++; 
        	       tot++;   
        	    }
        	    percentageList.put(switchID,(float)diffGreaterThanZero/(float)tot);
        	}
        	Map.Entry<String,Float> maxEntry=null;
        	for(Map.Entry<String, Float> entry: percentageList.entrySet()){
        		if(maxEntry==null || entry.getValue().compareTo(maxEntry.getValue())>0)
        			maxEntry=entry;
        	}
        	selectedSwitch=maxEntry.getKey(); 
        	produceAggregareResetMsg();
        }
        stt.close();
        return selectedSwitch;   	
	}
}
