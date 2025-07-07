package backgammon.rede;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import backgammon.modelo.JogoBackgammon;

public class Servidor {
    private final int porta;
    private ServerSocket socket;
    private final List<ClienteInfo> clientes = Collections.synchronizedList(new ArrayList<>());
    private final java.util.function.Consumer<String> callback;
    private JogoBackgammon jogo;
    private final Map<String, Integer> sorteioInicial = new ConcurrentHashMap<>();
    private final Map<String, int[]> dadosSorteio = new ConcurrentHashMap<>();
    private final AtomicBoolean jogoJaComecou = new AtomicBoolean(false);
    private final AtomicBoolean sortitionPhase = new AtomicBoolean(false);
    private final Map<String, Long> sortitionStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> lastConnectionTimes = new ConcurrentHashMap<>();
    private static final long MIN_CONNECTION_INTERVAL = 5_000; // 5 seconds
    private static final long SORTEIO_TIMEOUT = 0_000; // 60 seconds for sortition

    public Servidor(int porta, java.util.function.Consumer<String> callback) {
        this.porta = porta;
        this.callback = callback;
    }

    public String getIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao obter endereço IP: " + e.getMessage());
            return "127.0.0.1"; // Fallback to localhost
        }
    }

    public void start() {
        new Thread(() -> {
            try {
                socket = new ServerSocket(porta);
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Servidor iniciado na porta: " + porta);
                new Thread(() -> {
                    while (!socket.isClosed()) {
                        synchronized (clientes) {
                            Iterator<ClienteInfo> iterator = clientes.iterator();
                            while (iterator.hasNext()) {
                                ClienteInfo cliente = iterator.next();
                                if (cliente.nome == null && System.currentTimeMillis() - cliente.connectionTime > 30_000) {
                                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Removendo cliente inativo: " + cliente.socket.getInetAddress());
                                    handleClientDisconnect(cliente);
                                } else if (sortitionPhase.get() && sortitionStartTimes.containsKey(cliente.nome) &&
                                        System.currentTimeMillis() - sortitionStartTimes.get(cliente.nome) > SORTEIO_TIMEOUT) {
                                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Timeout: " + cliente.nome + " não enviou SORTEIO_INICIAL. Enviando lembrete.");
                                    try {
                                        if (!cliente.socket.isClosed()) {
                                            cliente.out.println("SORTEIO_VEZ");
                                            cliente.out.flush();
                                            sortitionStartTimes.put(cliente.nome, System.currentTimeMillis()); // Reset timeout
                                        } else {
                                            handleClientDisconnect(cliente);
                                        }
                                    } catch (Exception e) {
                                        System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao enviar lembrete para " + cliente.nome + ": " + e.getMessage());
                                        handleClientDisconnect(cliente);
                                    }
                                }
                            }
                        }
                        try {
                            Thread.sleep(5_000);
                        } catch (InterruptedException e) {
                            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro no thread de limpeza: " + e.getMessage());
                        }
                    }
                }).start();
                while (true) {
                    synchronized (clientes) {
                        if (clientes.size() >= 2 && allPlayersReady()) {
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Jogo já em andamento. Rejeitando nova conexão.");
                            Socket tempSocket = socket.accept();
                            PrintWriter tempOut = new PrintWriter(tempSocket.getOutputStream(), true);
                            tempOut.println("SERVIDOR_LOTADO:Tente novamente mais tarde.");
                            tempOut.flush();
                            tempSocket.close();
                            continue;
                        }
                    }
                    Socket clientSocket = socket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    Long lastConnection = lastConnectionTimes.get(clientIp);
                    if (lastConnection != null && System.currentTimeMillis() - lastConnection < MIN_CONNECTION_INTERVAL) {
                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Conexão muito rápida de " + clientIp + ". Rejeitando.");
                        PrintWriter tempOut = new PrintWriter(clientSocket.getOutputStream(), true);
                        tempOut.println("ERRO_CONEXAO:Tente novamente em alguns segundos.");
                        tempOut.flush();
                        clientSocket.close();
                        continue;
                    }
                    lastConnectionTimes.put(clientIp, System.currentTimeMillis());
                    ClienteInfo cliente = new ClienteInfo(clientSocket);
                    synchronized (clientes) {
                        clientes.add(cliente);
                    }
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Cliente conectado: " + clientSocket.getInetAddress());
                    new Thread(() -> listenClient(cliente)).start();
                }
            } catch (IOException e) {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao iniciar servidor: " + e.getMessage());
            }
        }).start();
    }

    private void listenClient(ClienteInfo cliente) {
        try {
            String line;
            while ((line = cliente.in.readLine()) != null) {
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Mensagem bruta recebida de " + (cliente.nome != null ? cliente.nome : cliente.socket.getInetAddress()) + ": " + line);
                if (cliente.socket.isClosed()) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Cliente desconectado antes de enviar PRONTO: " + cliente.socket.getInetAddress());
                    handleClientDisconnect(cliente);
                    return;
                }
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Recebido do cliente " + (cliente.nome != null ? cliente.nome : cliente.socket.getInetAddress()) + ": " + line);

                if (line.startsWith("PRONTO:")) {
                    if (cliente.nome != null) {
                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Ignorando PRONTO repetido de " + cliente.nome);
                        cliente.out.println("ERRO_NOME:Nome já definido.");
                        cliente.out.flush();
                        continue;
                    }
                    String nome = line.substring(7).trim();
                    if (nome.isEmpty() || !nome.matches("[a-zA-Z0-9]+")) {
                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Nome inválido recebido: " + nome);
                        cliente.out.println("ERRO_NOME:Nome inválido. Use apenas letras e números.");
                        cliente.out.flush();
                        handleClientDisconnect(cliente);
                        return;
                    }
                    synchronized (clientes) {
                        if (clientes.stream().anyMatch(c -> c.nome != null && c.nome.equals(nome))) {
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Nome duplicado recebido: " + nome);
                            cliente.out.println("ERRO_NOME:Nome já em uso. Escolha outro.");
                            cliente.out.flush();
                            handleClientDisconnect(cliente);
                            return;
                        }
                        cliente.nome = nome;
                    }
                    if (callback != null) {
                        callback.accept(cliente.nome);
                    }
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Jogador conectado: " + cliente.nome);

                    if (allPlayersReady()) {
                        startGame();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro com cliente " + (cliente.nome != null ? cliente.nome : cliente.socket.getInetAddress()) + ": " + e.getMessage());
            handleClientDisconnect(cliente);
        }
    }

    private void startReadingCommands() {
        for (ClienteInfo cliente : new ArrayList<>(clientes)) {
            ClienteInfo finalCliente = cliente;
            new Thread(() -> {
                try {
                    String comando;
                    while ((comando = finalCliente.in.readLine()) != null) {
                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Mensagem bruta recebida de " + finalCliente.nome + ": " + comando);
                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Comando recebido de " + finalCliente.nome + ": " + comando);

                        if (comando.equals("LANCAR")) {
                            if (jogo == null) return;

                            if (sortitionPhase.get() && !jogoJaComecou.get()) {
                                // Ignora aqui, porque o sorteio inicial é tratado noutro bloco
                                finalCliente.out.println("COMANDO_INVALIDO:Use SORTEIO_INICIAL no sorteio.");
                                finalCliente.out.flush();
                                return;
                            }

                            if (jogoJaComecou.get() && finalCliente.nome.equals(jogo.getJogadorAtual().getNome())) {
                                if (!jogo.getJogadorAtual().getDadosDisponiveis().isEmpty()) {
                                    finalCliente.out.println("COMANDO_INVALIDO:Já lançaste os dados neste turno.");
                                    finalCliente.out.flush();
                                    return;
                                }

                                jogo.lancarDados();
                                sendGameStateToAll();
                            } else {
                                finalCliente.out.println("COMANDO_INVALIDO:Não é a tua vez.");
                                finalCliente.out.flush();
                            }
                        } else if (comando.equals("PASSAR_TURNO")) {
                            if (jogo != null && jogoJaComecou.get() && finalCliente.nome.equals(jogo.getJogadorAtual().getNome())) {
                                jogo.verificarEFinalizarTurno();
                                sendGameStateToAll();
                            } else {
                                finalCliente.out.println("Não é a sua vez.");
                                finalCliente.out.flush();
                            }
                        } else if (comando.startsWith("MOVER")) {
                            String[] partes = comando.split(" ");
                            if (partes.length == 3) {
                                try {
                                    int origem = Integer.parseInt(partes[1]);
                                    int destino = Integer.parseInt(partes[2]);
                                    if (jogoJaComecou.get() && finalCliente.nome.equals(jogo.getJogadorAtual().getNome())) {
                                        boolean sucesso = jogo.moverPeca(origem, destino);
                                        if (sucesso) {
                                            String jogadaMsg = "JOGADA:" + origem + "," + destino;
                                            for (ClienteInfo c : new ArrayList<>(clientes)) {
                                                if (!c.socket.isClosed()) {
                                                    c.out.println(jogadaMsg);
                                                    c.out.flush();
                                                }
                                            }
                                            if (jogo.getJogadorAtual().getDadosDisponiveis().isEmpty()) {
                                                jogo.verificarEFinalizarTurno();;
                                            }
                                            sendGameStateToAll();
                                        } else {
                                            finalCliente.out.println("MOVIMENTO_INVALIDO");
                                            finalCliente.out.flush();
                                        }
                                    } else {
                                        finalCliente.out.println("Não é a sua vez.");
                                        finalCliente.out.flush();
                                    }
                                } catch (NumberFormatException e) {
                                    finalCliente.out.println("COMANDO_INVALIDO");
                                    finalCliente.out.flush();
                                }
                            } else {
                                finalCliente.out.println("COMANDO_INVALIDO");
                                finalCliente.out.flush();
                            }
                        } else if (comando.startsWith("SORTEIO_INICIAL:") && sortitionPhase.get() && !jogoJaComecou.get()) {
                            String[] partes = comando.split(":");
                            if (partes.length == 2) {
                                try {
                                    String[] valores = partes[1].split(",");
                                    int dado1 = Integer.parseInt(valores[0]);
                                    int dado2 = Integer.parseInt(valores[1]);
                                    if (dado1 < 1 || dado1 > 6 || dado2 < 1 || dado2 > 6) {
                                        finalCliente.out.println("SORTEIO_INVALIDO:Valores de dados inválidos.");
                                        finalCliente.out.flush();
                                        continue;
                                    }
                                    int soma = dado1 + dado2;

                                    System.out.println("=======================================");
                                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] SORTEIO de " + finalCliente.nome + ": D1=" + dado1 + ", D2=" + dado2 + " (Total=" + soma + ")");
                                    System.out.println("=======================================");

                                    synchronized (sorteioInicial) {
                                        if (!sorteioInicial.containsKey(finalCliente.nome)) {
                                            sorteioInicial.put(finalCliente.nome, soma);
                                            dadosSorteio.put(finalCliente.nome, new int[]{dado1, dado2});
                                            sortitionStartTimes.remove(finalCliente.nome);
                                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Sorteio registrado para " + finalCliente.nome + ": " + soma);
                                        } else {
                                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Ignorado SORTEIO_INICIAL de " + finalCliente.nome + " (duplicado)");
                                            finalCliente.out.println("SORTEIO_INVALIDO:Já enviou um sorteio. Aguardando adversário.");
                                            finalCliente.out.flush();
                                            continue;
                                        }

                                        finalCliente.out.println("SORTEIO_CONFIRMADO:" + dado1 + "," + dado2);
                                        finalCliente.out.flush();

                                        String tipo = finalCliente.nome.equals(clientes.get(0).nome) ? "BRANCO" : "VERMELHO";
                                        for (ClienteInfo c : new ArrayList<>(clientes)) {
                                            if (!c.socket.isClosed()) {
                                                c.out.println("DADOS_SORTEIO:" + dado1 + "," + dado2 + ":" + tipo);
                                                c.out.flush();
                                            }
                                        }

                                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Sorteios recebidos: " + sorteioInicial.size() + "/2");
                                        if (sorteioInicial.size() == 2) {
                                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Ambos os jogadores enviaram dados. Decidindo jogador inicial...");
                                            sortitionPhase.set(false);
                                            sortitionStartTimes.clear();
                                            decideInitialPlayer();
                                        }
                                    }
                                } catch (Exception e) {
                                    System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao processar SORTEIO_INICIAL de " + finalCliente.nome + ": " + e.getMessage());
                                    finalCliente.out.println("SORTEIO_INVALIDO:Erro ao processar sorteio.");
                                    finalCliente.out.flush();
                                }
                            }
                        } else if (comando.startsWith("MSG_CHAT:")) {
                            String msg = comando.substring("MSG_CHAT:".length()).trim();
                            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Mensagem de chat de " + finalCliente.nome + ": " + msg);

                            for (ClienteInfo c : new ArrayList<>(clientes)) {
                                if (!c.socket.isClosed()) {
                                    if (c.nome.equals(finalCliente.nome)) {
                                        c.out.println("CHAT_ENVIADO:" + msg);
                                    } else {
                                        c.out.println("CHAT:" + finalCliente.nome + ": " + msg);
                                    }
                                    c.out.flush();
                                }
                            }
                        } else {
                            finalCliente.out.println("COMANDO_DESCONHECIDO");
                            finalCliente.out.flush();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao ler comandos do cliente " + finalCliente.nome + ": " + e.getMessage());
                    handleClientDisconnect(finalCliente);
                }
            }).start();
        }
    }

    private boolean allPlayersReady() {
        synchronized (clientes) {
            if (clientes.size() != 2) {
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Aguardando dois jogadores. Atual: " + clientes.size());
                return false;
            }
            Set<String> nomes = new HashSet<>();
            for (ClienteInfo c : clientes) {
                if (c.nome == null) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Aguardando nome do cliente: " + c.socket.getInetAddress());
                    return false;
                }
                if (c.socket.isClosed()) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Cliente desconectado: " + c.socket.getInetAddress());
                    return false;
                }
                if (!nomes.add(c.nome)) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Nome duplicado detectado: " + c.nome);
                    c.out.println("ERRO_NOME:Escolha um nome diferente.");
                    c.out.flush();
                    handleClientDisconnect(c);
                    return false;
                }
            }
            return true;
        }
    }

    private void startGame() {
        synchronized (clientes) {
            if (clientes.size() != 2) {
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Não há jogadores suficientes para iniciar o jogo.");
                return;
            }
            jogo = new JogoBackgammon();
            ClienteInfo branco = clientes.get(0);
            ClienteInfo vermelho = clientes.get(1);

            if (branco.socket.isClosed() || vermelho.socket.isClosed()) {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Um ou ambos os clientes desconectados antes de iniciar o jogo.");
                handleClientDisconnect(branco.socket.isClosed() ? branco : vermelho);
                return;
            }

            jogo.inicializarJogo(branco.nome, vermelho.nome);
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Iniciando jogo: BRANCO=" + branco.nome + ", VERMELHO=" + vermelho.nome);

            try {
                sortitionPhase.set(true);
                long currentTime = System.currentTimeMillis();
                sortitionStartTimes.put(branco.nome, currentTime);
                sortitionStartTimes.put(vermelho.nome, currentTime);

                if (!branco.socket.isClosed()) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Enviando COR:BRANCO e SORTEIO_VEZ para " + branco.nome);
                    branco.out.println("COR:BRANCO");
                    branco.out.println("SORTEIO_VEZ");
                    branco.out.println("INICIAR_JOGO:" + branco.nome + "," + vermelho.nome);
                    branco.out.flush();
                } else {
                    System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Socket de " + branco.nome + " fechado ao enviar mensagens iniciais.");
                    handleClientDisconnect(branco);
                    return;
                }

                if (!vermelho.socket.isClosed()) {
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Enviando COR:VERMELHO e SORTEIO_VEZ para " + vermelho.nome);
                    vermelho.out.println("COR:VERMELHO");
                    vermelho.out.println("SORTEIO_VEZ");
                    vermelho.out.println("INICIAR_JOGO:" + branco.nome + "," + vermelho.nome);
                    vermelho.out.flush();
                } else {
                    System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Socket de " + vermelho.nome + " fechado ao enviar mensagens iniciais.");
                    handleClientDisconnect(vermelho);
                    return;
                }

                startReadingCommands();
            } catch (Exception e) {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao enviar mensagens iniciais: " + e.getMessage());
                sortitionPhase.set(false);
                sortitionStartTimes.clear();
                ClienteInfo disconnected = branco.nome.equals(jogo.getJogador1().getNome()) ? branco : vermelho;
                handleClientDisconnect(disconnected);
                synchronized (clientes) {
                    if (clientes.size() == 1) {
                        ClienteInfo remaining = clientes.get(0);
                        if (!remaining.socket.isClosed()) {
                            remaining.out.println("CHAT:Um jogador desconectou-se. Aguardando novo jogador.");
                            remaining.out.println("AGUARDANDO_JOGADOR");
                            remaining.out.flush();
                        }
                    }
                }
            }
        }
    }

    private void resetSorteio() {
        synchronized (sorteioInicial) {
            sorteioInicial.clear();
            dadosSorteio.clear();
            sortitionPhase.set(true);
            sortitionStartTimes.clear();
            synchronized (clientes) {
                if (clientes.size() == 2 && clientes.get(0).nome != null && clientes.get(1).nome != null) {
                    long currentTime = System.currentTimeMillis();
                    sortitionStartTimes.put(clientes.get(0).nome, currentTime);
                    sortitionStartTimes.put(clientes.get(1).nome, currentTime);
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Reiniciando sorteio para " + clientes.get(0).nome + " e " + clientes.get(1).nome);
                    try {
                        if (!clientes.get(0).socket.isClosed()) {
                            clientes.get(0).out.println("SORTEIO_VEZ");
                            clientes.get(0).out.println("CHAT:Empate no sorteio! Lançando novamente.");
                            clientes.get(0).out.flush();
                        } else {
                            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Socket de " + clientes.get(0).nome + " fechado ao reiniciar sorteio.");
                            handleClientDisconnect(clientes.get(0));
                            return;
                        }
                        if (!clientes.get(1).socket.isClosed()) {
                            clientes.get(1).out.println("SORTEIO_VEZ");
                            clientes.get(1).out.println("CHAT:Empate no sorteio! Lançando novamente.");
                            clientes.get(1).out.flush();
                        } else {
                            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Socket de " + clientes.get(1).nome + " fechado ao reiniciar sorteio.");
                            handleClientDisconnect(clientes.get(1));
                            return;
                        }
                    } catch (Exception e) {
                        System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao reiniciar sorteio: " + e.getMessage());
                        ClienteInfo disconnected = e.getMessage().contains(clientes.get(0).nome) ? clientes.get(0) : clientes.get(1);
                        handleClientDisconnect(disconnected);
                    }
                }
            }
        }
    }

    private void sendGameStateToAll() {
        StringBuilder dadosStr = new StringBuilder("DADOS:");
        List<Integer> ultimosDados = jogo.getJogadorAtual().getUltimosDados();
        if (!ultimosDados.isEmpty()) {
            for (Integer dado : ultimosDados) {
                dadosStr.append(dado).append(",");
            }
            dadosStr.deleteCharAt(dadosStr.length() - 1);
<<<<<<< HEAD
        } else {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Nenhum dado disponível para enviar no estado do jogo.");
            return;
        }

        for (ClienteInfo c : new ArrayList<>(clientes)) {
            try {
                if (!c.socket.isClosed()) {
                    if (c.nome.equals(jogo.getJogadorAtual().getNome())) {
=======
        }

        boolean semDados = jogo.getJogadorAtual().getDadosDisponiveis().isEmpty();
        boolean semMovimentos = jogo.getCamposComPecasMoveis().isEmpty();

        // Corrigir: garantir que o jogadorAtual do servidor está sempre sincronizado com o nome do jogador que deve jogar
        String nomeJogadorAtual = jogo.getJogadorAtual() != null ? jogo.getJogadorAtual().getNome() : null;

        for (ClienteInfo c : new ArrayList<>(clientes)) {
            try {
                if (!c.socket.isClosed()) {
                    // Garante que o comando SUA_VEZ é enviado para o jogador correto
                    if (nomeJogadorAtual != null && c.nome.equals(nomeJogadorAtual)) {
>>>>>>> 6df0c78 (ultimo commit)
                        c.out.println("SUA_VEZ");
                    } else {
                        c.out.println("VEZ_ADV");
                    }
                    c.out.println(dadosStr.toString());
                    c.out.flush();
<<<<<<< HEAD
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Enviado para " + c.nome + ": " + (c.nome.equals(jogo.getJogadorAtual().getNome()) ? "SUA_VEZ" : "VEZ_ADV") + ", " + dadosStr);
=======
                    System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Enviado para " + c.nome + ": " + (c.nome.equals(nomeJogadorAtual) ? "SUA_VEZ" : "VEZ_ADV") + ", " + dadosStr);
>>>>>>> 6df0c78 (ultimo commit)
                }
            } catch (Exception e) {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao enviar estado do jogo para " + c.nome + ": " + e.getMessage());
                handleClientDisconnect(c);
            }
        }
<<<<<<< HEAD
=======

        // Lógica para passar o turno automaticamente se não houver dados ou movimentos possíveis
        if ((semDados || semMovimentos) && jogoJaComecou.get()) {
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Passando turno automaticamente: sem dados ou sem movimentos possíveis.");
            jogo.verificarEFinalizarTurno();
            // Envia novo estado após passar turno
            sendGameStateToAll();
        }
>>>>>>> 6df0c78 (ultimo commit)
    }

    private void decideInitialPlayer() {
        String nomeBranco;
        String nomeVermelho;
        int valorBranco;
        int valorVermelho;
        int[] dadosBranco;
        int[] dadosVermelho;

        synchronized (sorteioInicial) {
            if (sorteioInicial.size() != 2) {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Não há dois sorteios disponíveis. Tamanho: " + sorteioInicial.size());
                resetSorteio();
                return;
            }

            nomeBranco = clientes.get(0).nome;
            nomeVermelho = clientes.get(1).nome;

            if (nomeBranco == null || nomeVermelho == null) {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Nomes dos jogadores não definidos. Branco: " + nomeBranco + ", Vermelho: " + nomeVermelho);
                resetSorteio();
                return;
            }

            valorBranco = sorteioInicial.getOrDefault(nomeBranco, 0);
            valorVermelho = sorteioInicial.getOrDefault(nomeVermelho, 0);
            dadosBranco = dadosSorteio.getOrDefault(nomeBranco, new int[]{0, 0});
            dadosVermelho = dadosSorteio.getOrDefault(nomeVermelho, new int[]{0, 0});

            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Resultado do sorteio:");
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] " + nomeBranco + " (branco): " + valorBranco + " (" + dadosBranco[0] + "," + dadosBranco[1] + ")");
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] " + nomeVermelho + " (vermelho): " + valorVermelho + " (" + dadosVermelho[0] + "," + dadosVermelho[1] + ")");

            if (valorBranco == 0 || valorVermelho == 0) {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Valores de sorteio inválidos. Branco: " + valorBranco + ", Vermelho: " + valorVermelho);
                for (ClienteInfo c : new ArrayList<>(clientes)) {
                    if (!c.socket.isClosed()) {
                        c.out.println("CHAT:Erro no sorteio. Tente novamente.");
                        c.out.println("EMPATE_SORTEIO");
                        c.out.flush();
                    }
                }
                resetSorteio();
                return;
            }

            if (valorBranco == valorVermelho) {
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Empate no sorteio! Repetindo lançamento...");
                for (ClienteInfo c : new ArrayList<>(clientes)) {
                    if (!c.socket.isClosed()) {
                        c.out.println("CHAT:Empate no sorteio! Dados de " + nomeBranco + ": " + valorBranco + " | " + nomeVermelho + ": " + valorVermelho);
                        c.out.println("EMPATE_SORTEIO");
                        c.out.flush();
                    }
                }
                resetSorteio();
                return;
            }

            try {
                String nomeVencedor = valorBranco > valorVermelho ? nomeBranco : nomeVermelho;
                int[] winnerDice = nomeVencedor.equals(nomeBranco) ? dadosBranco : dadosVermelho;
                String dadosStr = winnerDice[0] + "," + winnerDice[1];
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Jogador que começa: " + nomeVencedor);

                // Set initial player and update game state with winner's dice
                jogo.definirJogadorInicial(nomeVencedor);
<<<<<<< HEAD
=======

                // Corrigir: garantir que os dados do vencedor são aplicados ao jogadorAtual
                jogo.getJogadorAtual().getDadosDisponiveis().clear();
                jogo.getJogadorAtual().getUltimosDados().clear();
                jogo.getJogadorAtual().getDadosDisponiveis().add(winnerDice[0]);
                jogo.getJogadorAtual().getDadosDisponiveis().add(winnerDice[1]);
                jogo.getJogadorAtual().getUltimosDados().add(winnerDice[0]);
                jogo.getJogadorAtual().getUltimosDados().add(winnerDice[1]);

>>>>>>> 6df0c78 (ultimo commit)
                jogo.iniciarTurno();
                jogoJaComecou.set(true);
                sortitionPhase.set(false);
                sortitionStartTimes.clear();
                sorteioInicial.clear();
                dadosSorteio.clear();

<<<<<<< HEAD
=======
                // Corrigir: garantir que o jogadorAtual do servidor está correto antes de enviar comandos
                String nomeJogadorAtual = jogo.getJogadorAtual() != null ? jogo.getJogadorAtual().getNome() : null;

>>>>>>> 6df0c78 (ultimo commit)
                synchronized (clientes) {
                    for (ClienteInfo c : new ArrayList<>(clientes)) {
                        if (c.socket.isClosed()) {
                            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Cliente desconectado " + c.nome + " ao enviar resultado do sorteio.");
                            handleClientDisconnect(c);
                            return;
                        }
                        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Enviando mensagens de início do jogo para " + c.nome);
<<<<<<< HEAD
                        if (c.nome.equals(nomeVencedor)) {
=======
                        if (c.nome.equals(nomeJogadorAtual)) {
>>>>>>> 6df0c78 (ultimo commit)
                            c.out.println("VOCE_COMECA");
                            c.out.println("SUA_VEZ");
                        } else {
                            c.out.println("ADVERSARIO_COMECA");
                            c.out.println("VEZ_ADV");
                        }
                        c.out.println("DADOS:" + dadosStr);
                        c.out.flush();
                    }
                }
            } catch (Exception e) {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao enviar resultado do sorteio: " + e.getMessage());
                resetSorteio();
            }
        }
    }

    private void handleClientDisconnect(ClienteInfo cliente) {
        synchronized (clientes) {
            clientes.remove(cliente);
            try {
                if (!cliente.socket.isClosed()) {
                    cliente.out.println("CHAT:Você foi desconectado do servidor.");
                    cliente.out.flush();
                    cliente.socket.close();
                }
            } catch (IOException e) {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro ao fechar socket do cliente " + (cliente.nome != null ? cliente.nome : cliente.socket.getInetAddress()) + ": " + e.getMessage());
            }
            sortitionStartTimes.remove(cliente.nome);
            sorteioInicial.remove(cliente.nome);
            dadosSorteio.remove(cliente.nome);
            if (clientes.size() == 1 && jogoJaComecou.get()) {
                ClienteInfo remaining = clientes.get(0);
                if (!remaining.socket.isClosed()) {
                    remaining.out.println("CHAT:Um jogador desconectou-se. Aguardando novo jogador.");
                    remaining.out.println("AGUARDANDO_JOGADOR");
                    remaining.out.flush();
                }
                jogoJaComecou.set(false);
                sortitionPhase.set(false);
                sorteioInicial.clear();
                dadosSorteio.clear();
                sortitionStartTimes.clear();
                jogo = null;
            }
        }
    }

    private static class ClienteInfo {
        Socket socket;
        PrintWriter out;
        BufferedReader in;
        String nome;
        long connectionTime;

        ClienteInfo(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.connectionTime = System.currentTimeMillis();
        }
    }
}