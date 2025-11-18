package servidormulti;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Basededatos {
    private static final String DB_URL = "jdbc:sqlite:chat.db";
    private Connection connection;
    
    public Basededatos() {
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
        
        String sqlGrupos = """
            CREATE TABLE IF NOT EXISTS grupos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT UNIQUE NOT NULL,
                creador TEXT NOT NULL,
                es_sistema INTEGER DEFAULT 0,
                fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (creador) REFERENCES usuarios(username)
            )
        """;
        
        String sqlMiembrosGrupo = """
            CREATE TABLE IF NOT EXISTS miembros_grupo (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                grupo_id INTEGER NOT NULL,
                username TEXT NOT NULL,
                fecha_union TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(grupo_id, username),
                FOREIGN KEY (grupo_id) REFERENCES grupos(id),
                FOREIGN KEY (username) REFERENCES usuarios(username)
            )
        """;
        
        String sqlMensajesGrupo = """
            CREATE TABLE IF NOT EXISTS mensajes_grupo (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                grupo_id INTEGER NOT NULL,
                remitente TEXT NOT NULL,
                mensaje TEXT NOT NULL,
                fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (grupo_id) REFERENCES grupos(id),
                FOREIGN KEY (remitente) REFERENCES usuarios(username)
            )
        """;
        
        String sqlMensajesLeidos = """
            CREATE TABLE IF NOT EXISTS mensajes_leidos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario TEXT NOT NULL,
                mensaje_id INTEGER NOT NULL,
                fecha_lectura TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(usuario, mensaje_id),
                FOREIGN KEY (usuario) REFERENCES usuarios(username),
                FOREIGN KEY (mensaje_id) REFERENCES mensajes_grupo(id)
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlBloqueos);
            stmt.execute(sqlEstadisticas);
            stmt.execute(sqlHistorialPartidas);
            stmt.execute(sqlGrupos);
            stmt.execute(sqlMiembrosGrupo);
            stmt.execute(sqlMensajesGrupo);
            stmt.execute(sqlMensajesLeidos);
            System.out.println("Tablas creadas/verificadas correctamente");
            
            crearGrupoTodos();
        } catch (SQLException e) {
            System.err.println("Error al crear tablas: " + e.getMessage());
        }
    }
    
    private void crearGrupoTodos() {
        String sqlVerificar = "SELECT id FROM grupos WHERE nombre = 'Todos'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sqlVerificar)) {
            
            if (!rs.next()) {
                String sqlCrear = "INSERT INTO grupos (nombre, creador, es_sistema) VALUES ('Todos', 'sistema', 1)";
                stmt.execute(sqlCrear);
                System.out.println("Grupo 'Todos' creado");
            }
        } catch (SQLException e) {
            System.err.println("Error al crear grupo Todos: " + e.getMessage());
        }
    }
    
    public boolean registrarUsuario(String username, String password) {
        String sql = "INSERT INTO usuarios (username, password) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            
            inicializarEstadisticas(username);
            agregarUsuarioAGrupoTodos(username);
            System.out.println("[DB] Usuario registrado: " + username);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    private void agregarUsuarioAGrupoTodos(String username) {
        String sql = "INSERT INTO miembros_grupo (grupo_id, username) SELECT id, ? FROM grupos WHERE nombre = 'Todos'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al agregar usuario a grupo Todos: " + e.getMessage());
        }
    }
    
    private void inicializarEstadisticas(String username) {
        String sql = "INSERT INTO estadisticas_gato (username, victorias, empates, derrotas, puntos) VALUES (?, 0, 0, 0, 0)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
            System.out.println("[DB] Estadisticas inicializadas para: " + username);
        } catch (SQLException e) {
            System.err.println("Error al inicializar estadisticas: " + e.getMessage());
            e.printStackTrace();
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
        System.out.println("\n========================================");
        System.out.println("[DB] REGISTRANDO PARTIDA");
        System.out.println("[DB] Jugador 1: " + jugador1);
        System.out.println("[DB] Jugador 2: " + jugador2);
        System.out.println("[DB] Ganador: " + ganador);
        System.out.println("[DB] Empate: " + empate);
        System.out.println("========================================");
        
        String sqlHistorial = "INSERT INTO historial_partidas (jugador1, jugador2, ganador, empate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlHistorial)) {
            pstmt.setString(1, jugador1);
            pstmt.setString(2, jugador2);
            pstmt.setString(3, ganador);
            pstmt.setInt(4, empate ? 1 : 0);
            pstmt.executeUpdate();
            System.out.println("[DB] Partida guardada en historial");
        } catch (SQLException e) {
            System.err.println("Error al registrar partida en historial: " + e.getMessage());
            e.printStackTrace();
        }
        
        if (empate) {
            System.out.println("[DB] Actualizando empate para ambos jugadores");
            actualizarEstadisticas(jugador1, "empate");
            actualizarEstadisticas(jugador2, "empate");
        } else if (ganador != null) {
            System.out.println("[DB] Actualizando victoria y derrota");
            actualizarEstadisticas(ganador, "victoria");
            String perdedor = ganador.equals(jugador1) ? jugador2 : jugador1;
            actualizarEstadisticas(perdedor, "derrota");
        }
        
        System.out.println("\n[DB] VERIFICACION POST-PARTIDA:");
        verificarEstadisticas(jugador1);
        verificarEstadisticas(jugador2);
        System.out.println("========================================\n");
    }
    
    private void actualizarEstadisticas(String username, String resultado) {
        System.out.println("[DB] Actualizando " + resultado + " para: " + username);
        
        String sqlVerificar = "SELECT COUNT(*) as existe FROM estadisticas_gato WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlVerificar)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt("existe") == 0) {
                System.err.println("[DB] ERROR: No existen estadisticas para " + username);
                System.err.println("[DB] Creando estadisticas para " + username);
                inicializarEstadisticas(username);
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar estadisticas: " + e.getMessage());
        }
        
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
            int rows = pstmt.executeUpdate();
            System.out.println("[DB] Filas actualizadas: " + rows);
            
            if (rows == 0) {
                System.err.println("[DB] ADVERTENCIA: No se actualizo ninguna fila para " + username);
            } else {
                System.out.println("[DB] Actualizado correctamente: " + username);
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar estadisticas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void verificarEstadisticas(String username) {
        String sql = "SELECT * FROM estadisticas_gato WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("[DB] Stats de " + username + ": " +
                    "V=" + rs.getInt("victorias") + " " +
                    "E=" + rs.getInt("empates") + " " +
                    "D=" + rs.getInt("derrotas") + " " +
                    "Pts=" + rs.getInt("puntos"));
            } else {
                System.err.println("[DB] ERROR: No existen estadisticas para " + username);
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar estadisticas: " + e.getMessage());
        }
    }
    
    public List<String> obtenerRankingGeneral() {
        System.out.println("\n[DB] Obteniendo ranking general...");
        List<String> ranking = new ArrayList<>();
        String sql = """
            SELECT username, victorias, empates, derrotas, puntos
            FROM estadisticas_gato
            ORDER BY puntos DESC, victorias DESC, empates DESC
        """;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int posicion = 1;
            int totalEncontrados = 0;
            while (rs.next()) {
                totalEncontrados++;
                String username = rs.getString("username");
                int victorias = rs.getInt("victorias");
                int empates = rs.getInt("empates");
                int derrotas = rs.getInt("derrotas");
                int puntos = rs.getInt("puntos");
                int totalPartidas = victorias + empates + derrotas;
                
                System.out.println("[DB] Usuario encontrado: " + username + 
                    " V:" + victorias + " E:" + empates + " D:" + derrotas + 
                    " Pts:" + puntos + " Total:" + totalPartidas);
                
                if (totalPartidas > 0) {
                    String linea = String.format("%d. %s - Puntos: %d | V:%d E:%d D:%d | Total: %d",
                        posicion, username, puntos, victorias, empates, derrotas, totalPartidas);
                    ranking.add(linea);
                    posicion++;
                    
                    if (posicion > 20) break;
                }
            }
            System.out.println("[DB] Total usuarios encontrados en DB: " + totalEncontrados);
            System.out.println("[DB] Total usuarios con partidas: " + (posicion - 1));
        } catch (SQLException e) {
            System.err.println("Error al obtener ranking: " + e.getMessage());
            e.printStackTrace();
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
    
    public boolean crearGrupo(String nombreGrupo, String creador) {
        if (nombreGrupo.equalsIgnoreCase("Todos")) {
            return false;
        }
        
        String sql = "INSERT INTO grupos (nombre, creador, es_sistema) VALUES (?, ?, 0)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombreGrupo);
            pstmt.setString(2, creador);
            pstmt.executeUpdate();
            
            unirseAGrupo(nombreGrupo, creador);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public boolean borrarGrupo(String nombreGrupo, String solicitante) {
        if (nombreGrupo.equalsIgnoreCase("Todos")) {
            return false;
        }
        
        String sqlVerificar = "SELECT creador FROM grupos WHERE nombre = ? AND es_sistema = 0";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlVerificar)) {
            pstmt.setString(1, nombreGrupo);
            ResultSet rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                return false;
            }
            
            String creador = rs.getString("creador");
            if (!creador.equals(solicitante)) {
                return false;
            }
            
            String sqlGrupoId = "SELECT id FROM grupos WHERE nombre = ?";
            try (PreparedStatement pstmt2 = connection.prepareStatement(sqlGrupoId)) {
                pstmt2.setString(1, nombreGrupo);
                ResultSet rs2 = pstmt2.executeQuery();
                
                if (rs2.next()) {
                    int grupoId = rs2.getInt("id");
                    
                    String sqlBorrarMensajes = "DELETE FROM mensajes_grupo WHERE grupo_id = ?";
                    String sqlBorrarMiembros = "DELETE FROM miembros_grupo WHERE grupo_id = ?";
                    String sqlBorrarGrupo = "DELETE FROM grupos WHERE id = ?";
                    
                    try (PreparedStatement pstmt3 = connection.prepareStatement(sqlBorrarMensajes);
                         PreparedStatement pstmt4 = connection.prepareStatement(sqlBorrarMiembros);
                         PreparedStatement pstmt5 = connection.prepareStatement(sqlBorrarGrupo)) {
                        
                        pstmt3.setInt(1, grupoId);
                        pstmt3.executeUpdate();
                        
                        pstmt4.setInt(1, grupoId);
                        pstmt4.executeUpdate();
                        
                        pstmt5.setInt(1, grupoId);
                        pstmt5.executeUpdate();
                        
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al borrar grupo: " + e.getMessage());
        }
        return false;
    }
    
    public boolean unirseAGrupo(String nombreGrupo, String username) {
        String sqlGrupoId = "SELECT id FROM grupos WHERE nombre = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlGrupoId)) {
            pstmt.setString(1, nombreGrupo);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int grupoId = rs.getInt("id");
                
                String sqlUnirse = "INSERT INTO miembros_grupo (grupo_id, username) VALUES (?, ?)";
                try (PreparedStatement pstmt2 = connection.prepareStatement(sqlUnirse)) {
                    pstmt2.setInt(1, grupoId);
                    pstmt2.setString(2, username);
                    pstmt2.executeUpdate();
                    return true;
                } catch (SQLException e) {
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al unirse a grupo: " + e.getMessage());
        }
        return false;
    }
    
    public boolean salirDeGrupo(String nombreGrupo, String username) {
        if (nombreGrupo.equalsIgnoreCase("Todos")) {
            return false;
        }
        
        String sqlGrupoId = "SELECT id FROM grupos WHERE nombre = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlGrupoId)) {
            pstmt.setString(1, nombreGrupo);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int grupoId = rs.getInt("id");
                
                String sqlSalir = "DELETE FROM miembros_grupo WHERE grupo_id = ? AND username = ?";
                try (PreparedStatement pstmt2 = connection.prepareStatement(sqlSalir)) {
                    pstmt2.setInt(1, grupoId);
                    pstmt2.setString(2, username);
                    int rows = pstmt2.executeUpdate();
                    return rows > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al salir de grupo: " + e.getMessage());
        }
        return false;
    }
    
    public boolean existeGrupo(String nombreGrupo) {
        String sql = "SELECT 1 FROM grupos WHERE nombre = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombreGrupo);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error al verificar grupo: " + e.getMessage());
        }
        return false;
    }
    
    public boolean esMiembroDeGrupo(String nombreGrupo, String username) {
        String sql = """
            SELECT 1 FROM miembros_grupo mg
            JOIN grupos g ON mg.grupo_id = g.id
            WHERE g.nombre = ? AND mg.username = ?
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombreGrupo);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error al verificar membresia: " + e.getMessage());
        }
        return false;
    }
    
    public List<String> listarGrupos() {
        List<String> grupos = new ArrayList<>();
        String sql = "SELECT nombre FROM grupos ORDER BY es_sistema DESC, nombre";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                grupos.add(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar grupos: " + e.getMessage());
        }
        return grupos;
    }
    
    public List<String> listarGruposDeUsuario(String username) {
        List<String> grupos = new ArrayList<>();
        String sql = """
            SELECT g.nombre FROM grupos g
            JOIN miembros_grupo mg ON g.id = mg.grupo_id
            WHERE mg.username = ?
            ORDER BY g.es_sistema DESC, g.nombre
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                grupos.add(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar grupos de usuario: " + e.getMessage());
        }
        return grupos;
    }
    
    public List<String> listarMiembrosDeGrupo(String nombreGrupo) {
        List<String> miembros = new ArrayList<>();
        String sql = """
            SELECT mg.username FROM miembros_grupo mg
            JOIN grupos g ON mg.grupo_id = g.id
            WHERE g.nombre = ?
            ORDER BY mg.username
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombreGrupo);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                miembros.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar miembros: " + e.getMessage());
        }
        return miembros;
    }
    
    public boolean guardarMensajeGrupo(String nombreGrupo, String remitente, String mensaje) {
        String sqlGrupoId = "SELECT id FROM grupos WHERE nombre = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlGrupoId)) {
            pstmt.setString(1, nombreGrupo);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int grupoId = rs.getInt("id");
                
                String sqlInsert = "INSERT INTO mensajes_grupo (grupo_id, remitente, mensaje) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt2 = connection.prepareStatement(sqlInsert)) {
                    pstmt2.setInt(1, grupoId);
                    pstmt2.setString(2, remitente);
                    pstmt2.setString(3, mensaje);
                    pstmt2.executeUpdate();
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al guardar mensaje: " + e.getMessage());
        }
        return false;
    }
    
    public List<String> obtenerMensajesNoLeidosDeGrupo(String nombreGrupo, String username) {
        List<String> mensajes = new ArrayList<>();
        String sql = """
            SELECT mg.id, mg.remitente, mg.mensaje, mg.fecha_envio
            FROM mensajes_grupo mg
            JOIN grupos g ON mg.grupo_id = g.id
            WHERE g.nombre = ?
            AND mg.id NOT IN (
                SELECT mensaje_id FROM mensajes_leidos WHERE usuario = ?
            )
            ORDER BY mg.fecha_envio
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombreGrupo);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int mensajeId = rs.getInt("id");
                String remitente = rs.getString("remitente");
                String mensaje = rs.getString("mensaje");
                
                mensajes.add("[" + remitente + "]: " + mensaje);
                
                marcarMensajeComoLeido(username, mensajeId);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener mensajes no leidos: " + e.getMessage());
        }
        
        return mensajes;
    }
    
    private void marcarMensajeComoLeido(String username, int mensajeId) {
        String sql = "INSERT INTO mensajes_leidos (usuario, mensaje_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setInt(2, mensajeId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
        }
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