package backgammon.modelo;

import java.util.*;

public class JogoBackgammon {
    private Map<Integer, Campo> campos = new HashMap<>();
    private Jogador jogador1;
    private Jogador jogador2;
    private Jogador jogadorAtual;

    public JogoBackgammon() {
        for (int i = 1; i <= 24; i++) {
            campos.put(i, new Campo(i));
        }
        jogador1 = new Jogador("");
        jogador2 = new Jogador("");
        jogadorAtual = null;
    }

    private void inicializarPosicoesIniciais() {
        campos.values().forEach(campo -> campo.getPecas().clear());
        System.out.println("Tabuleiro inicializado: " + campos);

        adicionarPecas(1, jogador1, 2);
        adicionarPecas(12, jogador1, 5);
        adicionarPecas(17, jogador1, 3);
        adicionarPecas(19, jogador1, 5);

        adicionarPecas(6, jogador2, 5);
        adicionarPecas(8, jogador2, 3);
        adicionarPecas(13, jogador2, 5);
        adicionarPecas(24, jogador2, 2);
    }

    public Campo getCampo(int id) {
        return campos.get(id);
    }

    public Map<Integer, Campo> getCampos() {
        return campos;
    }

    public Jogador getJogador1() {
        return jogador1;
    }

    public Jogador getJogador2() {
        return jogador2;
    }

    public Jogador getJogadorAtual() {
        if (jogadorAtual == null) {
            System.err.println("Erro: Jogador atual não inicializado. Verifique o sorteio ou inicialização.");
            // Return jogador1 as a fallback for UI rendering to avoid null issues
            return jogador1.getNome().isEmpty() ? null : jogador1;
        }
        return jogadorAtual;
    }

    public void alternarJogador() {
        if (jogadorAtual != null && jogadorAtual.getDadosDisponiveis().isEmpty()) {
            jogadorAtual = (jogadorAtual == jogador1) ? jogador2 : jogador1;
            System.out.println("Turno alternado para: " + jogadorAtual.getNome());
        } else {
            System.err.println("Erro: Não é possível alternar jogador, dados ainda disponíveis ou jogadorAtual nulo.");
        }
    }

    public void lancarDados() {
        if (jogadorAtual != null) {
            jogadorAtual.rolarDados();
            System.out.println("Dados lançados para " + jogadorAtual.getNome() + ": " + jogadorAtual.getDadosDisponiveis());
        } else {
            System.err.println("Erro: Não é possível lançar dados, jogadorAtual é null.");
        }
    }

    public void iniciarTurno() {
        if (jogadorAtual == null) {
            System.err.println("Erro: jogadorAtual é null em iniciarTurno()");
            return;
        }
        lancarDados();
    }

    public void verificarEFinalizarTurno() {
        if (jogadorAtual == null) return;

        boolean semDados = jogadorAtual.getDadosDisponiveis().isEmpty();
        boolean semJogadas = getCamposComPecasMoveis().isEmpty();

        if (semDados || semJogadas) {
            jogadorAtual.getDadosDisponiveis().clear();
            jogadorAtual.getUltimosDados().clear();

            jogadorAtual = (jogadorAtual == jogador1) ? jogador2 : jogador1;
            System.out.println("Turno alternado para: " + jogadorAtual.getNome());

            iniciarTurno();
        }
    }


    public boolean podeLivrarPecas(Jogador jogador) {
        if (jogador == null || jogador.temPecasCapturadas()) return false;
        if (jogador == jogador1) {
            for (int i = 1; i <= 18; i++) {
                if (campos.get(i).temPecasDoJogador(jogador)) return false;
            }
            return true;
        } else {
            for (int i = 7; i <= 24; i++) {
                if (campos.get(i).temPecasDoJogador(jogador)) return false;
            }
            return true;
        }
    }

