package main.modelGen;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;



public class ParserDot {

	String j;
	int nbEtat = 0, nbTransition = 0;
	static int nbEtatTot, nbTransitionTot;


	public ParserDot(String j) {
		this.j = j;
	}


	public String parser(File f) {
		File file = new File(MainGen.dest+"/RESULTAT.txt");
		try {
			file.createNewFile();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		String fileName = null;
		ArrayList<String> arStrAvecDoublon = new ArrayList<String>();
		try {
			arStrAvecDoublon = readFile(f);
		}catch (IOException e1) {
			e1.printStackTrace();
		}
		String strEntier = nettoie(arStrAvecDoublon);
		ArrayList<Integer> listInt = spliter(strEntier);
		Set<Integer> sInt = new TreeSet<Integer>();
		sInt.addAll(listInt);
		ArrayList<Integer> finalInt = new ArrayList<Integer>(sInt);
		String stRemplaceInt = remplaceInt(finalInt, strEntier);
		try {
			fileName = ecriture(finalInt, stRemplaceInt);
		}catch (IOException e) {
			e.printStackTrace();
		}

		try {
			resultat(file);
		}catch(IOException e) {
			e.printStackTrace();
		}
		nbEtatTot = nbEtatTot+nbEtat;
		nbTransitionTot=nbTransitionTot+nbTransition;
		return fileName;
	}


	/**
	 * Reads the file f and stocks lines in an ArrayList.
	 */
	public ArrayList<String> readFile(File f) throws IOException {
		String line;
		ArrayList<String> tab = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(f));
		line = br.readLine();
		while(line != null) {
			tab.add(line);
			line = br.readLine();
		}
		br.close();
		f = null;
		return tab;
	}


	/**
	 * Removes duplicated string
	 */
	public String nettoie(ArrayList<String> arStrAvecDoublon) {
		Set<String> set = new LinkedHashSet<String>();
		set.addAll(arStrAvecDoublon);	
		ArrayList<String> arStrSansDoublon = new ArrayList<String>(set);
		String strEntier="";
		for(String sr:arStrSansDoublon) {
			strEntier = strEntier + sr + "\n";
		}
		return strEntier;
	}


	/**
	 * Obtains state numbers in an ArrayList
	 */
	public ArrayList<Integer> spliter(String str) {
		String[] splited = str.split("\n");
		ArrayList<Integer> listInt = new ArrayList<Integer>();		
		for(String line :splited) {
			if (line.startsWith("S")){
				if (line.contains(" -> ")) {
					String src = line.substring(1, line.indexOf(" -> "));
					listInt.add(Integer.parseInt(src));
					if (line.contains("[")) {
						String dest = line.substring(line.indexOf(" -> ") + 5, line.indexOf("[label"));
						listInt.add(Integer.parseInt(dest));
					}
					else {
						String dest = line.substring(line.indexOf(" -> ") + 5);
						listInt.add(Integer.parseInt(dest));
					}
				}
				else {
					String node = line.substring(1, line.indexOf("["));
					listInt.add(Integer.parseInt(node));
				}
			}
		}
		return listInt;
	}


	/**
	 * Replaces unnatural state names by new ones
	 */
	public String remplaceInt(ArrayList<Integer> finalInt, String strEntier) {
		String st="";
		for(int i = 0; i < finalInt.size(); i++) {
			st = strEntier.replaceAll("S"+finalInt.get(i)+" ", "S"+i+" ");
			strEntier = st;
		}
		for(int i = 0; i < finalInt.size(); i++) {
			st = strEntier.replaceAll("S"+finalInt.get(i)+"\\[", "S"+i+"\\[");
			strEntier = st;
		}
		return st;
	}


	/**
	 * Rewrite dot file with changes
	 */
	public String ecriture(ArrayList<Integer> finalInt, String st) throws IOException {
		String[] atrier = st.split("\n");
		HashMap<Integer, String> intShape = new HashMap<Integer, String>();
		for(String str:atrier) {
			if(str.contains("shape=")) {
				int numero = Integer.parseInt(str.substring(1, str.indexOf("[")));
				String shape = str.substring(str.indexOf("shape="), str.indexOf("]"));
				intShape.put(numero, shape);
			}
		}

		String fileName = MainGen.dest+"/dot/"+j+".dot";
		File file = new File(fileName);
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.write(atrier[0]+"\n");
		bw.write(atrier[1]+"\n");
		for(int i = 1; i < finalInt.size(); i++) {
			if(intShape.containsKey(i)) {
				bw.write("S" + i + "[label=S" + i + "," + intShape.get(i) + "];\n");
			}
			else {
				bw.write("S"+i+"[label=S"+i+",shape=circle];\n");
			}
			this.nbEtat++;
		}		
		bw.write("S00 -> S1\n");
		for(int i = 1; i < finalInt.size(); i++) {
			ArrayList<String> ar = new ArrayList<String>();
			for(int k = 3; k < atrier.length-1; k++) {
				if(atrier[k].contains("S"+i+" ") || atrier[k].contains("S"+i+"[") && !atrier[k].contains(" S"+i)){
					ar.add(atrier[k]);
				}
			}
			for(int l = 0; l < finalInt.size(); l++) {
				for(int m = 0; m < ar.size(); m++) {
					if(ar.get(m).contains("-> S"+l+"[")) {
						bw.write(ar.get(m)+"\n");
						this.nbTransition++;
					}
				}
			}
		}
		bw.write(atrier[atrier.length-1]+"\n");
		bw.close();
		return fileName;
	}


	public void resultat(File f) throws IOException {
		if(!f.exists()) {
			System.err.println("error, folder RESULTS does not exist");
			System.exit(1);
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
		bw.write(j+".dot contains :\n"+this.nbEtat+" states\n"+this.nbTransition+" transitions\n\n");
		bw.close();
	}

}
