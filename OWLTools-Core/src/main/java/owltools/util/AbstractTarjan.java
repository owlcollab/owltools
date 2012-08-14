package owltools.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Basic implementation for the Tarjan algorithm. This implementation relies on
 * adjacency information provided by {@link Adjacency}.
 * 
 * @param <NODE> identifier for a node in the graph
 */
public abstract class AbstractTarjan<NODE> implements Tarjan<NODE> {

	private int index = 0;
	private Stack<NODE> stack = new Stack<NODE>();
	private List<List<NODE>> SCC = new ArrayList<List<NODE>>();

	@Override
	public final List<List<NODE>> executeTarjan(Adjacency<NODE> graph){
		SCC.clear();
		index = 0;
		stack.clear();
		if(graph != null){
			for (NODE node : graph.getSources())
			{
				if(getIndex(node) == -1)
				{
					tarjan(node, graph);
				}
			}
		}
		return SCC;
	}
	
	protected abstract void setIndex(NODE n, int index);
	
	protected abstract int getIndex(NODE n);
	
	protected abstract void setLowlink(NODE n, int lowlink);
	
	protected abstract int getLowlink(NODE n);
	
	protected abstract boolean notEquals(NODE n1, NODE n2);
	
	/**
	 * Check: is the NODE n in the given stack<br>
	 * <br>
	 * <b>WARNING</b>: This is currently an inefficient implementation. 
	 * This should be converted from a linear lookup to a constant operation.
	 * 
	 * @param stack
	 * @param n
	 * @return boolean
	 */
	protected boolean isInStack(Stack<NODE> stack, NODE n) {
		// TODO make this more efficient
		return stack.contains(n);
	}
	
	private void tarjan(NODE v, Adjacency<NODE> adjacencyInfo){
		setIndex(v, index);
		setLowlink(v, index);
		index++;
		stack.push(v);
		for(NODE n : adjacencyInfo.getAdjacent(v)){
			if(getIndex(n) == -1){
				tarjan(n, adjacencyInfo);
				setLowlink(v, Math.min(getLowlink(v), getLowlink(n)));
			}else if(isInStack(stack, n)){
				setLowlink(v, Math.min(getLowlink(v), getIndex(n)));
			}
		}
		if(getLowlink(v) == getIndex(v)){
			NODE n;
			ArrayList<NODE> component = new ArrayList<NODE>();
			do{
				n = stack.pop();
				component.add(n);
			}while(notEquals(n, v));
			SCC.add(component);
		}
	}
}