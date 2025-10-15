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
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlBloqueos);
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
            return true;
        } catch (SQLException e) {
            return false;
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
            System.err.println("Error en autenticación: " + e.getMessage());
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
    
    public void cerrar() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Conexión a base de datos cerrada");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }
}