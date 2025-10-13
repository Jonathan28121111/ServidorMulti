package clientemulti;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ParaRecibir implements Runnable {
    private final DataInputStream entrada;
    private final Socket socket;
    
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
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.out.println("Conexion cerrada.");
            }
        } finally {
            try {
                entrada.close();
            } catch (IOException ignored) {}
        }
    }
}