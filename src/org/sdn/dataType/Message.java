package org.sdn.dataType;

public class Message {
 private msgType msgtype;
 private int frequency;
 private String[] dpid;
 private bandwidth[] portlist;
 
 public int getFrequency() {
	return frequency;
}
public void setFrequency(int frequency) {
	this.frequency = frequency;
}
public msgType getMsgtype() {
	return msgtype;
}
public void setMsgtype(msgType msgtype) {
	this.msgtype = msgtype;
}
public String[] getDpid() {
	return dpid;
}
public void setDpid(String[] dpid) {
	this.dpid = dpid;
}
public bandwidth[] getPortlist() {
	return portlist;
}
public void setPortlist(bandwidth[] portlist) {
	this.portlist = portlist;
}
}
