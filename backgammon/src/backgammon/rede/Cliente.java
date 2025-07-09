package backgammon.rede;

import java.io.*;
import java.net.Socket;

public class Cliente {
    public interface MessageListener {
        void onMessageReceived(String message);
    }

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile MessageListener messageListener;

    public Cliente() {
    }

    public void conectar(String ip, int porta) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, porta);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Conectado ao servidor " + ip + ":" + porta);

                String linha;
                while ((linha = in.readLine()) != null) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Cliente recebeu: " + linha + " (Socket conectado: " + (socket != null && socket.isConnected() && !socket.isClosed()) + ")");
                    synchronized (this) {
                        if (messageListener != null) {
                            messageListener.onMessageReceived(linha);
                        } else {
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Aviso: Nenhum MessageListener definido para mensagem: " + linha);
                        }
                    }
                }
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Conexão com o servidor encerrada: Fim do fluxo de entrada.");
                synchronized (this) {
                    if (messageListener != null) {
                        messageListener.onMessageReceived("Erro ao conectar ao servidor: Conexão encerrada.");
                    }
                }
            } catch (IOException e) {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Erro ao conectar ou comunicar com o servidor: " + e.getMessage());
                synchronized (this) {
                    if (messageListener != null) {
                        messageListener.onMessageReceived("Erro ao conectar ao servidor: " + e.getMessage());
                    }
                }
                try {
                    desconectar();
                } catch (Exception ex) {
                    System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Erro ao desconectar durante falha: " + ex.getMessage());
                }
            }
        }).start();
    }

    public void setMessageListener(MessageListener listener) {
        synchronized (this) {
            this.messageListener = listener;
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] MessageListener definido: " + (listener != null ? listener.getClass().getSimpleName() : "null"));
        }
    }

    public void removeMessageListener() {
        synchronized (this) {
            this.messageListener = null;
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] MessageListener removido.");
        }
    }

    public void enviarPronto(String nome) {
        if (isConectado() && out != null && nome != null && !nome.trim().isEmpty()) {
            String mensagem = "PRONTO:" + nome.trim();
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Enviando: " + mensagem);
            out.println(mensagem);
            out.flush();
        } else {
            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Erro: Não foi possível enviar PRONTO - Conectado: " + isConectado() + ", out: " + out + ", nome: '" + nome + "'");
            synchronized (this) {
                if (messageListener != null) {
                    messageListener.onMessageReceived("Erro ao enviar PRONTO: Cliente não conectado ou nome inválido.");
                }
            }
        }
    }

    public void enviarMensagemChat(String mensagem) {
        if (isConectado() && out != null && mensagem != null && !mensagem.trim().isEmpty()) {
            String comando = "MSG_CHAT:" + mensagem.trim();
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Enviando: " + comando);
            out.println(comando);
            out.flush();
        } else {
            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Erro: Não foi possível enviar mensagem de chat - Conectado: " + isConectado() + ", out: " + out + ", mensagem: '" + mensagem + "'");
            synchronized (this) {
                if (messageListener != null) {
                    messageListener.onMessageReceived("Erro ao enviar mensagem de chat: Cliente não conectado ou mensagem inválida.");
                }
            }
        }
    }

    public void enviarSorteioInicial(int dado1, int dado2) {
        if (isConectado() && out != null && dado1 >= 1 && dado1 <= 6 && dado2 >= 1 && dado2 <= 6) {
            String mensagem = "SORTEIO_INICIAL:" + dado1 + "," + dado2;
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Enviando: " + mensagem);
            out.println(mensagem);
            out.flush();
        } else {
            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Erro: Não foi possível enviar SORTEIO_INICIAL - Conectado: " + isConectado() + ", out: " + out + ", dados: " + dado1 + "," + dado2);
            synchronized (this) {
                if (messageListener != null) {
                    messageListener.onMessageReceived("Erro ao enviar SORTEIO_INICIAL: Cliente não conectado ou dados inválidos.");
                }
            }
        }
    }

    public void enviarComando(String comando) {
        if (isConectado() && out != null && comando != null && !comando.trim().isEmpty()) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Enviando comando: " + comando);
            out.println(comando);
            out.flush();
        } else {
            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Erro: Não foi possível enviar comando - Conectado: " + isConectado() + ", out: " + out + ", comando: '" + comando + "'");
            synchronized (this) {
                if (messageListener != null) {
                    messageListener.onMessageReceived("Erro ao enviar comando: Cliente não conectado ou comando inválido.");
                }
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isConectado() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    public void desconectar() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Cliente desconectado com sucesso.");
            }
        } catch (IOException e) {
            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "] Erro ao desconectar: " + e.getMessage());
            synchronized (this) {
                if (messageListener != null) {
                    messageListener.onMessageReceived("Erro ao desconectar: " + e.getMessage());
                }
            }
        }
    }
}