package clientemulti;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ParaMandar implements Runnable {
    private final DataOutputStream salida;
    private final Socket socket;
    private final Scanner scanner;
    private boolean mostrarMenu = true;
    
    public ParaMandar(Socket s, Scanner scanner) throws IOException {
        this.socket = s;
        this.scanner = scanner;
        this.salida = new DataOutputStream(s.getOutputStream());
    }
    
    @Override
    public void run() {
        try {
            Thread.sleep(500);
            
            while (!socket.isClosed()) {
                if (mostrarMenu) {
                    mostrarMenuPrincipal();
                }
                
                String opcion = scanner.nextLine().trim();
                if (opcion.isEmpty()) continue;
                
                procesarOpcion(opcion);
            }
        } catch (Exception ex) {
            System.out.println("Conexion cerrada");
        }
    }
    
    private void mostrarMenuPrincipal() {
        System.out.println("\n=== MENU ===");
        System.out.println("1. Enviar mensaje");
        System.out.println("2. Mensaje directo");
        System.out.println("3. Bloquear");
        System.out.println("4. Desbloquear");
        System.out.println("5. Ver bloqueados");
        System.out.println("6. Ver usuarios");
        System.out.println("7. Salir");
        System.out.print("Opcion: ");
    }
    
    private void procesarOpcion(String opcion) throws IOException, InterruptedException {
        mostrarMenu = false;
        
        switch (opcion) {
            case "1":
                System.out.print("\nMensaje: ");
                String mensaje = scanner.nextLine();
                if (!mensaje.trim().isEmpty()) {
                    salida.writeUTF(mensaje);
                    salida.flush();
                }
                break;
                
            case "2":
                salida.writeUTF("MENU:2");
                salida.flush();
                Thread.sleep(100);
                break;
                
            case "3":
                salida.writeUTF("MENU:3");
                salida.flush();
                Thread.sleep(100);
                break;
                
            case "4":
                salida.writeUTF("MENU:4");
                salida.flush();
                Thread.sleep(100);
                break;
                
            case "5":
                salida.writeUTF("MENU:5");
                salida.flush();
                Thread.sleep(300);
                break;
                
            case "6":
                salida.writeUTF("MENU:6");
                salida.flush();
                Thread.sleep(300);
                break;
                
            case "7":
                System.out.println("\nCerrando...");
                salida.writeUTF("salir");
                salida.flush();
                socket.close();
                return;
                
            default:
                System.out.println("Opcion invalida");
        }
        
        mostrarMenu = true;
    }
}