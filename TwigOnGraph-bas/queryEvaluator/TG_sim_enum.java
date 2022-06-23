package queryEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import org.roaringbitmap.RoaringBitmap;

import dao.BFLIndex;
import dao.Pool;
import dao.PoolEntry;
import enumerator.Tuple;
import enumerator.TupleEnumerator;
import global.Flags;
import graph.GraphNode;
import helper.QueryEvalStat;
import helper.TimeTracker;
import query.QNode;
import query.TreeQuery;

/******
 * 
 * combine TDW with answer graph building
 * 
 * @author xiaoying
 */
public class TG_sim_enum {

	TreeQuery mQuery;
	ArrayList<Pool> mPool;

	BFLIndex mBFL;
	TimeTracker tt;

	GraphNode[] nodes;

	RoaringBitmap[] tBitsIdxArr;

	ArrayList<ArrayList<GraphNode>> mInvLsts, mInvLstsByID;
		
	double totSolns = 0, sizeOfAnsGraph = 0, totNodes_before =0.0, totNodes_after = 0.0, totSolnNodes =0.0;

	
	public TG_sim_enum(TreeQuery query, ArrayList<ArrayList<GraphNode>> invLsts, BFLIndex bfl) {

		mQuery = query;
		mBFL = bfl;
		mInvLsts = invLsts;
		nodes = mBFL.getGraphNodes();
		init();
	}

	public TG_sim_enum(TreeQuery query, ArrayList<ArrayList<GraphNode>> invLsts,
			ArrayList<ArrayList<GraphNode>> invLstsByID, BFLIndex bfl) {

		mQuery = query;
		mBFL = bfl;
		mInvLsts = invLsts;
		mInvLstsByID = invLstsByID;
		nodes = mBFL.getGraphNodes();
		init();
	}

	public void clear() {

		for (Pool p : mPool)
			p.clear();
	}

	public QueryEvalStat run() {
		
		tt.Start();
		traverse();
		double matm = tt.Stop() / 1000;
		System.out.println("Time on TwigListD_sim_enumSolns:" + matm + "sec.");
		tt.Start();
		enumTuples();
		// printSolutions();
		//calTotSolns();
		double entm = tt.Stop() / 1000;
		System.out.println("Time on enumerating solutions:" + entm + "sec.");
		calAnsGraphSize();
		QueryEvalStat stat = new QueryEvalStat(matm, entm, 0, totSolns, sizeOfAnsGraph);
		stat.totNodesBefore = totNodes_before;
		stat.totSolnNodes = calTotSolnNodes();
		stat.totNodesAfter = stat.totSolnNodes;
		return stat;
	}
	
	

	
	private void enumTuples(){
	
		TupleEnumerator tenum = new TupleEnumerator(mQuery,mPool);
		tenum.run();
		ArrayList<Tuple> tuples = tenum.getTupleList();
		//for(Tuple t:tuples){
			
		//	System.out.println(t);
		//}
		System.out.println("total number of solutions: " + tuples.size());
	}
	
	public void calTotSolns() {

		QNode root = mQuery.mRoot;
		Pool rPool = mPool.get(root.id);
		ArrayList<PoolEntry> elist = rPool.elist();
		for (PoolEntry r : elist) {

			totSolns += r.size();

		}

		System.out.println("total number of solution tuples: " + totSolns);
	}

	private double calTotSolnNodes(){
		
		double totNodes = 0.0;
		for (Pool pool: mPool) {
			ArrayList<PoolEntry> elist = pool.elist();
			totNodes+= elist.size();

		}
		return totNodes;
	}
	
	public void calAnsGraphSize(){
		
		for(Pool pl:mPool){
			sizeOfAnsGraph +=pl.elist().size(); //nodes
			for (PoolEntry e :pl.elist()){
				sizeOfAnsGraph+=e.getNumChildEnties(); //edges
				
			}
			
		}
	}

