package es.ull.etsii.tfg;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

public class Image {

	public BufferedImage bi;
	public String name;
	public long size;
	public Map<String, List<Integer>> ConceptScores; 
	
	public static List<Image> Instances = new ArrayList<Image>();
	
	public static void Instantiate(String path) {
		Instances.add(new Image(path));
	}
	
	public static void LoadImages(String folder) {
		for (final File fileEntry : new File(folder).listFiles()) {
	        if (fileEntry.isFile() && IsImage(fileEntry.getName())) Image.Instantiate(fileEntry.getAbsolutePath());
	    }
		System.out.println("Found " + Instances.size() + " Images !!");
	}
	
	static boolean IsImage(String name) {
		int i = name.lastIndexOf('.');
		if (i >= 0) {
		    String extension = name.substring(i+1);
		    return extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg");
		}
		return false;
	}
	
	public static Image Instance(String name) {
		for (Image i : Instances)
			if (i.name.equals(name))
				return i;
		return null;
	}
	
	private Image(String path) {
		try {
			File file = new File(path);
			// Abrir imagen en memoria.
			bi = ImageIO.read(file);
			name = file.getName();
			size = file.length();
			ConceptScores = new HashMap<String, List<Integer>>();
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	public void AddConceptScore(String concept, int value) {
		if (!ConceptScores.containsKey(concept))
			ConceptScores.put(concept, new ArrayList<Integer>());
		ConceptScores.get(concept).add(value);
	}
	
	public static double Difference(Image imageA, Image imageB) {
		Map<String, List<Integer>> a = imageA.ConceptScores, b = imageB.ConceptScores;
		Set<String> setA = a.keySet().stream().collect(Collectors.toSet()),
					setB = b.keySet().stream().collect(Collectors.toSet());
		
		setA.removeAll(b.keySet());
		setB.removeAll(a.keySet());
		
		double dif = - setA.size() - setB.size();
		
		Set<String> common = a.keySet().stream().collect(Collectors.toSet());
		common.retainAll(b.keySet());
		
		for (String i : common) {
			double averageA = (double) a.get(i).stream().mapToInt(Integer::intValue).sum() / (double) a.get(i).size();
			double averageB = (double) b.get(i).stream().mapToInt(Integer::intValue).sum() / (double) b.get(i).size();
			dif += Math.abs(averageA - averageB);
		}
		
		return dif;
	}

}
