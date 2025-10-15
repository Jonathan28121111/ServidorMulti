package clientemulti;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ParaRecibir implements Runnable {
    private final DataInputStream entrada;
    private final Socket socket;
    private boolean autenticado = false;
    
    public ParaRecibir(Socket s) throws IOException {
        this.socket = s;
        this.entrada = new DataInputStream(s.getInputStream());
    }
    
    @Override
    public void run() {
        try {
            while (!socket.isClosed()) {
                String mensaje = entrada.readUTF();
                System.out.println(mensaje);
                
                if (mensaje.contains("OK: Registro exitoso") || mensaje.contains("OK: Bienvenido")) {
                    autenticado = true;
                    Thread.sleep(500);
                    mostrarMenu();
                }
                
                if (autenticado && (mensaje.contains("OK:") || mensaje.contains("ERROR:") || 
                    mensaje.contains("Total:") || mensaje.contains("ENVIADO") || 
                    mensaje.contains("Escribe tu mensaje"))) {
                    Thread.sleep(300);
                    mostrarMenu();
                }
            }
        } catch (IOException | InterruptedException e) {
            if (!socket.isClosed()) {
                System.out.println("Conexion cerrada");
            }
        }
    }
    
    private void mostrarMenu() {
        System.out.println("\n=== MENU ===");
        System.out.println("1. Enviar mensaje");
        System.out.println("2. Mensaje directo");
        System.out.println("3. Bloquear usuario");
        System.out.println("4. Desbloquear usuario");
        System.out.println("5. Ver bloqueados");
        System.out.println("6. Ver usuarios");
        System.out.println("7. Salir");
        System.out.print("Opcion: ");
    }
}