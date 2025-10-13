package servidormulti;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class UnCliente implements Runnable {
    final DataOutputStream salida;
    final DataInputStream entrada;
    private final int miId;
    private int mensajesEnviados = 0;
    private boolean autenticado = false;
    private String nombreUsuario = null;
    private Socket socket;
    private boolean esperandoMenu = false;
    private String opcionMenu = "";
    private String usuarioTemp = "";
    
    UnCliente(Socket s, int id) throws IOException {
        this.miId = id;
        this.socket = s;
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
    }
    
    @Override
    public void run() {
        try {
            enviarMensaje("BIENVENIDO");
            enviarMensaje("Eres el cliente #" + miId);
            enviarMensaje("Tienes 3 mensajes gratis. Despues debes autenticarte.");
            enviarMensaje("Comandos: @<id> <mensaje> para mensajes directos");
            enviarMensaje("==========================");
            
            while (!socket.isClosed()) {
                String mensaje = entrada.readUTF();
                System.out.println("Cliente #" + miId + " envio: " + mensaje);
                
                if (esperandoMenu) {
                    procesarMenu(mensaje);
                    continue;
                }
                
                if (mensaje.equalsIgnoreCase("salir")) {
                    enviarMensaje("[SISTEMA] Cerrando conexion...");
                    socket.close();
                    break;
                }
                
                if (!autenticado) {
                    if (mensajesEnviados >= 3) {
                        mostrarMenuAutenticacion();
                        continue;
                    }
                    
                    mensajesEnviados++;
                    int restantes = 3 - mensajesEnviados;
                    
                    System.out.println("Cliente #" + miId + " ha enviado " + mensajesEnviados + "/3 mensajes");
                    
                    if (restantes > 0) {
                        enviarMensaje("[AVISO] Te quedan " + restantes + " mensajes gratis");
                    } else {
                        enviarMensaje("[AVISO] Este fue tu ultimo mensaje gratis.");
                    }
                }
                
                if (mensaje.startsWith("@")) {
                    enviarMensajeDirecto(mensaje);
                    continue;
                }
              
                String prefijo = autenticado ? "[" + nombreUsuario + "]" : "[Invitado #" + miId + "]";
                for (UnCliente cliente : ServidorMulti.clientes.values()) {
                    if (cliente.miId != this.miId) {
                        cliente.enviarMensaje(prefijo + ": " + mensaje);
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Cliente #" + miId + " desconectado: " + ex.getMessage());
        } finally {
            ServidorMulti.clientes.remove(Integer.toString(miId));
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void mostrarMenuAutenticacion() throws IOException {
        enviarMensaje("");
        enviarMensaje("Ya no puedes enviar mensajes has alcanzado el limite");
        enviarMensaje("Autenticate para enviar mensaje sin limites");
        enviarMensaje("Selecciona una opcion:");
        enviarMensaje("1. Registrarse");
        enviarMensaje("2.Iniciar sesion");
        enviarMensaje("Escribe 1 o 2:");
        esperandoMenu = true;
        opcionMenu = "ELEGIR";
    }
    
    private void procesarMenu(String mensaje) throws IOException {
        switch (opcionMenu) {
            case "ELEGIR":
                if (mensaje.equals("1")) {
                    opcionMenu = "REGISTRO_USUARIO";
                    enviarMensaje("");
                    enviarMensaje("=== REGISTRO DE NUEVA CUENTA ===");
                    enviarMensaje("Ingresa tu nombre de usuario:");
                } else if (mensaje.equals("2")) {
                    opcionMenu = "LOGIN_USUARIO";
                    enviarMensaje("");
                    enviarMensaje("=== INICIAR SESION ===");
                    enviarMensaje("Ingresa tu nombre de usuario:");
                } else {
                    enviarMensaje("[ERROR] Opcion invalida. Escribe 1 o 2:");
                }
                break;
                
            case "REGISTRO_USUARIO":
                usuarioTemp = mensaje;
                opcionMenu = "REGISTRO_PASSWORD";
                enviarMensaje("Ingresa tu contrasena:");
                break;
                
            case "REGISTRO_PASSWORD":
                if (ServidorMulti.registrarUsuario(usuarioTemp, mensaje)) {
                    autenticado = true;
                    nombreUsuario = usuarioTemp;
                    mensajesEnviados = 0;
                    esperandoMenu = false;
                    enviarMensaje("");
                    enviarMensaje("BIENVENIDO TE HAS REGISTRADO");
                    enviarMensaje("Bienvenido " + nombreUsuario + "!" + " ".repeat(Math.max(0, 24 - nombreUsuario.length())));
                    enviarMensaje("Empieza a mandar mensaje a tus amigos");
                    enviarMensaje("");
                    System.out.println("Cliente #" + miId + " registrado como: " + nombreUsuario);
                } else {
                    enviarMensaje("");
                    enviarMensaje("[ERROR] El usuario '" + usuarioTemp + "' ya existe.");
                    enviarMensaje("Por favor elige otra opcion:");
                    enviarMensaje("1. Intentar con otro nombre de usuario");
                    enviarMensaje("2. Iniciar sesion con este usuario");
                    opcionMenu = "ELEGIR";
                }
                usuarioTemp = "";
                break;
                
            case "LOGIN_USUARIO":
                usuarioTemp = mensaje;
                opcionMenu = "LOGIN_PASSWORD";
                enviarMensaje("Ingresa tu contrasena:");
                break;
                
            case "LOGIN_PASSWORD":
                if (ServidorMulti.autenticarUsuario(usuarioTemp, mensaje)) {
                    autenticado = true;
                    nombreUsuario = usuarioTemp;
                    mensajesEnviados = 0;
                    esperandoMenu = false;
                    enviarMensaje("");
                    enviarMensaje("BIENVENIDO DE VUELTA");
                    enviarMensaje("║  Bienvenido de nuevo " + nombreUsuario + "!" + " ".repeat(Math.max(0, 16 - nombreUsuario.length())) + "║");
                    enviarMensaje("Empieza a mandar mensaje a tus amigos");
                    enviarMensaje("");
                    System.out.println("Cliente #" + miId + " inicio sesion como: " + nombreUsuario);
                } else {
                    enviarMensaje("");
                    enviarMensaje("[ERROR] Usuario o contrasena incorrectos.");
                    enviarMensaje("¿Que deseas hacer?");
                    enviarMensaje("1. Intentar de nuevo");
                    enviarMensaje("2. Registrarse como usuario nuevo");
                    opcionMenu = "ELEGIR";
                }
                usuarioTemp = "";
                break;
        }
    }
    
    private void enviarMensaje(String msg) throws IOException {
        salida.writeUTF(msg);
        salida.flush();
    }
    
    private void enviarMensajeDirecto(String mensaje) throws IOException {
        String[] partes = mensaje.split(" ", 2);
        if (partes.length < 2) {
            enviarMensaje("[ERROR] Formato: @<id> <mensaje>");
            return;
        }
        
        String aQuien = partes[0].substring(1);
        String textoMensaje = partes[1];
        
        UnCliente cliente = ServidorMulti.clientes.get(aQuien);
        
        if (cliente != null) {
            String prefijo = autenticado ? "[MD de " + nombreUsuario + "]" : "[MD de Invitado #" + miId + "]";
            cliente.enviarMensaje(prefijo + ": " + textoMensaje);
            enviarMensaje("[ENVIADO] Mensaje directo a #" + aQuien);
        } else {
            enviarMensaje("[ERROR] Cliente #" + aQuien + " no existe");
        }
    }
}