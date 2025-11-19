package clientemulti;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class ClienteMulti {
    
    public static void main(String[] args) {
        Socket s = null;
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.println("Conectando al servidor...");
            s = new Socket("localhost", 8080);
            System.out.println("Conectado al servidor\n");
            
            Thread hiloParaRecibir = new Thread(new ParaRecibir(s), "receiver");
            Thread hiloParaMandar = new Thread(new ParaMandar(s, scanner), "sender");
            
            hiloParaRecibir.start();
            hiloParaMandar.start();
            
            hiloParaMandar.join();
            hiloParaRecibir.join();
            
        } catch (ConnectException e) {
            System.err.println("\nError: No se pudo conectar al servidor");
            System.err.println("Verifica que el servidor este encendido en localhost:8080");
        } catch (SocketTimeoutException e) {
            System.err.println("\nError: Tiempo de espera agotado");
            System.err.println("El servidor no responde");
        } catch (SocketException e) {
            System.err.println("\nError: Se perdio la conexion con el servidor");
        } catch (IOException e) {
            System.err.println("\nError de comunicacion ");
        } catch (Exception e) {
            System.err.println("\nError inesperado " );
            e.printStackTrace();
        } finally {
            scanner.close();
            if (s != null && !s.isClosed()) {
                try { 
                    s.close(); 
                } catch (IOException ignore) {}
            }
        }
    }
    
    private static String traducirError(String mensajeOriginal) {
        if (mensajeOriginal == null) {
            return "Error desconocido";
        }
        
        if (mensajeOriginal.contains("Connection refused")) {
            return "Conexion rechazada - El servidor no esta disponible";
        }
        if (mensajeOriginal.contains("Connection timed out")) {
            return "Tiempo de espera agotado - No se pudo conectar al servidor";
        }
        if (mensajeOriginal.contains("Connection reset")) {
            return "Conexion interrumpida - El servidor cerro la conexion";
        }
        if (mensajeOriginal.contains("Network is unreachable")) {
            return "Red no disponible - Verifica tu conexion a internet";
        }
        if (mensajeOriginal.contains("No route to host")) {
            return "No se puede alcanzar el servidor";
        }
        if (mensajeOriginal.contains("Socket closed")) {
            return "Socket cerrado - La conexion se termino";
        }
        if (mensajeOriginal.contains("Broken pipe")) {
            return "Conexion interrumpida";
        }
        
        return mensajeOriginal;
    }
}