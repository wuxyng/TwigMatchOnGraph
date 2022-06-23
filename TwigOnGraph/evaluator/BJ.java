package evaluator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;

import org.roaringbitmap.RoaringBitmap;

import binaryJoin.JoinOP;
import binaryJoin.bas.JoinOptimizer;
import dao.BFLIndex;
import dao.Tuple;
import dao.TupleList;
import global.Consts.AxisType;
import global.Flags;
import graph.GraphNode;
import helper.IntPair;
import helper.LimitExceededException;
import helper.QueryEvalStat;
import helper.TimeTracker;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;

public class BJ {

	Query mQuery;
	ArrayList<ArrayList<GraphNode>> mInvLsts;
	BFLIndex mBFL;
	//GraphNode[] nodes;
	ArrayList<RoaringBitmap> mBitsIdxArr;

	TupleList[] mEdgeTab;// indexed by query edge id
	TupleList mSolnTab;// final result of the given query

	int[] edgeCard, nodeCard;
	double[] edgeCost;
	IntPair[] sjCard;

	TimeTracker tt;
	double mTupleCount;
	double plannm;
	double totNodes_before = 0.0, totNodes_after = 0.0;
	JoinOP mJOP;

	public BJ(Query query,  BFLIndex bfl,ArrayList<ArrayList<GraphNode>> invLsts, ArrayList<RoaringBitmap> bitsByIDArr) {

		mQuery = query;
		mBFL = bfl;
		mInvLsts = invLsts;
		//nodes = mBFL.getGraphNodes();
		mBitsIdxArr =bitsByIDArr;
		init();
	}

	public QueryEvalStat run() {

		tt.Start();
		evaluateEdges();
		double edgejointm = tt.Stop() / 1000;
		System.out.println("Edge join time:" + edgejointm + " sec.");

		tt.Start();
		JoinOptimizer jopt = new JoinOptimizer(mQuery, edgeCost, edgeCard, nodeCard);
		QEdge[] order = jopt.orderJoins(true);
		double plantm = tt.Stop() / 1000;
		plannm = jopt.getNumPlans();
		System.out.println("Join plan time:" + plantm + " sec.");

		tt.Start();
		mSolnTab = evaluateJoins(order);
		mTupleCount = mSolnTab.cardinality();
		double enumtm = tt.Stop() / 1000;
		printSolnTab();
		System.out.println("Tuple enumeration time:" + enumtm + " sec.");

		QueryEvalStat stat = new QueryEvalStat(0.0, plantm, edgejointm, enumtm, mTupleCount, plannm);

		clear();
		return stat;

	}

	// full enum
	public boolean run(QueryEvalStat stat) {

		tt.Start();
		evaluateEdges();
		double edgejointm = tt.Stop() / 1000;
		stat.setMatchTime(edgejointm);
		System.out.println("Edge join time:" + edgejointm + " sec.");

		tt.Start();
		JoinOptimizer jopt = new JoinOptimizer(mQuery, edgeCost, edgeCard, nodeCard);
		QEdge[] order = jopt.orderJoins(true);
		double plantm = tt.Stop() / 1000;
		double plannm = jopt.getNumPlans();
		stat.setPlanTime(plantm);
		stat.setNumPlans(plannm);
		System.out.println("Join plan time:" + plantm + " sec.");

		tt.Start();
		mSolnTab = evaluateJoins(order);
		double enumtm = tt.Stop() / 1000;
		mTupleCount = mSolnTab.cardinality();
		printSolnTab();
		stat.setNumSolns(mTupleCount);
		stat.setEnumTime(enumtm);
		System.out.println("Tuple enumeration time:" + enumtm + " sec.");

		clear();
		return true;
	}

	public boolean execute(QueryEvalStat stat) throws LimitExceededException {

		tt.Start();
		evaluateEdges();
		double edgejointm = tt.Stop() / 1000;
		stat.setMatchTime(edgejointm);
		System.out.println("Edge join time:" + edgejointm + " sec.");

		tt.Start();
		JoinOptimizer jopt = new JoinOptimizer(mQuery, edgeCost, edgeCard, nodeCard);
		QEdge[] order = jopt.orderJoins(true);
		double plantm = tt.Stop() / 1000;
		double plannm = jopt.getNumPlans();
		stat.setPlanTime(plantm);
		stat.setNumPlans(plannm);
		System.out.println("Join plan time:" + plantm + " sec.");

		tt.Start();
		mTupleCount = execute(order);
		double enumtm = tt.Stop() / 1000;
		System.out.println("Total #tuples = " + mTupleCount);

		// printSolnTab();
		stat.setNumSolns(mTupleCount);
		stat.setEnumTime(enumtm);
		System.out.println("Tuple enumeration time:" + enumtm + " sec.");

		clear();
		return true;
	}

