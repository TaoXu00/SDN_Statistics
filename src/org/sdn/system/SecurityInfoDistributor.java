package org.sdn.system;

import java.io.IOException;

import org.sdn.DB.DBFunction;
import org.sdn.dataType.KeyPair;
import org.sdn.dataType.host;
import org.sdn.tcpClient.tcpClient;

public class SecurityInfoDistributor extends Thread{
	host h;
	public SecurityInfoDistributor(host h){
		this.h=h;	
	}
	public void run(){
    try {
	  String IP=h.getIP();
	  tcpClient client=new tcpClient();
	  KeyPair kp=DBFunction.gethostSecurityInfo().get(IP).getKp();
      client.sendSecureInfo(IP,kp);
      
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}

}
