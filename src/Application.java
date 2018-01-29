import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Map;

import org.sdn.DB.VIB;
import org.sdn.client.StatisticClient;
import org.sdn.migrationPolicy.policyExecutor;
import org.sdn.system.hostInformationCollector;
import org.sdn.system.sshMonitor;
import org.sdn.system.systemInitialize;
import org.sdn.tcpClient.tcpClient;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;


public class Application {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub                                                                                                                                                                                            
	     systemInitialize.start();
	     Thread.sleep(5000);	
	    /* policyExecutor pex=new policyExecutor();
	     pex.excuteRandomPolicySecure("Shamir");*/
         StatisticClient client= new StatisticClient();
         client.run();
         
        /*--------------task of thread one to write to the database------------*
       
		/*--------------task of thread two make the decision--------------*/
		// System.out.println(VIB.getPacketsAggregatePerSwitchFull().size());
	   /*String srcIP="10.0.0.1";
		String dstIP="10.0.0.3";
	   tcpClient tclient=new tcpClient();
	   tclient.sendMigrationCommand(srcIP,dstIP);*/
		
	}

}