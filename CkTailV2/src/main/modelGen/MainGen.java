package main.modelGen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import fsa.FSA;
import fsa.GenerateDOT;
import miners.KTail.KTail;
import traces.Trace;

/**
 * @author Blot Elliott
 */

public class MainGen {
	
	static int rank = 2;
	static String dir;
	static boolean timerMode;
	static boolean tmp;
	static String dest;
	private static String separator = "|||";
	
	public static void main(String[] args) throws Exception {
		final long timeProg1 = System.currentTimeMillis();
		KtailOptions.setOptions(args);
		String[] tracesF = getTraces(dir);
		
		//1st part : IOLTS generation : split by component id
		final long timeCor1 = System.currentTimeMillis();
		String s = Extract.analysis(tracesF, dest);
		final long timeCor2 = System.currentTimeMillis();
		
		
		//2nd part : IOLTS generation : grouping
		final long timeClust1 = System.currentTimeMillis();
		sortFile();
		final long timeClust2 = System.currentTimeMillis();
		Group c = new Group(s);
		ArrayList<ArrayList<Trace>> alTraces = c.Synchronization();
		
		//3rd part : IOLTS generation : KTail
		final long timeKTail1 = System.currentTimeMillis();
		KTail instance = new KTail(rank);
		File d = new File(dest + "/dot");
		d.mkdirs();
		String destdot = dest + "/dot";
		for (ArrayList<Trace> traces : alTraces) {
			FSA test = instance.transform(traces);
			String i = getId(traces);
	    	GenerateDOT.printDot(test, destdot+"/"+i+"tmp.dot");
		}
		final long timeKTail2 = System.currentTimeMillis();
		
		//4th part : make the .dot file readable.
		final long timePars1 = System.currentTimeMillis();	
		String j;
		Pattern pat = Pattern.compile(".*/[^/]+tmp.dot");
		File dossier = new File(destdot);
		if (!dossier.exists()) {
		}
		File[] racine = dossier.listFiles();
		for (File dot : racine) {
			String dotStr = dot.toString();
			Matcher m = pat.matcher(dotStr);
			if(m.matches()) {
				j = dotStr.substring(destdot.length()+1, dotStr.length()-7);
				ParserDot parser = new ParserDot(j);
				String fileName = parser.parser(dot);
				GraphExporter.generatePngFileFromDotFile(fileName);
				if(!tmp) {
					dot.delete();
				}
			}
		}
		final long timePars2 = System.currentTimeMillis();
		
		final long timeProg2 = System.currentTimeMillis();
		if (timerMode) {
			System.out.println("Segmentation Duration: " + (timeCor2 - timeCor1) + " ms");
			System.out.println("Grouping Duration: " + (timeClust2 - timeClust1) + " ms");
			System.out.println("KTail Duration: " + (timeKTail2 - timeKTail1) + " ms");
			System.out.println("Parser Duration: " + (timePars2 - timePars1) + " ms");
            System.out.println("Program Duration: " + (timeProg2 - timeProg1) + " ms");
        }	
		
		File x = new File(MainGen.dest+"/RESULTAT.txt");
		if(x.exists()) {
			BufferedWriter br = new BufferedWriter(new FileWriter(x, true));
			br.write("--------------------------------\nTOTAL :\n"+ParserDot.nbEtatTot+" States\n"
				+ParserDot.nbTransitionTot+" Transitions\n"+ "Segmentation Duration: " 
				+ (timeCor2 - timeCor1) + " ms\nGrouping Duration: " + (timeClust2 - timeClust1) 
				+ " ms\n"+ "KTail Duration: " + (timeKTail2 - timeKTail1) + " ms\nParser Duration: " 
				+ (timePars2 - timePars1) + " ms\n"+ "Program Duration: " + (timeProg2 - timeProg1) + " ms\n");
			br.close();
		}
	}
	
	/**
	 * Get the identifier of the component represented in the set traces
	 */
	private static String getId(ArrayList<Trace> traces) {
		Trace t = traces.get(0);
		String event = t.getStatement(0).toString();
		if (event.startsWith("!")) {
			int h = event.indexOf("Host=");
			if (h != -1) {
				if (event.indexOf(separator, h+5) > h) {
					return event.substring(h + 5, event.indexOf(separator, h + 5));
				}
				else {
					return event.substring(h + 5, event.indexOf(")", h + 5));
				}
			}
		}
		else if (event.startsWith("?")) {
			int d = event.indexOf("Dest=");
			if (d != -1) {
				if (event.indexOf(separator, d+5) > d) {
					return event.substring(d + 5, event.indexOf(separator, d + 5));
				}
				else {
					return event.substring(d + 5, event.indexOf(")", d + 5));
				}
			}
		}
		return t.toString();

		
	}
	
	/**
	 * Sort the trace files, easing there analysis.
	 * @throws IOException
	 */
	private static void sortFile() throws IOException {
		File repertoire = new File(dest+"/trace/");
		File[] files = repertoire.listFiles();
		int j = 1;
		for (File f: files) {
			InputStream input = new FileInputStream(f);
			File out = new File(dest+"/trace/T"+j);
			OutputStream output = new FileOutputStream(out);
			IOUtils.copy(input, output);
			f.delete();
			input.close();
			output.close();
			j++;
		}
	}

	/**
	 * Return the list of traces 
	 */   
    private static String[] getTraces(String dir){
    	File d = new File(dir);
    	String[] traces = null;
    	if (d.exists()) {
    		if (d.isDirectory()) {
        		traces = d.list();
        		String[] tracesP = new String[traces.length];
        		for(int i = 0; i<traces.length; i++) {
        			tracesP[i] = MainGen.dir+"/"+traces[i];
        		}
        		return tracesP;
    		}
    	}
    	return traces;
    }
}
