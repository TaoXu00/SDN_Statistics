package org.sdn.DB;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.sdn.dataType.Time_NPackets;
import org.sdn.dataType.bandwidth;

public class VIB {
  private static Map<String,HashMap<String,bandwidth>> bandWidthStatistic=
		  new HashMap<String,HashMap<String,bandwidth>>();
  private static Map<String,LinkedList<Time_NPackets>> packetsAggregatePerSwitchFull =
		  new HashMap<String,LinkedList<Time_NPackets>>();
  //private static Map<String,List<Time_NPackets>> pac
  public static void clearBandWidthStatistic(){
	       bandWidthStatistic.clear();
  }
  public static void clearpacketsAggregatePerSwitchFull(){
          bandWidthStatistic.clear();
}
  public static Map<String,HashMap<String,bandwidth>> getbandWidthStatistic(){
	  return bandWidthStatistic;
  }
  public static Map<String,LinkedList<Time_NPackets>> getPacketsAggregatePerSwitchFull(){
	 return packetsAggregatePerSwitchFull;
  }
  public static LinkedList<Time_NPackets> getPacketsAggregatePerSwitch(String dpid){
   return getPacketsAggregatePerSwitchFull().get(dpid);
  }
  }
