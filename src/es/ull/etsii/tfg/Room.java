package es.ull.etsii.tfg;

import java.util.ArrayList;
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
	
	public static final int IMAGES_PER_PLAYER = 5;
	public long ID;
	public Map<String, List<String>> DealtHands = new HashMap<String, List<String>>();
	
	Room(long id, List<String> players) {
		ID = id;
		int index = 0;
		for (String player : players) {
			List<String> aux = new ArrayList<String>();
			for (int i = 0; i < IMAGES_PER_PLAYER; i++)
				aux.add(Image.Instances.get(index * IMAGES_PER_PLAYER + i).name);
			DealtHands.put(player, aux);
			index++;
		}
		//System.out.println(toString());
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
