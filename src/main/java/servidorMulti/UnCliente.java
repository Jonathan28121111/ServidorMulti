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
            enviarMensaje("\n=== BIENVENIDO AL CHAT + GATO ===\n");
            mostrarMenuAutenticacion();
            
            while (!socket.isClosed()) {
                String mensaje = entrada.readUTF();
                
                if (autenticado && !esperandoMenu) {
                    if (ServidorMulti.gestorJuegos.tieneInvitacionPendiente(nombreUsuario)) {
                        String invitador = ServidorMulti.gestorJuegos.obtenerInvitador(nombreUsuario);
                        if (invitador != null) {
                            enviarMensaje("\n" + invitador + " te invita a jugar GATO\nAceptar o Rechazar: ");
                            esperandoMenu = true;
                            opcionMenu = "RESPONDER_INVITACION";
                            continue;
                        }
                    }
                }
                
                if (esperandoMenu) {
                    procesarMenuAcciones(mensaje);
                    continue;
                }
                
                if (mensaje.equals("1")) {
                    if (autenticado && ServidorMulti.gestorJuegos.obtenerJuego(nombreUsuario) != null) {
                        enviarMensaje("ERROR: No puedes enviar mensajes mientras estas en un juego\n");
                        continue;
                    }
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
                    if (ServidorMulti.gestorJuegos.obtenerJuego(nombreUsuario) != null) {
                        enviarMensaje("ERROR: No puedes enviar mensajes mientras estas en un juego\n");
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
                    if (!autenticado) {
                        enviarMensaje("ERROR: Debes iniciar sesion");
                        continue;
                    }
                    listarUsuarios();
                    continue;
                }
                
                if (mensaje.equals("8")) {
                    if (!autenticado) {
                        enviarMensaje("ERROR: Debes iniciar sesion");
                        continue;
                    }
                    enviarMensaje("Usuario a invitar:");
                    esperandoMenu = true;
                    opcionMenu = "INVITAR_GATO";
                    continue;
                }
                
                if (mensaje.equals("9")) {
                    if (!autenticado) {
                        enviarMensaje("ERROR: Debes iniciar sesion");
                        continue;
                    }
                    mostrarEstadoJuego();
                    continue;
                }
                
                if (mensaje.equals("10")) {
                    if (!autenticado) {
                        enviarMensaje("ERROR: Debes iniciar sesion");
                        continue;
                    }
                    mostrarRankingGeneral();
                    continue;
                }
                
                if (mensaje.equals("11")) {
                    if (!autenticado) {
                        enviarMensaje("ERROR: Debes iniciar sesion");
                        continue;
                    }
                    enviarMensaje("Primer jugador:");
                    esperandoMenu = true;
                    opcionMenu = "COMPARAR_J1";
                    continue;
                }
                
                if (mensaje.equalsIgnoreCase("salir") || mensaje.equals("7")) {
                    enviarMensaje("Cerrando conexion...");
                    socket.close();
                    break;
                }
                
                if (!autenticado) {
                    if (mensajesEnviados >= 3) {
                        enviarMensaje("\nLIMITE ALCANZADO: Debes autenticarte para continuar\n");
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
                        if (cliente.autenticado && ServidorMulti.gestorJuegos.obtenerJuego(cliente.nombreUsuario) != null) {
                            continue;
                        }
                        
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
            manejarDesconexion();
        }
    }
    
    private void manejarDesconexion() {
        if (autenticado) {
            ServidorMulti.gestorJuegos.cancelarInvitacionesDeUsuario(nombreUsuario);
            
            String ganadorPorDesconexion = ServidorMulti.gestorJuegos.finalizarJuegoPorDesconexion(nombreUsuario);
            if (ganadorPorDesconexion != null) {
                System.out.println(nombreUsuario + " se desconecto. " + ganadorPorDesconexion + " gana por abandono");
                
                JuegoGato juegoTemp = new JuegoGato(nombreUsuario, ganadorPorDesconexion);
                ServidorMulti.db.registrarPartida(juegoTemp.getJugador1(), juegoTemp.getJugador2(), ganadorPorDesconexion, false);
                
                notificarGanadorPorDesconexion(ganadorPorDesconexion, nombreUsuario);
            }
        }
        
        ServidorMulti.clientes.remove(Integer.toString(miId));
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void notificarGanadorPorDesconexion(String ganador, String perdedor) {
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente.autenticado && cliente.nombreUsuario.equals(ganador)) {
                try {
                    cliente.enviarMensaje("\n*** GANASTE ***\n" + perdedor + " se desconecto\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
    
    private void procesarMenuAcciones(String mensaje) throws IOException {
        switch (opcionMenu) {
            case "ENVIAR_MENSAJE":
                procesarEnvioMensaje(mensaje);
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
                
            case "INVITAR_GATO":
                invitarAJugarGato(mensaje.trim());
                esperandoMenu = false;
                opcionMenu = "";
                break;
                
            case "RESPONDER_INVITACION":
                responderInvitacion(mensaje.trim());
                esperandoMenu = false;
                opcionMenu = "";
                break;
                
            case "JUGAR_GATO":
                procesarMovimientoGato(mensaje.trim());
                break;
                
            case "COMPARAR_J1":
                usuarioTemp = mensaje.trim();
                enviarMensaje("Segundo jugador:");
                opcionMenu = "COMPARAR_J2";
                break;
                
            case "COMPARAR_J2":
                compararJugadores(usuarioTemp, mensaje.trim());
                esperandoMenu = false;
                opcionMenu = "";
                usuarioTemp = "";
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
                
            case "LOGIN_REINTENTAR":
                procesarMenuLoginError(mensaje);
                break;
        }
    }
    
    private void procesarEnvioMensaje(String mensaje) throws IOException {
        JuegoGato juego = ServidorMulti.gestorJuegos.obtenerJuego(nombreUsuario);
        if (juego != null && juego.isJuegoActivo()) {
            enviarMensaje("ERROR: Estas en un juego\nUsa: FILA COLUMNA o RENDIRSE\n");
            esperandoMenu = true;
            opcionMenu = "JUGAR_GATO";
            return;
        }
        
        String prefijo = autenticado ? "[" + nombreUsuario + "]" : "[Invitado#" + miId + "]";
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente.miId != this.miId) {
                if (cliente.autenticado && ServidorMulti.gestorJuegos.obtenerJuego(cliente.nombreUsuario) != null) {
                    continue;
                }
                
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
    }
    
    private void invitarAJugarGato(String usuarioInvitado) throws IOException {
        if (usuarioInvitado.isEmpty()) {
            enviarMensaje("ERROR: Usuario vacio");
            return;
        }
        
        if (usuarioInvitado.equals(nombreUsuario)) {
            enviarMensaje("ERROR: No puedes jugar contigo mismo");
            return;
        }
        
        if (!ServidorMulti.db.existeUsuario(usuarioInvitado)) {
            enviarMensaje("ERROR: Usuario no existe");
            return;
        }
        
        UnCliente clienteInvitado = buscarClientePorNombre(usuarioInvitado);
        if (clienteInvitado == null) {
            enviarMensaje("ERROR: Usuario no conectado");
            return;
        }
        
        if (ServidorMulti.gestorJuegos.existeJuegoEntre(nombreUsuario, usuarioInvitado)) {
            enviarMensaje("ERROR: Ya tienes un juego con " + usuarioInvitado);
            return;
        }
        
        if (ServidorMulti.gestorJuegos.tieneInvitacionPendiente(usuarioInvitado)) {
            enviarMensaje("ERROR: " + usuarioInvitado + " ya tiene una invitacion pendiente");
            return;
        }
        
        if (ServidorMulti.gestorJuegos.obtenerJuego(usuarioInvitado) != null) {
            enviarMensaje("ERROR: " + usuarioInvitado + " esta en un juego");
            return;
        }
        
        if (ServidorMulti.gestorJuegos.obtenerJuego(nombreUsuario) != null) {
            enviarMensaje("ERROR: Ya estas en un juego");
            return;
        }
        
        if (ServidorMulti.gestorJuegos.enviarInvitacion(nombreUsuario, usuarioInvitado)) {
            enviarMensaje("Invitacion enviada a " + usuarioInvitado);
            clienteInvitado.enviarMensaje("\n" + nombreUsuario + " te invita a jugar GATO\nAceptar o Rechazar: ");
            clienteInvitado.esperandoMenu = true;
            clienteInvitado.opcionMenu = "RESPONDER_INVITACION";
            System.out.println(nombreUsuario + " invito a " + usuarioInvitado);
        } else {
            enviarMensaje("ERROR: No se pudo enviar invitacion");
        }
    }
    
    private void responderInvitacion(String respuesta) throws IOException {
        String invitador = ServidorMulti.gestorJuegos.obtenerInvitador(nombreUsuario);
        
        if (invitador == null) {
            enviarMensaje("ERROR: No tienes invitaciones pendientes");
            return;
        }
        
        if (respuesta.equalsIgnoreCase("ACEPTAR")) {
            JuegoGato juego = ServidorMulti.gestorJuegos.aceptarInvitacion(nombreUsuario);
            
            if (juego != null) {
                enviarMensaje("Invitacion aceptada!");
                iniciarPartida(juego);
                
                UnCliente clienteInvitador = buscarClientePorNombre(invitador);
                if (clienteInvitador != null) {
                    clienteInvitador.enviarMensaje("\n" + nombreUsuario + " acepto tu invitacion!");
                    clienteInvitador.iniciarPartida(juego);
                }
                
                System.out.println("Partida iniciada: " + juego.getJugador1() + " vs " + juego.getJugador2());
            } else {
                enviarMensaje("ERROR: No se pudo iniciar el juego");
            }
        } else if (respuesta.equalsIgnoreCase("RECHAZAR")) {
            ServidorMulti.gestorJuegos.rechazarInvitacion(nombreUsuario);
            enviarMensaje("Invitacion rechazada");
            
            UnCliente clienteInvitador = buscarClientePorNombre(invitador);
            if (clienteInvitador != null) {
                clienteInvitador.enviarMensaje("\n" + nombreUsuario + " rechazo tu invitacion");
            }
            
            System.out.println(nombreUsuario + " rechazo invitacion de " + invitador);
        } else {
            enviarMensaje("ERROR: Responde ACEPTAR o RECHAZAR");
            esperandoMenu = true;
            opcionMenu = "RESPONDER_INVITACION";
        }
    }
    
    private void iniciarPartida(JuegoGato juego) throws IOException {
        enviarMensaje("\n=== JUEGO DEL GATO ===");
        enviarMensaje("Jugador 1: " + juego.getJugador1() + " (X)");
        enviarMensaje("Jugador 2: " + juego.getJugador2() + " (O)");
        enviarMensaje("Empieza: " + juego.getTurnoActual());
        enviarMensaje("Tu simbolo: " + juego.getSimbolo(nombreUsuario));
        enviarMensaje(juego.obtenerTableroTexto());
        
        if (juego.getTurnoActual().equals(nombreUsuario)) {
            enviarMensaje(">>> ES TU TURNO <<<");
            enviarMensaje("Escribe FILA COLUMNA (ej: 0 1) o RENDIRSE\n");
        } else {
            enviarMensaje("Esperando a " + juego.getTurnoActual() + "...\n");
        }
        
        esperandoMenu = true;
        opcionMenu = "JUGAR_GATO";
    }
    
    private void procesarMovimientoGato(String entrada) throws IOException {
        JuegoGato juego = ServidorMulti.gestorJuegos.obtenerJuego(nombreUsuario);
        
        if (juego == null || !juego.isJuegoActivo()) {
            enviarMensaje("ERROR: No estas en un juego activo");
            esperandoMenu = false;
            opcionMenu = "";
            return;
        }
        
        if (entrada.equalsIgnoreCase("RENDIRSE")) {
            String oponente = juego.getOponente(nombreUsuario);
            
            ServidorMulti.db.registrarPartida(juego.getJugador1(), juego.getJugador2(), oponente, false);
            
            ServidorMulti.gestorJuegos.eliminarJuego(juego);
            
            enviarMensaje("\nTe rendiste. " + oponente + " gana!");
            esperandoMenu = false;
            opcionMenu = "";
            
            UnCliente clienteOponente = buscarClientePorNombre(oponente);
            if (clienteOponente != null) {
                clienteOponente.enviarMensaje("\n*** GANASTE ***\n" + nombreUsuario + " se rindio\n");
                clienteOponente.esperandoMenu = false;
                clienteOponente.opcionMenu = "";
            }
            
            System.out.println(nombreUsuario + " se rindio ante " + oponente);
            return;
        }
        
        if (!juego.getTurnoActual().equals(nombreUsuario)) {
            enviarMensaje("ERROR: No es tu turno. Espera a " + juego.getTurnoActual());
            return;
        }
        
        String[] partes = entrada.trim().split("\\s+");
        if (partes.length != 2) {
            enviarMensaje("ERROR: Formato incorrecto. Usa: FILA COLUMNA");
            return;
        }
        
        try {
            int fila = Integer.parseInt(partes[0]);
            int columna = Integer.parseInt(partes[1]);
            
            if (juego.realizarMovimiento(nombreUsuario, fila, columna)) {
                mostrarTableroActualizado(juego);
                
                JuegoGato.ResultadoJuego resultado = juego.verificarEstado();
                
                if (resultado.isFinalizado()) {
                    finalizarPartida(juego, resultado);
                } else {
                    String siguienteTurno = juego.getTurnoActual();
                    enviarMensaje("Esperando a " + siguienteTurno + "...");
                    
                    UnCliente clienteOponente = buscarClientePorNombre(siguienteTurno);
                    if (clienteOponente != null) {
                        clienteOponente.enviarMensaje("\n>>> ES TU TURNO <<<\nEscribe FILA COLUMNA o RENDIRSE\n");
                        clienteOponente.esperandoMenu = true;
                        clienteOponente.opcionMenu = "JUGAR_GATO";
                    }
                }
            } else {
                enviarMensaje("ERROR: Movimiento invalido");
            }
        } catch (NumberFormatException e) {
            enviarMensaje("ERROR: Debes ingresar numeros");
        }
    }
    
    private void mostrarTableroActualizado(JuegoGato juego) throws IOException {
        String oponente = juego.getOponente(nombreUsuario);
        
        enviarMensaje(juego.obtenerTableroTexto());
        
        UnCliente clienteOponente = buscarClientePorNombre(oponente);
        if (clienteOponente != null) {
            clienteOponente.enviarMensaje(juego.obtenerTableroTexto());
        }
    }
    
    private void finalizarPartida(JuegoGato juego, JuegoGato.ResultadoJuego resultado) throws IOException {
        String j1 = juego.getJugador1();
        String j2 = juego.getJugador2();
        
        UnCliente cliente1 = buscarClientePorNombre(j1);
        UnCliente cliente2 = buscarClientePorNombre(j2);
        
        if (resultado.isEmpate()) {
            ServidorMulti.db.registrarPartida(j1, j2, null, true);
            
            String mensajeEmpate = "\n=== EMPATE ===\nNadie gano\n";
            
            if (cliente1 != null) {
                cliente1.enviarMensaje(mensajeEmpate);
                cliente1.esperandoMenu = false;
                cliente1.opcionMenu = "";
            }
            if (cliente2 != null) {
                cliente2.enviarMensaje(mensajeEmpate);
                cliente2.esperandoMenu = false;
                cliente2.opcionMenu = "";
            }
            
            System.out.println("Empate: " + j1 + " vs " + j2);
        } else {
            String ganador = resultado.getGanador();
            String perdedor = ganador.equals(j1) ? j2 : j1;
            
            ServidorMulti.db.registrarPartida(j1, j2, ganador, false);
            
            UnCliente clienteGanador = buscarClientePorNombre(ganador);
            UnCliente clientePerdedor = buscarClientePorNombre(perdedor);
            
            if (clienteGanador != null) {
                clienteGanador.enviarMensaje("\n*** GANASTE ***\n");
                clienteGanador.esperandoMenu = false;
                clienteGanador.opcionMenu = "";
            }
            
            if (clientePerdedor != null) {
                clientePerdedor.enviarMensaje("\n*** PERDISTE ***\nGanador: " + ganador + "\n");
                clientePerdedor.esperandoMenu = false;
                clientePerdedor.opcionMenu = "";
            }
            
            System.out.println("Ganador: " + ganador);
        }
        
        ServidorMulti.gestorJuegos.eliminarJuego(juego);
    }
    
    private void mostrarEstadoJuego() throws IOException {
        JuegoGato juego = ServidorMulti.gestorJuegos.obtenerJuego(nombreUsuario);
        
        if (juego == null) {
            enviarMensaje("No estas en ningun juego activo");
            return;
        }
        
        enviarMensaje("\n=== ESTADO DEL JUEGO ===");
        enviarMensaje("Oponente: " + juego.getOponente(nombreUsuario));
        enviarMensaje("Tu simbolo: " + juego.getSimbolo(nombreUsuario));
        enviarMensaje("Turno actual: " + juego.getTurnoActual());
        enviarMensaje(juego.obtenerTableroTexto());
        
        if (juego.getTurnoActual().equals(nombreUsuario)) {
            enviarMensaje(">>> ES TU TURNO <<<");
        } else {
            enviarMensaje("Esperando movimiento...");
        }
    }
    
    private void mostrarRankingGeneral() throws IOException {
        List<String> ranking = ServidorMulti.db.obtenerRankingGeneral();
        
        if (ranking.isEmpty()) {
            enviarMensaje("\nNo hay partidas registradas aun\n");
            return;
        }
        
        enviarMensaje("\n=== RANKING GENERAL DEL GATO ===\n");
        for (String linea : ranking) {
            enviarMensaje(linea);
        }
        enviarMensaje("\nVictoria = 2 puntos | Empate = 1 punto\n");
    }
    
    private void compararJugadores(String jugador1, String jugador2) throws IOException {
        if (jugador1.isEmpty() || jugador2.isEmpty()) {
            enviarMensaje("ERROR: Nombres de jugadores vacios\n");
            return;
        }
        
        if (!ServidorMulti.db.existeUsuario(jugador1)) {
            enviarMensaje("ERROR: " + jugador1 + " no existe\n");
            return;
        }
        
        if (!ServidorMulti.db.existeUsuario(jugador2)) {
            enviarMensaje("ERROR: " + jugador2 + " no existe\n");
            return;
        }
        
        if (jugador1.equals(jugador2)) {
            enviarMensaje("ERROR: Debes comparar dos jugadores diferentes\n");
            return;
        }
        
        String estadisticas = ServidorMulti.db.obtenerEstadisticasEntreJugadores(jugador1, jugador2);
        enviarMensaje(estadisticas);
    }
    
    private UnCliente buscarClientePorNombre(String nombre) {
        for (UnCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente.autenticado && cliente.nombreUsuario != null && 
                cliente.nombreUsuario.equals(nombre)) {
                return cliente;
            }
        }
        return null;
    }
    
    private void bloquearUsuario(String usuarioABloquear) throws IOException {
        if (usuarioABloquear.equals(nombreUsuario)) {
            enviarMensaje("ERROR: No puedes bloquearte a ti mismo\n");
            return;
        }
        
        if (!ServidorMulti.db.existeUsuario(usuarioABloquear)) {
            enviarMensaje("ERROR: Usuario no existe\n");
            return;
        }
        
        if (ServidorMulti.db.estaBloqueado(nombreUsuario, usuarioABloquear)) {
            enviarMensaje("ERROR: Ya bloqueaste a este usuario\n");
            return;
        }
        
        if (ServidorMulti.db.bloquearUsuario(nombreUsuario, usuarioABloquear)) {
            enviarMensaje("OK: Usuario bloqueado\n");
            System.out.println(nombreUsuario + " bloqueo a " + usuarioABloquear);
        } else {
            enviarMensaje("ERROR: No se pudo bloquear\n");
        }
    }
    
    private void desbloquearUsuario(String usuarioADesbloquear) throws IOException {
        if (!ServidorMulti.db.estaBloqueado(nombreUsuario, usuarioADesbloquear)) {
            enviarMensaje("ERROR: Usuario no esta bloqueado\n");
            return;
        }
        
        if (ServidorMulti.db.desbloquearUsuario(nombreUsuario, usuarioADesbloquear)) {
            enviarMensaje("OK: Usuario desbloqueado\n");
            System.out.println(nombreUsuario + " desbloqueo a " + usuarioADesbloquear);
        } else {
            enviarMensaje("ERROR: No se pudo desbloquear\n");
        }
    }
    
    private void listarBloqueados() throws IOException {
        List<String> bloqueados = ServidorMulti.db.listarBloqueados(nombreUsuario);
        
        if (bloqueados.isEmpty()) {
            enviarMensaje("No tienes usuarios bloqueados\n");
            return;
        }
        
        enviarMensaje("=== USUARIOS BLOQUEADOS ===");
        for (String bloqueado : bloqueados) {
            enviarMensaje("  - " + bloqueado);
        }
        enviarMensaje("Total: " + bloqueados.size() + "\n");
    }
    
    private void listarUsuarios() throws IOException {
        List<String> usuarios = ServidorMulti.db.listarUsuarios();
        
        if (usuarios.isEmpty()) {
            enviarMensaje("No hay usuarios registrados\n");
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
            
            if (ServidorMulti.gestorJuegos.obtenerJuego(usuario) != null) {
                estado += " [EN JUEGO]";
            }
            
            boolean conectado = buscarClientePorNombre(usuario) != null;
            if (conectado) {
                estado += " [ONLINE]";
            }
            
            enviarMensaje("  - " + usuario + estado);
        }
        enviarMensaje("Total: " + usuarios.size() + "\n");
    }
    
    private void mostrarMenuAutenticacion() throws IOException {
        enviarMensaje("""
            
            1. Registrarse
            2. Iniciar sesion
            3. Continuar como invitado
            
            Opcion: """);
        esperandoMenu = true;
        opcionMenu = "ELEGIR";
    }
    
    private void procesarMenuAutenticacion(String mensaje) throws IOException {
        if (mensaje.equals("1")) {
            opcionMenu = "REGISTRO_USUARIO";
            enviarMensaje("\n=== REGISTRO ===\nUsuario: ");
        } else if (mensaje.equals("2")) {
            opcionMenu = "LOGIN_USUARIO";
            enviarMensaje("\n=== LOGIN ===\nUsuario: ");
        } else if (mensaje.equals("3")) {
            esperandoMenu = false;
            opcionMenu = "";
            enviarMensaje("\n=== MODO INVITADO ===\nTienes 3 mensajes gratis\n");
        } else {
            enviarMensaje("ERROR: Escribe 1, 2 o 3\n");
            mostrarMenuAutenticacion();
        }
    }
    
    private void procesarMenuLoginError(String mensaje) throws IOException {
        if (mensaje.equals("1")) {
            opcionMenu = "LOGIN_USUARIO";
            enviarMensaje("\n=== LOGIN ===\nUsuario: ");
        } else if (mensaje.equals("2")) {
            // Ir a registro
            opcionMenu = "REGISTRO_USUARIO";
            enviarMensaje("\n=== REGISTRO ===\nUsuario: ");
        } else {
            enviarMensaje("ERROR: Escribe 1 o 2\n\n1. Reintentar\n2. Registrarse\n\nOpcion: ");
        }
    }
    
    private void procesarRegistroUsuario(String mensaje) throws IOException {
        usuarioTemp = mensaje.trim();
        if (usuarioTemp.isEmpty()) {
            enviarMensaje("ERROR: No puede estar vacio\nUsuario: ");
            return;
        }
        if (ServidorMulti.db.existeUsuario(usuarioTemp)) {
            enviarMensaje("ERROR: Usuario ya existe\nUsuario: ");
            return;
        }
        opcionMenu = "REGISTRO_PASSWORD";
        enviarMensaje("Contraseña: ");
    }
    
    private void procesarRegistroPassword(String mensaje) throws IOException {
        String password = mensaje.trim();
        if (password.isEmpty()) {
            enviarMensaje("ERROR: No puede estar vacia\nContraseña: ");
            return;
        }
        
        if (ServidorMulti.db.registrarUsuario(usuarioTemp, password)) {
            autenticado = true;
            nombreUsuario = usuarioTemp;
            mensajesEnviados = 0;
            esperandoMenu = false;
            enviarMensaje("\nREGISTRO EXITOSO\nBienvenido " + nombreUsuario + "\n");
            mostrarMenuSiAutenticado();
            System.out.println("Nuevo usuario: " + nombreUsuario);
        } else {
            enviarMensaje("ERROR: No se pudo registrar\n");
            mostrarMenuAutenticacion();
        }
        usuarioTemp = "";
    }
    
    private void procesarLoginUsuario(String mensaje) throws IOException {
        usuarioTemp = mensaje.trim();
        opcionMenu = "LOGIN_PASSWORD";
        enviarMensaje("Contraseña: ");
    }
    
    private void procesarLoginPassword(String mensaje) throws IOException {
        if (ServidorMulti.db.autenticarUsuario(usuarioTemp, mensaje.trim())) {
            autenticado = true;
            nombreUsuario = usuarioTemp;
            mensajesEnviados = 0;
            esperandoMenu = false;
            enviarMensaje("\nINICIO DE SESION EXITOSO\nBienvenido " + nombreUsuario + "\n");
            mostrarMenuSiAutenticado();
            System.out.println(nombreUsuario + " inicio sesion");
        } else {
            enviarMensaje("\nERROR: Usuario o contraseña incorrectos\n\n1. Reintentar\n2. Registrarse\n\nOpcion: ");
            opcionMenu = "LOGIN_REINTENTAR";
        }
        usuarioTemp = "";
    }
    
    private void enviarMensaje(String msg) throws IOException {
        salida.writeUTF(msg);
        salida.flush();
    }
    
    private void mostrarMenuSiAutenticado() throws IOException {
        if (autenticado) {
            enviarMensaje("""
                
                === MENU ===
                1. Enviar mensaje
                2. Mensaje directo
                3. Bloquear usuario
                4. Desbloquear usuario
                5. Ver bloqueados
                6. Ver usuarios
                7. Salir
                ---------------------
                JUEGO DEL GATO:
                8. Invitar a jugar
                9. Ver estado juego
                ---------------------
                RANKINGS:
                10. Ver ranking general
                11. Comparar jugadores
                
                Opcion: """);
        }
    }
    
    private void enviarMensajeDirecto(String destinatario, String textoMensaje) throws IOException {
        UnCliente clienteDestino = buscarClientePorNombre(destinatario);
        
        if (clienteDestino == null) {
            enviarMensaje("ERROR: Usuario no conectado\n");
            return;
        }
        
        if (ServidorMulti.gestorJuegos.obtenerJuego(destinatario) != null) {
            enviarMensaje("ERROR: " + destinatario + " esta en un juego, no puede recibir mensajes\n");
            return;
        }
        
        if (autenticado && clienteDestino.autenticado) {
            if (ServidorMulti.db.estaBloqueado(clienteDestino.nombreUsuario, nombreUsuario)) {
                enviarMensaje("ERROR: No puedes enviar mensajes a este usuario\n");
                return;
            }
        }
        
        String prefijo = autenticado ? "[MD de " + nombreUsuario + "]" : "[MD de Invitado#" + miId + "]";
        clienteDestino.enviarMensaje(prefijo + ": " + textoMensaje);
        enviarMensaje("ENVIADO -> " + destinatario + "\n");
    }
}