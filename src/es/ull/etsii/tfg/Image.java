package es.ull.etsii.tfg;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	
	public int ConceptCount() {
		return ConceptScores.size();
	}
	
	public static double Difference(String imageA, String imageB) {
		return Difference(Instance(imageA), Instance(imageB));
	}
	
	public static double Difference(Image imageA, Image imageB) {
		return Difference1(imageA, imageB);
	}
	
	public static double Difference1(Image imageA, Image imageB) {
		Map<String, List<Integer>> a = imageA.ConceptScores, b = imageB.ConceptScores;
		Set<String> common = a.keySet().stream().collect(Collectors.toSet());
		common.retainAll(b.keySet());
		
		double dif = (a.size() - common.size()) + (b.size() - common.size());
		for (String i : common) {
			double averageA = Mean(a.get(i));
			double averageB = Mean(b.get(i));
			dif += Math.abs(averageA - averageB);
		}
		
		return dif;
	}
	
	public static double Mean(List<Integer> list) {
		double sum = 0, total = list.size();
		for (int i : list) sum += i;
		return sum / total;
	}
	
	public static double Difference2(Image imageA, Image imageB) {
		Map<String, List<Integer>> a = imageA.ConceptScores, b = imageB.ConceptScores;
		Set<String> common = a.keySet().stream().collect(Collectors.toSet());
		common.retainAll(b.keySet());
		
		double dif = (a.size() - common.size()) + (b.size() - common.size());
		for (String i : common) {
			double averageA = Mean(a.get(i));
			double averageB = Mean(b.get(i));
			
			double sumA = 0d, sumB = 0d;
			for (int j : a.get(i))
				sumA += Math.pow(j - averageA, 2);
			for (int j : b.get(i))
				sumB += Math.pow(j - averageB, 2);
			
			double varianceA = sumA / averageA;
			double varianceB = sumB / averageB;
			if (Double.isNaN(varianceA)) varianceA = 0d;
			if (Double.isNaN(varianceB)) varianceB = 0d;
			dif += Math.abs(varianceA - varianceB);
		}
		return dif;
	}
	
	public static double Difference3(Image imageA, Image imageB) {
		Map<String, List<Integer>> a = imageA.ConceptScores, b = imageB.ConceptScores;
		Set<String> common = a.keySet().stream().collect(Collectors.toSet());
		common.retainAll(b.keySet());
		
		double dif = (a.size() - common.size()) + (b.size() - common.size());
		for (String i : common)
			dif += Math.abs(Mode(a.get(i)) - Mode(b.get(i)));
		return dif;
	}
	
	public static int Mode(List<Integer> list) {
		Map<Integer, Integer> count = new HashMap<Integer,Integer>();
		
		for (int i : list)
			if (!count.containsKey(i))
				count.put(i, Collections.frequency(list, i));
		
		return Collections.max(count.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
	}
}
