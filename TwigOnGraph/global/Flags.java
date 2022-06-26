
package global;

import global.Consts.OrderType;
import helper.MemoryTracker;

public class Flags {
	public static boolean DEBUG = false;
	public static boolean OUTLIMIT = true; // false
	public static boolean PRUNELIMIT = true; //false; //false;
	public static OrderType ORDER =  OrderType.GQL; //OrderType.RI; //
	public static boolean COUNT = false;
	public static boolean sortByCard = true;
	public static int vis_cur = 0;
	public static int REPEATS = 1; // for running the experimental evaluation.
	public static MemoryTracker mt = new MemoryTracker();
}
