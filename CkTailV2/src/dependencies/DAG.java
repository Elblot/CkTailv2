package dependencies;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * 
 * @author Blot Elliott
 *
 */

public class DAG {
	public ArrayList<String> nodes;
	public HashSet<String[]> transitions;

	public DAG() {
		transitions = new HashSet<String[]>();
	}

	/**
	 *  Build the node of the graph once the transitions are done.
	 */
	public void makeNodes() {
		nodes = new ArrayList<String>();
		for (String[] trans: transitions) {
			for (String node: trans) {
				nodes.add(node);
			}
		}
	}

	/**
	 * Add a node in the list of nodes of the DAG.
	 * @param node
	 */
	public void addNode(String node) {
		nodes.add(node);
	}

	/**
	 * Add several nodes in the list of nodes of the DAG.
	 * @param node (HashSet<String>)
	 */
	public void addNodes(HashSet<String> node) {
		nodes.addAll(node);
	}

	/**
	 * Add the transition (node1, node2) to the set of transitions of the DAG.
	 * @param node1
	 * @param node2
	 */
	public void addTransition(String node1, String node2) {
		String[] transition = {node1, node2};
		for (String[] trans: transitions) {
			if (transition[0].equals(trans[0]) && transition[1].equals(trans[1])) {
				return;
			}
		}
		transitions.add(transition);
	}

	/**
	 * ADD several transitions to the set of transitions of the DAG,
	 * according to a list of component representing dependencies.
	 * @param dep
	 */
	public void addTransition(ArrayList<String> dep) {
		for (int i = 0; i < dep.size() - 1; i++) {
			addTransition(dep.get(i), dep.get(i+1));
		}
	}

	/**
	 *  Write the dot file representing the DAG in filename. 
	 *  @param filename
	 */
	public void dotGen(String filename) {
		String comp = "";
		try {
			File dot = new File(filename);
			BufferedWriter bw;
			bw = new BufferedWriter(new FileWriter(dot, true));
			bw.write("digraph DAG {\n");
			for (String compo: nodes) {
				if (filename.contains("/DAG/" + compo + ".dot")){
					comp = compo;
					bw.write("S" + nodes.indexOf(compo) +"[label=\"" + compo + "\",fillcolor=grey,style=filled];\n");
				}
				else {
					bw.write("S" + nodes.indexOf(compo) +"[label=\"" + compo + "\"];\n");
				}
			}
			for (String[] trans: transitions) {
				bw.write("S"+ nodes.indexOf(trans[0]) + " -> S" + nodes.indexOf(trans[1]) + "\n");
			}
			bw.write("}");
			bw.close();
		}catch (FileNotFoundException e) {
			System.out.println("file not found " + e);
		}catch (IOException e){
			System.out.println("error " + e);
		}
		
		/* generate the pdf from the dot */ 
		/* don't work and I don't know why */
		String execCommand = "dot -Tpdf " + comp + ".dot -o " + comp + ".pdf";
		Process dotProcess;
		try {
			dotProcess = Runtime.getRuntime().exec(execCommand);
		} catch (IOException e) {
			System.err.println("Could not run dotCommand '" + execCommand + "': " + e.getMessage());
			return;
		}

		try {
			dotProcess.waitFor();
		} catch (InterruptedException e) {
			System.err.println("Waiting for dot process interrupted '" + execCommand + "': " + e.getMessage());
			return;
		}
	}


}
