package evaluator;

import java.util.ArrayList;
import java.util.Collections;
import org.roaringbitmap.RoaringBitmap;

import dao.BFLIndex;
import dao.Pool;
import dao.PoolEntry;
import dao.Tuple;
import dao.TupleHash;
import global.Consts;
import global.Flags;
import global.Consts.AxisType;
import graph.GraphNode;
import helper.CartesianProduct;
import helper.LimitExceededException;
import helper.QueryEvalStat;
import helper.TimeTracker;
import helper.UnhandledException;
import prefilter.FilterBuilder;
import query.graph.QNode;
import query.graph.Query;
import queryPlan.PlanGenerator;
import tupleEnumerator.TreeTupleEnumBJ;
import tupleEnumerator.TreeTupleEnumCP;

public class BUP {

	Query mQuery;
	ArrayList<ArrayList<GraphNode>> mInvLsts;
	BFLIndex mBFL;
	TimeTracker tt;
	GraphNode[] nodes;
	FilterBuilder mFB;
	ArrayList<Pool> mPool;

	int[] order;
	double mTupleCount;

	TupleHash[] tupleCache;

	QNode mRoot;

	boolean prefilter = true;
	boolean enumByBacktracking = false;

	public BUP(Query query, FilterBuilder fb, BFLIndex bfl, ArrayList<ArrayList<GraphNode>> invLsts) {

		mQuery = query;
		mBFL = bfl;
		nodes = mBFL.getGraphNodes();
		mFB = fb;
		mInvLsts = invLsts;
		tt = new TimeTracker();

	}

	public boolean run(QueryEvalStat stat) throws UnhandledException, LimitExceededException {

		init();
		if (prefilter) {

			mFB.oneRun();
			double prunetm = mFB.getBuildTime();
			double totNodes_after = mFB.getTotNodes();
			mInvLsts = mFB.getInvLsts();
			stat.setPreTime(prunetm);
			stat.setTotNodesAfter(totNodes_after);
			System.out.println("Prune time:" + prunetm + " sec.");
		}

		else
			stat.setTotNodesAfter(stat.totNodesBefore);
		initPool();
		tt.Start();
		boolean rs = traverseBUP();
		double matm = tt.Stop() / 1000;
		stat.setMatchTime(matm);
		stat.calAnsGraphSize(mPool);
		System.out.println("Time on BUP traversal:" + matm + "sec.");

		if (rs) {
			tt.Start();
			if (Flags.COUNT) {
				calTotTreeSolns();
			} else {

				if (enumByBacktracking)
					this.enumTuples_TD();
				else
					this.enumTuples_DP();

			}

			double enumtm = tt.Stop() / 1000;
			stat.setEnumTime(enumtm);
			System.out.println("Tuple enumeration time:" + enumtm + " sec.");
		}
		stat.setNumSolns(mTupleCount);
		clear();
		return true;
	}

	public double getTupleCount() {

		return mTupleCount;
	}

	public void clear() {

		if (mPool != null)
			for (Pool p : mPool)
				p.clear();
	}
	///////////////////////////

	private boolean traverseBUP() {

		// QNode root = mTree.getSources().get(0);

		mRoot = mQuery.getSources().get(0);

		return traverseBUP(mRoot);
	}

