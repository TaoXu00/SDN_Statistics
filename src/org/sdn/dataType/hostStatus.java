package org.sdn.dataType;

import java.util.HashMap;
import java.util.Map;

public class hostStatus {
	//key is the image name,the value container name
	private Map<String,String> containers;
	private  int counter;
	public hostStatus(){
		this.containers=new HashMap<String,String>();
	}
    public Map<String, String> getContainers() {
		return containers;
	}
	public void setContainers(Map<String, String> containers) {
		this.containers = containers;
	}
	public int getCounter() {
		return counter;
	}
	public void setCounter(int counter) {
		this.counter = counter;
	} 
}
