package org.sdn.dataType;

public class bandwidth {
  String dpid;
  String port;
  String updated;
  String linkSpeedBitsPerSecond;
  String bitsPerSecondRx;
  String bitsPerSecondTx;
 
public String getDpid() {
	return dpid;
}
public void setDpid(String dpid) {
	this.dpid = dpid;
}
public String getPort() {
	return port;
}
public void setPort(String port) {
	this.port = port;
}
public String getUpdated() {
	return updated;
}
public void setUpdated(String updated) {
	this.updated = updated;
}
public String getLinkSpeedBitsPerSecond() {
	return linkSpeedBitsPerSecond;
}
public void setLinkSpeedBitsPerSecond(String linkSpeedBitsPerSecond) {
	this.linkSpeedBitsPerSecond = linkSpeedBitsPerSecond;
}
public String getBitsPerSecondRx() {
	return bitsPerSecondRx;
}
public void setBitsPerSecondRx(String bitsPerSecondRx) {
	this.bitsPerSecondRx = bitsPerSecondRx;
}
public String getBitsPerSecondTx() {
	return bitsPerSecondTx;
}
public void setBitsPerSecondTx(String bitsPerSecondTx) {
	this.bitsPerSecondTx = bitsPerSecondTx;
}

  
}
