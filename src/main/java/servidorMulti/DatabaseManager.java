package servidormulti;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:chat.db";
    private Connection connection;
    
    public DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            crearTablas();
            System.out.println("Base de datos conectada exitosamente");
        } catch (Exception e) {
            System.err.println("Error al conectar con la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void crearTablas() {
        String sqlUsuarios = """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String sqlBloqueos = """
            CREATE TABLE IF NOT EXISTS bloqueos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                bloqueador TEXT NOT NULL,
                bloqueado TEXT NOT NULL,
                fecha_bloqueo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(bloqueador, bloqueado),
                FOREIGN KEY (bloqueador) REFERENCES usuarios(username),
                FOREIGN KEY (bloqueado) REFERENCES usuarios(username)
            )
        """;
        
        String sqlEstadisticas = """
            CREATE TABLE IF NOT EXISTS estadisticas_gato (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                victorias INTEGER DEFAULT 0,
                empates INTEGER DEFAULT 0,
                derrotas INTEGER DEFAULT 0,
                puntos INTEGER DEFAULT 0,
                FOREIGN KEY (username) REFERENCES usuarios(username)
            )
        """;
        
        String sqlHistorialPartidas = """
            CREATE TABLE IF NOT EXISTS historial_partidas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                jugador1 TEXT NOT NULL,
                jugador2 TEXT NOT NULL,
                ganador TEXT,
                empate INTEGER DEFAULT 0,
                fecha_partida TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (jugador1) REFERENCES usuarios(username),
                FOREIGN KEY (jugador2) REFERENCES usuarios(username)
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlBloqueos);
            stmt.execute(sqlEstadisticas);
            stmt.execute(sqlHistorialPartidas);
            System.out.println("Tablas creadas/verificadas correctamente");
        } catch (SQLException e) {
            System.err.println("Error al crear tablas: " + e.getMessage());
        }
    }
    
    
    public boolean registrarUsuario(String username, String password) {
        String sql = "INSERT INTO usuarios (username, password) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            
            inicializarEstadisticas(username);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    private void inicializarEstadisticas(String username) {
        String sql = "INSERT INTO estadisticas_gato (username, victorias, empates, derrotas, puntos) VALUES (?, 0, 0, 0, 0)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al inicializar estadisticas: " + e.getMessage());
        }
    }
    
    public boolean autenticarUsuario(String username, String password) {
        String sql = "SELECT password FROM usuarios WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password").equals(password);
            }
        } catch (SQLException e) {
            System.err.println("Error en autenticacion: " + e.getMessage());
        }
        return false;
    }
    
    public boolean existeUsuario(String username) {
        String sql = "SELECT 1 FROM usuarios WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error al verificar usuario: " + e.getMessage());
        }
        return false;
    }
    
    public List<String> listarUsuarios() {
        List<String> usuarios = new ArrayList<>();
        String sql = "SELECT username FROM usuarios ORDER BY username";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usuarios.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar usuarios: " + e.getMessage());
        }
        return usuarios;
    }
    
    
    public boolean bloquearUsuario(String bloqueador, String bloqueado) {
        if (bloqueador.equals(bloqueado)) {
            return false;
        }     
        if (!existeUsuario(bloqueado)) {
            return false; 
        }      
        if (estaBloqueado(bloqueador, bloqueado)) {
            return false;
        }
        
        String sql = "INSERT INTO bloqueos (bloqueador, bloqueado) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al bloquear: " + e.getMessage());
            return false;
        }
    }
    
    public boolean desbloquearUsuario(String bloqueador, String bloqueado) {
        if (!estaBloqueado(bloqueador, bloqueado)) {
            return false; 
        }
        
        String sql = "DELETE FROM bloqueos WHERE bloqueador = ? AND bloqueado = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error al desbloquear: " + e.getMessage());
            return false;
        }
    }
    
    public boolean estaBloqueado(String bloqueador, String bloqueado) {
        String sql = "SELECT 1 FROM bloqueos WHERE bloqueador = ? AND bloqueado = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error al verificar bloqueo: " + e.getMessage());
        }
        return false;
    }
    
    public List<String> listarBloqueados(String usuario) {
        List<String> bloqueados = new ArrayList<>();
        String sql = "SELECT bloqueado FROM bloqueos WHERE bloqueador = ? ORDER BY bloqueado";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                bloqueados.add(rs.getString("bloqueado"));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar bloqueados: " + e.getMessage());
        }
        return bloqueados;
    }
    
    public void registrarPartida(String jugador1, String jugador2, String ganador, boolean empate) {
        String sqlHistorial = "INSERT INTO historial_partidas (jugador1, jugador2, ganador, empate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlHistorial)) {
            pstmt.setString(1, jugador1);
            pstmt.setString(2, jugador2);
            pstmt.setString(3, ganador);
            pstmt.setInt(4, empate ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al registrar partida en historial: " + e.getMessage());
        }
        
        if (empate) {
            actualizarEstadisticas(jugador1, "empate");
            actualizarEstadisticas(jugador2, "empate");
        } else if (ganador != null) {
            actualizarEstadisticas(ganador, "victoria");
            String perdedor = ganador.equals(jugador1) ? jugador2 : jugador1;
            actualizarEstadisticas(perdedor, "derrota");
        }
    }
    
    private void actualizarEstadisticas(String username, String resultado) {
        String sql = "";
        switch (resultado) {
            case "victoria":
                sql = "UPDATE estadisticas_gato SET victorias = victorias + 1, puntos = puntos + 2 WHERE username = ?";
                break;
            case "empate":
                sql = "UPDATE estadisticas_gato SET empates = empates + 1, puntos = puntos + 1 WHERE username = ?";
                break;
            case "derrota":
                sql = "UPDATE estadisticas_gato SET derrotas = derrotas + 1 WHERE username = ?";
                break;
        }
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar estadisticas: " + e.getMessage());
        }
    }
    
    public List<String> obtenerRankingGeneral() {
        List<String> ranking = new ArrayList<>();
        String sql = """
            SELECT username, victorias, empates, derrotas, puntos,
                   (victorias + empates + derrotas) as total_partidas
            FROM estadisticas_gato
            WHERE total_partidas > 0
            ORDER BY puntos DESC, victorias DESC, empates DESC
            LIMIT 20
        """;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int posicion = 1;
            while (rs.next()) {
                String username = rs.getString("username");
                int victorias = rs.getInt("victorias");
                int empates = rs.getInt("empates");
                int derrotas = rs.getInt("derrotas");
                int puntos = rs.getInt("puntos");
                int totalPartidas = rs.getInt("total_partidas");
                
                String linea = String.format("%d. %s - Puntos: %d | V:%d E:%d D:%d | Total: %d",
                    posicion, username, puntos, victorias, empates, derrotas, totalPartidas);
                ranking.add(linea);
                posicion++;
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener ranking: " + e.getMessage());
        }
        
        return ranking;
    }
    
    public String obtenerEstadisticasEntreJugadores(String jugador1, String jugador2) {
        StringBuilder resultado = new StringBuilder();
        
        String sqlEstadisticas = "SELECT victorias, empates, derrotas, puntos FROM estadisticas_gato WHERE username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sqlEstadisticas)) {
            pstmt.setString(1, jugador1);
            ResultSet rs1 = pstmt.executeQuery();
            int v1 = 0, e1 = 0, d1 = 0, p1 = 0;
            if (rs1.next()) {
                v1 = rs1.getInt("victorias");
                e1 = rs1.getInt("empates");
                d1 = rs1.getInt("derrotas");
                p1 = rs1.getInt("puntos");
            }
            
            pstmt.setString(1, jugador2);
            ResultSet rs2 = pstmt.executeQuery();
            int v2 = 0, e2 = 0, d2 = 0, p2 = 0;
            if (rs2.next()) {
                v2 = rs2.getInt("victorias");
                e2 = rs2.getInt("empates");
                d2 = rs2.getInt("derrotas");
                p2 = rs2.getInt("puntos");
            }
            
            resultado.append("\n=== ESTADISTICAS GENERALES ===\n");
            resultado.append(String.format("%s: V:%d E:%d D:%d Pts:%d\n", jugador1, v1, e1, d1, p1));
            resultado.append(String.format("%s: V:%d E:%d D:%d Pts:%d\n\n", jugador2, v2, e2, d2, p2));
            
        } catch (SQLException e) {
            System.err.println("Error al obtener estadisticas individuales: " + e.getMessage());
        }
        
        String sqlHistorial = """
            SELECT ganador, empate FROM historial_partidas
            WHERE (jugador1 = ? AND jugador2 = ?) OR (jugador1 = ? AND jugador2 = ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sqlHistorial)) {
            pstmt.setString(1, jugador1);
            pstmt.setString(2, jugador2);
            pstmt.setString(3, jugador2);
            pstmt.setString(4, jugador1);
            
            ResultSet rs = pstmt.executeQuery();
            
            int victoriasJ1 = 0, victoriasJ2 = 0, empates = 0;
            
            while (rs.next()) {
                boolean esEmpate = rs.getInt("empate") == 1;
                if (esEmpate) {
                    empates++;
                } else {
                    String ganador = rs.getString("ganador");
                    if (ganador.equals(jugador1)) {
                        victoriasJ1++;
                    } else {
                        victoriasJ2++;
                    }
                }
            }
            
            int totalPartidas = victoriasJ1 + victoriasJ2 + empates;
            
            resultado.append("=== ENFRENTAMIENTOS DIRECTOS ===\n");
            if (totalPartidas == 0) {
                resultado.append("No han jugado entre si\n");
            } else {
                resultado.append(String.format("Total de partidas: %d\n", totalPartidas));
                resultado.append(String.format("%s: %d victorias (%.1f%%)\n", 
                    jugador1, victoriasJ1, (victoriasJ1 * 100.0 / totalPartidas)));
                resultado.append(String.format("%s: %d victorias (%.1f%%)\n", 
                    jugador2, victoriasJ2, (victoriasJ2 * 100.0 / totalPartidas)));
                resultado.append(String.format("Empates: %d (%.1f%%)\n", 
                    empates, (empates * 100.0 / totalPartidas)));
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener historial directo: " + e.getMessage());
        }
        
        return resultado.toString();
    }
    
    public void cerrar() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Conexion a base de datos cerrada");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar conexion: " + e.getMessage());
        }
    }
}