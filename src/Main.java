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

    private int velocidadeSeuJogador = 4;
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

                // --- SISTEMA ANTI-TRAVAMENTO REFORÇADO (ZONA DE EJEÇÃO DOS CANTOS) ---
                // Se a bola estiver muito próxima das quinas extremas do campo, força um empurrão para o centro
                if (bola.x < 50 && bola.y < 100) { // Canto Superior Esquerdo
                    bola.velX += 0.8;
                    bola.velY += 0.8;
                } else if (bola.x < 50 && bola.y > 540) { // Canto Inferior Esquerdo
                    bola.velX += 0.8;
                    bola.velY -= 0.8;
                } else if (bola.x > 720 && bola.y < 100) { // Canto Superior Direita
                    bola.velX -= 0.8;
                    bola.velY += 0.8;
                } else if (bola.x > 720 && bola.y > 540) { // Canto Inferior Direita
                    bola.velX -= 0.8;
                    bola.velY -= 0.8;
                }

                if (cooldownChuteBot > 0) cooldownChuteBot--;

                // --- MOVIMENTAÇÃO PLAYER ---
                int oldX = cenario.linhaEsquerda.x;
                int oldY = cenario.linhaEsquerda.y;

                if (teclaW) cenario.linhaEsquerda.y -= velocidadeSeuJogador;
                if (teclaS) cenario.linhaEsquerda.y += velocidadeSeuJogador;
                if (teclaA) cenario.linhaEsquerda.x -= velocidadeSeuJogador;
                if (teclaD) cenario.linhaEsquerda.x += velocidadeSeuJogador;

                if (estaForaDoCampo(cenario.linhaEsquerda) || isNaAreaProibida(cenario.linhaEsquerda)) {
                    cenario.linhaEsquerda.x = oldX;
                    cenario.linhaEsquerda.y = oldY;
                }

                // ANTI-PRISÃO PLAYER: Se forçado para dentro da área, ejeta para fora
                if (isNaAreaProibida(cenario.linhaEsquerda)) {
                    if (cenario.linhaEsquerda.x < 400) {
                        cenario.linhaEsquerda.x = 105;
                    } else {
                        cenario.linhaEsquerda.x = 680 - cenario.linhaEsquerda.largura;
                    }
                }

                // --- INTELIGÊNCIA DO GOLEIRO ESQUERDA ---
                boolean bolaNaAreaEsq = (bola.x >= 10 && bola.x <= 100 && bola.y >= 195 && bola.y <= 465);

                if (bolaNaAreaEsq) {
                    if (cenario.goleiroEsquerda.x < bola.x) cenario.goleiroEsquerda.x += velocidadeGoleiro;
                    if (cenario.goleiroEsquerda.x > bola.x) cenario.goleiroEsquerda.x -= velocidadeGoleiro;

                    if (bola.y > cenario.goleiroEsquerda.y + 20) cenario.goleiroEsquerda.y += velocidadeGoleiro;
                    else if (bola.y < cenario.goleiroEsquerda.y + 20) cenario.goleiroEsquerda.y -= velocidadeGoleiro;
                } else {
                    if (bola.y > cenario.goleiroEsquerda.y + 20 && cenario.goleiroEsquerda.y < 420) cenario.goleiroEsquerda.y += velocidadeGoleiro;
                    else if (bola.y < cenario.goleiroEsquerda.y + 20 && cenario.goleiroEsquerda.y > 195) cenario.goleiroEsquerda.y -= velocidadeGoleiro;

                    if (bola.x > 250) {
                        if (cenario.goleiroEsquerda.x < 80) cenario.goleiroEsquerda.x += 2;
                    } else {
                        if (cenario.goleiroEsquerda.x > 30) cenario.goleiroEsquerda.x -= 2;
                    }
                }

                // --- INTELIGÊNCIA DO GOLEIRO DIREITA ---
                boolean bolaNaAreaDir = (bola.x >= 685 && bola.x <= 775 && bola.y >= 195 && bola.y <= 465);

                if (bolaNaAreaDir) {
                    if (cenario.goleiroDireita.x < bola.x) cenario.goleiroDireita.x += velocidadeGoleiro;
                    if (cenario.goleiroDireita.x > bola.x) cenario.goleiroDireita.x -= velocidadeGoleiro;

                    if (bola.y > cenario.goleiroDireita.y + 20) cenario.goleiroDireita.y += velocidadeGoleiro;
                    else if (bola.y < cenario.goleiroDireita.y + 20) cenario.goleiroDireita.y -= velocidadeGoleiro;
                } else {
                    if (bola.y > cenario.goleiroDireita.y + 20 && cenario.goleiroDireita.y < 420) cenario.goleiroDireita.y += velocidadeGoleiro;
                    else if (bola.y < cenario.goleiroDireita.y + 20 && cenario.goleiroDireita.y > 195) cenario.goleiroDireita.y -= velocidadeGoleiro;

                    if (bola.x < 550) {
                        if (cenario.goleiroDireita.x > 690) cenario.goleiroDireita.x -= 2;
                    } else {
                        if (cenario.goleiroDireita.x < 740) cenario.goleiroDireita.x += 2;
                    }
                }

                if (cenario.goleiroEsquerda.x < 10) cenario.goleiroEsquerda.x = 10;
                if (cenario.goleiroEsquerda.x > 100) cenario.goleiroEsquerda.x = 100;
                if (cenario.goleiroDireita.x < 685) cenario.goleiroDireita.x = 685;
                if (cenario.goleiroDireita.x > 775) cenario.goleiroDireita.x = 775;

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

                if (isNaAreaProibida(cenario.linhaDireita)) {
                    if (cenario.linhaDireita.x > 400) {
                        cenario.linhaDireita.x = 680 - cenario.linhaDireita.largura;
                    } else {
                        cenario.linhaDireita.x = 105;
                    }
                }

                // --- INTELIGÊNCIA DE CHUTE DO BOT COM PROTEÇÃO DE CANTO ---
                double distBotBola = Math.hypot(cenario.linhaDireita.x - bola.x, cenario.linhaDireita.y - bola.y);
                double distBotPlayer = Math.hypot(cenario.linhaDireita.x - cenario.linhaEsquerda.x, cenario.linhaDireita.y - cenario.linhaEsquerda.y);

                boolean bolaNoCanto = (bola.x < 45 || bola.x > 735 || bola.y < 95 || bola.y > 565);

                if (bolaNoCanto && distBotBola < 60) {
                    bola.velX = (bola.x < 400) ? 5.0 : -5.0;
                    bola.velY = (bola.y < 300) ? 4.5 : -4.5;
                }

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

    private boolean estaForaDoCampo(Jogador j) {
        if (j.x < 10 || j.x > (775 - j.largura) || j.y < 60 || j.y > (600 - j.altura)) {
            return true;
        }

        int raioCurva = 50;
        double centroJogadorX = j.x + j.largura / 2.0;
        double centroJogadorY = j.y + j.altura / 2.0;
        double limiteColisao = raioCurva - (j.largura / 2.0);

        if (centroJogadorX < 10 + raioCurva && centroJogadorY < 60 + raioCurva) {
            double dx = centroJogadorX - (10 + raioCurva);
            double dy = centroJogadorY - (60 + raioCurva);
            if (Math.hypot(dx, dy) > limiteColisao) {
                ajustarJogadorNaCurva(j, 10 + raioCurva, 60 + raioCurva, limiteColisao);
            }
        }
        else if (centroJogadorX < 10 + raioCurva && centroJogadorY > 600 - raioCurva) {
            double dx = centroJogadorX - (10 + raioCurva);
            double dy = centroJogadorY - (600 - raioCurva);
            if (Math.hypot(dx, dy) > limiteColisao) {
                ajustarJogadorNaCurva(j, 10 + raioCurva, 600 - raioCurva, limiteColisao);
            }
        }
        else if (centroJogadorX > 775 - raioCurva && centroJogadorY < 60 + raioCurva) {
            double dx = centroJogadorX - (775 - raioCurva);
            double dy = centroJogadorY - (60 + raioCurva);
            if (Math.hypot(dx, dy) > limiteColisao) {
                ajustarJogadorNaCurva(j, 775 - raioCurva, 60 + raioCurva, limiteColisao);
            }
        }
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

            double dx = centroBolaX - centroJogadorX;
            double dy = centroBolaY - centroJogadorY;
            double distancia = Math.hypot(dx, dy);

            if (distancia == 0) {
                dx = 1;
                dy = 1;
                distancia = Math.hypot(dx, dy);
            }

            b.velX = (dx / distancia) * 4.5 * multiplicador;
            b.velY = (dy / distancia) * 4.5 * multiplicador;

            if (b.x < 45 || b.x > 735) {
                b.velY += (b.velY >= 0) ? 2.5 : -2.5;
            }
            if (b.y < 90 || b.y > 570) {
                b.velX += (b.velX >= 0) ? 2.5 : -2.5;
            }

            double sobreposicaoX = (j.largura / 2.0 + b.tamanho / 2.0) - Math.abs(dx);
            double sobreposicaoY = (j.altura / 2.0 + b.tamanho / 2.0) - Math.abs(dy);

            if (sobreposicaoX > 0 && sobreposicaoY > 0) {
                if (sobreposicaoX < sobreposicaoY) {
                    b.x += (dx > 0) ? sobreposicaoX : -sobreposicaoX;
                } else {
                    b.y += (dy > 0) ? sobreposicaoY : -sobreposicaoY;
                }
            }
        }
    }

    private boolean checarColisaoJogadores(Jogador j1, Jogador j2) {
        return new Rectangle(j1.x, j1.y, j1.largura, j1.altura).intersects(new Rectangle(j2.x, j2.y, j2.largura, j2.altura));
    }

    public static void main(String[] args) { new Main().setVisible(true); }
}