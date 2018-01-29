package org.sdn.dataType;

public class link {
  private String src;
  private String dst;
public link(String src,String dst){
	  this.src=src;
	  this.dst=dst;
  }
public String getSrc() {
	return src;
}
public String getDst() {
	return dst;
}
}