    private boolean existemPecasAtras(int campoOrigemId, Jogador jogador) {
        if (jogador == null) return false;
        if (jogador == jogador1) {
            for (int i = 19; i < campoOrigemId; i++) {
                if (campos.get(i).temPecasDoJogador(jogador1)) {
                    return true;
                }
            }
        } else {
            for (int i = campoOrigemId + 1; i <= 6; i++) {
                if (campos.get(i).temPecasDoJogador(jogador2)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean moverPeca(int campoOrigemId, int campoDestinoId) {
        System.out.println("Tentando mover de " + campoOrigemId + " para " + campoDestinoId);

        if (jogadorAtual == null) {
            System.err.println("Erro: Jogador atual não definido.");
            return false;
        }

        if (podeLivrarPecas(jogadorAtual)) {
            if (jogadorAtual == jogador1 && campoOrigemId >= 19 && campoOrigemId <= 24 && campoDestinoId == 25) {
                int valorDadoNecessario = 25 - campoOrigemId;
                Campo origem = campos.get(campoOrigemId);
                if (origem == null || origem.getPecas().isEmpty()) return false;
                Peca peca = origem.getTopo();
                if (peca.getJogador() != jogadorAtual) return false;
                boolean existeAtras = existemPecasAtras(campoOrigemId, jogadorAtual);

                System.out.println("Jogador1 | Dados disponíveis: " + jogadorAtual.getDadosDisponiveis() +
                        " | Valor necessário: " + valorDadoNecessario + " | Existe peças atrás? " + existeAtras);

                for (int dado : new ArrayList<>(jogadorAtual.getDadosDisponiveis())) {
                    if (dado == valorDadoNecessario) {
                        origem.removerPeca(peca);
                        jogadorAtual.usarDado(valorDadoNecessario);
                        System.out.println("Peça de jogador1 livrada do campo " + campoOrigemId);
                        return true;
                    } else if (dado > valorDadoNecessario && !existeAtras) {
                        origem.removerPeca(peca);
                        jogadorAtual.usarDado(dado);
                        System.out.println("Peça de jogador1 livrada do campo " + campoOrigemId + " (dado maior)");
                        return true;
                    }
                }
                System.out.println("Não foi possível livrar peça (jogador1)!");
                return false;
            }

            if (jogadorAtual == jogador2 && campoOrigemId >= 1 && campoOrigemId <= 6 && campoDestinoId == 0) {
                int valorDadoNecessario = campoOrigemId;
                Campo origem = campos.get(campoOrigemId);
                if (origem == null || origem.getPecas().isEmpty()) return false;
                Peca peca = origem.getTopo();
                if (peca.getJogador() != jogadorAtual) return false;
                boolean existeAtras = existemPecasAtras(campoOrigemId, jogadorAtual);

                System.out.println("Jogador2 | Dados disponíveis: " + jogadorAtual.getDadosDisponiveis() +
                        " | Valor necessário: " + valorDadoNecessario + " | Existe peças atrás? " + existeAtras);

                for (int dado : new ArrayList<>(jogadorAtual.getDadosDisponiveis())) {
                    if (dado == valorDadoNecessario) {
                        origem.removerPeca(peca);
                        jogadorAtual.usarDado(valorDadoNecessario);
                        System.out.println("Peça de jogador2 livrada do campo " + campoOrigemId);
                        return true;
                    } else if (dado > valorDadoNecessario && !existeAtras) {
                        origem.removerPeca(peca);
                        jogadorAtual.usarDado(dado);
                        System.out.println("Peça de jogador2 livrada do campo " + campoOrigemId + " (dado maior)");
                        return true;
                    }
                }
                System.out.println("Não foi possível livrar peça (jogador2)!");
                return false;
            }
        }

        Campo destino = campos.get(campoDestinoId);
        if (destino == null) return false;

        if (jogadorAtual.temPecasCapturadas()) {
            List<Integer> entradasPossiveis = getCamposEntradaDisponiveis();
            if (!entradasPossiveis.contains(campoDestinoId)) return false;
            if (!podeMoverPara(destino)) return false;

            Peca pecaReentrada = jogadorAtual.getPecasCapturadas().get(0);
            destino.adicionarPeca(pecaReentrada);
            jogadorAtual.libertarPecaCapturada();

            int valorMovimento = jogadorAtual == jogador1 ? campoDestinoId : 25 - campoDestinoId;
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

        int valorMovimento = jogadorAtual == jogador1
                ? campoDestinoId - campoOrigemId
                : campoOrigemId - campoDestinoId;

        if (!jogadorAtual.getDadosDisponiveis().contains(valorMovimento)) return false;

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
        if (jogadorAtual == null) {
            System.err.println("Erro: Jogador atual não definido para mover peça capturada.");
            return false;
        }
        Campo destino = campos.get(campoDestinoId);
        if (destino == null) return false;

        if (!jogadorAtual.temPecasCapturadas()) return false;

        int valorDado = jogadorAtual == jogador1
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
        if (jogadorAtual == null) {
            System.err.println("Erro: Jogador atual nulo ao verificar campos móveis.");
            return camposMoveis;
        }
        System.out.println("Verificando campos móveis para " + jogadorAtual.getNome() + " com dados: " + jogadorAtual.getDadosDisponiveis());

        if (jogadorAtual.temPecasCapturadas()) {
            camposMoveis.addAll(getCamposEntradaDisponiveis());
            System.out.println("Peças capturadas. Campos de entrada disponíveis: " + camposMoveis);
            return camposMoveis;
        }

        for (Map.Entry<Integer, Campo> entry : campos.entrySet()) {
            int campoId = entry.getKey();
            Campo campo = entry.getValue();
            if (!campo.getPecas().isEmpty() && campo.getTopo().getJogador() == jogadorAtual) {
                List<Integer> destinos = getDestinosValidos(campoId);
                if (!destinos.isEmpty()) {
                    camposMoveis.add(campoId);
                    System.out.println("Campo " + campoId + " tem destinos válidos: " + destinos);
                }
            }
        }

        if (podeLivrarPecas(jogadorAtual)) {
            List<Integer> camposBearing = new ArrayList<>();
            List<Integer> dados = jogadorAtual.getDadosDisponiveis();

            if (jogadorAtual == jogador1) {
                for (int dado : dados) {
                    int campo = 25 - dado;
                    if (campo >= 19 && campo <= 24) {
                        Campo c = campos.get(campo);
                        if (c != null && !c.getPecas().isEmpty() && c.getTopo().getJogador() == jogadorAtual)
                            camposBearing.add(campo);
                    }
                }
                int maisAtras = 25;
                for (int i = 19; i <= 24; i++) {
                    Campo c = campos.get(i);
                    if (c != null && !c.getPecas().isEmpty() && c.getTopo().getJogador() == jogadorAtual)
                        maisAtras = Math.min(maisAtras, i);
                }
                boolean existemAtras = false;
                for (int i = 19; i < maisAtras; i++) {
                    if (campos.get(i).temPecasDoJogador(jogadorAtual)) {
                        existemAtras = true;
                        break;
                    }
                }
                if (existemAtras) {
                    for (int i = 19; i < maisAtras; i++) {
                        Campo c = campos.get(i);
                        if (c != null && !c.getPecas().isEmpty() && c.getTopo().getJogador() == jogadorAtual)
                            if (!camposMoveis.contains(i))
                                camposMoveis.add(i);
                    }
                } else {
                    for (int campo : camposBearing) {
                        if (!camposMoveis.contains(campo))
                            camposMoveis.add(campo);
                    }
                }
            } else {
                for (int dado : dados) {
                    int campo = dado;
                    if (campo >= 1 && campo <= 6) {
                        Campo c = campos.get(campo);
                        if (c != null && !c.getPecas().isEmpty() && c.getTopo().getJogador() == jogadorAtual)
                            camposBearing.add(campo);
                    }
                }
                int maisAtras = 0;
                for (int i = 6; i >= 1; i--) {
                    Campo c = campos.get(i);
                    if (c != null && !c.getPecas().isEmpty() && c.getTopo().getJogador() == jogadorAtual)
                        maisAtras = Math.max(maisAtras, i);
                }
                boolean existemAtras = false;
                for (int i = 6; i > maisAtras; i--) {
                    if (campos.get(i).temPecasDoJogador(jogadorAtual)) {
                        existemAtras = true;
                        break;
                    }
                }
                if (existemAtras) {
                    for (int i = 6; i > maisAtras; i--) {
                        Campo c = campos.get(i);
                        if (c != null && !c.getPecas().isEmpty() && c.getTopo().getJogador() == jogadorAtual)
                            if (!camposMoveis.contains(i))
                                camposMoveis.add(i);
                    }
                } else {
                    for (int campo : camposBearing) {
                        if (!camposMoveis.contains(campo))
                            camposMoveis.add(campo);
                    }
                }
            }
            System.out.println("Pode livrar peças. Campos bearing off: " + camposMoveis);
        }

        System.out.println("Campos móveis finais: " + camposMoveis);
        return camposMoveis;
    }

    public List<Integer> getCamposEntradaDisponiveis() {
        List<Integer> entradas = new ArrayList<>();
        if (jogadorAtual == null) {
            System.err.println("Erro: Jogador atual nulo ao verificar campos de entrada.");
            return entradas;
        }
        List<Integer> dados = jogadorAtual.getDadosDisponiveis();
        System.out.println("Campos de entrada para " + jogadorAtual.getNome() + " com dados: " + dados);

        for (int dado : dados) {
            int campoEntrada = jogadorAtual == jogador1 ? dado : 25 - dado;
            Campo campo = campos.get(campoEntrada);
            if (campo != null && podeMoverPara(campo)) {
                entradas.add(campoEntrada);
            }
        }
        System.out.println("Campos de entrada disponíveis: " + entradas);
        return entradas;
    }

    public List<Integer> getDestinosValidos(int campoOrigemId) {
        List<Integer> destinos = new ArrayList<>();
        Campo origem = campos.get(campoOrigemId);

        if (jogadorAtual == null) {
            System.err.println("Erro: Jogador atual nulo ao verificar destinos válidos.");
            return destinos;
        }

        if (origem == null || origem.getPecas().isEmpty() ||
                origem.getTopo().getJogador() != jogadorAtual) {
            System.out.println("Nenhum destino válido para campo " + campoOrigemId + ": origem inválida ou sem peças do jogador atual.");
            return destinos;
        }

        for (int dado : jogadorAtual.getDadosDisponiveis()) {
            int destinoId = calcularDestino(campoOrigemId, dado);
            if (destinoId >= 1 && destinoId <= 24) {
                Campo destino = campos.get(destinoId);
                if (podeMoverPara(destino)) {
                    destinos.add(destinoId);
                }
            }
        }

        if (podeLivrarPecas(jogadorAtual)) {
            if (jogadorAtual == jogador1 && campoOrigemId >= 19 && campoOrigemId <= 24) {
                int valorDadoNecessario = 25 - campoOrigemId;
                boolean existeAtras = existemPecasAtras(campoOrigemId, jogador1);

                for (int dado : jogadorAtual.getDadosDisponiveis()) {
                    if (dado == valorDadoNecessario) {
                        if (!destinos.contains(25)) destinos.add(25);
                    } else if (dado > valorDadoNecessario && !existeAtras) {
                        if (!destinos.contains(25)) destinos.add(25);
                    }
                }
            }
            if (jogadorAtual == jogador2 && campoOrigemId >= 1 && campoOrigemId <= 6) {
                int valorDadoNecessario = campoOrigemId;
                boolean existeAtras = existemPecasAtras(campoOrigemId, jogador2);

                for (int dado : jogadorAtual.getDadosDisponiveis()) {
                    if (dado == valorDadoNecessario) {
                        if (!destinos.contains(0)) destinos.add(0);
                    } else if (dado > valorDadoNecessario && !existeAtras) {
                        if (!destinos.contains(0)) destinos.add(0);
                    }
                }
            }
        }
        System.out.println("Destinos válidos para campo " + campoOrigemId + ": " + destinos);
        return destinos;
    }

    private int calcularDestino(int origem, int dado) {
        if (jogadorAtual == null) return -1;
        if (jogadorAtual == jogador1) {
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

    public void inicializarJogo(String nomeJogadorBrancas, String nomeJogadorVermelhas) {
        if (nomeJogadorBrancas == null || nomeJogadorVermelhas == null) {
            throw new IllegalArgumentException("Nomes dos jogadores não podem ser nulos.");
        }
        this.jogador1.setNome(nomeJogadorBrancas);
        this.jogador2.setNome(nomeJogadorVermelhas);
        this.jogadorAtual = null;
        inicializarPosicoesIniciais();
        System.out.println("Jogo inicializado com " + jogador1.getNome() + " (brancas) e " + jogador2.getNome() + " (vermelhas).");
    }

    public void definirJogadorInicial(String nomeJogador) {
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Definindo jogador inicial: " + nomeJogador);
        if (nomeJogador == null || nomeJogador.trim().isEmpty()) {
            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Nome do jogador inicial é null ou vazio.");
            return;
        }
        String nomeTrimmed = nomeJogador.trim();
        if (jogador1 == null || jogador2 == null) {
            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Jogadores não inicializados. Jogador1: " + (jogador1 != null ? jogador1.getNome() : "null") + ", Jogador2: " + (jogador2 != null ? jogador2.getNome() : "null"));
            return;
        }
        if (nomeTrimmed.equalsIgnoreCase(jogador1.getNome())) {
            jogadorAtual = jogador1;
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Jogador atual definido: " + jogadorAtual.getNome());
        } else if (nomeTrimmed.equalsIgnoreCase(jogador2.getNome())) {
            jogadorAtual = jogador2;
            System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Jogador atual definido: " + jogadorAtual.getNome());
        } else {
            System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Nome do jogador inicial não corresponde a nenhum jogador. Recebido: '" + nomeTrimmed + "', Jogador1: '" + jogador1.getNome() + "', Jogador2: '" + jogador2.getNome() + "'");
            // Fallback: Set jogadorAtual to jogador1 if initialized
            if (!jogador1.getNome().isEmpty()) {
                jogadorAtual = jogador1;
                System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Fallback: Jogador atual definido como jogador1: " + jogador1.getNome());
            } else {
                System.err.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] Erro: Não foi possível definir jogador inicial, jogador1 não inicializado.");
            }
        }
    }

    public void limparDadosJogadorAtual() {
        if (jogadorAtual != null) {
            jogadorAtual.getUltimosDados().clear();
            jogadorAtual.getDadosDisponiveis().clear();
            System.out.println("Dados limpos para " + jogadorAtual.getNome());
        } else {
            System.err.println("Erro: Não é possível limpar dados, jogadorAtual é null.");
        }
    }
}