	private boolean traverseBUP(QNode q) {

		if (q.isSink())
			return true;
		ArrayList<QNode> children = null;
		children = mQuery.getChildren(q.id);

		if (Flags.sortByCard) {

			// Collections.sort(children, QNode.AxisComparator);
			sortByCard(children);
		}

		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[children.size()];
		int i = 0;

		for (QNode c : children) {
			boolean rs = traverseBUP(c);
			if (!rs)
				return false;
			AxisType axis = c.E_I.get(0).axis;
			ArrayList<PoolEntry> targets = mPool.get(c.id).elist();
			if (axis == AxisType.child) {
				RoaringBitmap t_bits = new RoaringBitmap();
				for (PoolEntry e : targets)
					t_bits.add(e.getValue().L_interval.mStart);
				// t_bits.add(e.getValue().id);
				tBitsIdxArr[i] = t_bits;

			}
			i++;

		}

		// for regular node

		ArrayList<GraphNode> invLst = mInvLsts.get(q.id);
		Pool qAct = mPool.get(q.id);

		for (GraphNode qn : invLst) {

			PoolEntry actEntry = new PoolEntry(q, qn);
			boolean found = checkChildMatch(actEntry, children, tBitsIdxArr);
			if (found)
				qAct.addEntry(actEntry);

		}

		if (qAct.isEmpty())
			return false;

		return true;
	}

	private void sortByCard(ArrayList<QNode> children) {

		int[] ints = new int[children.size()];
		for (int i = 0; i < children.size(); i++) {
			QNode c = children.get(i);
			ints[i] = mPool.get(c.id).elist().size();

		}
		Collections.sort(children, (left, right) -> ints[children.indexOf(left)] - ints[children.indexOf(right)]);
	}

	private boolean checkChildMatch(PoolEntry r, RoaringBitmap t_bits, ArrayList<PoolEntry> list) {

		GraphNode s = r.getValue();

		if (s.N_O_SZ == 0)
			return false;
		/*
		 * RoaringBitmap s_bits = new RoaringBitmap();
		 * 
		 * for (int c : s.N_O) {
		 * 
		 * s_bits.add(c); } RoaringBitmap rs_and = RoaringBitmap.and(s_bits,
		 * t_bits);
		 */
		RoaringBitmap rs_and = RoaringBitmap.and(s.adj_bits_o, t_bits);

		if (rs_and.isEmpty())
			return false;

		for (int ti : rs_and) {
			PoolEntry e = list.get(t_bits.rank(ti) - 1);
			r.addChild(e);
		}

		return true;
	}

	private boolean checkChildMatch(PoolEntry r, ArrayList<QNode> children, RoaringBitmap[] tBitsIdxArr) {

		GraphNode tq = r.getValue();
		int c = 0;
		for (QNode qi : children) {

			Pool pqi = mPool.get(qi.id);
			boolean found = false;
			AxisType axis = qi.E_I.get(0).axis;
			if (axis == AxisType.child) {

				found = checkChildMatch(r, tBitsIdxArr[c], pqi.elist());
			} else
				for (PoolEntry hp : pqi.elist()) {
					GraphNode tp = hp.getValue();

					// added on 2018.5.21, skip the self-linking case
					if (tq.id == tp.id)
						continue;

					// add this to short cut the checking

					if (tq.L_interval.mEnd < tp.L_interval.mStart) {

						break;

					}

					if (mBFL.reach(tq, tp) == 1) {

						r.addChild(hp);

						found = true;
					}

				}

			if (!found)
				return false; // don't need to continue;
			c++;
		}

		return true;
	}

	/**************************
	 * 
	 * Tuple enumeration phase
	 * 
	 *************************/

	private void enumTuples_TD() throws LimitExceededException {

		PoolEntry[] match = new PoolEntry[mQuery.V];
		int[] count = new int[mQuery.V];
		mTupleCount = 0;
		// backtrack(mQuery.V, 0, match, count, stat);
		backtrack(mQuery.V, 0, match);
		System.out.println("Total solution tuples:" + mTupleCount);
	}

	private void backtrack(int max_depth, int depth, PoolEntry[] match) throws LimitExceededException {

		int cur_vertex = order[depth];
		QNode qn = mQuery.getNode(cur_vertex);
		ArrayList<PoolEntry> candList = getCandList(qn, match);
		if (candList.isEmpty()) {

			return;
		}

		for (PoolEntry e : candList) {

			match[qn.id] = e;

			if (depth == max_depth - 1) {

				mTupleCount++;
				// System.out.println(t);
				if (Flags.OUTLIMIT && mTupleCount >= Consts.OutputLimit) {
					throw new LimitExceededException();
				}

			} else

				backtrack(max_depth, depth + 1, match);
			match[qn.id] = null;

		}

	}

