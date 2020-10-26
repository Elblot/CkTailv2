package main.modelGen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * @author Blot Elliott
 *
 */

public class Extract {

	private static ArrayList<String> identifiers = new ArrayList<String>();
	private static String separator = "|||";

	
	/**
	 * Build the new traces containing the behaviour of a single component.
	 */
	public static String analysis(String[] traces, String fName){
		int j = 1;
		File x = new File(fName + "/trace");
		x.mkdirs();
		ArrayList<ArrayList<String>> alFiles = loadFiles(traces);
		buildID(alFiles);
		for(ArrayList<String> alFile:alFiles) {
			for(int i = 0; i < alFile.size(); i++) {
				String event = alFile.get(i);
				int source = identifiers.indexOf(getSource(event));
				int destination = identifiers.indexOf(getDestination(event));
				BufferedWriter bw;
				try {			
					if (source == destination) { // internal actions
						bw = new BufferedWriter(new FileWriter(new File(fName + "/trace/T" + (j+source-1)+"tmp"), true));
						bw.write(event + "\n");
						bw.close();
					}
					else {
						bw = new BufferedWriter(new FileWriter(new File(fName + "/trace/T" + (j+source-1)+"tmp"), true));
						bw.write("!" + event + "\n");
						bw.close();
						bw = new BufferedWriter(new FileWriter(new File(fName + "/trace/T" + (j+destination-1)+"tmp"), true));
						bw.write("?" + event + "\n");
						bw.close();
					}
				}
				catch (FileNotFoundException e) {
					System.out.println("file /trace/T"+  (j+source-1) + "tmp or file /trace/T"+  (j+destination-1) + "tmp not found " + e);
					return null;
				} catch (IOException e){
					System.out.println("unknown error " + e);
					return null;
				}
			}
			j+=identifiers.size();
		}
		return fName + "/trace";
	}

	/**
	 * Build the ArrayList that contains all the identifier of the component
	 */
	static void buildID(ArrayList<ArrayList<String>> files) {
		for(ArrayList<String> alFile:files) {
			for(int i = 0; i < alFile.size(); i++) {
				String Host = getSource(alFile.get(i));
				String Dest = getDestination(alFile.get(i));
				if (!identifiers.contains(Host)) {
					identifiers.add(Host);
				}
				if (!identifiers.contains(Dest)) {
					identifiers.add(Dest);
				}
			}
		}
	}

	/**
	 * Get the source of the message.
	 */
	private static String getSource(String sequence) {
		String Host = "????";
		int h = sequence.indexOf("Host=");
		if (h != -1) { //TODO else throw exception
			if (sequence.indexOf(separator, h+5) > h) {
				Host = sequence.substring(h + 5, sequence.indexOf(separator, h+5));
			}
			else {
				Host = sequence.substring(h + 5, sequence.indexOf(")", h+5));
			}
		}
		return Host;
	}

	/**
	 * Get the destination of the message.
	 */
	private static String getDestination(String sequence) {
		String Dest = "????";
		int h = sequence.indexOf("Dest=");
		if (h != -1) { //TODO else throw exception
			if (sequence.indexOf(separator, h+5) > h) {
				Dest = sequence.substring(h + 5, sequence.indexOf(separator, h+5));
			}
			else {
				Dest = sequence.substring(h + 5, sequence.indexOf(")", h+5));
			}
		}
		return Dest;
	}

	/**
	 * Read the traces and save them in an ArrayList.
	 */
	public static ArrayList<ArrayList<String>> loadFiles(String[] traces){
		ArrayList<ArrayList<String>> alFiles = new ArrayList<ArrayList<String>>();
		String line;
		try {
			for(int i = 0; i < traces.length; i++) {
				ArrayList<String> alFile = new ArrayList<String>();
				File f = new File(traces[i]);
				BufferedReader br = new BufferedReader(new FileReader(f));
				line = br.readLine();
				while(line != null) {
					alFile.add(line);
					line = br.readLine();
				}
				alFiles.add(alFile);
				br.close();
			}
			return alFiles;
		}catch(Exception e) {}
		return null;
	}
	
}
