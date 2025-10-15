package servidormulti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServidorMulti {
    static HashMap<String, UnCliente> clientes = new HashMap<>();
    static DatabaseManager db;
    
    public static void main(String[] args) {
        int puerto = 8080;
        int contador = 0;
        
        db = new DatabaseManager();
       
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Cerrando servidor...");
            db.cerrar();
        }));
        
        try (ServerSocket servidorSocket = new ServerSocket(puerto)) {
            System.out.println("EL SERVER HA INICIADO");
            System.out.println("Puerto: " + puerto);
            System.out.println("Base de datos: SQLite");
            
            while (true) {
                Socket socket = servidorSocket.accept();
                
                String idCliente = Integer.toString(contador);
                UnCliente unCliente = new UnCliente(socket, contador);
                Thread hilo = new Thread(unCliente);
                
                clientes.put(idCliente, unCliente);
                hilo.start();
                
                System.out.println("Cliente #" + contador + " conectado");
                contador++;
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            db.cerrar();
        }
    }
}