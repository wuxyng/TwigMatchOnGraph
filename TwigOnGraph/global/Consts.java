/**
 * 
 */
package global;

/**
 * @author xiaoying
 *
 */
public class Consts {

	public static final String NEWLINE = System.getProperty("line.separator");

	// type of Cursor states

	public static final int OPEN = 1;
	public static final int EOF = 2;
	public static final int CLOSED = 3;
	
	
	public static final String HOME  = "/home/work";
	public static final String INDIR  = HOME + "/experiments/input/";
	public static final String OUTDIR = HOME + "/experiments/output/";
	
	public static enum status_vals {success, timeout, outOfMemory, exceedLimit, failure};
	public static enum OrderType {RI, GQL, HYB};
	public static String OUTFILE = "";
	
	public static int FirstK = 100000;
	public static int TimeLimit = 10; // minutes
	public static int OutputLimit = 10000000; //100000000;   //1000000000; 
	public static int PruneLimit = 10;
	public static int K = 5;
	public static int D = 320 * K;
	public static enum Color {
		/** not yet seen */
		white,
		/** processing, in dfs stack */
		grey,
		/** already processed */
		black;
	}
	
	public enum DirType {
		BWD, FWD, NOD;
	}

	public enum AxisType {
		child, descendant, none;
	}
	
}
