package clientemulti;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClienteMulti {
    
    public static void main(String[] args) {
        Socket s = null;
        Scanner scanner = new Scanner(System.in);
        
        try {
            s = new Socket("localhost", 8080);
            System.out.println("Conectado al servidor\n");
            
            Thread hiloParaRecibir = new Thread(new ParaRecibir(s), "receiver");
            Thread hiloParaMandar = new Thread(new ParaMandar(s, scanner), "sender");
            
            hiloParaRecibir.start();
            hiloParaMandar.start();
            
            hiloParaMandar.join();
            hiloParaRecibir.join();
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            scanner.close();
            if (s != null && !s.isClosed()) {
                try { 
                    s.close(); 
                } catch (IOException ignore) {}
            }
        }
    }
}