	private void backtrack(int max_depth, int depth, PoolEntry[] match, int[] count, QueryEvalStat stat)
			throws LimitExceededException {

		int cur_vertex = order[depth];
		QNode qn = mQuery.getNode(cur_vertex);
		ArrayList<PoolEntry> candList = getCandList(qn, match);
		if (candList.isEmpty()) {

			return;
		}

		if (qn.isSink()) {
			count[cur_vertex] = candList.size();
			if (depth == max_depth - 1) {

				mTupleCount += product(count);
				stat.setNumSolns(mTupleCount);
				// System.out.println("No. of tuples so far:" + mTupleCount);
				if (Flags.OUTLIMIT && mTupleCount >= Consts.OutputLimit)
					throw new LimitExceededException();
			} else

				backtrack(max_depth, depth + 1, match, count, stat);

		} else {
			count[cur_vertex] = 1;
			for (PoolEntry e : candList) {

				match[qn.id] = e;
				backtrack(max_depth, depth + 1, match, count, stat);
				match[qn.id] = null;

			}
		}
	}

	private double product(int[] count) {

		double rs = 1;

		for (int c : count) {

			rs *= c;
		}

		return rs;
	}

	private ArrayList<PoolEntry> getCandList(QNode qn, PoolEntry[] match) {

		// int num = qn.N_I_SZ;
		int qid = qn.id;

		if (qn.isSource()) {

			return mPool.get(qn.id).elist();
		}
		int pid = mQuery.getNode(qid).N_I.get(0); // tree parent
		PoolEntry pm = match[pid];
		ArrayList<PoolEntry> qmatList = pm.mFwdEntries.get(qid);
		return qmatList;

	}

	private void printMatch(PoolEntry[] match) {

		for (PoolEntry v : match) {

			System.out.print(v + " ");
		}

		System.out.println();
	}

	/**************************
	 * 
	 * Tuple enumeration phase
	 * 
	 *************************/

	private void enumTuples_DP() throws LimitExceededException {

		TreeTupleEnumCP ttenum = new TreeTupleEnumCP(mQuery, mPool);
		//TreeTupleEnumBJ ttenum = new TreeTupleEnumBJ(mQuery, mPool);
		ttenum.enumTuples();

		mTupleCount = ttenum.getTupleCount();
	}

	/////////////////////////////////////

	private void initPool() {

		mPool = new ArrayList<Pool>(mQuery.V);
		QNode[] qnodes = mQuery.nodes;
		for (int i = 0; i < qnodes.length; i++) {
			QNode q = qnodes[i];

			ArrayList<GraphNode> invLst = mInvLsts.get(q.id);
			Pool pool = new Pool();
			mPool.add(q.id, pool);
			if (q.isSink()) {

				for (GraphNode n : invLst) {
					PoolEntry e = new PoolEntry(q, n);
					pool.addEntry(e);
				}

			}

		}

	}

	private void init() {

		order = PlanGenerator.generateTopoQueryPlan(mQuery);

		tupleCache = new TupleHash[mQuery.V];

		for (int i = 0; i < mQuery.V; i++) {

			tupleCache[i] = new TupleHash();
		}

		tt = new TimeTracker();

	}

	private void calTotTreeSolns() {

		QNode root = mQuery.getSources().get(0);
		Pool rPool = mPool.get(root.id);

		ArrayList<PoolEntry> elist = rPool.elist();
		for (PoolEntry r : elist) {

			mTupleCount += r.size();

		}
		System.out.println("total number of solution tuples: " + mTupleCount);

	}

	public static void main(String[] args) {

	}

}
