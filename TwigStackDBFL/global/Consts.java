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

	public static String OUTFILE = "";
	
	
	public static int K = 5;
	public static int D = 320 * K;
	
}
