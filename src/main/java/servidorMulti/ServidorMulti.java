package servidormulti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServidorMulti {
    static HashMap<String, UnCliente> clientes = new HashMap<>();
    static Map<String, String> usuarios = new HashMap<>(); 
    
    public static void main(String[] args) {
        int puerto = 8080;
        int contador = 0;
        
        try (ServerSocket servidorSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en el puerto " + puerto);
            
            while (true) {
                Socket socket = servidorSocket.accept();
                
                String idCliente = Integer.toString(contador);
                UnCliente unCliente = new UnCliente(socket, contador);
                Thread hilo = new Thread(unCliente);
                
                clientes.put(idCliente, unCliente);
                hilo.start();
                
                System.out.println("Se conectó el cliente #" + contador);
                contador++;
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }
    
    public static boolean registrarUsuario(String usuario, String password) {
        if (usuarios.containsKey(usuario)) {
            return false;
        }
        usuarios.put(usuario, password);
        return true;
    }
    
    public static boolean autenticarUsuario(String usuario, String password) {
        return usuarios.containsKey(usuario) && usuarios.get(usuario).equals(password);
    }
}