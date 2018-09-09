package es.ull.etsii.tfg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Scores {
	
	static Map<String, Integer> Instances = new HashMap<String, Integer>();
	
	public static void Load(String path) {
		String line;
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            while ((line = br.readLine()) != null) {
            	String[] tokens = line.split(";");
            	if (tokens.length < 2) continue;
            	Instances.put(tokens[0], Integer.parseInt(tokens[1]));
            }
        } catch (IOException e) {
        	return;
            //e.printStackTrace();
        }
	}
	
	public static void Add(String player, int value) {
		if (Instances.containsKey(player))
			Instances.computeIfPresent(player, (k, v) -> v + value);
		else
			Instances.put(player, value);
	}
	
	public static Map<String, Integer> sort() {
		return Instances.entrySet().stream()
                .sorted(Collections.reverseOrder(Entry.comparingByValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                         (e1,e2) -> e1, LinkedHashMap::new));
	}
	
	public static String instancesToString() {
		String aux = "";
		for (Map.Entry<String, Integer> i : sort().entrySet()) aux += i.getKey() + ";" + i.getValue() + "\n";
		return aux.substring(0, aux.length()-1);
	}
	
	public static void toFile() {
		try { Files.write(Paths.get(Path.RESULTS), instancesToString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING); }
		catch (IOException e) { e.printStackTrace(); }
	}

}
