package clientemulti;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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
                
                Thread.sleep(100);
            }
        } catch (Exception ex) {
            System.out.println("Conexion cerrada");
        }
    }
}