	public void printSolutions() {

		QNode root = mQuery.mRoot;
		Pool rPool = mPool.get(root.id);
		ArrayList<PoolEntry> elist = rPool.elist();
		if (elist.isEmpty())
			return;

		for (PoolEntry r : elist) {

			System.out.println(r);

		}

	}

	
	private void init() {
		mQuery.extractQueryInfo();
		int size = mQuery.V;
		mPool = new ArrayList<Pool>(size);

		tBitsIdxArr = new RoaringBitmap[size];

		QNode[] nodes = mQuery.nodes;
		for (int i = 0; i < nodes.length; i++) {
			QNode q = nodes[i];

			ArrayList<GraphNode> invLst = mInvLsts.get(q.lb);
			totNodes_before +=invLst.size();
			if (!q.isSource() && q.E_I.get(0).axis == 0) {

				// Collections.sort(invLst, GraphNode.NodeIDComparator);
				invLst = mInvLstsByID.get(q.lb);

			}
			Pool pool = new Pool();
			mPool.add(q.id, pool);
			if (q.isSink()) {

				for (GraphNode n : invLst) {
					PoolEntry e = new PoolEntry(q, n);
					pool.addEntry(e);

				}

			}

		}

		tt = new TimeTracker();

	}

	/****************************
	 * TwigListD alg starts here
	 ****************************/

	private void traverse() {
		QNode root = mQuery.mRoot;
		pruneBUP(root);
		findMatches(root);
	}

	private void pruneBUP(QNode q) {

		if (q.isSink())
			return;

		ArrayList<QNode> children = mQuery.getChildren(q.id);
		if (Flags.sortByCard) {

			//Collections.sort(children, QNode.AxisComparator);
			sortByCard(children);
		}


		for (QNode c : children) {
			pruneBUP(c);
			int axis = c.E_I.get(0).axis;
			ArrayList<PoolEntry> targets = mPool.get(c.id).elist();
			if (axis == 0) {
				RoaringBitmap t_bits = new RoaringBitmap();
				for (PoolEntry e : targets)
					t_bits.add(e.getValue().id);
				tBitsIdxArr[c.id] = t_bits;

			}

		}

		ArrayList<GraphNode> invLst = mInvLsts.get(q.lb);
		if (!q.isSource() && q.E_I.get(0).axis == 0) {

			// Collections.sort(invLst, GraphNode.NodeIDComparator);
			invLst = mInvLstsByID.get(q.lb);
		}

		Pool qAct = mPool.get(q.id);

		for (GraphNode qn : invLst) {

			PoolEntry actEntry = new PoolEntry(q, qn);
			boolean found = checkChildMatch(actEntry, children);
			if (found)
				qAct.addEntry(actEntry);

		}

	}
	
	private void sortByCard(ArrayList<QNode> children) {

		int[] ints = new int[children.size()];
		for (int i = 0; i < children.size(); i++) {
			QNode c = children.get(i);
			ints[i] = mPool.get(c.id).elist().size();

		}
		Collections.sort(children, (left, right) -> ints[children.indexOf(left)] - ints[children.indexOf(right)]);
	}


	private void findMatches(QNode q) {

		ArrayList<QNode> children = mQuery.getChildren(q.id);
		//RoaringBitmap[] tBitsIdxArr = getTBitsIdxArr(children);

		ArrayList<PoolEntry> sources = mPool.get(q.id).elist();
		
		for (QNode c : children) {

			int axis = c.E_I.get(0).axis;
			ArrayList<PoolEntry> targets = mPool.get(c.id).elist();
			ArrayList<PoolEntry> targets_new = null;
			if (axis == 0) {

				targets_new = addChildMatch(sources, targets, tBitsIdxArr[c.id]);
			} else
				targets_new = addChildMatch(sources, targets);

			mPool.get(c.id).setList(targets_new);
		
			findMatches(c);
		}

	}

