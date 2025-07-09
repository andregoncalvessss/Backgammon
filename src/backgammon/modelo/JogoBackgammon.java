package backgammon.modelo;

import java.util.*;

public class JogoBackgammon {
    private Map<Integer, Campo> campos = new HashMap<>();
    private Jogador jogadorServidor;  // Você (Brancas)
    private Jogador jogadorCliente;   // Adversário (Vermelhas)
    private Jogador jogadorAtual;

    public JogoBackgammon() {
        for (int i = 1; i <= 24; i++) {
            campos.put(i, new Campo(i));
        }

        jogadorServidor = new Jogador("Você (Brancas)");
        jogadorCliente = new Jogador("Adversário (Vermelhas)");
        jogadorAtual = jogadorServidor;

        inicializarPosicoesIniciais();
    }

    private void inicializarPosicoesIniciais() {
        campos.values().forEach(campo -> campo.getPecas().clear());

        adicionarPecas(1, jogadorServidor, 2);
        adicionarPecas(12, jogadorServidor, 5);
        adicionarPecas(17, jogadorServidor, 3);
        adicionarPecas(19, jogadorServidor, 5);

        adicionarPecas(6, jogadorCliente, 5);
        adicionarPecas(8, jogadorCliente, 3);
        adicionarPecas(13, jogadorCliente, 5);
        adicionarPecas(24, jogadorCliente, 2);
    }

    public Campo getCampo(int id) {
        return campos.get(id);
    }

    public Map<Integer, Campo> getCampos() {
        return campos;
    }

    public Jogador getJogadorServidor() {
        return jogadorServidor;
    }

    public Jogador getJogadorCliente() {
        return jogadorCliente;
    }

    public Jogador getJogadorAtual() {
        return jogadorAtual;
    }

    public void alternarJogador() {
        jogadorAtual = (jogadorAtual == jogadorServidor) ? jogadorCliente : jogadorServidor;
        System.out.println("Turno: " + jogadorAtual.getNome());
    }

    public void lancarDados() {
        if (jogadorAtual != null) {
            Random random = new Random();
            int dado1 = random.nextInt(6) + 1;
            int dado2 = random.nextInt(6) + 1;

            jogadorAtual.getDadosDisponiveis().clear();
            if (dado1 == dado2) {
                // Duplo: quatro dados iguais
                for (int i = 0; i < 4; i++) jogadorAtual.getDadosDisponiveis().add(dado1);
            } else {
                jogadorAtual.getDadosDisponiveis().add(dado1);
                jogadorAtual.getDadosDisponiveis().add(dado2);
            }
            System.out.println("Dados lançados: " + jogadorAtual.getDadosDisponiveis());
        }
    }

    // --- BEARING OFF / LIVRAR PEÇAS ---
    public boolean podeLivrarPecas(Jogador jogador) {
        if (jogador.temPecasCapturadas()) return false;
        if (jogador == jogadorServidor) {
            // Brancas só nos campos 19 a 24
            for (int i = 1; i <= 18; i++) {
                if (campos.get(i).temPecasDoJogador(jogador)) return false;
            }
            return true;
        } else {
            // Vermelhas só nos campos 1 a 6
            for (int i = 7; i <= 24; i++) {
                if (campos.get(i).temPecasDoJogador(jogador)) return false;
            }
            return true;
        }
    }

