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
                
                if (mensaje.contains("OK: Registro exitoso") || mensaje.contains("OK: Bienvenido") ||
                    mensaje.contains("REGISTRO EXITOSO") || mensaje.contains("INICIO DE SESION EXITOSO")) {
                    autenticado = true;
                    Thread.sleep(500);
                    mostrarMenu();
                }
       
                if (autenticado && !mensaje.contains("ERROR: Debes iniciar sesion") && 
                    !mensaje.contains("Escribe tu mensaje") &&
                    (mensaje.contains("OK:") || mensaje.contains("Total:") || 
                    mensaje.contains("ENVIADO"))) {
                    Thread.sleep(300);
                    mostrarMenu();
                }
                
                if (!autenticado && mensaje.contains("ERROR: Debes iniciar sesion")) {
                    continue;
                }
                
                if (mensaje.contains("GANASTE") || mensaje.contains("PERDISTE") || 
                    mensaje.contains("EMPATE") || mensaje.contains("gana por abandono")) {
                    Thread.sleep(500);
                    if (autenticado) {
                        mostrarMenu();
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            if (!socket.isClosed()) {
                System.out.println("Conexion cerrada");
            }
        }
    }
    
    private void mostrarMenu() {
        System.out.println("""
            
            === MENU ===
            1. Enviar mensaje
            2. Mensaje directo
            3. Bloquear usuario
            4. Desbloquear usuario
            5. Ver bloqueados
            6. Ver usuarios
            7. Salir
            ------------------------------------
            JUEGO DEL GATO:
            8. Invitar a jugar
            9. Ver estado del juego
            ------------------------------------
            RANKINGS:
            10. Ver ranking general
            11. Comparar jugadores
            ====================================
            """);
        System.out.print("Opcion: ");
    }
}