	private ArrayList<PoolEntry> addChildMatch(ArrayList<PoolEntry> sources, ArrayList<PoolEntry> targets) {

		RoaringBitmap t_bits_new = new RoaringBitmap();
		for (PoolEntry r : sources) {
			GraphNode tq = r.getValue();
			for (int i = 0; i < targets.size(); i++) {
				PoolEntry hp = targets.get(i);
				GraphNode tp = hp.getValue();

				// added on 2018.5.21, skip the self-linking case
				if (tq.id == tp.id)
					continue;
				if (tq.L_interval.mEnd < tp.L_interval.mStart) {

					break;

				}

				if (mBFL.reach(tq, tp) == 1) {

					r.addChild(hp);
					t_bits_new.add(i);
				}
			}
		}

		ArrayList<PoolEntry> targets_new = new ArrayList<PoolEntry>(t_bits_new.getCardinality());
		for (int ti : t_bits_new) {
			PoolEntry e = targets.get(ti);
			targets_new.add(e);
		}

		return targets_new;
	}

	private ArrayList<PoolEntry> addChildMatch(ArrayList<PoolEntry> sources, ArrayList<PoolEntry> targets,
			RoaringBitmap t_bits) {

		RoaringBitmap t_bits_new = new RoaringBitmap();
		for (PoolEntry r : sources) {

			addChildMatch(r, t_bits, t_bits_new, targets);
		}

		ArrayList<PoolEntry> targets_new = new ArrayList<PoolEntry>(t_bits_new.getCardinality());

		for (int ti : t_bits_new) {
			PoolEntry e = targets.get(t_bits.rank(ti) - 1);
			targets_new.add(e);
		}

		return targets_new;
	}

	private void addChildMatch(PoolEntry r, RoaringBitmap t_bits, RoaringBitmap t_bits_new,
			ArrayList<PoolEntry> targets) {

		GraphNode s = r.getValue();

		if (s.N_O_SZ == 0)
			return;

		RoaringBitmap s_bits = new RoaringBitmap();

		for (int c : s.N_O) {

			s_bits.add(c);
		}

		RoaringBitmap rs_and = RoaringBitmap.and(s_bits, t_bits);
		t_bits_new.or(rs_and);
		for (int ti : rs_and) {
			PoolEntry e = targets.get(t_bits.rank(ti) - 1);
			r.addChild(e);
		}
	}

	private RoaringBitmap[] getTBitsIdxArr(ArrayList<QNode> children) {

		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[children.size()];
		int i = 0;
		for (QNode c : children) {

			int axis = c.E_I.get(0).axis;
			ArrayList<PoolEntry> targets = mPool.get(c.id).elist();
			if (axis == 0) {
				RoaringBitmap t_bits = new RoaringBitmap();
				for (PoolEntry e : targets)
					t_bits.add(e.getValue().id);
				tBitsIdxArr[i] = t_bits;

			}
			i++;
		}
		return tBitsIdxArr;
	}

	private boolean checkChildMatch(PoolEntry r, ArrayList<QNode> children) {

		GraphNode tq = r.getValue();
		for (QNode qi : children) {

			Pool pqi = mPool.get(qi.id);
			boolean found = false;
			int axis = qi.E_I.get(0).axis;

			if (axis == 0) {

				found = checkChildMatch(r, tBitsIdxArr[qi.id]);
			} else
				for (PoolEntry hp : pqi.elist()) {
					GraphNode tp = hp.getValue();

					// added on 2018.5.21, skip the self-linking case
					if (tq.id == tp.id)
						continue;

					// add this to short cut the checking

					if (tq.L_interval.mEnd < tp.L_interval.mStart) {

						if (!found) {
							return false;
						}

					}

					if (mBFL.reach(tq, tp) == 1) {

						found = true;
						break;
					}

				}

			if (!found)
				return false; // don't need to continue;
			
		}

		return true;
	}

	private boolean checkChildMatch(PoolEntry r, RoaringBitmap t_bits) {

		GraphNode s = r.getValue();

		if (s.N_O_SZ == 0)
			return false;

		RoaringBitmap s_bits = new RoaringBitmap();

		for (int c : s.N_O) {

			s_bits.add(c);
		}

		return RoaringBitmap.andCardinality(s_bits, t_bits) > 0 ? true : false;
	}

	////////////////////////////////

	private void printEntries(ArrayList<PoolEntry> list) {

		for (PoolEntry e : list) {
			System.out.println(e.getValue());

		}
	}

	public static void main(String[] args) {

	}

}
