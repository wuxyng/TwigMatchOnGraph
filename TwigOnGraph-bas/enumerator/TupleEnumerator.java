package enumerator;

import java.util.ArrayList;

import dao.Pool;
import dao.PoolEntry;
import helper.CartesianProduct;
import query.QNode;
import query.TreeQuery;

/*
 * enumerate solution tuples from answer graph
 * 
 */

public class TupleEnumerator {

	TreeQuery mQuery;
	ArrayList<Pool> mAnswerGraph;
	ArrayList<Tuple> mTuples;
	
	public TupleEnumerator(TreeQuery query, ArrayList<Pool> answerGraph){
		
		mQuery = query;
		mAnswerGraph = answerGraph;
		init();
	}
	
	public void run(){
		
		QNode root = mQuery.mRoot;
		Pool rPool = mAnswerGraph.get(root.id);
		ArrayList<PoolEntry> elist = rPool.elist();
		mTuples = new ArrayList<Tuple>();
		for (PoolEntry r : elist) {
			
			mTuples.addAll(getTupleList(r));
		}
	}
	
	public ArrayList<Tuple> getTupleList(){
		
		return mTuples;
	}
	
	private ArrayList<Tuple> getTupleList(PoolEntry e){
		
		ArrayList<Tuple> solnList = null;
		QNode q = e.getQNode();
		if(q.isSink()){
			
			solnList = new ArrayList<Tuple>(1);
			Tuple t = new Tuple(mQuery.V);
			t.add(q.id, e.getValue().id);
			solnList.add(t);
			return solnList;
		}
		
		
		ArrayList<QNode> children = mQuery.getChildren(q.id);
		ArrayList<ArrayList<Tuple>> tupleLists = new ArrayList<ArrayList<Tuple>>(children.size());
		int idx=0;
		for (QNode c : children) {
			ArrayList<PoolEntry> elist = e.mSubEntries.get(c.id);
			ArrayList<Tuple> tupleList = new ArrayList<Tuple>();
			for (int i = 0; i < elist.size(); i++) {
				PoolEntry sub = elist.get(i);
				//tupleLists.add(getTupleList(sub)); 
				tupleList.addAll(getTupleList(sub));
			}
			tupleLists.add(idx++, tupleList);
		}
		
		solnList = getCartesianProduct(e,tupleLists);
		return solnList;
	}
	
	private ArrayList<Tuple> getCartesianProduct(PoolEntry e, ArrayList<ArrayList<Tuple>> tupleLists){
		
		int num = tupleLists.size();
		long[] lens = new long[num];
		int i = 0;
		for(ArrayList<Tuple> l:tupleLists){
			
			lens[i++] = l.size();
		}
		ArrayList<Tuple> result = new ArrayList<Tuple>();
		for (long[] indices : new CartesianProduct(lens)) {
			Tuple t = new Tuple(mQuery.V);
			t.add(e.getQID(), e.getValue().id);
			for(i=0; i<num; i++){
				int idx = (int) (indices[i]);
				ArrayList<Tuple> tupleList = tupleLists.get(i);
			    Tuple ti = tupleList.get(idx);
			    t.copy(ti);
			    
			}
			
			result.add(t);
		}
		
		return result;
	}
	
	private void init() {
		mQuery.extractQueryInfo();
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
