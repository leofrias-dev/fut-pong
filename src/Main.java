import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


public class Main extends JFrame {

    private Cenario cenario;
    private Bola bola;
    private FisicaJogo fisicaJogo;
    private Timer timer;

    private boolean teclaW, teclaA, teclaS, teclaD;

    private boolean pisaAtivo = false;
    private int raioAura = 0;

    private boolean botChutando = false;
    private int raioAuraBot = 0;
    private int cooldownChuteBot = 0;

    // --- VELOCIDADES ATUALIZADAS ---
    private int velocidadeSeuJogador = 5;   // Você um pouco mais rápido (era 4)
    private int velocidadeGoleiro = 3;
    private int velocidadeLinhaDireita = 4; // Bot mais rápido, mas menor que a sua (era 3)

    private int tempoDesvioBot = 0;
    private int direcaoDesvioY = 0;
    private int tempoRecuoParedeBot = 0;

    private int periodoAtual = 1;
    private int minutosVirtuais = 0;
    private int segundosVirtuais = 0;
    private double acumuladorMilis = 0;
    private boolean fimDeJogo = false;

    // Variável para contar o tempo de espera no final do jogo
    private int framesEsperaFimDeJogo = 0;

    private float alphaTextoTempo = 0.25f;

    public Main() {
        setTitle("Fut-Pong");
        setSize(DimensoesJogo.TELA_LARGURA, DimensoesJogo.TELA_ALTURA);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        bola = new Bola();
        cenario = new Cenario(bola);
        fisicaJogo = new FisicaJogo(cenario, bola);
        add(cenario);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (fimDeJogo) return;

                int c = e.getKeyCode();
                if (c == KeyEvent.VK_W) teclaW = true;
                if (c == KeyEvent.VK_A) teclaA = true;
                if (c == KeyEvent.VK_S) teclaS = true;
                if (c == KeyEvent.VK_D) teclaD = true;

                if (c == KeyEvent.VK_SPACE && !pisaAtivo) {
                    pisaAtivo = true;
                    raioAura = 0;
                    executarPisaoPlayer();
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

                // Se o jogo NÃO acabou, roda a física normalmente
                if (!fimDeJogo) {
                    if (fisicaJogo.atualizar()) {
                        tempoRecuoParedeBot = 35;
                    }

                    acumuladorMilis += 16 * 45;

                    if (acumuladorMilis >= 1000) {
                        segundosVirtuais += (int)(acumuladorMilis / 1000);
                        acumuladorMilis = acumuladorMilis % 1000;
                    }

                    if (segundosVirtuais >= 60) {
                        minutosVirtuais += segundosVirtuais / 60;
                        segundosVirtuais = segundosVirtuais % 60;
                    }

                    if (minutosVirtuais >= 45) {
                        minutosVirtuais = 45;
                        segundosVirtuais = 0;

                        if (periodoAtual == 1) {
                            periodoAtual = 2;
                            minutosVirtuais = 0;
                            segundosVirtuais = 0;
                            acumuladorMilis = 0;
                            alphaTextoTempo = 0.25f;

                            bola.x = DimensoesJogo.BOLA_INICIAL_X;
                            bola.y = DimensoesJogo.BOLA_INICIAL_Y;
                            bola.velX = 0;
                            bola.velY = 0;
                        } else if (periodoAtual == 2) {
                            fimDeJogo = true;
                        }
                    }

                    if (alphaTextoTempo > 0.0f) {
                        alphaTextoTempo -= 0.0015f;
                        if (alphaTextoTempo < 0.0f) alphaTextoTempo = 0.0f;
                    }

                    if (bola.x < 50 && bola.y < 100) {
                        bola.velX += 0.8;
                        bola.velY += 0.8;
                    } else if (bola.x < 50 && bola.y > 540) {
                        bola.velX += 0.8;
                        bola.velY -= 0.8;
                    } else if (bola.x > 720 && bola.y < 100) {
                        bola.velX -= 0.8;
                        bola.velY += 0.8;
                    } else if (bola.x > 720 && bola.y > 540) {
                        bola.velX -= 0.8;
                        bola.velY -= 0.8;
                    }

                    if (cooldownChuteBot > 0) cooldownChuteBot--;

                    int oldX = cenario.linhaEsquerda.x;
                    int oldY = cenario.linhaEsquerda.y;

                    if (teclaW) cenario.linhaEsquerda.y -= velocidadeSeuJogador;
                    if (teclaS) cenario.linhaEsquerda.y += velocidadeSeuJogador;
                    if (teclaA) cenario.linhaEsquerda.x -= velocidadeSeuJogador;
                    if (teclaD) cenario.linhaEsquerda.x += velocidadeSeuJogador;

                    if (fisicaJogo.estaForaDoCampo(cenario.linhaEsquerda)
                            || fisicaJogo.isNaAreaProibida(cenario.linhaEsquerda)) {
                        cenario.linhaEsquerda.x = oldX;
                        cenario.linhaEsquerda.y = oldY;
                    }

                    if (fisicaJogo.isNaAreaProibida(cenario.linhaEsquerda)) {
                        if (cenario.linhaEsquerda.x < 400) {
                            cenario.linhaEsquerda.x = 105;
                        } else {
                            cenario.linhaEsquerda.x = 680 - cenario.linhaEsquerda.largura;
                        }
                    }

                    boolean bolaNaAreaEsq = (bola.x >= DimensoesJogo.AREA_ESQUERDA_X
                            && bola.x <= DimensoesJogo.AREA_ESQUERDA_X + DimensoesJogo.AREA_LARGURA
                            && bola.y >= DimensoesJogo.AREA_TOPO
                            && bola.y <= DimensoesJogo.AREA_TOPO + DimensoesJogo.AREA_ALTURA);

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

                    boolean bolaNaAreaDir = (bola.x >= DimensoesJogo.AREA_DIREITA_X
                            && bola.x <= DimensoesJogo.CAMPO_DIREITA
                            && bola.y >= DimensoesJogo.AREA_TOPO
                            && bola.y <= DimensoesJogo.AREA_TOPO + DimensoesJogo.AREA_ALTURA);

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

                    fisicaJogo.limitarGoleiroNaArea(
                            cenario.goleiroEsquerda,
                            DimensoesJogo.AREA_ESQUERDA_X,
                            DimensoesJogo.AREA_ESQUERDA_X + DimensoesJogo.AREA_LARGURA
                    );
                    fisicaJogo.limitarGoleiroNaArea(
                            cenario.goleiroDireita,
                            DimensoesJogo.AREA_DIREITA_X,
                            DimensoesJogo.CAMPO_DIREITA
                    );

                    int oldBotX = cenario.linhaDireita.x;
                    int oldBotY = cenario.linhaDireita.y;

                    double alvoBolaX = Math.max(
                            DimensoesJogo.CAMPO_ESQUERDA,
                            Math.min(DimensoesJogo.CAMPO_DIREITA - cenario.linhaDireita.largura,
                                    bola.x + bola.tamanho / 2.0 - cenario.linhaDireita.largura / 2.0)
                    );
                    double alvoBolaY = Math.max(
                            DimensoesJogo.CAMPO_TOPO,
                            Math.min(DimensoesJogo.CAMPO_FUNDO - cenario.linhaDireita.altura,
                                    bola.y + bola.tamanho / 2.0 - cenario.linhaDireita.altura / 2.0)
                    );

                    if (bola.velY < -2 && bola.y < 160) {
                        alvoBolaY = bola.y + 50;
                    } else if (bola.velY > 2 && bola.y > 500) {
                        alvoBolaY = bola.y - 50;
                    }

                    if (bola.y < 85) {
                        alvoBolaY = 95;
                    } else if (bola.y > 575) {
                        alvoBolaY = 565;
                    }

                    double alvoMovimentoX = alvoBolaX;
                    double alvoMovimentoY = alvoBolaY;
                    double velocidadeMovimentoBot = velocidadeLinhaDireita;

                    boolean goleiroDireitoTemPrioridade = bola.x + bola.tamanho
                            >= DimensoesJogo.AREA_DIREITA_X - 10
                            && bola.x <= DimensoesJogo.CAMPO_DIREITA
                            && bola.y + bola.tamanho >= DimensoesJogo.AREA_TOPO
                            && bola.y <= DimensoesJogo.AREA_TOPO + DimensoesJogo.AREA_ALTURA;

                    if (tempoRecuoParedeBot > 0) {
                        // Depois de prensar a bola, afasta-se por alguns instantes.
                        // Isso dá tempo para ela escapar pela lateral da parede.
                        alvoMovimentoX = DimensoesJogo.CAMPO_MEIO_X
                                - cenario.linhaDireita.largura / 2.0;
                        alvoMovimentoY = (DimensoesJogo.CAMPO_TOPO + DimensoesJogo.CAMPO_FUNDO) / 2.0
                                - cenario.linhaDireita.altura / 2.0;
                        velocidadeMovimentoBot = velocidadeLinhaDireita + 1;
                        tempoRecuoParedeBot--;
                        tempoDesvioBot = 0;
                    } else if (goleiroDireitoTemPrioridade) {
                        // Quando a bola está chegando à área, o jogador de linha
                        // abre espaço para o goleiro e espera uma possível sobra.
                        alvoMovimentoX = DimensoesJogo.AREA_DIREITA_X
                                - cenario.linhaDireita.largura - 35;
                        alvoMovimentoY = Math.max(
                                DimensoesJogo.CAMPO_TOPO,
                                Math.min(DimensoesJogo.CAMPO_FUNDO - cenario.linhaDireita.altura,
                                        bola.y + bola.tamanho / 2.0
                                                - cenario.linhaDireita.altura / 2.0)
                        );
                        tempoDesvioBot = 0;
                    } else if (tempoDesvioBot <= 0
                            && jogadorBloqueiaCaminhoDoBot(alvoBolaX, alvoBolaY)) {
                        iniciarDesvioBot();
                    }

                    if (tempoRecuoParedeBot <= 0
                            && !goleiroDireitoTemPrioridade
                            && tempoDesvioBot > 0) {
                        double centroPlayerY = cenario.linhaEsquerda.y + cenario.linhaEsquerda.altura / 2.0;
                        double distanciaParaContornar = (cenario.linhaEsquerda.altura
                                + cenario.linhaDireita.altura) / 2.0 + 25;

                        // O ponto de desvio continua na direção da bola, mas passa
                        // por cima ou por baixo do jogador que fechou o caminho.
                        alvoMovimentoY = centroPlayerY
                                + direcaoDesvioY * distanciaParaContornar
                                - cenario.linhaDireita.altura / 2.0;
                        alvoMovimentoY = Math.max(
                                DimensoesJogo.CAMPO_TOPO,
                                Math.min(DimensoesJogo.CAMPO_FUNDO - cenario.linhaDireita.altura,
                                        alvoMovimentoY)
                        );
                        velocidadeMovimentoBot = velocidadeLinhaDireita + 1;
                        tempoDesvioBot--;
                    }

                    moverBotEmDirecao(alvoMovimentoX, alvoMovimentoY, velocidadeMovimentoBot);

                    if (fisicaJogo.isNaAreaProibida(cenario.linhaDireita)) {
                        if (cenario.linhaDireita.x > 400) {
                            cenario.linhaDireita.x = 680 - cenario.linhaDireita.largura;
                        } else {
                            cenario.linhaDireita.x = 105;
                        }
                    }

                    // Não deixa o bot continuar entrando na bola quando ela já está
                    // encostada em uma parede. Isso evita que ela fique prensada.
                    if (fisicaJogo.bolaPertoDaParede(bola)
                            && fisicaJogo.jogadorEncostaNaBola(cenario.linhaDireita, bola)) {
                        cenario.linhaDireita.x = oldBotX;
                        cenario.linhaDireita.y = oldBotY;
                        tempoRecuoParedeBot = 35;
                    }

                    double distBotBola = Math.hypot(cenario.linhaDireita.x - bola.x, cenario.linhaDireita.y - bola.y);
                    double distBotPlayer = Math.hypot(cenario.linhaDireita.x - cenario.linhaEsquerda.x, cenario.linhaDireita.y - cenario.linhaEsquerda.y);

                    boolean bolaNoCanto = (bola.x < 45 || bola.x > 735 || bola.y < 95 || bola.y > 565);

                    double velocidadeBola = Math.hypot(bola.velX, bola.velY);

                    if (bolaNoCanto && distBotBola < 60 && velocidadeBola < 2.0) {
                        bola.velX = (bola.x < 400) ? 5.0 : -5.0;
                        bola.velY = (bola.y < 300) ? 4.5 : -4.5;
                    }

                    if (cooldownChuteBot == 0 && !botChutando) {
                        boolean botPertoDaBolaNaParede = fisicaJogo.bolaPertoDaParede(bola)
                                && distBotBola < 65;

                        if (botPertoDaBolaNaParede) {
                            botChutando = true;
                            raioAuraBot = 0;
                            cooldownChuteBot = 90;
                            tempoRecuoParedeBot = 35;
                            executarPisaoEscapeParedeBot();
                        } else if (!bolaNoCanto && (distBotBola < 55 || distBotPlayer < 75)) {
                            botChutando = true;
                            raioAuraBot = 0;
                            cooldownChuteBot = 90;
                            executarChuteBot();
                        }
                    }

                    if (fisicaJogo.checarColisaoJogadores(
                            cenario.linhaEsquerda, cenario.linhaDireita)) {
                        cenario.linhaEsquerda.x = oldX;
                        cenario.linhaEsquerda.y = oldY;
                        cenario.linhaDireita.x = oldBotX;
                        cenario.linhaDireita.y = oldBotY;
                        if (tempoDesvioBot <= 0) {
                            iniciarDesvioBot();
                        }
                    }

                    cenario.verificarGol();

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

                } else {
                    // --- CÓDIGO DE RETORNO AUTOMÁTICO (ESPERANDO 5 SEGUNDOS) ---
                    framesEsperaFimDeJogo++;
                    if (framesEsperaFimDeJogo >= 300) { // 300 frames * 16ms ~= 4.8 a 5 segundos

                        // Reseta todas as variáveis do Main
                        fimDeJogo = false;
                        framesEsperaFimDeJogo = 0;
                        periodoAtual = 1;
                        minutosVirtuais = 0;
                        segundosVirtuais = 0;
                        acumuladorMilis = 0;
                        alphaTextoTempo = 0.25f;

                        // Envia comando pro Cenário voltar pra tela de Início
                        cenario.resetarParaMenu();
                    }
                }

                // Mantém o cronômetro/placar e fade atualizados (importante para mostrar o "FIM DE JOGO")
                cenario.atualizarCronometro(periodoAtual, minutosVirtuais, segundosVirtuais, fimDeJogo);
                cenario.atualizarTextoFade(periodoAtual, alphaTextoTempo);
            }
            cenario.repaint();
        });
        timer.start();
    }

    private void executarPisaoPlayer() {
        double forcaImpacto = 16.0;
        double centroPlayerX = cenario.linhaEsquerda.x + cenario.linhaEsquerda.largura / 2.0;
        double centroPlayerY = cenario.linhaEsquerda.y + cenario.linhaEsquerda.altura / 2.0;
        double centroBolaX = bola.x + bola.tamanho / 2.0;
        double centroBolaY = bola.y + bola.tamanho / 2.0;

        double dxBola = centroBolaX - centroPlayerX;
        double dyBola = centroBolaY - centroPlayerY;
        double distBola = Math.hypot(dxBola, dyBola);

        if (distBola < 80) {
            if (distBola < 0.0001) {
                dxBola = 1;
                dyBola = 0;
                distBola = 1;
            }
            bola.velX = (dxBola / distBola) * forcaImpacto;
            bola.velY = (dyBola / distBola) * forcaImpacto;
        }

        double centroBotX = cenario.linhaDireita.x + cenario.linhaDireita.largura / 2.0;
        double centroBotY = cenario.linhaDireita.y + cenario.linhaDireita.altura / 2.0;
        double dxBot = centroBotX - centroPlayerX;
        double dyBot = centroBotY - centroPlayerY;
        double distBot = Math.hypot(dxBot, dyBot);

        if (distBot < 80) {
            if (distBot < 0.0001) {
                dxBot = 1;
                dyBot = 0;
                distBot = 1;
            }
            int novoX = cenario.linhaDireita.x + (int) ((dxBot / distBot) * 40);
            int novoY = cenario.linhaDireita.y + (int) ((dyBot / distBot) * 40);
            Jogador testeBot = new Jogador(novoX, novoY, "direita");
            if (!fisicaJogo.estaForaDoCampo(testeBot)) {
                cenario.linhaDireita.x = novoX;
                cenario.linhaDireita.y = novoY;
            }
        }
    }

    private void executarChuteBot() {
        double forcaImpacto = 13.0;
        double centroBotX = cenario.linhaDireita.x + cenario.linhaDireita.largura / 2.0;
        double centroBotY = cenario.linhaDireita.y + cenario.linhaDireita.altura / 2.0;
        double centroBolaX = bola.x + bola.tamanho / 2.0;
        double centroBolaY = bola.y + bola.tamanho / 2.0;

        double dxBola = centroBolaX - centroBotX;
        double dyBola = centroBolaY - centroBotY;
        double distBola = Math.hypot(dxBola, dyBola);

        if (distBola < 80) {
            if (distBola < 0.0001) {
                dxBola = -1;
                dyBola = 0;
                distBola = 1;
            }
            bola.velX = (dxBola / distBola) * forcaImpacto;
            bola.velY = (dyBola / distBola) * forcaImpacto;
        }

        double centroPlayerX = cenario.linhaEsquerda.x + cenario.linhaEsquerda.largura / 2.0;
        double centroPlayerY = cenario.linhaEsquerda.y + cenario.linhaEsquerda.altura / 2.0;
        double dxPlayer = centroPlayerX - centroBotX;
        double dyPlayer = centroPlayerY - centroBotY;
        double distPlayer = Math.hypot(dxPlayer, dyPlayer);

        if (distPlayer < 80) {
            if (distPlayer < 0.0001) {
                dxPlayer = -1;
                dyPlayer = 0;
                distPlayer = 1;
            }
            int novoX = cenario.linhaEsquerda.x + (int) ((dxPlayer / distPlayer) * 40);
            int novoY = cenario.linhaEsquerda.y + (int) ((dyPlayer / distPlayer) * 40);
            Jogador testeP1 = new Jogador(novoX, novoY, "esquerda");
            if (!fisicaJogo.estaForaDoCampo(testeP1)) {
                cenario.linhaEsquerda.x = novoX;
                cenario.linhaEsquerda.y = novoY;
            }
        }
    }

    private void executarPisaoEscapeParedeBot() {
        double centroBolaY = bola.y + bola.tamanho / 2.0;
        double direcaoX = -1.0; // O bot ataca o gol da esquerda.
        double direcaoY = 0.0;

        boolean pertoDoTopo = bola.y <= DimensoesJogo.CAMPO_TOPO + 5;
        boolean pertoDoFundo = bola.y + bola.tamanho >= DimensoesJogo.CAMPO_FUNDO - 5;
        boolean pertoDaEsquerda = bola.x <= DimensoesJogo.CAMPO_ESQUERDA + 5;
        boolean pertoDaDireita = bola.x + bola.tamanho >= DimensoesJogo.CAMPO_DIREITA - 5;

        if (pertoDaEsquerda) {
            direcaoX = 1.0; // Precisa voltar ao campo antes de atacar.
            direcaoY = Math.signum(
                    (DimensoesJogo.GOL_TOPO + DimensoesJogo.GOL_FUNDO) / 2.0 - centroBolaY
            ) * 0.55;
        } else if (pertoDaDireita) {
            direcaoX = -1.0;
            direcaoY = Math.signum(
                    (DimensoesJogo.CAMPO_TOPO + DimensoesJogo.CAMPO_FUNDO) / 2.0 - centroBolaY
            ) * 0.55;
        }

        // Nos cantos, as duas correções se combinam e apontam para dentro.
        if (pertoDoTopo) direcaoY = 0.65;
        if (pertoDoFundo) direcaoY = -0.65;

        double tamanhoDirecao = Math.hypot(direcaoX, direcaoY);
        double forcaEscape = 14.0;
        bola.velX = direcaoX / tamanhoDirecao * forcaEscape;
        bola.velY = direcaoY / tamanhoDirecao * forcaEscape;

        // Coloca a bola um pouco para dentro, evitando outra colisão imediata
        // com a mesma parede antes que o impulso faça efeito.
        if (pertoDoTopo) bola.y = DimensoesJogo.CAMPO_TOPO + 1;
        if (pertoDoFundo) bola.y = DimensoesJogo.CAMPO_FUNDO - bola.tamanho - 1;
        if (pertoDaEsquerda) bola.x = DimensoesJogo.CAMPO_ESQUERDA + 1;
        if (pertoDaDireita) bola.x = DimensoesJogo.CAMPO_DIREITA - bola.tamanho - 1;
    }

    private void moverBotEmDirecao(double alvoX, double alvoY, double velocidade) {
        double dx = alvoX - cenario.linhaDireita.x;
        double dy = alvoY - cenario.linhaDireita.y;
        double distancia = Math.hypot(dx, dy);

        if (distancia < 0.5) {
            return;
        }

        double tamanhoPasso = Math.min(velocidade, distancia);
        int passoX = (int) Math.round((dx / distancia) * tamanhoPasso);
        int passoY = (int) Math.round((dy / distancia) * tamanhoPasso);

        if (passoX == 0 && passoY == 0) {
            if (Math.abs(dx) >= Math.abs(dy)) passoX = dx > 0 ? 1 : -1;
            else passoY = dy > 0 ? 1 : -1;
        }

        int destinoX = cenario.linhaDireita.x + passoX;
        int destinoY = cenario.linhaDireita.y + passoY;

        // Primeiro tenta o passo diagonal completo.
        Jogador tentativaDiagonal = new Jogador(destinoX, destinoY, "direita");
        if (!fisicaJogo.estaForaDoCampo(tentativaDiagonal)
                && !fisicaJogo.isNaAreaProibida(tentativaDiagonal)) {
            cenario.linhaDireita.x = tentativaDiagonal.x;
            cenario.linhaDireita.y = tentativaDiagonal.y;
            return;
        }

        // Perto dos limites, tenta aproveitar a parte livre do movimento.
        Jogador tentativaX = new Jogador(destinoX, cenario.linhaDireita.y, "direita");
        if (!fisicaJogo.estaForaDoCampo(tentativaX)
                && !fisicaJogo.isNaAreaProibida(tentativaX)) {
            cenario.linhaDireita.x = tentativaX.x;
            cenario.linhaDireita.y = tentativaX.y;
        }

        Jogador tentativaY = new Jogador(cenario.linhaDireita.x, destinoY, "direita");
        if (!fisicaJogo.estaForaDoCampo(tentativaY)
                && !fisicaJogo.isNaAreaProibida(tentativaY)) {
            cenario.linhaDireita.x = tentativaY.x;
            cenario.linhaDireita.y = tentativaY.y;
        }
    }

    private boolean jogadorBloqueiaCaminhoDoBot(double alvoX, double alvoY) {
        double botCentroX = cenario.linhaDireita.x + cenario.linhaDireita.largura / 2.0;
        double botCentroY = cenario.linhaDireita.y + cenario.linhaDireita.altura / 2.0;
        double playerCentroX = cenario.linhaEsquerda.x + cenario.linhaEsquerda.largura / 2.0;
        double playerCentroY = cenario.linhaEsquerda.y + cenario.linhaEsquerda.altura / 2.0;

        double rotaX = alvoX - cenario.linhaDireita.x;
        double rotaY = alvoY - cenario.linhaDireita.y;
        double comprimentoQuadrado = rotaX * rotaX + rotaY * rotaY;
        if (comprimentoQuadrado < 1.0) {
            return false;
        }

        double atePlayerX = playerCentroX - botCentroX;
        double atePlayerY = playerCentroY - botCentroY;
        double posicaoNaRota = (atePlayerX * rotaX + atePlayerY * rotaY) / comprimentoQuadrado;

        // Só considera bloqueio quando o jogador está entre o bot e seu alvo.
        if (posicaoNaRota <= 0.0 || posicaoNaRota >= 1.0) {
            return false;
        }

        double pontoRotaX = botCentroX + posicaoNaRota * rotaX;
        double pontoRotaY = botCentroY + posicaoNaRota * rotaY;
        double distanciaDaRota = Math.hypot(
                playerCentroX - pontoRotaX,
                playerCentroY - pontoRotaY
        );
        double distanciaBotPlayer = Math.hypot(atePlayerX, atePlayerY);

        return distanciaDaRota < 45 && distanciaBotPlayer < 150;
    }

    private void iniciarDesvioBot() {
        double botCentroY = cenario.linhaDireita.y + cenario.linhaDireita.altura / 2.0;
        double playerCentroY = cenario.linhaEsquerda.y + cenario.linhaEsquerda.altura / 2.0;
        double diferencaY = botCentroY - playerCentroY;

        if (Math.abs(diferencaY) > 10) {
            // Se já está um pouco acima ou abaixo, continua por esse lado.
            direcaoDesvioY = diferencaY > 0 ? 1 : -1;
        } else {
            // Quando estão alinhados, escolhe o lado com mais espaço livre.
            double espacoAcima = playerCentroY - DimensoesJogo.CAMPO_TOPO;
            double espacoAbaixo = DimensoesJogo.CAMPO_FUNDO - playerCentroY;
            direcaoDesvioY = espacoAbaixo >= espacoAcima ? 1 : -1;
        }

        double margemNecessaria = 80;
        double espacoNoLadoEscolhido = direcaoDesvioY > 0
                ? DimensoesJogo.CAMPO_FUNDO - playerCentroY
                : playerCentroY - DimensoesJogo.CAMPO_TOPO;
        if (espacoNoLadoEscolhido < margemNecessaria) {
            direcaoDesvioY *= -1;
        }

        tempoDesvioBot = 55;
    }

    public static void main(String[] args) { new Main().setVisible(true); }
}
