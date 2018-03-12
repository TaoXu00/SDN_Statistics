package org.sdn.tcpClient;
import java.net.*;
import java.util.HashMap;

import org.sdn.DB.DBFunction;
import org.sdn.dataType.KeyPair;
import org.sdn.dataType.SecurityInfo;
import org.sdn.dataType.host;
import org.sdn.dataType.hostStatus;
import org.sdn.migrationPolicy.policyExecutor;

import java.io.*;
public class tcpClient {
	 private Socket clientSocket;
	 private OutputStream out;
	 private InputStream in;
	 private DBFunction db=new DBFunction();
	 private long unixMigrationStartTime;
	 private long unixMigrationSuccessTime;
	 private long migrationTime;
	 
	 public void sendStartIperfClient(String ClientIP,String ServerIP){
		 try{
		 startConnection(ClientIP);
		 String Command="IperfClient "+ServerIP+"\n"; 
		 sendMessage(Command);
		 System.out.println("send "+Command);
		// receiveMessage();
		 stopConnection();
		 } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			 }	 
	 }
	 public void sendStartContainer(String IP,String image,String container) {
	  try {
		 
		 startConnection(IP);
		 String Command="START "+image+" "+IP+"-"+container+"\n";
		 sendMessage(Command);
		 System.out.println("send "+Command+" to "+IP);
		 receiveMessage();
		 stopConnection();
		 } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }	 
	 }
	 public void sendMigrationCommandSecure(String mode, host migrationSrcHost,host migrationDstHost,int randomNumber, HashMap<String,Integer> lantencyTable,String KeyNumber){
		 try {
		 String MigrationSrcIP=migrationSrcHost.getIP();
		 startConnection(MigrationSrcIP);
		 int session_id=DBFunction.getSessionID();
		 String Command="SECURE_MIGRATE "+mode+" "+randomNumber+" "+KeyNumber+" "+migrationSrcHost.getIP()+" sessionID:"+session_id+"\n";
	     sendMessage(Command);
	     unixMigrationStartTime=System.currentTimeMillis();
	     System.out.println("send msg:"+Command+" "+db.getCurrentTime());
	     ObjectOutputStream objOut=new ObjectOutputStream(out);
	     objOut.writeObject(lantencyTable);
		 //host migrationDstHost=getDstHost(randomNumber);
		 receiveMsg(migrationSrcHost,migrationDstHost);
		 } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	 }
	public void sendMigrationCommand(host migrationSrcHost,host migrationDstHost){
    	try {
    
    	 String MigrationSrcIP=migrationSrcHost.getIP();
    	 String TargetHostIP=migrationDstHost.getIP();
        /* String MigrationSrcIP="10.0.0.2";
         String TargetHostIP="10.0.0.3";*/
    	 startConnection(MigrationSrcIP);
    	 int session_Id=DBFunction.getSessionID();
         String Command="MIGRATE "+MigrationSrcIP+" "+TargetHostIP+" sessionID:"+session_Id+"\n";
		 sendMessage(Command);
 /******************************************************************************************/
		 unixMigrationStartTime=System.currentTimeMillis();
		 System.out.println("send msg:"+Command+" "+db.getCurrentTime());
 /******************************************************************************************/
         receiveMsg(migrationSrcHost,migrationDstHost);
		 } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }
        
      }
     private void receiveMsg(host migrationSrcHost,host migrationDstHost){
       try {
		 String res;
	     res = receiveMessage();		
		 stopConnection();
		 if(res.equals("SUCCESS")){
		   unixMigrationSuccessTime=System.currentTimeMillis();
		   updateDeploymentStatus(migrationSrcHost,migrationDstHost);
 /******************************* change server to 10.0.0.5***************************************************************/ 
		   Thread.sleep(1000);
		   migrationTime= (unixMigrationSuccessTime- unixMigrationStartTime)/1000;
		   System.out.println("Migration time :"+migrationTime);
		   sendStartIperfClient("10.0.0.1",migrationDstHost.getIP());
  		   System.out.println("*************Iperf Client 10.0.0.1"+ "Iperf server:"+ migrationDstHost.getIP()+" started: "+db.getCurrentTime());
  		   Thread.sleep(100000);
  		   policyExecutor p=new policyExecutor();
  		   String src=p.getswitchDPIDPort(migrationSrcHost.getAttachPoint());
  		   String dst=p.getswitchDPIDPort(migrationDstHost.getAttachPoint());	   
  		   doPlot(getTable(src),getTable(dst),migrationSrcHost,migrationDstHost,migrationTime);
		   }
       } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
     }
     private void doPlot(String srctable, String dsttable, host migrationSrcHost, host migrationDstHost, long migrationTime) throws IOException {
		// TODO Auto-generated method stub
    	 System.out.println("migrationSource Port:"+srctable+" "+dsttable+" "+migrationSrcHost.getIP()+" "+migrationDstHost.getIP()+" "+migrationTime);
    	 Runtime r=Runtime.getRuntime(); 
 		 String[] cmd={"python", "/home/xu/thesis_file/plot/plot.py", srctable, dsttable, migrationSrcHost.getIP(), migrationDstHost.getIP(), String.valueOf(migrationTime)};
 		 System.out.println("plot information");
 		 System.out.println(srctable+" "+dsttable+" "+migrationSrcHost.getIP()+" "+migrationDstHost.getIP()+" "+String.valueOf(migrationTime));
 		 Process P=r.exec(cmd);
 		 BufferedReader stdInput=new BufferedReader(new InputStreamReader(P.getInputStream()));
 		 BufferedReader stdError=new BufferedReader(new InputStreamReader(P.getErrorStream()));
	     String s=null;
	   //  System.out.println("standard output:");
	     while((s=stdInput.readLine())!=null)
	    	 System.out.println(s);
	    // System.out.println("standard Error:");
	     while((s=stdError.readLine())!=null){
	    	 System.out.println(s);
	     }
	    
     
     }
	public void updateDeploymentStatus(host migrationSrcHost,host migrationDstHost){
    	 //move the SrcHost out of the busyHostMap,move the DestHost in the busyMap
    	 //move the DstHost out if the freeHostList,move the SrcHost in the freeMap
    	 DBFunction.getDeploymentStatus().getBusyHosts().remove(migrationSrcHost);
    	 DBFunction.getDeploymentStatus().getFreeHosts().add(migrationSrcHost);
         hostStatus hStatus=new hostStatus();
         hStatus.getContainers().put(System.getProperty("image"),migrationDstHost.getIP()+"-"+System.getProperty("container"));
    	 DBFunction.getDeploymentStatus().getBusyHosts().put(migrationDstHost, hStatus);
    	 DBFunction.getDeploymentStatus().getFreeHosts().remove(migrationDstHost);
     }
      public void startConnection(String ip){
    	  try {
    		int port=Integer.parseInt(System.getProperty("port"));
			clientSocket=new Socket(ip,port);
			out=clientSocket.getOutputStream();
			in=clientSocket.getInputStream();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
      }
      public void sendObject(SecurityInfo s) throws IOException{
    	  ObjectOutputStream outputObjStream = new  ObjectOutputStream(out);
    	  outputObjStream.writeObject(s);
    	  outputObjStream.flush();
      }
      public void sendMessage(String msg) throws IOException{
    	  DataOutputStream dos=new DataOutputStream(out);
    	  dos.writeBytes(msg);
    	  dos.flush();
      }
      public String receiveMessage() throws IOException{
    	 @SuppressWarnings("deprecation")
    	 DataInputStream dis=new DataInputStream(clientSocket.getInputStream()); 
		 String response= dis.readLine();
		 int parts=response.split(" ").length;
		 String res;
		 if(parts>=2)
			 res=response.split(" ")[1];
		 else 
			 res=response;
		 String session_Id=response.split(" ")[0];
		 System.out.println(response);
    	 if(res.equals("SUCCESS")){
    		 unixMigrationSuccessTime=System.currentTimeMillis();
    		 System.out.println(session_Id+" Migration execute successfully!!!!!!!!!!!!  "+db.getCurrentTime());

    	 }
    	 else if(res.equals("Error")){
    		 System.out.println(session_Id+" Migration execute error with error code "+dis.readInt());
    		 }
    	 else if(res.equals("OK")){
    		 System.out.println("Start Container OK");
    	 }else if(res.equals("Container Start Error"))
    		 System.out.println("Start Container with Error");
    	 return  res;
      }
       public void stopConnection() throws IOException{
    	   in.close();
    	   out.close();
    	   clientSocket.close();
       }
      
      public String getTable(String dpidPort){
      String[] parts=dpidPort.split("_");
      String dpid=parts[0];
      String portNum=parts[1];
      String table="temp_bandwidth_"+dpid.replace(":","")+"P"+portNum;
      return table;
      }
	public void sendSecureInfo(String ip, KeyPair kp) throws IOException {
		// TODO Auto-generated method stub
		startConnection(ip);
		String command="REC_SECINFO"+" "+kp.getX()+" "+kp.getY()+"\n";
		sendMessage(command);
		String lookupTablePath=System.getProperty("lookupTableDir")+"/"+ip;
		DataOutputStream dos=new DataOutputStream(out);
		sendFile(lookupTablePath,dos,ip);
	}
	private void sendFile(String path,DataOutputStream out,String filename) throws IOException{
		// TODO Auto-generated method stub
		//first send file name,which is the relative path of checkpoint
		
		out.writeUTF(filename);
		out.flush();
		//second send the file size
		File file=new File(path);
		long filesize=file.length();
		out.writeLong(filesize);
		out.flush();
	//	writeTolog(" filesize:"+filesize);
        //finally send the file content of the file
		FileInputStream fis=new FileInputStream(path);
		int n=0;
		byte[] buf=new byte[4092];
		while((n=fis.read(buf))!=-1){
			out.write(buf,0,n);
			out.flush();
		}
		fis.close();
		System.out.println("lookup Table"+filename+" size "+filesize+" bytes send");
	}
}
