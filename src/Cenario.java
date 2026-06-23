import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Cenario extends JPanel {

    private Bola bola;

    public int golsP1 = 0;
    public int golsBot = 0;

    private boolean emPartida = false;

    private final int botaoX = 312;
    private final int botaoY = 280;
    private final int botaoLargura = 160;
    private final int botaoAltura = 50;

    public Jogador goleiroEsquerda;
    public Jogador linhaEsquerda;
    public Jogador goleiroDireita;
    public Jogador linhaDireita;

    public Cenario(Bola bola) {
        this.bola = bola;
        setBackground(Color.BLACK);

        // Posições iniciais e definição de times fixas
        this.goleiroEsquerda = new Jogador(30, 280, "esquerda");
        this.linhaEsquerda   = new Jogador(200, 280, "esquerda");
        this.goleiroDireita  = new Jogador(740, 280, "direita");
        this.linhaDireita    = new Jogador(570, 280, "direita");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!emPartida) {
                    int mx = e.getX();
                    int my = e.getY();

                    if (mx >= botaoX && mx <= botaoX + botaoLargura &&
                            my >= botaoY && my <= botaoY + botaoAltura) {
                        emPartida = true;
                        repaint();
                    }
                }
            }
        });
    }

    public boolean jogoRodando() {
        return emPartida;
    }

    public void verificarGol() {
        if (!emPartida) return;

        if (bola.x <= 10 && bola.y >= 270 && bola.y <= 390) {
            golsBot++;
            resetarBola();
        }
        else if (bola.x + bola.tamanho >= 775 && bola.y >= 270 && bola.y <= 390) {
            golsP1++;
            resetarBola();
        }
    }

    private void resetarBola() {
        bola.x = 385;
        bola.y = 320;
        if (bola.velocidadeX != 0) {
            bola.velocidadeX = -bola.velocidadeX;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- TELA INICIAL ---
        if (!emPartida) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 42));
            g2d.drawString("FUT PONG", 295, 200);

            g2d.drawRect(botaoX, botaoY, botaoLargura, botaoAltura);

            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("JOGAR", 355, 312);
            return;
        }

        // --- TELA DO JOGO ---
        g2d.setColor(Color.WHITE);

        g2d.drawRect(10, 60, 765, 540);
        g2d.drawLine(392, 60, 392, 600);
        g2d.drawOval(342, 280, 100, 100);
        g2d.drawRect(10, 195, 90, 270);
        g2d.drawRect(2, 270, 8, 120);
        g2d.drawRect(685, 195, 90, 270);
        g2d.drawRect(775, 270, 8, 120);

        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        g2d.drawString("FUT PONG", 50, 40);
        g2d.drawString("2° | 44:55", 640, 40);

        String textoPlacar = "|    P1 - " + golsP1 + " / " + golsBot + " - BOT    |";
        g2d.drawString(textoPlacar, 285, 40);

        g2d.fillOval(bola.x, bola.y, bola.tamanho, bola.tamanho);

        // Renderização dos Jogadores com Orientação Fixa de Olhar
        Jogador[] todosJogadores = {goleiroEsquerda, linhaEsquerda, goleiroDireita, linhaDireita};
        for (Jogador j : todosJogadores) {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillOval(j.x, j.y, j.largura, j.altura);

            g2d.setColor(Color.WHITE);
            int tamanhoCabeca = 24;
            int cabecaY = j.y + (j.altura / 2) - (tamanhoCabeca / 2);

            // CORREÇÃO AQUI: Em vez de checar a posição X atual, checa as instâncias específicas dos times
            int cabecaX;
            if (j == goleiroEsquerda || j == linhaEsquerda) {
                // Time da Esquerda: olha fixo para a DIREITA
                cabecaX = j.x + j.largura - (int)(tamanhoCabeca * 0.9);
            } else {
                // Time da Direita: olha fixo para a ESQUERDA
                cabecaX = j.x - tamanhoCabeca + (int)(tamanhoCabeca * 0.9);
            }

            g2d.fillOval(cabecaX, cabecaY, tamanhoCabeca, tamanhoCabeca);

            // Mantém o desenho das luvas dos goleiros nas extremidades corretas
            if (j == goleiroEsquerda || j == goleiroDireita) {
                int tamanhoMao = 10;
                int maoX = (j == goleiroEsquerda) ? (j.x + j.largura - (tamanhoMao / 2)) : (j.x - (tamanhoMao / 2));
                g2d.fillOval(maoX, j.y - (tamanhoMao / 2), tamanhoMao, tamanhoMao);
                g2d.fillOval(maoX, j.y + j.altura - (tamanhoMao / 2), tamanhoMao, tamanhoMao);
            }
        }
    }
}