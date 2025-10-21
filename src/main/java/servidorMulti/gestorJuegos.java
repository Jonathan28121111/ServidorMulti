package servidormulti;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class gestorJuegos {
    private final Map<String, String> invitacionesPendientes;
    private final Map<String, JuegoGato> juegosActivos;
    
    public gestorJuegos() {
        this.invitacionesPendientes = new HashMap<>();
        this.juegosActivos = new HashMap<>();
    }
    
    public synchronized boolean enviarInvitacion(String invitador, String invitado) {
        if (invitador.equals(invitado)) {
            return false;
        }
        if (invitacionesPendientes.containsKey(invitado)) {
            return false;
        }
        if (existeJuegoEntre(invitador, invitado)) {
            return false;
        }
        
        invitacionesPendientes.put(invitado, invitador);
        return true;
    }
    
    public synchronized JuegoGato aceptarInvitacion(String invitado) {
        String invitador = invitacionesPendientes.remove(invitado);
        if (invitador == null) {
            return null;
        }
        
        JuegoGato juego = new JuegoGato(invitador, invitado);
        String clave = generarClave(invitador, invitado);
        juegosActivos.put(clave, juego);
        
        return juego;
    }
    
    public synchronized boolean rechazarInvitacion(String invitado) {
        return invitacionesPendientes.remove(invitado) != null;
    }
    

    public synchronized String obtenerInvitador(String invitado) {
        return invitacionesPendientes.get(invitado);
    }
    
    public synchronized boolean tieneInvitacionPendiente(String usuario) {
        return invitacionesPendientes.containsKey(usuario);
    }
    

    public synchronized JuegoGato obtenerJuego(String usuario) {
        for (JuegoGato juego : juegosActivos.values()) {
            if (juego.esJugadorEnPartida(usuario)) {
                return juego;
            }
        }
        return null;
    }
    

    public synchronized boolean existeJuegoEntre(String usuario1, String usuario2) {
        String clave1 = generarClave(usuario1, usuario2);
        String clave2 = generarClave(usuario2, usuario1);
        return juegosActivos.containsKey(clave1) || juegosActivos.containsKey(clave2);
    }
    

    public synchronized String finalizarJuegoPorDesconexion(String usuarioDesconectado) {
        JuegoGato juego = obtenerJuego(usuarioDesconectado);
        if (juego == null) {
            return null;
        }
        
        String oponente = juego.getOponente(usuarioDesconectado);
        eliminarJuego(juego);
        
        return oponente;
    }
    
    public synchronized void eliminarJuego(JuegoGato juego) {
        String clave = generarClave(juego.getJugador1(), juego.getJugador2());
        juegosActivos.remove(clave);
        String claveInversa = generarClave(juego.getJugador2(), juego.getJugador1());
        juegosActivos.remove(claveInversa);
        
        juego.finalizarJuego();
    }
    
    public synchronized void cancelarInvitacionesDeUsuario(String usuario) {
        invitacionesPendientes.remove(usuario);
        
        Set<String> destinatariosAEliminar = invitacionesPendientes.entrySet()
            .stream()
            .filter(entry -> entry.getValue().equals(usuario))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        destinatariosAEliminar.forEach(invitacionesPendientes::remove);
    }
    
 
    private String generarClave(String jugador1, String jugador2) {
        if (jugador1.compareTo(jugador2) < 0) {
            return jugador1 + ":" + jugador2;
        } else {
            return jugador2 + ":" + jugador1;
        }
    }
    
  
    public synchronized String obtenerEstadisticas() {
        return String.format("Invitaciones pendientes: %d | Juegos activos: %d",
            invitacionesPendientes.size(), juegosActivos.size());
    }
}