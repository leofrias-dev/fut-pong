import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
// --- IMPORTAÇÕES NOVAS PARA A TRANSPARÊNCIA E TEXTO ---
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.util.function.IntConsumer;


public class Cenario extends JPanel {

    private Bola bola;

    public int golsP1 = 0;
    public int golsBot = 0;

    public int auraX, auraY, auraRaio;
    public boolean desenharAura = false;

    public int auraBotX, auraBotY, auraBotRaio;
    public boolean desenharAuraBot = false;

    private boolean emPartida = false;
    private boolean pausado = false;
    private int opcaoPausaSelecionada = 0;
    private int opcaoPausaSobMouse = -1;
    private IntConsumer acaoMenuPausa;
    private static final String[] OPCOES_PAUSA = {
            "Voltar ao jogo", "Recomeçar", "Voltar à tela de início"
    };

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

    private int deslocamentoCampoX() {
        return (getWidth() - DimensoesJogo.CAMPO_LARGURA) / 2 - DimensoesJogo.CAMPO_ESQUERDA;
    }

    private int deslocamentoCampoY() {
        return (getHeight() - DimensoesJogo.CAMPO_ALTURA) / 2 - DimensoesJogo.CAMPO_TOPO;
    }

    public Cenario(Bola bola) {
        this.bola = bola;
        setBackground(Color.BLACK);

        this.goleiroEsquerda = new Jogador(DimensoesJogo.GOLEIRO_ESQUERDA_INICIAL_X, DimensoesJogo.JOGADORES_INICIAL_Y, "esquerda");
        this.linhaEsquerda   = new Jogador(DimensoesJogo.JOGADOR_ESQUERDA_INICIAL_X, DimensoesJogo.JOGADORES_INICIAL_Y, "esquerda");
        this.goleiroDireita  = new Jogador(DimensoesJogo.GOLEIRO_DIREITA_INICIAL_X, DimensoesJogo.JOGADORES_INICIAL_Y, "direita");
        this.linhaDireita    = new Jogador(DimensoesJogo.JOGADOR_DIREITA_INICIAL_X, DimensoesJogo.JOGADORES_INICIAL_Y, "direita");

        MouseAdapter mouseMenu = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (pausado) {
                    int opcao = opcaoPausaNoPonto(e.getX(), e.getY());
                    if (opcao >= 0) {
                        opcaoPausaSelecionada = opcao;
                        confirmarOpcaoPausa();
                    }
                    return;
                }
                if (!emPartida) {
                    int mx = e.getX() - deslocamentoCampoX();
                    int my = e.getY() - deslocamentoCampoY();
                    if (mx >= DimensoesJogo.BOTAO_JOGAR_X && mx <= DimensoesJogo.BOTAO_JOGAR_X + DimensoesJogo.BOTAO_JOGAR_LARGURA &&
                            my >= DimensoesJogo.BOTAO_JOGAR_Y && my <= DimensoesJogo.BOTAO_JOGAR_Y + DimensoesJogo.BOTAO_JOGAR_ALTURA) {
                        reiniciarPartida();
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (!pausado) return;
                int opcao = opcaoPausaNoPonto(e.getX(), e.getY());
                if (opcao == opcaoPausaSobMouse) return;
                opcaoPausaSobMouse = opcao;
                if (opcao >= 0) opcaoPausaSelecionada = opcao;
                setCursor(Cursor.getPredefinedCursor(opcao >= 0
                        ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                opcaoPausaSobMouse = -1;
                setCursor(Cursor.getDefaultCursor());
            }
        };
        addMouseListener(mouseMenu);
        addMouseMotionListener(mouseMenu);
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

    public boolean isPausado() { return pausado; }

    public void definirPausado(boolean pausado) {
        this.pausado = emPartida && pausado;
        opcaoPausaSelecionada = 0;
        opcaoPausaSobMouse = -1;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    public void configurarAcaoMenuPausa(IntConsumer acao) {
        this.acaoMenuPausa = acao;
    }

    public void selecionarOpcaoPausa(int deslocamento) {
        if (!pausado) return;
        opcaoPausaSelecionada = Math.floorMod(opcaoPausaSelecionada + deslocamento, OPCOES_PAUSA.length);
        opcaoPausaSobMouse = -1;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    public void confirmarOpcaoPausa() {
        if (pausado && acaoMenuPausa != null) {
            acaoMenuPausa.accept(opcaoPausaSelecionada);
        }
    }

    private Rectangle painelPausa() {
        int centroY = (DimensoesJogo.CAMPO_TOPO + DimensoesJogo.CAMPO_FUNDO) / 2;
        return new Rectangle(DimensoesJogo.CAMPO_MEIO_X - 180, centroY - 172, 360, 344);
    }

    private Rectangle botaoPausa(int opcao) {
        Rectangle painel = painelPausa();
        return new Rectangle(painel.x + 30, painel.y + 112 + opcao * 58, 300, 46);
    }

    private int opcaoPausaNoPonto(int telaX, int telaY) {
        int campoX = telaX - deslocamentoCampoX();
        int campoY = telaY - deslocamentoCampoY();
        for (int i = 0; i < OPCOES_PAUSA.length; i++) {
            if (botaoPausa(i).contains(campoX, campoY)) return i;
        }
        return -1;
    }

    public boolean verificarGol() {
        return verificarGol(bola.x, bola.y);
    }

    public boolean verificarGol(double xAnterior, double yAnterior) {
        if (!emPartida || pausado) return false;

        // Usa a borda de trás da bola: ela precisa entrar por inteiro.
        double limiteEsquerdo = DimensoesJogo.CAMPO_ESQUERDA - bola.tamanho;
        boolean golDoBot = bola.x <= limiteEsquerdo;
        boolean golDoPlayer = bola.x >= DimensoesJogo.CAMPO_DIREITA;
        if (!golDoBot && !golDoPlayer) return false;

        double linhaCruzada = golDoBot ? limiteEsquerdo : DimensoesJogo.CAMPO_DIREITA;
        double alturaNoCruzamento = bola.y;
        boolean cruzouNestePasso = golDoBot ? xAnterior > linhaCruzada : xAnterior < linhaCruzada;
        if (cruzouNestePasso) {
            // Em chutes diagonais, a posição no fim do passo pode já estar
            // fora da altura do gol. Vale a altura no instante do cruzamento.
            double fracaoDoPasso = (linhaCruzada - xAnterior) / (bola.x - xAnterior);
            alturaNoCruzamento = yAnterior + (bola.y - yAnterior) * fracaoDoPasso;
        }
        if (alturaNoCruzamento < DimensoesJogo.GOL_TOPO
                || alturaNoCruzamento + bola.tamanho > DimensoesJogo.GOL_FUNDO) return false;

        if (golDoBot) golsBot++;
        else golsP1++;
        resetarBola();
        return true;
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
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.translate(deslocamentoCampoX(), deslocamentoCampoY());

        if (!emPartida) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 42));
            g2d.drawString("FUT PONG", 295, 200);
            g2d.drawRect(DimensoesJogo.BOTAO_JOGAR_X, DimensoesJogo.BOTAO_JOGAR_Y, DimensoesJogo.BOTAO_JOGAR_LARGURA, DimensoesJogo.BOTAO_JOGAR_ALTURA);
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("JOGAR", 355, 312);
            g2d.setColor(new Color(160, 171, 176));
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 13));
            desenharTextoCentralizado(g2d, "Durante a partida, Esc abre o menu de pausa.",
                    DimensoesJogo.CAMPO_MEIO_X, 355);
            g2d.dispose();
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
        g2d.drawRect(DimensoesJogo.AREA_DIREITA_X, DimensoesJogo.AREA_TOPO, DimensoesJogo.AREA_LARGURA, DimensoesJogo.AREA_ALTURA);
        desenharGol(g2d, true);
        desenharGol(g2d, false);

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
            int xTexto = DimensoesJogo.CAMPO_MEIO_X - fm.stringWidth(texto) / 2;
            int centroCampoY = (DimensoesJogo.CAMPO_TOPO + DimensoesJogo.CAMPO_FUNDO) / 2;
            int yTexto = centroCampoY - fm.getHeight() / 2 + fm.getAscent() - 20;

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

        desenharPlacar(g2d);

        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(Color.WHITE);

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

        g2d.setColor(new Color(125, 138, 144));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        desenharTextoCentralizado(g2d, "Esc · pausar", DimensoesJogo.CAMPO_MEIO_X,
                DimensoesJogo.CAMPO_FUNDO + 24);

        if (pausado) desenharMenuPausa(g2d);
        g2d.dispose();
    }

    private void desenharMenuPausa(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 185));
        g2d.fillRect(-deslocamentoCampoX(), -deslocamentoCampoY(), getWidth(), getHeight());

