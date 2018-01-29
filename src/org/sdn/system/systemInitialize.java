package org.sdn.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;

public class systemInitialize {
    public static void start(){
    	//1.start floodlight
    
    		//String[] cmd=new String[]{"/bin/sh","../custom/systemStart.sh"};
    		try {
				
    			//Process p=new ProcessBuilder("/bin/sh","/home/xu/SDN_APP/SDN_Statistics/custom/systemStart.sh").start();
    			initializeFromConfigurationFile();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  	
    }
    public static void initializeFromConfigurationFile() throws Exception{
		URL resource=ClassLoader.getSystemResource("configuration");
		File file=new File(resource.toURI());
		FileInputStream confFile=new FileInputStream(file);
	    Properties p=new Properties(System.getProperties());
	    p.load(confFile);
	    System.setProperties(p);
	} 

}
