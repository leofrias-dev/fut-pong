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
// --- IMPORTAÇÕES NOVAS PARA A TRANSPARÊNCIA E TEXTO ---
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.FontMetrics;


public class Cenario extends JPanel {

    private Bola bola;

    public int golsP1 = 0;
    public int golsBot = 0;

    public int auraX, auraY, auraRaio;
    public boolean desenharAura = false;

    public int auraBotX, auraBotY, auraBotRaio;
    public boolean desenharAuraBot = false;

    private boolean emPartida = false;

    public Jogador goleiroEsquerda;
    public Jogador linhaEsquerda;
    public Jogador goleiroDireita;
    public Jogador linhaDireita;

    // --- VARIÁVEIS DO CRONÔMETRO DINÂMICO ---
    private int periodoPlacar = 1;
    private int minPlacar = 0;
    private int segPlacar = 0;
    private boolean partidaTerminada = false;

    // --- VARIÁVEIS DO FADE-OUT (TEXTO NO FUNDO) ---
    private int periodoTextoFade = 1;
    private float alphaTextoFade = 0.25f;

    public Cenario(Bola bola) {
        this.bola = bola;
        setBackground(Color.BLACK);

        this.goleiroEsquerda = new Jogador(DimensoesJogo.GOLEIRO_ESQUERDA_INICIAL_X, DimensoesJogo.JOGADORES_INICIAL_Y, "esquerda");
        this.linhaEsquerda   = new Jogador(DimensoesJogo.JOGADOR_ESQUERDA_INICIAL_X, DimensoesJogo.JOGADORES_INICIAL_Y, "esquerda");
        this.goleiroDireita  = new Jogador(DimensoesJogo.GOLEIRO_DIREITA_INICIAL_X, DimensoesJogo.JOGADORES_INICIAL_Y, "direita");
        this.linhaDireita    = new Jogador(DimensoesJogo.JOGADOR_DIREITA_INICIAL_X, DimensoesJogo.JOGADORES_INICIAL_Y, "direita");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!emPartida) {
                    int mx = e.getX();
                    int my = e.getY();
                    if (mx >= DimensoesJogo.BOTAO_JOGAR_X && mx <= DimensoesJogo.BOTAO_JOGAR_X + DimensoesJogo.BOTAO_JOGAR_LARGURA &&
                            my >= DimensoesJogo.BOTAO_JOGAR_Y && my <= DimensoesJogo.BOTAO_JOGAR_Y + DimensoesJogo.BOTAO_JOGAR_ALTURA) {
                        emPartida = true;
                        repaint();
                    }
                }
            }
        });
    }

    public void atualizarCronometro(int periodo, int minutos, int segundos, boolean fim) {
        this.periodoPlacar = periodo;
        this.minPlacar = minutos;
        this.segPlacar = segundos;
        this.partidaTerminada = fim;
    }

    // --- NOVO MÉTODO: Recebe a opacidade do Main ---
    public void atualizarTextoFade(int periodo, float alpha) {
        this.periodoTextoFade = periodo;
        this.alphaTextoFade = alpha;
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
        if (bola.x <= DimensoesJogo.CAMPO_ESQUERDA && bola.y >= DimensoesJogo.GOL_TOPO && bola.y <= DimensoesJogo.GOL_FUNDO) {
            golsBot++;
            resetarBola();
        } else if (bola.x + bola.tamanho >= DimensoesJogo.CAMPO_DIREITA && bola.y >= DimensoesJogo.GOL_TOPO && bola.y <= DimensoesJogo.GOL_FUNDO) {
            golsP1++;
            resetarBola();
        }
    }

    private void resetarBola() {
        bola.x = DimensoesJogo.BOLA_INICIAL_X;
        bola.y = DimensoesJogo.BOLA_INICIAL_Y;
        bola.velX = 0;
        bola.velY = 0;

        goleiroEsquerda.x = DimensoesJogo.GOLEIRO_ESQUERDA_INICIAL_X;  goleiroEsquerda.y = DimensoesJogo.JOGADORES_INICIAL_Y;
        linhaEsquerda.x = DimensoesJogo.JOGADOR_ESQUERDA_INICIAL_X;    linhaEsquerda.y = DimensoesJogo.JOGADORES_INICIAL_Y;
        goleiroDireita.x = DimensoesJogo.GOLEIRO_DIREITA_INICIAL_X;    goleiroDireita.y = DimensoesJogo.JOGADORES_INICIAL_Y;
        linhaDireita.x = DimensoesJogo.JOGADOR_DIREITA_INICIAL_X;      linhaDireita.y = DimensoesJogo.JOGADORES_INICIAL_Y;

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
            g2d.drawRect(DimensoesJogo.BOTAO_JOGAR_X, DimensoesJogo.BOTAO_JOGAR_Y, DimensoesJogo.BOTAO_JOGAR_LARGURA, DimensoesJogo.BOTAO_JOGAR_ALTURA);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("JOGAR", 355, 312);
            return;
        }

        g2d.setColor(Color.WHITE);

        int raioCurva = DimensoesJogo.RAIO_CANTO;

        // Linhas retas
        g2d.drawLine(DimensoesJogo.CAMPO_ESQUERDA + raioCurva, DimensoesJogo.CAMPO_TOPO, DimensoesJogo.CAMPO_DIREITA - raioCurva, DimensoesJogo.CAMPO_TOPO);
        g2d.drawLine(DimensoesJogo.CAMPO_ESQUERDA + raioCurva, DimensoesJogo.CAMPO_FUNDO, DimensoesJogo.CAMPO_DIREITA - raioCurva, DimensoesJogo.CAMPO_FUNDO);

        g2d.drawLine(DimensoesJogo.CAMPO_ESQUERDA, DimensoesJogo.CAMPO_TOPO + raioCurva, DimensoesJogo.CAMPO_ESQUERDA, DimensoesJogo.GOL_TOPO);
        g2d.drawLine(DimensoesJogo.CAMPO_ESQUERDA, DimensoesJogo.GOL_FUNDO, DimensoesJogo.CAMPO_ESQUERDA, DimensoesJogo.CAMPO_FUNDO - raioCurva);

        g2d.drawLine(DimensoesJogo.CAMPO_DIREITA, DimensoesJogo.CAMPO_TOPO + raioCurva, DimensoesJogo.CAMPO_DIREITA, DimensoesJogo.GOL_TOPO);
        g2d.drawLine(DimensoesJogo.CAMPO_DIREITA, DimensoesJogo.GOL_FUNDO, DimensoesJogo.CAMPO_DIREITA, DimensoesJogo.CAMPO_FUNDO - raioCurva);

        // Arcos das quinas curvas
        g2d.drawArc(DimensoesJogo.CAMPO_ESQUERDA, DimensoesJogo.CAMPO_TOPO, raioCurva * 2, raioCurva * 2, 90, 90);
        g2d.drawArc(DimensoesJogo.CAMPO_ESQUERDA, DimensoesJogo.CAMPO_FUNDO - (raioCurva * 2), raioCurva * 2, raioCurva * 2, 180, 90);
        g2d.drawArc(DimensoesJogo.CAMPO_DIREITA - (raioCurva * 2), DimensoesJogo.CAMPO_TOPO, raioCurva * 2, raioCurva * 2, 0, 90);
        g2d.drawArc(DimensoesJogo.CAMPO_DIREITA - (raioCurva * 2), DimensoesJogo.CAMPO_FUNDO - (raioCurva * 2), raioCurva * 2, raioCurva * 2, 270, 90);

        // Meio de campo e áreas
        g2d.drawLine(DimensoesJogo.CAMPO_MEIO_X, DimensoesJogo.CAMPO_TOPO, DimensoesJogo.CAMPO_MEIO_X, DimensoesJogo.CAMPO_FUNDO);
        g2d.drawOval(342, 280, 100, 100);
        g2d.drawRect(DimensoesJogo.AREA_ESQUERDA_X, DimensoesJogo.AREA_TOPO, DimensoesJogo.AREA_LARGURA, DimensoesJogo.AREA_ALTURA);
        g2d.drawRect(DimensoesJogo.CAMPO_ESQUERDA - DimensoesJogo.GOL_PROFUNDIDADE, DimensoesJogo.GOL_TOPO, DimensoesJogo.GOL_PROFUNDIDADE, DimensoesJogo.GOL_ALTURA);
        g2d.drawRect(DimensoesJogo.AREA_DIREITA_X, DimensoesJogo.AREA_TOPO, DimensoesJogo.AREA_LARGURA, DimensoesJogo.AREA_ALTURA);
        g2d.drawRect(DimensoesJogo.CAMPO_DIREITA, DimensoesJogo.GOL_TOPO, DimensoesJogo.GOL_PROFUNDIDADE, DimensoesJogo.GOL_ALTURA);

        // --- DESENHO DO TEXTO DE FUNDO (MARCA D'ÁGUA) ---
        if (alphaTextoFade > 0.0f) {
            Composite originalComposite = g2d.getComposite(); // Salva a opacidade padrão

            // Aplica a transparência que está vindo do Main
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaTextoFade));
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 100)); // Fonte bem grande

            String texto = periodoTextoFade + "º TEMPO";

            // Centraliza o texto perfeitamente na tela
            FontMetrics fm = g2d.getFontMetrics();
            int xTexto = (DimensoesJogo.TELA_LARGURA - fm.stringWidth(texto)) / 2;
            int yTexto = (DimensoesJogo.TELA_ALTURA - fm.getHeight()) / 2 + fm.getAscent() - 20;

            g2d.drawString(texto, xTexto, yTexto);

            // Restaura a opacidade para que os jogadores e bola não fiquem transparentes!
            g2d.setComposite(originalComposite);
        }

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

        // --- EXIBIÇÃO DINÂMICA DO PLACAR E CRONÔMETRO ---
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        g2d.drawString("FUT PONG", 50, 40);

        if (partidaTerminada) {
            g2d.drawString("FIM DE JOGO", 600, 40);
            String textoPlacar = "|    P1 - " + golsP1 + " / " + golsBot + " - BOT    |";
            g2d.drawString(textoPlacar, 285, 40);
        } else {
            String tempoFormatado = String.format("%02d:%02d", minPlacar, segPlacar);
            g2d.drawString(periodoPlacar + "° | " + tempoFormatado, 640, 40);

            String textoPlacar = "|    P1 - " + golsP1 + " / " + golsBot + " - BOT    |";
            g2d.drawString(textoPlacar, 285, 40);
        }

        // Desenha a bola
        g2d.fillOval((int)bola.x, (int)bola.y, bola.tamanho, bola.tamanho);

        // Desenha os jogadores
        Jogador[] todosJogadores = {goleiroEsquerda, linhaEsquerda, goleiroDireita, linhaDireita};
        for (Jogador j : todosJogadores) {
            AffineTransform oldTransform = g2d.getTransform();

            int centroX = j.x + (j.largura / 2);
            int centroY = j.y + (j.altura / 2);

            double dx = (bola.x + bola.tamanho/2.0) - centroX;
            double dy = (bola.y + bola.tamanho/2.0) - centroY;
            double angulo = Math.atan2(dy, dx);

            g2d.translate(centroX, centroY);

            if (j == goleiroEsquerda || j == goleiroDireita) {
                g2d.rotate(angulo);
            } else {
                g2d.rotate(j.lado.equals("esquerda") ? 0 : Math.PI);
            }

            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillOval(-j.largura/2, -j.altura/2, j.largura, j.altura);

            g2d.setColor(Color.WHITE);
            int tamanhoCabeca = 24;
            g2d.fillOval(j.largura/2 - (int)(tamanhoCabeca * 0.8), -tamanhoCabeca/2, tamanhoCabeca, tamanhoCabeca);

            if (j == goleiroEsquerda || j == goleiroDireita) {
                int tamanhoMao = 10;
                g2d.fillOval(j.largura/2 - 5, -j.altura/2 - 2, tamanhoMao, tamanhoMao);
                g2d.fillOval(j.largura/2 - 5, j.altura/2 - 8, tamanhoMao, tamanhoMao);
            }

            g2d.setTransform(oldTransform);
        }
    }

    // --- NOVO MÉTODO ADICIONADO AQUI ---
    // Método chamado pelo Main quando passam os 5 segundos após o fim do jogo
    public void resetarParaMenu() {
        this.emPartida = false;
        this.golsP1 = 0;
        this.golsBot = 0;
        resetarBola(); // Reaproveita o seu código que centraliza bola e jogadores
    }
}
