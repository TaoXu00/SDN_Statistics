package org.sdn.system;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.sdn.DB.DBFunction;
import org.sdn.dataType.link;

public class hostInformationCollector {
	DBFunction db=new DBFunction();
    public void hostInfoToDatabase(){
      //1.reading host date and write to the db
    	String FILENAME=System.getProperty("hostInfoDir");
    	try {
			BufferedReader br=new BufferedReader(new FileReader(FILENAME));
			String sCurrentLine;
			int type=0;   //when type =0,means now is reading connection,1 means reading is host ip
			Connection conn= db.newConnection();
			while((sCurrentLine=br.readLine()) !=null){
			    if(sCurrentLine.equals("connections")){
			      type=0;
			      continue;
			    }
				if(sCurrentLine.equals("IP")){
				  type=1;
				  continue;
				}if(sCurrentLine.equals("link bandwidth")){
					type=2;
					continue;
				}
			    if(type==0){
				 String[] parts=sCurrentLine.split("<->");
				 String source=parts[0];
				 String destination=parts[1];
				// String[] sourceParts=source.split("-");
				 String[] destParts=destination.split("-");
			   //	 String src=sourceParts[0];
			   //	 String srcPort=sourceParts[1];
				 String dest=destParts[0];
				 String destPort=destParts[1];
				 if(dest.charAt(0)=='h'){
			      Statement stt=conn.createStatement();
				  stt.execute("INSERT INTO connections(source,dstType,destination) VALUES" +
							   "('"+source+"','host','"+destination+"')");		     
					stt.close();
					
				 }else if(dest.charAt(0)=='s'){
					 Statement stt=conn.createStatement();
					  stt.execute("INSERT INTO connections(source,dstType,destination) VALUES" +
							      "('"+source+"','switch','"+destination+"')");		     
						stt.close();
				 }	 
			    }else if(type==1){
			    	String[] parts=sCurrentLine.split(" ");
			    	String host=parts[0];
			    	String ip=parts[1];
			    	 Statement stt=conn.createStatement();
					  stt.execute("INSERT INTO mapping VALUES" +
								   "('"+host+"','host','"+ip+"')");
					  String sql="UPDATE connections SET destIP="+
								   "'"+ip+"' WHERE destination='"+host+"-eth0'";
					  stt.execute(sql);
			    }else{
			    /*	String[] parts=sCurrentLine.split(":");
			    	String[] linknodes=parts[0].split("<->");
			    	link l=new link(linknodes[0],linknodes[1]);			    	
			    	int bandwidth=Integer.parseInt(parts[1]);
			    	DBFunction.getLinkBandwidth().put(l,bandwidth);	*/
			    	String[] parts=sCurrentLine.split(":");
			    	String[] linknodes=parts[0].split("<->");
			    	String l=linknodes[0]+"-"+linknodes[1];			    	
			    	int bandwidth=Integer.parseInt(parts[1]);
			    	DBFunction.getLinkBandwidth().put(l,bandwidth);
			    }
			}
			conn.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
    }
}
