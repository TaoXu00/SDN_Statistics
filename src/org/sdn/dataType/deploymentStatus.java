package org.sdn.dataType;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class deploymentStatus {
 private Map<host,hostStatus> busyHosts;
 private List<host> freeHosts;
 public deploymentStatus(){
	busyHosts=new HashMap<host,hostStatus>();
	freeHosts=new LinkedList<host>();
 }
 public Map<host, hostStatus> getBusyHosts() {
	return busyHosts;
}
public void setBusyHosts(Map<host, hostStatus> busyHosts) {
	this.busyHosts = busyHosts;
}
public List<host> getFreeHosts() {
	return freeHosts;
}
public void setFreeHosts(List<host> freeHosts) {
	this.freeHosts = freeHosts;
}

 
}
