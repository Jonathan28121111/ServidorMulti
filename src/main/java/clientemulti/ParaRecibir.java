package clientemulti;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

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
        } catch (EOFException e) {
            System.err.println("\nConexion cerrada: El servidor termino la comunicacion");
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
            ------------------------------------
            GRUPOS:
            12. Ver todos los grupos
            13. Crear grupo
            14. Unirse a grupo
            15. Salir de grupo
            16. Borrar grupo
            17. Cambiar grupo actual
            18. Ver grupo actual
            19. Ver mis grupos
            20. Ver miembros de grupo
            ====================================
            """);
        System.out.print("Opcion: ");
    }
    
    private String traducirError(String mensajeOriginal) {
        if (mensajeOriginal == null) {
            return "Error desconocido";
        }
        
        if (mensajeOriginal.contains("Connection reset")) {
            return "Conexion interrumpida por el servidor";
        }
        if (mensajeOriginal.contains("Connection timed out")) {
            return "Tiempo de espera agotado";
        }
        if (mensajeOriginal.contains("Socket closed")) {
            return "Socket cerrado";
        }
        
        return mensajeOriginal;
    }
}