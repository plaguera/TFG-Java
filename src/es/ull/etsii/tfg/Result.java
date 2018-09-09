package es.ull.etsii.tfg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Result {
	
	public static List<Result> Instances = new ArrayList<Result>();
	
	public static void Load(String path) {
		String line;
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            while ((line = br.readLine()) != null) {
            	String[] tokens = line.split(";");
            	String concept = tokens[0];
            	Map<String, Integer> votes = new HashMap<String, Integer>();
            	for (int i = 1; i < tokens.length; i++) {
            		String[] subtokens = tokens[i].split(",");
            		votes.put((subtokens[0]), Integer.parseInt(subtokens[1]));
            	}
            	Instantiate(concept, votes);
            }
        } catch (IOException e) {
        	return;
            //e.printStackTrace();
        }
	}
	
	public static Result Instantiate(String concept, Map<String, Integer> votes) {
		Result aux = new Result(concept, votes);
		Instances.add(aux);
		return aux;
	}
	
	public String Concept;
	public Map<String, Integer> Votes;
	
	private Result(String concept, Map<String, Integer> votes) {
		Concept = concept;
		Votes = votes;
		try { Files.write(Paths.get(Path.RESULTS), toString().getBytes(), StandardOpenOption.APPEND); }
		catch (IOException e) { e.printStackTrace(); }
	}
	
	@Override
	public String toString() {
		String aux = Concept;
		for (Map.Entry<String, Integer> i : Votes.entrySet()) aux += ";" + i.getKey() + "," + i.getValue();
		return aux + "\n";
	}

}
