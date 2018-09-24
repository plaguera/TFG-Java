package es.ull.etsii.tfg;

import java.net.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import java.io.*;
import java.lang.management.ManagementFactory;

/**
 * Maneja multiples conexiones de socket con clientes Unity3D.
 * @author Pedro Miguel Lagüera Cabrera
 * Server.java
 */
public class Server {

	// Instancia del Servidor
	public static Server Instance;
	// Hilo de escucha del servidor, añade clientes cuando se conectan.
    private ServerSocket serverSocket;
    // Hilo de input por teclado.
    private InputThread inputThread;

    /**
     * Escuchar a otros sockets en el puerto especificado.
     * @param port
     */
    public void start(int port) {
        try {
        	Instance = this;
        	inputThread = new InputThread();
        	inputThread.start();
            serverSocket = new ServerSocket(port);
            
            // Cada vez que se acepta una nueva conexion con un socket,
            // se crea un 'handler' en un nuevo hilo para manejar la comunicacion.
            while (true) new ClientHandler(serverSocket.accept()).start();
        } catch (IOException e) { e.printStackTrace(); }
        finally { stop(); }
    }

    /**
     * Dejar de escuchar.
     */
    public void stop() {
        try { serverSocket.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Maneja la conexion 'socket <--> socket' entre el servidor y un cliente en un hilo distinto del principal.
     * @author Pedro Miguel Lagüera Cabrera
     * Server.java
     */
    private static class ClientHandler extends Thread {
    	
    	// Lista de todas las conexiones abiertas.
    	public static List<ClientHandler> Instances = new ArrayList<ClientHandler>();
    	// Socket para comunicarse con el cliente.
        private Socket clientSocket;
        // Stream que envia mensajes al socket del cliente.
        private PrintWriter out;
        // Stream que recibe los mensajes que manda el cliente.
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            Instances.add(this);
        }

        /**
         * Funcion que ejecuta el hilo. Escanea constantemente si se han recibido datos del cliente.
         */
        public void run() {
            try {
            	// Stream de envio de datos.
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                // Stream de recepcion de datos.
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null && !clientSocket.isClosed()) {
                	// Mostrar el remitente, fecha, hora y contenido del mensaje recibido.
                	System.out.println(clientSocket.getRemoteSocketAddress().toString() + " @ " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + " : " + inputLine);
                	String[] tokens = inputLine.split("#");
                	
                	// Mensaje de creacion de lobby.
                	if (tokens[0].equals("create") && tokens[1].equals("room")) {
                		long id = Long.parseLong(tokens[2]);
                		List<String> aux = new ArrayList<String>();
                		for (int i = 3; i < tokens.length; i++)
                			aux.add(tokens[i]);
                		out.println(Room.Instantiate(id, aux).pack());
                	}
	            	
                	// Mensaje de peticion de envio de imagen.
                    if (tokens[0].equals("request") && tokens[1].equals("image"))
                    	send(Image.Instance(tokens[2]));
                    
                    // Mensaje de adicion de resultado de un turno.
                    if (tokens[0].equals("result")) {
                    	Map<String, Integer> aux = new HashMap<String, Integer>();
                    	String concept = tokens[1];
                    	for (int i = 2; i < tokens.length; i++)
                    	{
                    		String[] subtokens = tokens[i].split(",");
                    		aux.put(subtokens[0], Integer.parseInt(subtokens[1]));
                    	}
                    	Result.Instantiate(concept, aux);
                    }
                    
                    // Mensaje de adicion de resultado de una partida.
                    if (tokens[0].equals("scores")) {
                    	for (int i = 1; i < tokens.length; i++)
                    	{
                    		String[] subtokens = tokens[i].split(",");
                    		Scores.Add(subtokens[0], Integer.parseInt(subtokens[1]));
                    	}
                    	Scores.toFile();
                    }
                }
                // Cerrar stream de entrada.
                in.close();
                // Cerrar stream de salida.
                out.close();
                // Cerrar socket.
                clientSocket.close();

            } catch (IOException e) {
               e.printStackTrace();
            }
        }
        
        /**
         * Cerrar Socket.
         */
        public void close() {
        	try { clientSocket.close(); }
        	catch (IOException e) { e.printStackTrace(); }
        }
        
        /**
         * Envia una imagen al socket del cliente en formato de byte[].
         * @param image
         */
        void send(Image image) {
        	try {
        		Iterator<ImageWriter> iterator = ImageIO.getImageWritersByFormatName("jpg");
    		    ImageWriter imageWriter = iterator.next();
    		    ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();
    		    imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    		    imageWriteParam.setCompressionQuality(.5f);
    		    ImageOutputStream imageOutputStream = new MemoryCacheImageOutputStream(clientSocket.getOutputStream());
    		    imageWriter.setOutput(imageOutputStream);
    		    IIOImage iioimage = new IIOImage(image.bi, null, null);
    		    imageWriter.write(null, iioimage, imageWriteParam);
    		    imageOutputStream.flush();
        	} catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    /**
     * Hilo encargado de permitir entrada de texto por teclado.
     * @author Pedro Miguel Lagüera Cabrera
     * Server.java
     */
    public static class InputThread extends Thread {
    	
        Scanner inputReader = new Scanner(System.in);

        public void run() {
            while(true) {
                if (inputReader.hasNext()) {
                    String input = inputReader.next().trim();
                    
                    // Todos los comandos deben empezar por '/'.
                    if (input.startsWith("/")) {
                    	String command = input.substring(1);
                    	
                    	// Mostrar las conexiones abiertas por pantalla.
                    	if (command.equalsIgnoreCase("connections")) {
                    		String aux = "";
                    		for (ClientHandler i : ClientHandler.Instances)
                    			aux += i.clientSocket.getRemoteSocketAddress().toString() + "";
                    		System.out.println(aux);
                    	}
                    	
                    	// Mostrar las imagenes caragadas en la memoria de programa por pantalla.
                    	else if (command.equalsIgnoreCase("images")) {
                    		String aux = "Count = " + Image.Instances.size() + "[ ";
                    		for (Image i : Image.Instances)
                    			aux += i.name + ", ";
                    		System.out.println(aux.substring(0, aux.length()-2) + " ]");
                    	}
                    	
                    	// Mostrar tabla de jugadores ordenados por su puntuacion en orden descendente por pantalla.
                    	else if (command.equalsIgnoreCase("leaderboard")) {
                    		int count = 1;
                    		String aux = "";
                    		for (Map.Entry<String, Integer> i : Scores.sort().entrySet())
	                    		aux += count++ + ". " + i.getKey() + " = " + i.getValue() + " Puntos.";
                    		System.out.println(aux);
                    	}
                    	
                    	// Mostrar tabla de jugadores ordenados por su puntuacion en orden descendente por pantalla.
                    	else if (command.equalsIgnoreCase("distance")) {
                    		String aux = "";
                    		for (Image i : Image.Instances) {
                    			for (Image j : Image.Instances)
                    				aux += Image.Difference(i, j) + "\t";
                    			aux += "\n";
                    		}
                    		System.out.println(aux);
                    	}
                    	
                    	// Mostrar el tiempo que lleva ejecutandose el programa por pantalla.
                    	else if (command.equalsIgnoreCase("uptime")) {
                    		long durationInMillis = ManagementFactory.getRuntimeMXBean().getUptime();
                    		long millis = durationInMillis % 1000;
                    		long second = (durationInMillis / 1000) % 60;
                    		long minute = (durationInMillis / (1000 * 60)) % 60;
                    		long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

                    		String time = String.format("%02d:%02d:%02d.%d", hour, minute, second, millis);
                    		System.out.println(time);
                    	}
                    	
                    	// Salir
                    	else if (command.equalsIgnoreCase("quit"))
                    	{
                    		for (ClientHandler i : ClientHandler.Instances) i.close();
                    		Server.Instance.stop();
                    		break;
                    	}
                    }
                }
            }
        }

    }

    public static void main(String[] args) {
    	// Cargar Imagenes
    	Image.LoadImages(Path.IMAGES);
    	
    	/*// Cargar Puntuaciones
    	Scores.Load(Path.PLAYER_SCORES);
    	// Cargar Resultados
    	Result.Load(Path.RESULTS);
    	// Instanciar Servidor.
    	Server server = new Server();
    	// Comenzar escucha en puerto 5555.
        server.start(5555);
        */
    	FromCSV1();
    }
    
    public static void FromCSV() {
    	String line;
		try (BufferedReader br = new BufferedReader(new FileReader("Artificial.csv"))) {
			line = br.readLine();
			String[] images = line.split(";");
			List<String> concepts = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
            	String[] tokens = line.split(";");
            	String concept = tokens[0];
            	concepts.add(concept);
            	for (int i = 1; i < tokens.length; i++)
            		Image.Instance(images[i]).AddConceptScore(concept, Integer.parseInt(tokens[i]));
            }
            br.close();
            
            DistributedRandomNumberGenerator drng02 = new DistributedRandomNumberGenerator();
            drng02.addNumber(0, .8d);
            drng02.addNumber(1, .09d);
            drng02.addNumber(2, .06d);
            drng02.addNumber(3, .04d);
            drng02.addNumber(4, .01d);
            
            DistributedRandomNumberGenerator drng24 = new DistributedRandomNumberGenerator();
            drng24.addNumber(0, .6d);
            drng24.addNumber(1, .3d);
            drng24.addNumber(2, .15d);
            drng24.addNumber(3, .15d);
            drng24.addNumber(4, .1d);
            
            DistributedRandomNumberGenerator drng46 = new DistributedRandomNumberGenerator();
            drng46.addNumber(0, .25d);
            drng46.addNumber(1, .2d);
            drng46.addNumber(2, .1d);
            drng46.addNumber(3, .2d);
            drng46.addNumber(4, .25d);
            
            DistributedRandomNumberGenerator drng68 = new DistributedRandomNumberGenerator();
            drng68.addNumber(0, .1d);
            drng68.addNumber(1,  15d);
            drng68.addNumber(2, .2d);
            drng68.addNumber(3, .2d);
            drng68.addNumber(4, .35d);
    		
    		DistributedRandomNumberGenerator drng80 = new DistributedRandomNumberGenerator();
    		drng80.addNumber(0, .05d);
    		drng80.addNumber(1, .1d);
    		drng80.addNumber(2, .1d);
    		drng80.addNumber(3, .3d);
    		drng80.addNumber(4, .5d);
            
    		Random rand = new Random();
            for (Image i : Image.Instances) {
            	for (Map.Entry<String, List<Integer>> j : i.ConceptScores.entrySet()) {
            		
            		DistributedRandomNumberGenerator drng = null;
            		int value = j.getValue().get(0);
            		if (value <= 20) drng = drng02;
            		else if (value <= 20) drng = drng02;
            		else if (value <= 40) drng = drng24;
            		else if (value <= 60) drng = drng46;
            		else if (value <= 80) drng = drng68;
            		else if (value <= 100) drng = drng80;
            		
            		int n = rand.nextInt(20) + 1;
            		for (int k = 0; k < n; k++)
            			i.AddConceptScore(j.getKey(), drng.getDistributedRandomNumber());
            	}
            }
            
            FileWriter fw = new FileWriter("Humano.csv",false);
    		
    		String aux = "";
    		for (Image i : Image.Instances)
    			aux += ";" + i.name;
    		aux += "\n";
    		for (String i : concepts) {
    			aux += i;
    			for (Image j : Image.Instances) {
    				String aux1 = "";
    				for (int k : j.ConceptScores.get(i))
    					aux1 += k + ",";
    				aux += ";" + aux1.substring(0, aux1.length()-1);
    			}
    			aux += "\n";
    		}
    		fw.write(aux);
            fw.close();
            
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static void FromCSV1() {
    	String line;
		try (BufferedReader br = new BufferedReader(new FileReader("Humano.csv"))) {
            line = br.readLine();
			String[] images = line.split(";");
			List<String> concepts = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
            	String[] tokens = line.split(";");
            	String concept = tokens[0];
            	concepts.add(concept);
            	for (int i = 1; i < tokens.length; i++) {
            		String[] sub = tokens[i].split(",");
            		for (String j : sub)
            			Image.Instance(images[i]).AddConceptScore(concept, Integer.parseInt(j));
            	}
            }
            br.close();
            
            Print("PrimeraImplementacion.csv", 1);
            Print("SegundaImplementacion.csv", 2);
            Print("TerceraImplementacion.csv", 3);
            
        } catch (IOException e) {
        	return;
        }
    }
    
    public static void Print(String output, int function) {
    	int n = Image.Instances.size();
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++)
        	for (int j = 0; j < n; j++)
        		switch(function) {
        			case 1: matrix[i][j] = Image.Difference1(Image.Instances.get(i), Image.Instances.get(j)); break;
        			case 2: matrix[i][j] = Image.Difference2(Image.Instances.get(i), Image.Instances.get(j)); break;
        			case 3: matrix[i][j] = Image.Difference3(Image.Instances.get(i), Image.Instances.get(j)); break;
        			default: break;
        		}
        
        try {
        	FileWriter fw = new FileWriter(output,false);
            String aux = "";
            for (Image i : Image.Instances)
            	aux += ";" + i.name;
            aux += "\n";
            int index = 0;
            double max = Double.MIN_VALUE, tmp;
            for (double[] i : matrix) {
            	tmp = Arrays.stream(i).max().getAsDouble();
            	if (tmp > max) max = tmp;
            }
            	
            for (double[] i : matrix) {
            	aux += Image.Instances.get(index).name;
            	index ++;
    			for (double j : i)
    				aux += ";" + new DecimalFormat("##.##").format((j / max) * 100);
    			aux += "\n";
    		}
            fw.write(aux);
            fw.close();
            System.out.println(aux);
        } catch(Exception e) { e.printStackTrace(); }
    }
}