package clientemulti;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class ParaMandar implements Runnable {
    private final DataOutputStream salida;
    private final Socket socket;
    private final Scanner scanner;
    
    public ParaMandar(Socket s, Scanner scanner) throws IOException {
        this.socket = s;
        this.scanner = scanner;
        this.salida = new DataOutputStream(s.getOutputStream());
    }
    
    @Override
    public void run() {
        try {
            Thread.sleep(1000);
            
            while (!socket.isClosed()) {
                String entrada = scanner.nextLine();
                
                if (entrada.trim().isEmpty()) {
                    continue;
                }
                
                salida.writeUTF(entrada);
                salida.flush();
                
                if (entrada.equalsIgnoreCase("salir")) {
                    socket.close();
                    break;
                }
               
                Thread.sleep(50);
            }
        } catch (SocketException e) {
            if (!socket.isClosed()) {
                System.err.println("\nError: Se perdio la conexion con el servidor");
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("\nError de comunicacion: " + traducirError(e.getMessage()));
            }
        } catch (InterruptedException e) {
            System.err.println("\nError: Thread interrumpido");
        } catch (Exception e) {
            if (!socket.isClosed()) {
                System.err.println("\nError inesperado: " + e.getMessage());
            }
        }
    }
    
    private String traducirError(String mensajeOriginal) {
        if (mensajeOriginal == null) {
            return "Error desconocido";
        }
        
        if (mensajeOriginal.contains("Connection reset")) {
            return "Conexion interrumpida por el servidor";
        }
        if (mensajeOriginal.contains("Broken pipe")) {
            return "Conexion interrumpida";
        }
        if (mensajeOriginal.contains("Socket closed")) {
            return "Socket cerrado";
        }
        
        return mensajeOriginal;
    }
}