package servidormulti;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

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
    private String destinatarioTemp = "";
    
    UnCliente(Socket s, int id) throws IOException {
        this.miId = id;
        this.socket = s;
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
    }
    
    @Override
    public void run() {
        try {
            enviarMensaje("=== BIENVENIDO AL CHAT ===");
            enviarMensaje("Cliente #" + miId);
            enviarMensaje("Tienes 3 mensajes gratis");
            enviarMensaje("==========================");
            
            while (!socket.isClosed()) {
                String mensaje = entrada.readUTF();
                
                if (esperandoMenu) {
                    procesarMenuAcciones(mensaje);
                    continue;
                }
                
                if (mensaje.equals("1")) {
                    enviarMensaje("Escribe tu mensaje:");
                    esperandoMenu = true;
                    opcionMenu = "ENVIAR_MENSAJE";
                    continue;
                }
                
                if (mensaje.equals("2")) {
                    if (!autenticado) {
                        enviarMensaje("ERROR: Debes iniciar sesion");
                        continue;
                    }
                    enviarMensaje("Usuario destinatario:");
                    esperandoMenu = true;
                    opcionMenu = "MD_USUARIO";
                    continue;
                }
                
                if (mensaje.equals("3")) {
                    if (!autenticado) {
                        enviarMensaje("ERROR: Debes iniciar sesion");
                        continue;
                    }
                    enviarMensaje("Usuario a bloquear:");
                    esperandoMenu = true;
                    opcionMenu = "BLOQUEAR";
                    continue;
                }
                
                if (mensaje.equals("4")) {
                    if (!autenticado) {
                        enviarMensaje("ERROR: Debes iniciar sesion");
                        continue;
                    }
                    enviarMensaje("Usuario a desbloquear:");
                    esperandoMenu = true;
                    opcionMenu = "DESBLOQUEAR";
                    continue;
                }
                
                if (mensaje.equals("5")) {
                    if (!autenticado) {
                        enviarMensaje("ERROR: Debes iniciar sesion");
                        continue;
                    }
                    listarBloqueados();
                    continue;
                }
                
                if (mensaje.equals("6")) {
                    listarUsuarios();
                    continue;
                }
                
                if (mensaje.equalsIgnoreCase("salir") || mensaje.equals("7")) {
                    enviarMensaje("Cerrando conexion...");
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
                    
                    if (restantes > 0) {
                        enviarMensaje("INFO: Te quedan " + restantes + " mensajes");
                    } else {
                        enviarMensaje("INFO: Ultimo mensaje gratis");
                    }
                }
                
                String prefijo = autenticado ? "[" + nombreUsuario + "]" : "[Invitado#" + miId + "]";
                for (UnCliente cliente : ServidorMulti.clientes.values()) {
                    if (cliente.miId != this.miId) {
                        if (cliente.autenticado && autenticado) {
                            if (ServidorMulti.db.estaBloqueado(cliente.nombreUsuario, nombreUsuario)) {
                                continue;
                            }
                        }
                        cliente.enviarMensaje(prefijo + ": " + mensaje);
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Cliente #" + miId + " desconectado");
        } finally {
            ServidorMulti.clientes.remove(Integer.toString(miId));
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void procesarMenuAcciones(String mensaje) throws IOException {
        switch (opcionMenu) {
            case "ENVIAR_MENSAJE":
                String prefijo = autenticado ? "[" + nombreUsuario + "]" : "[Invitado#" + miId + "]";
                for (UnCliente cliente : ServidorMulti.clientes.values()) {
                    if (cliente.miId != this.miId) {
                        if (cliente.autenticado && autenticado) {
                            if (ServidorMulti.db.estaBloqueado(cliente.nombreUsuario, nombreUsuario)) {
                                continue;
                            }
                        }
                        cliente.enviarMensaje(prefijo + ": " + mensaje);
                    }
                }
                esperandoMenu = false;
                opcionMenu = "";
                break;
                
            case "MD_USUARIO":
                destinatarioTemp = mensaje.trim();
                enviarMensaje("Mensaje:");
                opcionMenu = "MD_MENSAJE";
                break;
                
            case "MD_MENSAJE":
                enviarMensajeDirecto(destinatarioTemp, mensaje);
                esperandoMenu = false;
                opcionMenu = "";
                destinatarioTemp = "";
                break;
                
            case "BLOQUEAR":
                bloquearUsuario(mensaje.trim());
                esperandoMenu = false;
                opcionMenu = "";
                break;
                
            case "DESBLOQUEAR":
                desbloquearUsuario(mensaje.trim());
                esperandoMenu = false;
                opcionMenu = "";
                break;
                
            case "ELEGIR":
                procesarMenuAutenticacion(mensaje);
                break;
                
            case "REGISTRO_USUARIO":
                procesarRegistroUsuario(mensaje);
                break;
                
            case "REGISTRO_PASSWORD":
                procesarRegistroPassword(mensaje);
                break;
                
            case "LOGIN_USUARIO":
                procesarLoginUsuario(mensaje);
                break;
                
            case "LOGIN_PASSWORD":
                procesarLoginPassword(mensaje);
                break;
        }
    }
    
    private void bloquearUsuario(String usuarioABloquear) throws IOException {
        if (usuarioABloquear.equals(nombreUsuario)) {
            enviarMensaje("ERROR: No puedes bloquearte a ti mismo");
            return;
        }
        
        if (!ServidorMulti.db.existeUsuario(usuarioABloquear)) {
            enviarMensaje("ERROR: Usuario '" + usuarioABloquear + "' no existe");
            return;
        }
        
        if (ServidorMulti.db.estaBloqueado(nombreUsuario, usuarioABloquear)) {
            enviarMensaje("ERROR: Ya bloqueaste a '" + usuarioABloquear + "'");
            return;
        }
        
        if (ServidorMulti.db.bloquearUsuario(nombreUsuario, usuarioABloquear)) {
            enviarMensaje("OK: Bloqueaste a '" + usuarioABloquear + "'");
            System.out.println(nombreUsuario + " bloqueo a " + usuarioABloquear);
        } else {
            enviarMensaje("ERROR: No se pudo bloquear");
        }
    }
    
    private void desbloquearUsuario(String usuarioADesbloquear) throws IOException {
        if (!ServidorMulti.db.estaBloqueado(nombreUsuario, usuarioADesbloquear)) {
            enviarMensaje("ERROR: '" + usuarioADesbloquear + "' no esta bloqueado");
            return;
        }
        
        if (ServidorMulti.db.desbloquearUsuario(nombreUsuario, usuarioADesbloquear)) {
            enviarMensaje("OK: Desbloqueaste a '" + usuarioADesbloquear + "'");
            System.out.println(nombreUsuario + " desbloqueo a " + usuarioADesbloquear);
        } else {
            enviarMensaje("ERROR: No se pudo desbloquear");
        }
    }
    
    private void listarBloqueados() throws IOException {
        List<String> bloqueados = ServidorMulti.db.listarBloqueados(nombreUsuario);
        
        if (bloqueados.isEmpty()) {
            enviarMensaje("No tienes usuarios bloqueados");
            return;
        }
        
        enviarMensaje("=== USUARIOS BLOQUEADOS ===");
        for (String bloqueado : bloqueados) {
            enviarMensaje("  - " + bloqueado);
        }
        enviarMensaje("Total: " + bloqueados.size());
    }
    
    private void listarUsuarios() throws IOException {
        List<String> usuarios = ServidorMulti.db.listarUsuarios();
        
        if (usuarios.isEmpty()) {
            enviarMensaje("No hay usuarios registrados");
            return;
        }
        
        enviarMensaje("=== USUARIOS REGISTRADOS ===");
        for (String usuario : usuarios) {
            String estado = "";
            if (autenticado && ServidorMulti.db.estaBloqueado(nombreUsuario, usuario)) {
                estado = " [BLOQUEADO]";
            }
            if (autenticado && usuario.equals(nombreUsuario)) {
                estado = " [TU]";
            }
            enviarMensaje("  - " + usuario + estado);
        }
        enviarMensaje("Total: " + usuarios.size());
    }
    
    private void mostrarMenuAutenticacion() throws IOException {
        enviarMensaje("");
        enviarMensaje("*** LIMITE ALCANZADO ***");
        enviarMensaje("Debes autenticarte para continuar");
        enviarMensaje("");
        enviarMensaje("1. Registrarse");
        enviarMensaje("2. Iniciar sesion");
        enviarMensaje("");
        enviarMensaje("Elige (1 o 2):");
        esperandoMenu = true;
        opcionMenu = "ELEGIR";
    }
    
    private void procesarMenuAutenticacion(String mensaje) throws IOException {
        if (mensaje.equals("1")) {
            opcionMenu = "REGISTRO_USUARIO";
            enviarMensaje("");
            enviarMensaje("=== REGISTRO ===");
            enviarMensaje("Usuario:");
        } else if (mensaje.equals("2")) {
            opcionMenu = "LOGIN_USUARIO";
            enviarMensaje("");
            enviarMensaje("=== LOGIN ===");
            enviarMensaje("Usuario:");
        } else {
            enviarMensaje("ERROR: Escribe 1 o 2");
        }
    }
    
    private void procesarRegistroUsuario(String mensaje) throws IOException {
        usuarioTemp = mensaje.trim();
        if (usuarioTemp.isEmpty()) {
            enviarMensaje("ERROR: No puede estar vacio");
            enviarMensaje("Usuario:");
            return;
        }
        if (ServidorMulti.db.existeUsuario(usuarioTemp)) {
            enviarMensaje("ERROR: Usuario ya existe");
            enviarMensaje("Usuario:");
            return;
        }
        opcionMenu = "REGISTRO_PASSWORD";
        enviarMensaje("Contraseña:");
    }
    
    private void procesarRegistroPassword(String mensaje) throws IOException {
        String password = mensaje.trim();
        if (password.isEmpty()) {
            enviarMensaje("ERROR: No puede estar vacia");
            enviarMensaje("Contraseña:");
            return;
        }
        
        if (ServidorMulti.db.registrarUsuario(usuarioTemp, password)) {
            autenticado = true;
            nombreUsuario = usuarioTemp;
            mensajesEnviados = 0;
            esperandoMenu = false;
            enviarMensaje("");
            enviarMensaje("OK: Registro exitoso!");
            enviarMensaje("Bienvenido " + nombreUsuario);
            enviarMensaje("");
            System.out.println("Nuevo usuario: " + nombreUsuario);
        } else {
            enviarMensaje("ERROR: No se pudo registrar");
            opcionMenu = "ELEGIR";
        }
        usuarioTemp = "";
    }
    
    private void procesarLoginUsuario(String mensaje) throws IOException {
        usuarioTemp = mensaje.trim();
        opcionMenu = "LOGIN_PASSWORD";
        enviarMensaje("Contraseña:");
    }
    
    private void procesarLoginPassword(String mensaje) throws IOException {
        if (ServidorMulti.db.autenticarUsuario(usuarioTemp, mensaje.trim())) {
            autenticado = true;
            nombreUsuario = usuarioTemp;
            mensajesEnviados = 0;
            esperandoMenu = false;
            enviarMensaje("");
            enviarMensaje("OK: Bienvenido " + nombreUsuario);
            enviarMensaje("");
            System.out.println(nombreUsuario + " inicio sesion");
        } else {
            enviarMensaje("");
            enviarMensaje("ERROR: Credenciales incorrectas");
            enviarMensaje("1. Reintentar");
            enviarMensaje("2. Registrarse");
            opcionMenu = "ELEGIR";
        }
        usuarioTemp = "";
    }
    
    private void enviarMensaje(String msg) throws IOException {
        salida.writeUTF(msg);
        salida.flush();
    }
    
    private void enviarMensajeDirecto(String destinatario, String textoMensaje) throws IOException {
        UnCliente clienteDestino = null;
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente.autenticado && cliente.nombreUsuario.equals(destinatario)) {
                clienteDestino = cliente;
                break;
            }
        }
        
        if (clienteDestino == null) {
            enviarMensaje("ERROR: Usuario '" + destinatario + "' no conectado");
            return;
        }
        
        if (autenticado && clienteDestino.autenticado) {
            if (ServidorMulti.db.estaBloqueado(clienteDestino.nombreUsuario, nombreUsuario)) {
                enviarMensaje("ERROR: No puedes enviar mensajes a '" + destinatario + "'");
                return;
            }
        }
        
        String prefijo = autenticado ? "[MD de " + nombreUsuario + "]" : "[MD de Invitado#" + miId + "]";
        clienteDestino.enviarMensaje(prefijo + ": " + textoMensaje);
        enviarMensaje("ENVIADO -> " + destinatario);
    }
}