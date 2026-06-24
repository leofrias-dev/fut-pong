import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;

public class Cenario extends JPanel {

    private Bola bola;

    public int golsP1 = 0;
    public int golsBot = 0;

    public int auraX, auraY, auraRaio;
    public boolean desenharAura = false;

    public int auraBotX, auraBotY, auraBotRaio;
    public boolean desenharAuraBot = false;

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

    public void configurarAnimacaoAura(int x, int y, int raio, boolean ativo) {
        this.auraX = x;
        this.auraY = y;
        this.auraRaio = raio;
        this.desenharAura = ativo;
    }

    public void configurarAnimacaoAuraBot(int x, int y, int raio, boolean ativo) {
        this.auraBotX = x;
        this.auraBotY = y;
        this.auraBotRaio = raio;
        this.desenharAuraBot = ativo;
    }

    public boolean jogoRodando() { return emPartida; }

    public void verificarGol() {
        if (!emPartida) return;
        if (bola.x <= 10 && bola.y >= 270 && bola.y <= 390) {
            golsBot++;
            resetarBola();
        } else if (bola.x + bola.tamanho >= 775 && bola.y >= 270 && bola.y <= 390) {
            golsP1++;
            resetarBola();
        }
    }

    private void resetarBola() {
        bola.x = 385;
        bola.y = 320;
        bola.velX = 0;
        bola.velY = 0;

        goleiroEsquerda.x = 30;  goleiroEsquerda.y = 280;
        linhaEsquerda.x = 200;   linhaEsquerda.y = 280;
        goleiroDireita.x = 740;  goleiroDireita.y = 280;
        linhaDireita.x = 570;    linhaDireita.y = 280;

        desenharAura = false;
        desenharAuraBot = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (!emPartida) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 42));
            g2d.drawString("FUT PONG", 295, 200);
            g2d.drawRect(botaoX, botaoY, botaoLargura, botaoAltura);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("JOGAR", 355, 312);
            return;
        }

        g2d.setColor(Color.WHITE);
        g2d.drawRect(10, 60, 765, 540);
        g2d.drawLine(392, 60, 392, 600);
        g2d.drawOval(342, 280, 100, 100);
        g2d.drawRect(10, 195, 90, 270);
        g2d.drawRect(2, 270, 8, 120);
        g2d.drawRect(685, 195, 90, 270);
        g2d.drawRect(775, 270, 8, 120);

        if (desenharAura) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(auraX - auraRaio + 12, auraY - auraRaio + 30, auraRaio * 2, auraRaio * 2);
        }

        if (desenharAuraBot) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(auraBotX - auraBotRaio + 12, auraBotY - auraBotRaio + 30, auraBotRaio * 2, auraBotRaio * 2);
        }

        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(Color.WHITE);

        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        g2d.drawString("FUT PONG", 50, 40);
        g2d.drawString("2° | 44:55", 640, 40);
        String textoPlacar = "|    P1 - " + golsP1 + " / " + golsBot + " - BOT    |";
        g2d.drawString(textoPlacar, 285, 40);

        g2d.fillOval((int)bola.x, (int)bola.y, bola.tamanho, bola.tamanho);

        Jogador[] todosJogadores = {goleiroEsquerda, linhaEsquerda, goleiroDireita, linhaDireita};
        for (Jogador j : todosJogadores) {

            // Salva o estado atual do canvas para rotacionar apenas esse boneco
            AffineTransform oldTransform = g2d.getTransform();

            // Centro do jogador para rotação
            int centroX = j.x + (j.largura / 2);
            int centroY = j.y + (j.altura / 2);

            // Calcula o ângulo olhando para a bola
            double dx = (bola.x + bola.tamanho/2.0) - centroX;
            double dy = (bola.y + bola.tamanho/2.0) - centroY;
            double angulo = Math.atan2(dy, dx);

            g2d.translate(centroX, centroY);

            // Se for goleiro, ele vira o corpo (mira a bola!). Se for jogador de linha, mantém o padrão do seu lado.
            if (j == goleiroEsquerda || j == goleiroDireita) {
                g2d.rotate(angulo);
            } else {
                g2d.rotate(j.lado.equals("esquerda") ? 0 : Math.PI);
            }

            // Desenha o corpo (Centralizado no 0,0 local)
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillOval(-j.largura/2, -j.altura/2, j.largura, j.altura);

            // Desenha a cabeça virada para frente
            g2d.setColor(Color.WHITE);
            int tamanhoCabeca = 24;
            g2d.fillOval(j.largura/2 - (int)(tamanhoCabeca * 0.8), -tamanhoCabeca/2, tamanhoCabeca, tamanhoCabeca);

            // Desenha as mãos se for goleiro
            if (j == goleiroEsquerda || j == goleiroDireita) {
                int tamanhoMao = 10;
                g2d.fillOval(j.largura/2 - 5, -j.altura/2 - 2, tamanhoMao, tamanhoMao);
                g2d.fillOval(j.largura/2 - 5, j.altura/2 - 8, tamanhoMao, tamanhoMao);
            }

            // Restaura o canvas para o próximo objeto
            g2d.setTransform(oldTransform);
        }
    }
}