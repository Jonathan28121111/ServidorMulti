package servidormulti;

import java.util.Random;

public class JuegoGato {
    private final String jugador1;
    private final String jugador2;
    private final char[][] tablero;
    private String turnoActual;
    private boolean juegoActivo;
    private final char SIMBOLO_J1 = 'X';
    private final char SIMBOLO_J2 = 'O';
    private final char VACIO = '-';
    
    public JuegoGato(String jugador1, String jugador2) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.tablero = new char[3][3];
        this.juegoActivo = true;
        inicializarTablero();
        determinarQuienEmpieza();
    }
    
    private void inicializarTablero() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tablero[i][j] = VACIO;
            }
        }
    }
    
    private void determinarQuienEmpieza() {
        Random random = new Random();
        turnoActual = random.nextBoolean() ? jugador1 : jugador2;
    }
    
    public String getTurnoActual() {
        return turnoActual;
    }
    
    public String getJugador1() {
        return jugador1;
    }
    
    public String getJugador2() {
        return jugador2;
    }
    
    public String getOponente(String jugador) {
        return jugador.equals(jugador1) ? jugador2 : jugador1;
    }
    
    public boolean esJugadorEnPartida(String jugador) {
        return jugador.equals(jugador1) || jugador.equals(jugador2);
    }
    
    public boolean isJuegoActivo() {
        return juegoActivo;
    }
    
    public void finalizarJuego() {
        juegoActivo = false;
    }
    
    public char getSimbolo(String jugador) {
        return jugador.equals(jugador1) ? SIMBOLO_J1 : SIMBOLO_J2;
    }
    
    public boolean realizarMovimiento(String jugador, int fila, int columna) {
        if (!juegoActivo) {
            return false;
        }
        
        if (!turnoActual.equals(jugador)) {
            return false;
        }
        
        if (fila < 0 || fila > 2 || columna < 0 || columna > 2) {
            return false;
        }
        
        if (tablero[fila][columna] != VACIO) {
            return false;
        }
        
        tablero[fila][columna] = getSimbolo(jugador);
        cambiarTurno();
        return true;
    }
    
    private void cambiarTurno() {
        turnoActual = turnoActual.equals(jugador1) ? jugador2 : jugador1;
    }
    
    public ResultadoJuego verificarEstado() {
        // Verificar filas
        for (int i = 0; i < 3; i++) {
            if (tablero[i][0] != VACIO && 
                tablero[i][0] == tablero[i][1] && 
                tablero[i][1] == tablero[i][2]) {
                return new ResultadoJuego(true, getGanadorPorSimbolo(tablero[i][0]), false);
            }
        }
        
        // Verificar columnas
        for (int j = 0; j < 3; j++) {
            if (tablero[0][j] != VACIO && 
                tablero[0][j] == tablero[1][j] && 
                tablero[1][j] == tablero[2][j]) {
                return new ResultadoJuego(true, getGanadorPorSimbolo(tablero[0][j]), false);
            }
        }
        
        // Verificar diagonal principal
        if (tablero[0][0] != VACIO && 
            tablero[0][0] == tablero[1][1] && 
            tablero[1][1] == tablero[2][2]) {
            return new ResultadoJuego(true, getGanadorPorSimbolo(tablero[0][0]), false);
        }
        
        // Verificar diagonal secundaria
        if (tablero[0][2] != VACIO && 
            tablero[0][2] == tablero[1][1] && 
            tablero[1][1] == tablero[2][0]) {
            return new ResultadoJuego(true, getGanadorPorSimbolo(tablero[0][2]), false);
        }
        
        // Verificar empate
        boolean tableroLleno = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (tablero[i][j] == VACIO) {
                    tableroLleno = false;
                    break;
                }
            }
        }
        
        if (tableroLleno) {
            return new ResultadoJuego(true, null, true);
        }
        
        return new ResultadoJuego(false, null, false);
    }
    
    private String getGanadorPorSimbolo(char simbolo) {
        return simbolo == SIMBOLO_J1 ? jugador1 : jugador2;
    }
    
    public String obtenerTableroTexto() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n┌───┬───┬───┐\n");
        for (int i = 0; i < 3; i++) {
            sb.append("│");
            for (int j = 0; j < 3; j++) {
                char celda = tablero[i][j];
                sb.append(" ").append(celda == VACIO ? " " : celda).append(" ");
                sb.append(j < 2 ? "│" : "");
            }
            sb.append("│\n");
            if (i < 2) {
                sb.append("├───┼───┼───┤\n");
            }
        }
        sb.append("└───┴───┴───┘\n");
        sb.append("  0   1   2\n");
        return sb.toString();
    }
    
    public static class ResultadoJuego {
        private final boolean finalizado;
        private final String ganador;
        private final boolean empate;
        
        public ResultadoJuego(boolean finalizado, String ganador, boolean empate) {
            this.finalizado = finalizado;
            this.ganador = ganador;
            this.empate = empate;
        }
        
        public boolean isFinalizado() {
            return finalizado;
        }
        
        public String getGanador() {
            return ganador;
        }
        
        public boolean isEmpate() {
            return empate;
        }
    }
}