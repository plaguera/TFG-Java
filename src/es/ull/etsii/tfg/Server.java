package es.ull.etsii.tfg;

import java.net.*;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.management.ManagementFactory;

/**
 * Maneja múltiples conexiones de socket con clientes Unity.
 * @author Pedro Miguel Lagüera Cabrera
 * Server.java
 */
public class Server {

	public static Server Instance;
    private ServerSocket serverSocket;
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
        	// Crear socket de escucha.
            serverSocket = new ServerSocket(port);
            
            // Cada vez que se acepta una nueva conexión con un socket,
            // se crea un 'handler' en un nuevo hilo para manejar la comunicación.
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
     * Maneja la conexión 'socket <--> socket' entre el servidor y un cliente en un hilo distinto del principal.
     */
    private static class ClientHandler extends Thread {
    	
    	public static List<ClientHandler> Instances = new ArrayList<ClientHandler>();
    	// Socket para comunicarse con el cliente.
        private Socket clientSocket;
        // Stream que envía mensajes al socket del cliente.
        private PrintWriter out;
        // Stream que recibe los mensajes que manda el cliente.
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            Instances.add(this);
        }

        /**
         * Función que ejecuta el hilo. Escanea constantemente si se han recibido datos del cliente.
         */
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null && !clientSocket.isClosed()) {
                	System.out.println(clientSocket.getRemoteSocketAddress().toString() + " @ " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + " : " + inputLine);
                	String[] tokens = inputLine.split("#");
                	
                	if (tokens[0].equals("create") && tokens[1].equals("room")) {
                		long id = Long.parseLong(tokens[2]);
                		List<String> aux = new ArrayList<String>();
                		for (int i = 3; i < tokens.length; i++)
                			aux.add(tokens[i]);
                		out.println(Room.Instantiate(id, aux).pack());
                	}
	            	
                    if (tokens[0].equals("request") && tokens[1].equals("image"))
                    	send(Image.Instance(tokens[2]));
                    
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
    
    public static class InputThread extends Thread {

        Scanner inputReader = new Scanner(System.in);

        public void run() {
            while(true) {
                if (inputReader.hasNext()) {
                    String input = inputReader.next().trim();
                    if (input.startsWith("/")) {
                    	String command = input.substring(1);
                    	if (command.equalsIgnoreCase("connections")) {
                    		String aux = "";
                    		for (ClientHandler i : ClientHandler.Instances)
                    			aux += i.clientSocket.getRemoteSocketAddress().toString() + "\n";
                    		System.out.println(aux);
                    	}
                    	else if (command.equalsIgnoreCase("images")) {
                    		String aux = "Count = " + Image.Instances.size() + "[ ";
                    		for (Image i : Image.Instances)
                    			aux += i.name + ", ";
                    		System.out.println(aux.substring(0, aux.length()-2) + " ]");
                    	}
                    	else if (command.equalsIgnoreCase("leaderboard")) {
                    		int count = 1;
                    		String aux = "";
                    		for (Map.Entry<String, Integer> i : Scores.sort().entrySet())
	                    		aux += count++ + ". " + i.getKey() + " = " + i.getValue() + " Puntos.\n";
                    		System.out.println(aux);
                    	}
                    	else if (command.equalsIgnoreCase("uptime")) {
                    		long durationInMillis = ManagementFactory.getRuntimeMXBean().getUptime();
                    		long millis = durationInMillis % 1000;
                    		long second = (durationInMillis / 1000) % 60;
                    		long minute = (durationInMillis / (1000 * 60)) % 60;
                    		long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

                    		String time = String.format("%02d:%02d:%02d.%d", hour, minute, second, millis);
                    		System.out.println(time);
                    	}
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
    	Image.LoadImages(Path.IMAGES);
    	Scores.Load(Path.PLAYER_SCORES);
    	Result.Load(Path.RESULTS);
    	// Instanciar Servidor.
    	Server server = new Server();
    	// Comenzar escucha en puerto 5555.
        server.start(5555);
    }
}