	public double getPlanNums() {

		return plannm;
	}

	public void clear() {
		if (mEdgeTab != null)
			for (TupleList t : mEdgeTab) {
				if (t != null)
					t.clear();
			}
		if (mSolnTab != null)
			mSolnTab.clear();
	}

	private void printSolnTab() {

		System.out.println("Total #tuples = " + mSolnTab.cardinality());

		LinkedList<Tuple> tlist = mSolnTab.getList();

		/*
		 * for(Tuple t:tlist){
		 * 
		 * System.out.println(t); }
		 */

	}

	private TupleList evaluateJoins(QEdge[] order) {

		TupleList leftTab = mEdgeTab[order[0].eid];
		for (int i = 1; i < order.length; i++) {
			TupleList rightTab = mEdgeTab[order[i].eid];
			leftTab = mJOP.join(leftTab, rightTab);
		}

		return leftTab;

	}

	private long execute(QEdge[] order) throws LimitExceededException {

		TupleList leftTab = mEdgeTab[order[0].eid];
		for (int i = 1; i < order.length - 1; i++) {
			TupleList rightTab = mEdgeTab[order[i].eid];
			leftTab = mJOP.join(leftTab, rightTab);
		}

		if (Flags.COUNT)
			mJOP.joinCount(leftTab, mEdgeTab[order[order.length - 1].eid]);
		else
			mJOP.joinFinal(leftTab, mEdgeTab[order[order.length - 1].eid]);
		return mJOP.getNumOutTuples();

	}

	private IntPair computeSemiJoinCardinality(QEdge e, TupleList tlist) {

		BitSet s1 = new BitSet(), s2 = new BitSet();

		for (Tuple t : tlist.getList()) {

			s1.set(t.getValue(e.from));
			s2.set(t.getValue(e.to));
		}

		IntPair pair = new IntPair(s1.cardinality(), s2.cardinality());

		return pair;
	}

	private void evaluateEdges() {

		QEdge[] edges = mQuery.edges;
		QNode[] nodes = mQuery.nodes;
		edgeCard = new int[mQuery.E];
		edgeCost = new double[mQuery.E];
		mEdgeTab = new TupleList[mQuery.E];
		for (QEdge e : edges) {

			edgeCost[e.eid] = nodeCard[e.from] * nodeCard[e.to];

			if (e.axis == AxisType.descendant) {
				// descendant
				// TupleList join(ArrayList<GraphNode> from,
				// ArrayList<GraphNode> to, BFLIndex bfl)
				ArrayList<GraphNode> from = mInvLsts.get(nodes[e.from].id), to = mInvLsts.get(nodes[e.to].id);
				
				mEdgeTab[e.eid] = mJOP.join(mQuery.V, e, from, to, mBFL);
			} else {
				// child
				// join(ArrayList<GraphNode> from, ArrayList<GraphNode> to,
				// RoaringBitmap t_bits)
				ArrayList<GraphNode> from = mInvLsts.get(nodes[e.from].lb), to = mInvLsts.get(nodes[e.to].lb);
				RoaringBitmap t_bits = mBitsIdxArr.get(nodes[e.to].lb);

				mEdgeTab[e.eid] = mJOP.join(mQuery.V, e, from, to, t_bits);

			}

			edgeCard[e.eid] = mEdgeTab[e.eid].cardinality();

		}

	}

	private void init() {
		int size = mQuery.V;
		QNode[] qnodes = mQuery.nodes;
		nodeCard = new int[size];
		for (int i = 0; i < qnodes.length; i++) {
			QNode q = qnodes[i];
			ArrayList<GraphNode> invLst = mInvLsts.get(q.lb);
			totNodes_before += invLst.size();
			nodeCard[q.id] = invLst.size();
			
		}
		mJOP = new JoinOP();
		tt = new TimeTracker();
	}

	public static void main(String[] args) {

	}

}
