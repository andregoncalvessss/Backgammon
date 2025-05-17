package backgammon.modelo;

public class JogoBackgammon {

    private Jogador jogadorServidor;
    private Jogador jogadorCliente;
    private Jogador jogadorAtual;

    public JogoBackgammon() {
        jogadorServidor = new Jogador();
        jogadorCliente = new Jogador();
        jogadorAtual = jogadorServidor;  // começa o servidor
    }

    public void lancarDados() {
        jogadorAtual.rolarDados();
    }

    public Jogador getJogadorAtual() {
        return jogadorAtual;
    }

    public void alternarJogador() {
        if (jogadorAtual == jogadorServidor) {
            jogadorAtual = jogadorCliente;
        } else {
            jogadorAtual = jogadorServidor;
        }
    }

    public Jogador getJogadorServidor() {
        return jogadorServidor;
    }

    public Jogador getJogadorCliente() {
        return jogadorCliente;
    }
}