    // Verifica se existem peças "atrás" na home board (para regra oficial do bearing off)
    private boolean existemPecasAtras(int campoOrigemId, Jogador jogador) {
        if (jogador == jogadorServidor) {
            // Para brancas: ver se há peças do jogador entre 19 e campoOrigemId-1
            for (int i = 19; i < campoOrigemId; i++) {
                if (campos.get(i).temPecasDoJogador(jogadorServidor)) {
                    return true;
                }
            }
        } else {
            // Para vermelhas: ver se há peças do jogador entre campoOrigemId+1 e 6
            for (int i = campoOrigemId + 1; i <= 6; i++) {
                if (campos.get(i).temPecasDoJogador(jogadorCliente)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean moverPeca(int campoOrigemId, int campoDestinoId) {
        System.out.println("Tentando mover de " + campoOrigemId + " para " + campoDestinoId);

        // --- LIVRAR PEÇAS (BEARING OFF) ---
        if (podeLivrarPecas(jogadorAtual)) {
            // Brancas
            if (jogadorAtual == jogadorServidor && campoOrigemId >= 19 && campoOrigemId <= 24 && campoDestinoId == 25) {
                int valorDadoNecessario = 25 - campoOrigemId;
                Campo origem = campos.get(campoOrigemId);
                if (origem == null || origem.getPecas().isEmpty()) return false;
                Peca peca = origem.getTopo();
                if (peca.getJogador() != jogadorAtual) return false;
                boolean existeAtras = existemPecasAtras(campoOrigemId, jogadorAtual);

                System.out.println("Brancas | Dados disponíveis: " + jogadorAtual.getDadosDisponiveis() +
                        " | Valor necessário: " + valorDadoNecessario + " | Existe peças atrás? " + existeAtras);

                for (int dado : jogadorAtual.getDadosDisponiveis()) {
                    System.out.println("Verificando dado: " + dado);
                    if (dado == valorDadoNecessario) {
                        origem.removerPeca(peca);
                        jogadorAtual.usarDado(valorDadoNecessario);
                        System.out.println("Peça branca livrada do campo " + campoOrigemId);
                        return true;
                    } else if (dado > valorDadoNecessario && !existeAtras) {
                        origem.removerPeca(peca);
                        jogadorAtual.usarDado(dado);
                        System.out.println("Peça branca livrada do campo " + campoOrigemId + " (dado maior)");
                        return true;
                    }
                }
                System.out.println("Não foi possível livrar peça (brancas)!");
                return false;
            }

            // Vermelhas
            if (jogadorAtual == jogadorCliente && campoOrigemId >= 1 && campoOrigemId <= 6 && campoDestinoId == 0) {
                int valorDadoNecessario = campoOrigemId;
                Campo origem = campos.get(campoOrigemId);
                if (origem == null || origem.getPecas().isEmpty()) return false;
                Peca peca = origem.getTopo();
                if (peca.getJogador() != jogadorAtual) return false;
                boolean existeAtras = existemPecasAtras(campoOrigemId, jogadorAtual);

                System.out.println("Vermelhas | Dados disponíveis: " + jogadorAtual.getDadosDisponiveis() +
                        " | Valor necessário: " + valorDadoNecessario + " | Existe peças atrás? " + existeAtras);

                for (int dado : jogadorAtual.getDadosDisponiveis()) {
                    System.out.println("Verificando dado: " + dado);
                    if (dado == valorDadoNecessario) {
                        origem.removerPeca(peca);
                        jogadorAtual.usarDado(valorDadoNecessario);
                        System.out.println("Peça vermelha livrada do campo " + campoOrigemId);
                        return true;
                    } else if (dado > valorDadoNecessario && !existeAtras) {
                        origem.removerPeca(peca);
                        jogadorAtual.usarDado(dado);
                        System.out.println("Peça vermelha livrada do campo " + campoOrigemId + " (dado maior)");
                        return true;
                    }
                }
                System.out.println("Não foi possível livrar peça (vermelhas)!");
                return false;
            }
        }

        // Movimento normal
        Campo destino = campos.get(campoDestinoId);
        if (destino == null) return false;

        // Caso tenha peças capturadas, só pode fazer reentrada
        if (jogadorAtual.temPecasCapturadas()) {
            List<Integer> entradasPossiveis = getCamposEntradaDisponiveis();
            if (!entradasPossiveis.contains(campoDestinoId)) return false;
            if (!podeMoverPara(destino)) return false;

            // Reintroduz a peça
            Peca pecaReentrada = jogadorAtual.getPecasCapturadas().get(0);
            destino.adicionarPeca(pecaReentrada);
            jogadorAtual.libertarPecaCapturada();

            int valorMovimento = jogadorAtual == jogadorServidor ? campoDestinoId : 25 - campoDestinoId;
            jogadorAtual.usarDado(valorMovimento);

            System.out.println("Peça reintroduzida no campo " + campoDestinoId);
            return true;
        }

        Campo origem = campos.get(campoOrigemId);
        if (origem == null || origem.getPecas().isEmpty()) return false;

        Peca pecaParaMover = origem.getTopo();
        if (pecaParaMover.getJogador() != jogadorAtual) return false;

        if (destino.temPecas() &&
                destino.getTopo().getJogador() != jogadorAtual &&
                destino.getPecas().size() > 1) return false;

        int valorMovimento = jogadorAtual == jogadorServidor
                ? campoDestinoId - campoOrigemId
                : campoOrigemId - campoDestinoId;

        if (!jogadorAtual.getDadosDisponiveis().contains(valorMovimento)) return false;

        // Captura se possível
        if (destino.temPecas() &&
                destino.getTopo().getJogador() != jogadorAtual &&
                destino.getPecas().size() == 1) {
            Peca pecaAdversaria = destino.getTopo();
            destino.removerPeca(pecaAdversaria);
            pecaAdversaria.getJogador().capturarPeca(pecaAdversaria);
            System.out.println("Peça capturada!");
        }

        origem.removerPeca(pecaParaMover);
        destino.adicionarPeca(pecaParaMover);
        jogadorAtual.usarDado(valorMovimento);

        return true;
    }

    public boolean moverPecaCapturada(int campoDestinoId) {
        Campo destino = campos.get(campoDestinoId);
        if (destino == null) return false;

        if (!jogadorAtual.temPecasCapturadas()) return false;

        int valorDado = jogadorAtual == jogadorServidor
                ? campoDestinoId
                : 25 - campoDestinoId;

        if (!jogadorAtual.getDadosDisponiveis().contains(valorDado)) return false;

        if (!podeMoverPara(destino)) return false;

        Peca peca = jogadorAtual.libertarPecaCapturada();

        if (destino.temPecas() && destino.getTopo().getJogador() != jogadorAtual) {
            Peca pecaAdversaria = destino.getTopo();
            destino.removerPeca(pecaAdversaria);
            pecaAdversaria.getJogador().capturarPeca(pecaAdversaria);
            System.out.println("Peça capturada ao reentrar!");
        }

        destino.adicionarPeca(peca);
        jogadorAtual.usarDado(valorDado);
        return true;
    }

    public List<Integer> getCamposComPecasMoveis() {
        List<Integer> camposMoveis = new ArrayList<>();

        if (jogadorAtual == null) return camposMoveis;

        // Se houver peças capturadas, só se pode tentar reentrar
        if (jogadorAtual.temPecasCapturadas()) {
            List<Integer> entradasPossiveis = getCamposEntradaDisponiveis();
            camposMoveis.addAll(entradasPossiveis);
            return camposMoveis;
        }

        // Movimento normal
        for (Map.Entry<Integer, Campo> entry : campos.entrySet()) {
            int campoId = entry.getKey();
            Campo campo = entry.getValue();

            if (!campo.getPecas().isEmpty() &&
                    campo.getTopo().getJogador() == jogadorAtual &&
                    !getDestinosValidos(campoId).isEmpty()) {
                camposMoveis.add(campoId);
            }
        }

        // BEARING OFF: se pode livrar peças, permite campos de bearing off (25/0) como possíveis
        if (podeLivrarPecas(jogadorAtual)) {
            if (jogadorAtual == jogadorServidor) {
                for (int i = 19; i <= 24; i++) {
                    Campo campo = campos.get(i);
                    if (!campo.getPecas().isEmpty() && campo.getTopo().getJogador() == jogadorAtual) {
                        camposMoveis.add(i);
                    }
                }
            } else {
                for (int i = 1; i <= 6; i++) {
                    Campo campo = campos.get(i);
                    if (!campo.getPecas().isEmpty() && campo.getTopo().getJogador() == jogadorAtual) {
                        camposMoveis.add(i);
                    }
                }
            }
        }

        return camposMoveis;
    }

    public List<Integer> getCamposEntradaDisponiveis() {
        List<Integer> entradas = new ArrayList<>();
        List<Integer> dados = jogadorAtual.getDadosDisponiveis();

        for (int dado : dados) {
            int campoEntrada = jogadorAtual == jogadorServidor ? dado : 25 - dado;
            Campo campo = campos.get(campoEntrada);
            if (campo != null && podeMoverPara(campo)) {
                entradas.add(campoEntrada);
            }
        }

        return entradas;
    }

    public List<Integer> getDestinosValidos(int campoOrigemId) {
        List<Integer> destinos = new ArrayList<>();
        Campo origem = campos.get(campoOrigemId);

        if (origem == null || origem.getPecas().isEmpty() ||
                origem.getTopo().getJogador() != jogadorAtual) {
            return destinos;
        }

        // Movimentos normais para dentro do tabuleiro
        for (int dado : jogadorAtual.getDadosDisponiveis()) {
            int destinoId = calcularDestino(campoOrigemId, dado);
            if (destinoId >= 1 && destinoId <= 24) {
                Campo destino = campos.get(destinoId);
                if (podeMoverPara(destino)) {
                    destinos.add(destinoId);
                }
            }
        }

        // --- BEARING OFF (LIVRAR PEÇAS) ---
        if (podeLivrarPecas(jogadorAtual)) {
            if (jogadorAtual == jogadorServidor && campoOrigemId >= 19 && campoOrigemId <= 24) {
                int valorDadoNecessario = 25 - campoOrigemId;
                boolean existeAtras = existemPecasAtras(campoOrigemId, jogadorServidor);

                for (int dado : jogadorAtual.getDadosDisponiveis()) {
                    if (dado == valorDadoNecessario) {
                        if (!destinos.contains(25)) destinos.add(25);
                    } else if (dado > valorDadoNecessario && !existeAtras) {
                        if (!destinos.contains(25)) destinos.add(25);
                    }
                }
            }
            if (jogadorAtual == jogadorCliente && campoOrigemId >= 1 && campoOrigemId <= 6) {
                int valorDadoNecessario = campoOrigemId;
                boolean existeAtras = existemPecasAtras(campoOrigemId, jogadorCliente);

                for (int dado : jogadorAtual.getDadosDisponiveis()) {
                    if (dado == valorDadoNecessario) {
                        if (!destinos.contains(0)) destinos.add(0);
                    } else if (dado > valorDadoNecessario && !existeAtras) {
                        if (!destinos.contains(0)) destinos.add(0);
                    }
                }
            }
        }

        return destinos;
    }

    private int calcularDestino(int origem, int dado) {
        if (jogadorAtual == jogadorServidor) {
            int destino = origem + dado;
            return (destino <= 24) ? destino : -1;
        } else {
            int destino = origem - dado;
            return (destino >= 1) ? destino : -1;
        }
    }

    private boolean podeMoverPara(Campo destino) {
        if (destino == null) return false;
        if (!destino.temPecas()) return true;
        return destino.getTopo().getJogador() == jogadorAtual ||
                destino.getPecas().size() == 1;
    }

    private void adicionarPecas(int campoId, Jogador jogador, int quantidade) {
        Campo campo = campos.get(campoId);
        for (int i = 0; i < quantidade; i++) {
            campo.adicionarPeca(new Peca(jogador));
        }
    }
}