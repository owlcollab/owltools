package owltools.profile;

import java.util.HashMap;
import java.util.Map;

public class Profiler {
	
	Map<String,Long> taskTotalTimeMap = new HashMap<String,Long>();
	Map<String,Long> taskInitTimeMap = new HashMap<String,Long>();

	public Profiler() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public void startTaskNotify(String task) {
		taskInitTimeMap.put(task, System.nanoTime());
	}

	public void endTaskNotify(String task) {
		long ct = 0;
		if (taskTotalTimeMap.containsKey(task)) {
			ct = taskTotalTimeMap.get(task);
		}
		long t1 = taskInitTimeMap.get(task);
		long t2 = System.nanoTime();
		taskTotalTimeMap.put(task, ct + (t2-t1));
	}
	
	public void report() {
		for (String task : taskTotalTimeMap.keySet()) {
			System.out.println("TASK:"+task+" TIME:"+((float)taskTotalTimeMap.get(task)) / 1000000000);
		}
	}
	
}
