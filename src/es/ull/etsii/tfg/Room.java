package es.ull.etsii.tfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Room {
	
	public static List<Room> Instances = new ArrayList<Room>();
	
	public static Room Instantiate(long id, List<String> players) {
		Room aux = new Room(id, players);
		Instances.add(aux);
		return aux;
	}
	
	public static final int IMAGES_PER_PLAYER = 10;
	public long ID;
	public Map<String, List<String>> DealtHands = new HashMap<String, List<String>>();
	
	Room(long id, List<String> players) {
		ID = id;
		List<Image> aux = new ArrayList<Image>(Image.Instances);
		Map<String, String> initials = new HashMap<String, String>();
		for (String player : players) {
			Image min = Collections.min(aux, Comparator.comparing(s -> s.ConceptCount()));
			aux.remove(min);
			initials.put(player, min.name);
			DealtHands.put(player, new ArrayList<String>());
			DealtHands.get(player).add(min.name);
		}
		
		Map<String, Map<String, Double>> distances = new HashMap<String, Map<String, Double>>();
		for (String image : initials.values()) {
			Map<String, Double> tmp = new HashMap<String, Double>();
			for (Image other : aux) {
				tmp.put(other.name, Image.Difference(image, other.name));
			}
			distances.put(image, tmp);
		}
		while (!AllHaveCards()) {
			for (String player : players) {
				for (String other : players) {
					if (!player.equals(other)) {
						String max = Collections.max(distances.get(initials.get(player)).entrySet(), Comparator.comparingDouble(Map.Entry::getValue)).getKey();
						for (Map.Entry<String, Map<String, Double>> j : distances.entrySet())
							j.getValue().remove(max);
						DealtHands.get(player).add(max);
					}
				}
			}
		}
		
	}
	
	boolean AllHaveCards() {
		for (Map.Entry<String, List<String>> i : DealtHands.entrySet())
			if (i.getValue().size() < IMAGES_PER_PLAYER)
				return false;
		return true;
	}
	
	public String pack() {
		String aux = "";
		for (Map.Entry<String, List<String>> i : DealtHands.entrySet()) {
			aux += i.getKey();
			for (String j : i.getValue())
				aux += "," + j;
			aux += "#";
		}
		return aux.substring(0, aux.length() - 1);
	}
	
	public String toString() {
		String aux = "ID = " + ID + "\n";
		for (Map.Entry<String, List<String>> i : DealtHands.entrySet()) {
			aux += i.getKey() + " --> ";
			for (String j : i.getValue())
				aux += j + ",";
			aux = aux.substring(0, aux.length() - 1) + "\n";
		}
		return aux;
	}

}