        Rectangle painel = painelPausa();
        int centroX = painel.x + painel.width / 2;
        Color destaque = new Color(60, 220, 145);
        desenharPainelPlacar(g2d, painel.x, painel.y, painel.width, painel.height,
                new Color(17, 21, 24), destaque);

        g2d.setColor(destaque);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        desenharTextoCentralizado(g2d, "FUT PONG", centroX, painel.y + 31);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 30));
        desenharTextoCentralizado(g2d, "JOGO PAUSADO", centroX, painel.y + 68);
        g2d.setColor(new Color(160, 171, 176));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 13));
        desenharTextoCentralizado(g2d, "A partida continua de onde você parou.", centroX, painel.y + 91);

        for (int i = 0; i < OPCOES_PAUSA.length; i++) {
            Rectangle botao = botaoPausa(i);
            boolean selecionado = i == opcaoPausaSelecionada;
            desenharPainelPlacar(g2d, botao.x, botao.y, botao.width, botao.height,
                    selecionado ? new Color(32, 57, 46) : new Color(25, 31, 35),
                    selecionado ? destaque : new Color(58, 68, 73));
            if (selecionado) {
                g2d.setColor(destaque);
                g2d.fillRoundRect(botao.x + 12, botao.y + 15, 3, 16, 3, 3);
            }
            g2d.setColor(selecionado ? Color.WHITE : new Color(202, 211, 215));
            g2d.setFont(new Font("SansSerif", selecionado ? Font.BOLD : Font.PLAIN, 16));
            desenharTextoCentralizado(g2d, OPCOES_PAUSA[i], centroX, botao.y + 29);
        }

        g2d.setColor(new Color(160, 171, 176));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        desenharTextoCentralizado(g2d, "↑ ↓ selecionar   ·   Enter confirmar", centroX, painel.y + 307);
        desenharTextoCentralizado(g2d, "Esc para continuar   ·   ou clique em uma opção", centroX, painel.y + 326);
    }

    private void desenharPlacar(Graphics2D g2d) {
        Color fundoPainel = new Color(17, 21, 24);
        Color bordaPainel = new Color(58, 68, 73);
        Color textoSecundario = new Color(160, 171, 176);
        Color destaque = new Color(60, 220, 145);

        desenharPainelPlacar(g2d, 10, 8, 180, 42, fundoPainel, bordaPainel);
        desenharPainelPlacar(g2d, 212, 5, 366, 48, fundoPainel, destaque);
        desenharPainelPlacar(g2d, 600, 8, 175, 42, fundoPainel, bordaPainel);

        // Identidade do jogo.
        g2d.setColor(destaque);
        g2d.fillRoundRect(22, 19, 4, 20, 4, 4);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        desenharTextoCentralizado(g2d, "FUT PONG", 106, 36);

        // Resultado: os nomes ficam separados dos números para facilitar a leitura.
        g2d.setColor(textoSecundario);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        desenharTextoCentralizado(g2d, "P1", 307, 19);
        desenharTextoCentralizado(g2d, "BOT", 483, 19);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 25));
        desenharTextoCentralizado(g2d, String.valueOf(golsP1), 307, 44);
        desenharTextoCentralizado(g2d, "-", 395, 42);
        desenharTextoCentralizado(g2d, String.valueOf(golsBot), 483, 44);

        // Situação da partida e cronômetro.
        if (partidaTerminada) {
            g2d.setColor(destaque);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            desenharTextoCentralizado(g2d, "FIM DE JOGO", 687, 35);
        } else {
            String tempoFormatado = String.format("%02d:%02d", minPlacar, segPlacar);
            g2d.setColor(textoSecundario);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 9));
            desenharTextoCentralizado(g2d, periodoPlacar + "º TEMPO", 687, 21);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
            desenharTextoCentralizado(g2d, tempoFormatado, 687, 42);
        }
    }

    private void desenharPainelPlacar(Graphics2D g2d, int x, int y, int largura, int altura,
                                      Color fundo, Color borda) {
        g2d.setColor(fundo);
        g2d.fillRoundRect(x, y, largura, altura, 14, 14);

        g2d.setColor(borda);
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawRoundRect(x, y, largura, altura, 14, 14);
    }

    private void desenharTextoCentralizado(Graphics2D g2d, String texto, int centroX, int linhaBase) {
        FontMetrics metricas = g2d.getFontMetrics();
        g2d.drawString(texto, centroX - metricas.stringWidth(texto) / 2, linhaBase);
    }

    private void desenharGol(Graphics2D g2d, boolean esquerda) {
        int linhaDoGolX = esquerda
                ? DimensoesJogo.CAMPO_ESQUERDA
                : DimensoesJogo.CAMPO_DIREITA;
        int fundoDaRedeX = esquerda
                ? linhaDoGolX - DimensoesJogo.GOL_PROFUNDIDADE
                : linhaDoGolX + DimensoesJogo.GOL_PROFUNDIDADE;
        int inicioX = Math.min(linhaDoGolX, fundoDaRedeX);
        int fimX = Math.max(linhaDoGolX, fundoDaRedeX);

        // Malha da rede. A frente fica aberta para a bola entrar.
        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.setStroke(new BasicStroke(1));
        for (int x = inicioX + 9; x < fimX; x += 9) {
            g2d.drawLine(x, DimensoesJogo.GOL_TOPO, x, DimensoesJogo.GOL_FUNDO);
        }
        for (int y = DimensoesJogo.GOL_TOPO + 10; y < DimensoesJogo.GOL_FUNDO; y += 10) {
            g2d.drawLine(inicioX, y, fimX, y);
        }

        // Laterais e fundo da rede, sem fechar a boca do gol.
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(linhaDoGolX, DimensoesJogo.GOL_TOPO, fundoDaRedeX, DimensoesJogo.GOL_TOPO);
        g2d.drawLine(linhaDoGolX, DimensoesJogo.GOL_FUNDO, fundoDaRedeX, DimensoesJogo.GOL_FUNDO);
        g2d.drawLine(fundoDaRedeX, DimensoesJogo.GOL_TOPO, fundoDaRedeX, DimensoesJogo.GOL_FUNDO);

        // Pequenos círculos deixam as duas traves bem visíveis.
        int tamanhoTrave = DimensoesJogo.TRAVE_RAIO * 2;
        g2d.fillOval(linhaDoGolX - tamanhoTrave / 2,
                DimensoesJogo.GOL_TOPO - tamanhoTrave / 2,
                tamanhoTrave, tamanhoTrave);
        g2d.fillOval(linhaDoGolX - tamanhoTrave / 2,
                DimensoesJogo.GOL_FUNDO - tamanhoTrave / 2,
                tamanhoTrave, tamanhoTrave);
        g2d.setStroke(new BasicStroke(1));
    }

    public void reiniciarPartida() {
        this.emPartida = true;
        this.golsP1 = 0;
        this.golsBot = 0;
        definirPausado(false);
        resetarBola();
        atualizarCronometro(1, 0, 0, false);
        atualizarTextoFade(1, 0.25f);
        repaint();
    }

    // Também usado pela opção de sair da partida no menu de pausa.
    public void resetarParaMenu() {
        this.emPartida = false;
        this.golsP1 = 0;
        this.golsBot = 0;
        definirPausado(false);
        resetarBola();
        atualizarCronometro(1, 0, 0, false);
        atualizarTextoFade(1, 0.25f);
        repaint();
    }
}
