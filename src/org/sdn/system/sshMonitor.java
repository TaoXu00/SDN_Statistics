package org.sdn.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class sshMonitor {
	 private String user="xu";
 	 private String password="123";
     public Session newConnection(String host,String user,String password){
    	// String command="ls";
    	 Session session=null;
    	 try{
    		 java.util.Properties config=new java.util.Properties();
    		 config.put("StrictHostKeyChecking", "no");
    		 JSch jsch=new JSch();
    		 session=jsch.getSession(user,host,22);
    		 session.setPassword(password);
    		 session.setConfig(config);
    		 session.connect();
    		 System.out.println("Connected");
    		 
    	 }catch(Exception e){
    		 e.printStackTrace();
    		 
    	 }
    	 return session;
     }
     public void startMigrationServer(String host){
      	String command="cd /home/xu/Desktop/migrationServer; java -jar migrationServer.jar";
      	try{
  		Session session=newConnection(host,user,password);
  		Channel channel= session.openChannel("exec");
  		((ChannelExec) channel).setCommand("sudo -S -p '' "+command);
  		InputStream in=channel.getInputStream();
  		OutputStream out=channel.getOutputStream();
  		channel.connect();
  		out.write((password+"\n").getBytes());
  		out.flush();
  		OutputSSH(in,channel);
  		channel.disconnect();
  		session.disconnect();
      }catch(Exception e){
    	  e.printStackTrace();
      }
  		
     }
     public void  executeMigration(String srcIP,String distIP,String srcDir,String distDir){	
    	String command1="sudo docker checkpoint create --checkpoint-dir="+srcDir+" looper2 checkpoint --leave-running";
    	String command2="scp -r /home/xu/mininet/H1Dir/6b80b035e426c06470c8b8e22552862410f057dd39116d1402fdab10a765b449/checkpoints/checkpoint xu@"+distIP+":/home/xu/mininet/H2Dir ";
    	Session session=newConnection(srcIP,user,password);	
    	/*Channel channel2 =session.openChannel("shell");
   		InputStream in2=channel2.getInputStream();
   		OutputStream ops2=channel2.getOutputStream();
   		PrintStream ps2=new PrintStream(ops2,true);
   		channel2.connect();
   		ps2.println(command2);
   		OutputSSH(in2,channel2);*/
//    	excuteSSHCommand(session,command1);
    	excuteSSHCommand(session,command2);
     }
     public void excuteSSHCommand(Session session,String command){
      try {
			Channel channel = session.openChannel("shell"); 
     	    InputStream in=channel.getInputStream();
    		OutputStream ops=channel.getOutputStream();
    		PrintStream ps=new PrintStream(ops,true);
    		channel.connect();
    		ps.println(command);   		
    		ps.println(password);
     	    OutputSSH(in,channel);
     	    channel.disconnect();
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
     }
     public void OutputSSH(InputStream in,Channel channel){
    	byte[] tmp=new byte[1024];
        try {
   		while(true){
   			
				while(in.available()>0){
					int i=in.read(tmp,0,1024);
					if(i<0) break;
					System.out.print(new String(tmp,0,i));
		   }
   			if(channel.isClosed()){
   				System.out.println("exit-status: "+channel.getExitStatus());
   				break;
   			}
   		 }
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
     }
}
