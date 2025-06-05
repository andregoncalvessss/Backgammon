package backgammon.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Jogador {
    private List<Peca> pecas;
    private List<Integer> dadosDisponiveis;  // Dados que ainda podem ser usados na jogada
    private List<Integer> ultimosDados;      // Últimos dados lançados (mesmo quando já usados)
    private final String nome; // Identificador do jogador

    private final List<Peca> pecasCapturadas = new ArrayList<>(); // 🔁 Novidade: peças fora do tabuleiro

    public Jogador(String nome) {
        this.nome = nome;
        this.pecas = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            pecas.add(new Peca(this));
        }
        dadosDisponiveis = new ArrayList<>();
        ultimosDados = new ArrayList<>();
    }

    public String getNome() {
        return nome;
    }

    public void rolarDados() {
        Random random = new Random();
        int d1 = random.nextInt(6) + 1;
        int d2 = random.nextInt(6) + 1;

        dadosDisponiveis.clear();
        ultimosDados.clear();

        if (d1 == d2) {
            for (int i = 0; i < 4; i++) {
                dadosDisponiveis.add(d1);
                ultimosDados.add(d1);
            }
        } else {
            dadosDisponiveis.add(d1);
            dadosDisponiveis.add(d2);
            ultimosDados.add(d1);
            ultimosDados.add(d2);
        }
    }


    public List<Integer> getDadosDisponiveis() {
        return dadosDisponiveis;
    }

    public List<Integer> getUltimosDados() {
        return ultimosDados;
    }

    public boolean usarDado(int valor) {
        return dadosDisponiveis.remove(Integer.valueOf(valor));
    }

    public List<Peca> getPecas() {
        return pecas;
    }

    public Peca retiraPeca() {
        if (!pecas.isEmpty()) {
            return pecas.remove(0);
        }
        return null;
    }

    // 🔽 NOVO: Captura e reentrada

    public List<Peca> getPecasCapturadas() {
        return pecasCapturadas;
    }

    public void capturarPeca(Peca peca) {
        pecasCapturadas.add(peca);
    }

    public boolean temPecasCapturadas() {
        return !pecasCapturadas.isEmpty();
    }

    public Peca libertarPecaCapturada() {
        if (!pecasCapturadas.isEmpty()) {
            return pecasCapturadas.remove(0);
        }
        return null;
    }

    @Override
    public String toString() {
        return nome;
    }
}
