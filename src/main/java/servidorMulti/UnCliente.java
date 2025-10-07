package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class UnCliente implements Runnable {
    final DataOutputStream salida;
    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    final DataInputStream entrada;
    private final int miId;

    UnCliente(Socket s, int id) throws IOException {
        this.miId = id;
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        while (true) {
            try {
                String mensaje = entrada.readUTF();
                if (mensaje.startsWith("@")) {
                    String[] partes = mensaje.split(" ", 2);
                    String aQuien = partes[0].substring(1);
                    
                    UnCliente cliente = ServidorMulti.clientes.get(aQuien);
                    
                    if (cliente != null) {
                        cliente.salida.writeUTF("[De #" + miId + "]: " + mensaje);
                        cliente.salida.flush();
                    } else {
                        salida.writeUTF("Cliente #" + aQuien + " no existe");
                        salida.flush();
                    }
                    continue;
                }

                for (UnCliente cliente : ServidorMulti.clientes.values()) {
                    if (cliente.miId != this.miId) {
                        cliente.salida.writeUTF("[Cliente #" + miId + "]: " + mensaje);
                        cliente.salida.flush();
                    }
                }

            } catch (IOException ex) {
                System.out.println("Cliente #" + miId + " desconectado");
                ServidorMulti.clientes.remove(Integer.toString(miId));
                break;
            }
        }
    }
}