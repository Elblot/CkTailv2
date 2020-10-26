package main.split;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import dependencies.Dependency;
import model.Event;
import model.Trace;

/**
 * @author Blot Elliott
 *
 */

public class MainSplit {

	public static String log;
	public static String output;
	public static boolean timerMode;
	public static String mode;

	public static HashMap<String, Double> means;
	public static Trace trace;
	public static Regex regex;
	public static Trace logOrigin;
	public static double interval = 5000.0;//in milliseconds 
	//public static double fact = 10.0;

	public static Dependency Dep;

	public static void main(String[] args) {
		//parse the log
		final long timebuildingTraces1 = System.currentTimeMillis();
		ArrayList<Trace> T = new ArrayList<Trace>();
		means = new HashMap<String, Double>();
		try {
			MapperOptions.setOptions(args);
		} catch (Exception e) {
			System.err.println("pb option");
			System.exit(3);
		}
		trace = new Trace(new File(log), regex);
		System.out.println("traces built");
		final long timebuildingTraces2 = System.currentTimeMillis();

		//split the log and detect sessions
		final long timesplit1 = System.currentTimeMillis();
		Dep = new Dependency();
		logOrigin = trace;
		System.out.println("mode: " + mode);
		if (mode.equals("classic")) {
			T.addAll(Split(trace));
		}
		else if (mode.equals("id")) {
			T.addAll(SplitID(trace));
		}
		System.out.println("split done");
		final long timesplit2 = System.currentTimeMillis();

		//write the traces in files
		final long timegenfile1 = System.currentTimeMillis();
		int i = 1;
		File dir = new File(output);
		dir.mkdir();
		dir = new File(output + "/traces");
		dir.mkdir();
		try {
			for(Trace t: T) {
				File f = new File(output+"/traces/"+i);
				t.writeTrace(f);
				i++;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		final long timegenfile2 = System.currentTimeMillis();

		//build the dependency graphs.
		final long timedag1 = System.currentTimeMillis();
		Dep.buildDAG(output);
		/*System.out.println("Dep : " + Dep.toString());
		System.out.println("DAG :\n " + Dep.getDag());*/
		System.out.println("Files generated");
		final long timedag2 = System.currentTimeMillis();

		if(timerMode) {
			System.out.println("building Traces: " + (timebuildingTraces2 - timebuildingTraces1) + " ms");
			System.out.println("split: " + (timesplit2 - timesplit1) + " ms");
			System.out.println("file generation: " + (timegenfile2 - timegenfile1) + " ms");
			System.out.println("dag generation: " + (timedag2 - timedag1) + " ms");
			System.out.println("Total: " + (timedag2 + timesplit2 + timebuildingTraces2 + timegenfile2 - timegenfile1 -  timebuildingTraces1 - timesplit1 - timedag1) + " ms");
		}
	}

	/**
	 * Algorithm Split that extract the traces of session and build the dependency lists.
	 * This algorithm is used when no session identifier is present in the events.
	 * (mode: classic). 
	 */
	public static ArrayList<Trace> Split(Trace trace){
		ArrayList<Trace> T = new ArrayList<Trace>();
		int i = 1;
		Event aj = trace.getEvent(i - 1);
		while(aj.isResp()) {
			System.out.println("first event is not a req: " + aj.debug());
			i++;
			if (i >= trace.getSize()) {
				return T;
			}
			aj = trace.getEvent(i - 1);
		}
		Trace tprime = new Trace();
		HashSet<ArrayList<Event>> SR = new HashSet<ArrayList<Event>>();
		HashSet<Event> OLReq = new HashSet<Event>();//
		Trace tprimeprime = new Trace(); 
		HashSet<String> SA = new HashSet<String>();
		SA.add(aj.getFrom());
		SA.add(aj.getTo());
		eventAnalysis:
			while (i <= trace.getSize()) {
				aj = trace.getEvent(i - 1);
				updateOLreq(OLReq, aj);
				//case 1
				ArrayList<Event> LReq = C1(SR, aj);	
				if(LReq != null) {
					tprime.addEvent(aj);
					OLReq.add(LReq.get(LReq.size() -1));
					LReq.remove(LReq.size() - 1);
					SA.add(aj.getFrom());
					SA.add(aj.getTo());
					i++;
					continue eventAnalysis;	
				}
				//case 2
				if(C2(OLReq, aj)) {
					tprime.addEvent(aj);
					SA.add(aj.getFrom());
					SA.add(aj.getTo());
					i++;
					continue eventAnalysis;	
				}
				//case 3
				LReq = C3(SR, aj);
				if (LReq != null) {
					checkDependencies(tprime, aj); //make dep if not done
					tprime.addEvent(aj);
					LReq.add(aj);
					SA.add(aj.getFrom());
					SA.add(aj.getTo());
					Dep.extend(LReq);
					i++;
					continue eventAnalysis;			
				}
				//case 4
				if (aj.isReq() && SA.contains(aj.getFrom()) && (tprime.isEmpty() || checkTime(tprime.getEvent(0), aj) || checkDependencies(tprime, aj))) {
					checkDependencies(tprime, aj); //make dep if not done
					tprime.addEvent(aj);
					ArrayList<Event> LR = new ArrayList<Event>();
					LR.add(aj);
					SR.add(LR);
					SA.add(aj.getFrom());
					SA.add(aj.getTo());
					Dep.extend(LR);
					i++;
					continue eventAnalysis;	
				}
				//case 5
				if (aj.isInter() && (tprime.isEmpty() || checkTime(tprime.getEvent(0), aj) || checkDependencies(tprime, aj) )) {
					checkDependencies(tprime, aj); //make dep if not done
					tprime.addEvent(aj);
					i++;
					continue eventAnalysis;	
				}
				tprimeprime.addEvent(aj);
				i++;
			}
		boolean empty = true;
		for (ArrayList<Event> LReq: SR) {
			if (!LReq.isEmpty()) { 
				empty = false; 
			}
		}
		if (empty){
			T.add(tprime);
		}
		else {
			System.out.println("not finished, there are stil pending requests: " + tprime.debug());
		}
		if (!tprimeprime.isEmpty()) {
			T.addAll(Split(tprimeprime));
		}
		return T;
	}

	/**
	 * Algorithm Split that extract the traces of session and build the dependency lists.
	 * This algorithm is used when session identifier are present in the events.
	 * (mode: id). 
	 */
	public static ArrayList<Trace> SplitID(Trace trace){
		ArrayList<Trace> T = sepID(trace);
		HashSet<ArrayList<Event>> SR = new HashSet<ArrayList<Event>>();
		HashSet<String> SA = new HashSet<String>();
		for (Trace t : T) {
			int i = 1;
			Event aj = t.getEvent(i - 1);
			SA.add(aj.getFrom());
			SA.add(aj.getTo());
			eventAnalysis:
				while (i <= t.getSize()) {
					aj = t.getEvent(i - 1);
					//case1
					ArrayList<Event> LReq = C1(SR, aj);	
					if(LReq != null) {
						LReq.remove(LReq.size() - 1);
						SA.add(aj.getFrom());
						SA.add(aj.getTo());
						i++;
						continue eventAnalysis;	
					}
					//case 3
					LReq = C3(SR, aj);
					if (LReq != null) {
						LReq.add(aj);
						Dep.extend(LReq); 
						SA.add(aj.getFrom());
						SA.add(aj.getTo());
						i++;
						continue eventAnalysis;			
					}
					//case 4
					if (aj.isReq() && SA.contains(aj.getFrom()) && (i == 1 || checkTime(t.getEvent(0), aj) || checkDependenciesID(t, t.subTrace(0, i), aj))) {
						checkDependenciesID(t, t.subTrace(0, i), aj); //make dep if not done
						ArrayList<Event> LR = new ArrayList<Event>();
						LR.add(aj);
						SR.add(LR);
						SA.add(aj.getFrom());
						SA.add(aj.getTo());
						Dep.extend(LR);
						i++;
						continue eventAnalysis;	
					}
					i++;
				}
		}
		return T;
	}

	/**
	 * Split the log into several traces, each representing a session,
	 * using the session identifiers in the events.
	 */
	private static ArrayList<Trace> sepID(Trace trace){
		ArrayList<Trace> res =  new ArrayList<Trace>();
		ArrayList<String> ids = new ArrayList<String>();
		int size = trace.getSize();
		for (int i = 0; i < size; ++i) {
			Event e = trace.getEvent(i);
			String id = e.getSessionID();
			if (!ids.contains(id)) {
				ids.add(id);
				res.add(new Trace());
			}
			int index = ids.indexOf(id);
			res.get(index).addEvent(e);
		}		
		return res;
	}

	/**
	 * Update the OLReq set
	 */
	private static void updateOLreq(HashSet<Event> OLReq, Event ai) {
		HashSet<Event> Lr = new HashSet<Event>();
		if (ai.isResp()) {
			for (Event e : OLReq) {
				if (e.getTo().equals(ai.getFrom())) {
					Lr.add(e);
				}
			}
		}
		@SuppressWarnings("unchecked")
		HashSet<Event> save = (HashSet<Event>) OLReq.clone();
		for (Event e : save) {
			if (e.getFrom().equals(ai.getFrom()) | e.getTo().equals(ai.getFrom())) {
				OLReq.remove(e);
			}
		}
		OLReq.addAll(Lr);
	}

	/**
	 * Check if condition C1 is validated:
	 * ai is a response of a pending request.
	 * Return the list containing the request linked to the response. 
	 */
	private static ArrayList<Event> C1(HashSet<ArrayList<Event>> SR, Event ai){
		if (ai.isReq()) {
			return null;
		}
		ArrayList<Event> res = null;
		for (ArrayList<Event> LReq : SR) {
			if (!LReq.isEmpty() && ai.getFrom().equals(LReq.get(LReq.size() - 1).getTo()) &&
					ai.getTo().equals(LReq.get(LReq.size() - 1).getFrom())) {
				if (res == null) {
					res = LReq;
				}
				else {// has to be unique.
					return null;
				}
			}
		}
		return res;
	}

	/**
	 * Check if condition C2 is validated.
	 * Return true if ai form nested request with pending requests.
	 */
	private static boolean C2(HashSet<Event> OLReq, Event ai){
		if (ai.isReq()) {
			return false;
		}
		for (Event e : OLReq) {
			if (ai.getFrom().equals(e.getTo()) &&
					ai.getTo().equals(e.getFrom())){
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if condition C3 is validated.
	 * ai is a response of a request that has already received a response.
	 * Return the list containing the request linked to the response. 
	 */
	private static ArrayList<Event> C3(HashSet<ArrayList<Event>> SR, Event ai){
		if (!ai.isReq()) {
			return null;
		}
		for (ArrayList<Event> LReq : SR) {
			if (!LReq.isEmpty() && ai.getFrom().equals(LReq.get(LReq.size() - 1).getTo()) && !pendingRequest(ai, SR))	{
				return LReq;
			}
		}
		return null;
	}

	/**
	 * Return true if the source of aj has a pending request
	 */
	public static boolean pendingRequest(Event aj, HashSet<ArrayList<Event>> SR) {
		for (ArrayList<Event> LReq: SR) {
			for (Event e: LReq) {
				if (e.getFrom().equals(aj.getFrom())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check the time between the event first and aj is lower than interval.
	 */
	public static boolean checkTime(Event first, Event aj) {
		double diff = aj.getDate(regex).getTime() - first.getDate(regex).getTime();
		if (diff > 0 && diff < interval) { 
			return true;
		}
		return false;
	}

	/**
	 *  Check if there exist data dependencies in t, including the event aj 
	 *  used in mode: classic
	 */
	public static boolean checkDependencies(Trace t, Event aj) {
		ArrayList<Event> chain = new ArrayList<Event>();
		Trace sub = logOrigin.subTrace(0,logOrigin.indexOf(aj));//
		ArrayList<Event> dependency = new ArrayList<Event>();
		int c = 0;
		ArrayList<Event> begin = new ArrayList<Event>();
		begin.add(aj);
		for (@SuppressWarnings("unused") Event e: begin) {
			ArrayList<Event> dep = checkDependencies(sub, aj, chain);//
			if (!dep.isEmpty()) {
				c++;
				dependency.addAll(dep);
				if (c > 1) {
					return false;
				}
			}
		}
		if (c == 1 && t.containsAll(dependency) && !aj.getTo().equals(dependency.get(0).getFrom())) {
			if (!aj.isInter()) {
				ArrayList<String> ld = new ArrayList<String>();
				ld.add(aj.getTo());
				ld.add(dependency.get(0).getFrom());
				Dep.add(ld);
			}
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 *  Check if there exist data dependencies in t, including the event aj 
	 *  used in mode: id
	 */
	public static boolean checkDependenciesID(Trace origin, Trace t, Event aj) {
		ArrayList<Event> chain = new ArrayList<Event>();
		Trace sub = origin.subTrace(0,origin.indexOf(aj));//
		ArrayList<Event> dependency = new ArrayList<Event>();
		int c = 0;
		ArrayList<Event> begin = new ArrayList<Event>();
		begin.add(aj);
		for (@SuppressWarnings("unused") Event e: begin) {
			ArrayList<Event> dep = checkDependencies(sub, aj, chain);//
			if (!dep.isEmpty()) {
				c++;
				dependency.addAll(dep);
				if (c > 1) {
					return false;
				}
			}
		}
		if (c == 1 && t.containsAll(dependency) && !aj.getTo().equals(dependency.get(0).getFrom())) {
			if (!aj.isInter()) {
				ArrayList<String> ld = new ArrayList<String>();
				ld.add(aj.getTo());
				ld.add(dependency.get(0).getFrom());
				Dep.add(ld);
			}
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Try to find the longest chain of event such that there is data dependency
	 * between them, and return it if it is unique.
	 */
	public static ArrayList<Event> checkDependencies(Trace t, Event aj, ArrayList<Event> chain) {
		ArrayList<Event> res = new ArrayList<Event>();
		Event aprime = null;
		boolean one = false;
		int j = 0;
		for(int i = t.getSize() - 1 ; i >= 0; i--) {
			Event e = t.getEvent(i);

			if (e.dataSimilarity(aj) && e.getTo().equals(aj.getFrom()) && !e.isInter() && !chain.contains(e)){
				if (one) {
					return new ArrayList<Event>();
				}
				aprime = e;
				one = true;
				j = i;
				res.add(e);
				res.addAll(chain);
			}
		}
		if (one) {
			res = checkDependencies(t.subTrace(0, j), aprime, res);
		}
		else {
			res.addAll(chain);
		}
		return res;
	}

	/**
	 * Get the mean of time between request of comp.
	 * May be used to define a new CheckTime.
	 * 
	 */
	public static double getMean(String comp) {
		if (means.containsKey(comp)) {
			return means.get(comp);
		}
		else {
			double sum = 0.0;
			double c = 0.0;
			Date d1 = null;
			for (Event e: trace.getSeq()) {
				if ((e.getFrom().equals(comp) || e.getTo().equals(comp)) && e.isReq()) {
					Date d2 = e.getDate(regex);
					if(d1 == null){
						d1 = d2;
						continue;
					}
					else {
						double dif = d2.getTime()-d1.getTime();
						c++;
						sum = sum + dif;
						d1 = d2;
					}
				}
			}
			means.put(comp, sum/c);
			return sum/c;
		}
	}

}
