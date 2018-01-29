package org.sdn.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sdn.DB.DBFunction;
import org.sdn.dataType.bandwidth;
import org.sdn.dataType.host;

public class tester {
	private Connection conn;
	private DBFunction db=new DBFunction();
public  bandwidth[] getAlltheHost() throws Exception{
		/**********test store the bandwidth change  the src and dest host start to collect the bandwidth at the same time*************/
		List<host> freehosts=DBFunction.getDeploymentStatus().getFreeHosts();
	    Set<host>  busyhosts=DBFunction.getDeploymentStatus().getBusyHosts().keySet();
	    List<host> hosts=new LinkedList<host>();
	    hosts.addAll(freehosts);
	    hosts.addAll(busyhosts);
	    List<bandwidth> bws=new LinkedList<bandwidth>();
	    for(host h:hosts){
	    String attachpoint=h.getAttachPoint();
	    conn=db.newConnection();
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
   /* 	for(bandwidth bw:portlist){
    		System.out.println("++++++++++portlist "+bw.getDpid()+bw.getPort());
    	}*/
    	return portlist;
/************************************************************************************************************************/
	
}
}
