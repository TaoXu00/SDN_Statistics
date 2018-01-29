package org.sdn.dataType;

public class host {
private String name;
private String IP;
private String attachPoint;
public host(String name,String Ip){
	this.name=name;
	this.IP=Ip;
}
public void setAttachPoint(String port){
	this.attachPoint=port;
}
public String getName() {
	return name;
}
public String getIP() {
	return IP;
}
public String getAttachPoint(){
	return attachPoint;
}
}
