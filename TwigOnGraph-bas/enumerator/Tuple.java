package enumerator;

import java.util.Arrays;

public class Tuple {

	int[] entry;
	
	int len;
	
	public Tuple(int len){
		
		entry = new int[len];
		this.len = len; 
		Arrays.fill(entry, -1);
		
	}
	
	public void copy(Tuple o){
		
		for (int i=0; i<len; i++){
			
			if(o.entry[i]!=-1){
				
				entry[i] = o.entry[i];
			}
		}
	}
	
	public void add(int idx, int val){
		
	    entry[idx] = val;	
	}
	
	public void destroy(){
		
		entry = null;
	}
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		for(int v:entry){
			
			s.append(v + " ");
		}
		
		return s.toString();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
