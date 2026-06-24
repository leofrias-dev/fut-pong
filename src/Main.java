import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Main extends JFrame {

    private Cenario cenario;
    private Bola bola;
    private Timer timer;

    private boolean teclaW, teclaA, teclaS, teclaD;

    private boolean pisaAtivo = false;
    private int raioAura = 0;

    private boolean botChutando = false;
    private int raioAuraBot = 0;
    private int cooldownChuteBot = 0;

    private int velocidadSeuJogador = 4;
    private int velocidadeGoleiro = 3;
    private int velocidadeLinhaDireita = 3;

    private int tempoDesvioBot = 0;
    private int direcaoDesvioY = 0;

    public Main() {
        setTitle("Fut-Pong");
        setSize(800, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        bola = new Bola();
        cenario = new Cenario(bola);
        add(cenario);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int c = e.getKeyCode();
                if (c == KeyEvent.VK_W) teclaW = true;
                if (c == KeyEvent.VK_A) teclaA = true;
                if (c == KeyEvent.VK_S) teclaS = true;
                if (c == KeyEvent.VK_D) teclaD = true;

                if (c == KeyEvent.VK_SPACE && !pisaAtivo) {
                    pisaAtivo = true;
                    raioAura = 0;
                    executarChutePlayer();
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                int c = e.getKeyCode();
                if (c == KeyEvent.VK_W) teclaW = false;
                if (c == KeyEvent.VK_A) teclaA = false;
                if (c == KeyEvent.VK_S) teclaS = false;
                if (c == KeyEvent.VK_D) teclaD = false;
            }
        });

        timer = new Timer(16, e -> {
            if (cenario.jogoRodando()) {
                bola.mexer();
                aplicarFisicaCurvaCenario();

                if (cooldownChuteBot > 0) cooldownChuteBot--;

                // --- MOVIMENTAÇÃO PLAYER ---
                int oldX = cenario.linhaEsquerda.x;
                int oldY = cenario.linhaEsquerda.y;

                if (teclaW) cenario.linhaEsquerda.y -= velocidadSeuJogador;
                if (teclaS) cenario.linhaEsquerda.y += velocidadSeuJogador;
                if (teclaA) cenario.linhaEsquerda.x -= velocidadSeuJogador;
                if (teclaD) cenario.linhaEsquerda.x += velocidadSeuJogador;

                if (estaForaDoCampo(cenario.linhaEsquerda) || isNaAreaProibida(cenario.linhaEsquerda)) {
                    cenario.linhaEsquerda.x = oldX;
                    cenario.linhaEsquerda.y = oldY;
                }

                // --- GOLEIRO ESQUERDA INTELIGENTE ---
                if (bola.y > cenario.goleiroEsquerda.y + 20 && cenario.goleiroEsquerda.y < 420) cenario.goleiroEsquerda.y += velocidadeGoleiro;
                else if (bola.y < cenario.goleiroEsquerda.y + 20 && cenario.goleiroEsquerda.y > 195) cenario.goleiroEsquerda.y -= velocidadeGoleiro;

                if (bola.x < 150) {
                    if (cenario.goleiroEsquerda.x < 80) cenario.goleiroEsquerda.x += 2;
                } else {
                    if (cenario.goleiroEsquerda.x > 30) cenario.goleiroEsquerda.x -= 2;
                }

                // --- GOLEIRO DIREITA INTELIGENTE ---
                if (bola.y > cenario.goleiroDireita.y + 20 && cenario.goleiroDireita.y < 420) cenario.goleiroDireita.y += velocidadeGoleiro;
                else if (bola.y < cenario.goleiroDireita.y + 20 && cenario.goleiroDireita.y > 195) cenario.goleiroDireita.y -= velocidadeGoleiro;

                if (bola.x > 650) {
                    if (cenario.goleiroDireita.x > 690) cenario.goleiroDireita.x -= 2;
                } else {
                    if (cenario.goleiroDireita.x < 740) cenario.goleiroDireita.x += 2;
                }

                // --- BOT MOVIMENTO INTELIGENTE ---
                int oldBotX = cenario.linhaDireita.x;
                int oldBotY = cenario.linhaDireita.y;
                int destinoX = cenario.linhaDireita.x;
                int destinoY = cenario.linhaDireita.y;

                if (tempoDesvioBot > 0) {
                    destinoY += direcaoDesvioY * (velocidadeLinhaDireita + 1);
                    tempoDesvioBot--;
                } else {
                    if (Math.abs(bola.y - destinoY) > 5) {
                        if (bola.y > destinoY) destinoY += velocidadeLinhaDireita;
                        else if (bola.y < destinoY) destinoY -= velocidadeLinhaDireita;
                    }
                    if (bola.x > destinoX + 5) destinoX += (velocidadeLinhaDireita - 1);
                    else if (bola.x < destinoX - 5) destinoX -= (velocidadeLinhaDireita - 1);
                }

                Jogador tentX = new Jogador(destinoX, cenario.linhaDireita.y, "direita");
                if (!estaForaDoCampo(tentX) && !isNaAreaProibida(tentX)) cenario.linhaDireita.x = destinoX;

                Jogador tentY = new Jogador(cenario.linhaDireita.x, destinoY, "direita");
                if (!estaForaDoCampo(tentY) && !isNaAreaProibida(tentY)) cenario.linhaDireita.y = destinoY;

                // --- INTELIGÊNCIA DE CHUTE DO BOT ---
                double distBotBola = Math.hypot(cenario.linhaDireita.x - bola.x, cenario.linhaDireita.y - bola.y);
                double distBotPlayer = Math.hypot(cenario.linhaDireita.x - cenario.linhaEsquerda.x, cenario.linhaDireita.y - cenario.linhaEsquerda.y);
                boolean bolaNoCanto = (bola.x < 35 || bola.x > 745 || bola.y < 85 || bola.y > 575);

                if (cooldownChuteBot == 0 && !botChutando && !bolaNoCanto) {
                    if (distBotBola < 55 || distBotPlayer < 75) {
                        botChutando = true;
                        raioAuraBot = 0;
                        cooldownChuteBot = 90;
                        executarChuteBot();
                    }
                }

                // --- COLISÃO ENTRE JOGADORES DE LINHA ---
                if (checarColisaoJogadores(cenario.linhaEsquerda, cenario.linhaDireita)) {
                    cenario.linhaEsquerda.x = oldX;
                    cenario.linhaEsquerda.y = oldY;
                    if (tempoDesvioBot <= 0) {
                        tempoDesvioBot = 35;
                        direcaoDesvioY = (cenario.linhaDireita.y >= cenario.linhaEsquerda.y) ? 1 : -1;
                    }
                }

                // --- CONDUÇÃO ---
                if (!pisaAtivo) aplicarColisaoFisica(cenario.linhaEsquerda, bola, 1.0);
                aplicarColisaoFisica(cenario.goleiroEsquerda, bola, 1.1);
                aplicarColisaoFisica(cenario.goleiroDireita, bola, 1.1);
                aplicarColisaoFisica(cenario.linhaDireita, bola, 1.0);

                cenario.verificarGol();

                // --- ANIMAR AURAS ---
                if (pisaAtivo) {
                    raioAura += 6;
                    cenario.configurarAnimacaoAura(cenario.linhaEsquerda.x, cenario.linhaEsquerda.y, raioAura, true);
                    if (raioAura >= 80) {
                        pisaAtivo = false;
                        raioAura = 0;
                        cenario.configurarAnimacaoAura(0, 0, 0, false);
                    }
                }

                if (botChutando) {
                    raioAuraBot += 6;
                    cenario.configurarAnimacaoAuraBot(cenario.linhaDireita.x, cenario.linhaDireita.y, raioAuraBot, true);
                    if (raioAuraBot >= 80) {
                        botChutando = false;
                        raioAuraBot = 0;
                        cenario.configurarAnimacaoAuraBot(0, 0, 0, false);
                    }
                }
            }
            cenario.repaint();
        });
        timer.start();
    }

    // --- FÍSICA CURVA DA BOLA ---
    private void aplicarFisicaCurvaCenario() {
        int raioCurva = 50;
        double centroBolaX = bola.x + bola.tamanho / 2.0;
        double centroBolaY = bola.y + bola.tamanho / 2.0;

        verificarCantoRedondo(centroBolaX, centroBolaY, 10 + raioCurva, 60 + raioCurva, raioCurva, "sup_esq");
        verificarCantoRedondo(centroBolaX, centroBolaY, 10 + raioCurva, 600 - raioCurva, raioCurva, "inf_esq");
        verificarCantoRedondo(centroBolaX, centroBolaY, 775 - raioCurva, 60 + raioCurva, raioCurva, "sup_dir");
        verificarCantoRedondo(centroBolaX, centroBolaY, 775 - raioCurva, 600 - raioCurva, raioCurva, "inf_dir");
    }

    private void verificarCantoRedondo(double bx, double by, double cx, double cy, double raio, String canto) {
        double dx = bx - cx;
        double dy = by - cy;
        double distanciaDoCentroDaCurva = Math.hypot(dx, dy);
        double limiteColisao = raio - (bola.tamanho / 2.0);

        if (distanciaDoCentroDaCurva > limiteColisao) {
            boolean naAreaDoCanto = false;
            if (canto.equals("sup_esq") && dx < 0 && dy < 0) naAreaDoCanto = true;
            if (canto.equals("inf_esq") && dx < 0 && dy > 0) naAreaDoCanto = true;
            if (canto.equals("sup_dir") && dx > 0 && dy < 0) naAreaDoCanto = true;
            if (canto.equals("inf_dir") && dx > 0 && dy > 0) naAreaDoCanto = true;

            if (naAreaDoCanto) {
                double angulo = Math.atan2(dy, dx);
                bola.x = (cx + Math.cos(angulo) * limiteColisao) - (bola.tamanho / 2.0);
                bola.y = (cy + Math.sin(angulo) * limiteColisao) - (bola.tamanho / 2.0);

                double dotProduct = bola.velX * Math.cos(angulo) + bola.velY * Math.sin(angulo);
                if (dotProduct > 0) {
                    bola.velX = (bola.velX - 2 * dotProduct * Math.cos(angulo)) * 0.8;
                    bola.velY = (bola.velY - 2 * dotProduct * Math.sin(angulo)) * 0.8;
                }
            }
        }
    }

    private void executarChutePlayer() {
        double forcaImpacto = 16.0;
        double dxBola = bola.x - cenario.linhaEsquerda.x;
        double dyBola = bola.y - cenario.linhaEsquerda.y;
        double distBola = Math.hypot(dxBola, dyBola);

        if (distBola < 80) {
            bola.velX = (dxBola / distBola) * forcaImpacto;
            bola.velY = (dyBola / distBola) * forcaImpacto;
        }

        double dxBot = cenario.linhaDireita.x - cenario.linhaEsquerda.x;
        double dyBot = cenario.linhaDireita.y - cenario.linhaEsquerda.y;
        double distBot = Math.hypot(dxBot, dyBot);

        if (distBot < 80) {
            int novoX = cenario.linhaDireita.x + (int) ((dxBot / distBot) * 40);
            int novoY = cenario.linhaDireita.y + (int) ((dyBot / distBot) * 40);
            Jogador testeBot = new Jogador(novoX, novoY, "direita");
            if (!estaForaDoCampo(testeBot)) {
                cenario.linhaDireita.x = novoX;
                cenario.linhaDireita.y = novoY;
            }
        }
    }

    private void executarChuteBot() {
        double forcaImpacto = 13.0;
        double dxBola = bola.x - cenario.linhaDireita.x;
        double dyBola = bola.y - cenario.linhaDireita.y;
        double distBola = Math.hypot(dxBola, dyBola);

        if (distBola < 80) {
            bola.velX = (dxBola / distBola) * forcaImpacto;
            bola.velY = (dyBola / distBola) * forcaImpacto;
        }

        double dxPlayer = cenario.linhaEsquerda.x - cenario.linhaDireita.x;
        double dyPlayer = cenario.linhaEsquerda.y - cenario.linhaDireita.y;
        double distPlayer = Math.hypot(dxPlayer, dyPlayer);

        if (distPlayer < 80) {
            int novoX = cenario.linhaEsquerda.x + (int) ((dxPlayer / distPlayer) * 40);
            int novoY = cenario.linhaEsquerda.y + (int) ((dyPlayer / distPlayer) * 40);
            Jogador testeP1 = new Jogador(novoX, novoY, "esquerda");
            if (!estaForaDoCampo(testeP1)) {
                cenario.linhaEsquerda.x = novoX;
                cenario.linhaEsquerda.y = novoY;
            }
        }
    }

    private boolean isNaAreaProibida(Jogador j) {
        Rectangle rectJogador = new Rectangle(j.x, j.y, j.largura, j.altura);
        Rectangle areaEsquerda = new Rectangle(10, 195, 90, 270);
        Rectangle areaDireita = new Rectangle(685, 195, 90, 270);
        return rectJogador.intersects(areaEsquerda) || rectJogador.intersects(areaDireita);
    }

    // --- COLISÃO CURVA DOS JOGADORES ---
    private boolean estaForaDoCampo(Jogador j) {
        // 1. Limite retangular padrão
        if (j.x < 10 || j.x > (775 - j.largura) || j.y < 60 || j.y > (600 - j.altura)) {
            return true;
        }

        // 2. Deslizar jogador pelos arcos das quinas curvas
        int raioCurva = 50;
        double centroJogadorX = j.x + j.largura / 2.0;
        double centroJogadorY = j.y + j.altura / 2.0;
        double limiteColisao = raioCurva - (j.largura / 2.0);

        // Quina Superior Esquerda
        if (centroJogadorX < 10 + raioCurva && centroJogadorY < 60 + raioCurva) {
            double dx = centroJogadorX - (10 + raioCurva);
            double dy = centroJogadorY - (60 + raioCurva);
            if (Math.hypot(dx, dy) > limiteColisao) {
                ajustarJogadorNaCurva(j, 10 + raioCurva, 60 + raioCurva, limiteColisao);
            }
        }
        // Quina Inferior Esquerda
        else if (centroJogadorX < 10 + raioCurva && centroJogadorY > 600 - raioCurva) {
            double dx = centroJogadorX - (10 + raioCurva);
            double dy = centroJogadorY - (600 - raioCurva);
            if (Math.hypot(dx, dy) > limiteColisao) {
                ajustarJogadorNaCurva(j, 10 + raioCurva, 600 - raioCurva, limiteColisao);
            }
        }
        // Quina Superior Direito
        else if (centroJogadorX > 775 - raioCurva && centroJogadorY < 60 + raioCurva) {
            double dx = centroJogadorX - (775 - raioCurva);
            double dy = centroJogadorY - (60 + raioCurva);
            if (Math.hypot(dx, dy) > limiteColisao) {
                ajustarJogadorNaCurva(j, 775 - raioCurva, 60 + raioCurva, limiteColisao);
            }
        }
        // Quina Inferior Direito
        else if (centroJogadorX > 775 - raioCurva && centroJogadorY > 600 - raioCurva) {
            double dx = centroJogadorX - (775 - raioCurva);
            double dy = centroJogadorY - (600 - raioCurva);
            if (Math.hypot(dx, dy) > limiteColisao) {
                ajustarJogadorNaCurva(j, 775 - raioCurva, 600 - raioCurva, limiteColisao);
            }
        }

        return false;
    }

    private void ajustarJogadorNaCurva(Jogador j, double cx, double cy, double limite) {
        double centroJogadorX = j.x + j.largura / 2.0;
        double centroJogadorY = j.y + j.altura / 2.0;

        double dx = centroJogadorX - cx;
        double dy = centroJogadorY - cy;
        double angulo = Math.atan2(dy, dx);

        j.x = (int) ((cx + Math.cos(angulo) * limite) - (j.largura / 2.0));
        j.y = (int) ((cy + Math.sin(angulo) * limite) - (j.altura / 2.0));
    }

    private void aplicarColisaoFisica(Jogador j, Bola b, double multiplicador) {
        Rectangle rectBola = b.getLimites();
        Rectangle rectJogador = new Rectangle(j.x, j.y, j.largura, j.altura);

        if (rectBola.intersects(rectJogador)) {
            double centroBolaX = b.x + (b.tamanho / 2.0);
            double centroBolaY = b.y + (b.tamanho / 2.0);
            double centroJogadorX = j.x + (j.largura / 2.0);
            double centroJogadorY = j.y + (j.altura / 2.0);

            b.velX = (centroBolaX - centroJogadorX) * 0.28 * multiplicador;
            b.velY = (centroBolaY - centroJogadorY) * 0.28 * multiplicador;
        }
    }

    private boolean checarColisaoJogadores(Jogador j1, Jogador j2) {
        return new Rectangle(j1.x, j1.y, j1.largura, j1.altura).intersects(new Rectangle(j2.x, j2.y, j2.largura, j2.altura));
    }

    public static void main(String[] args) { new Main().setVisible(true); }
}