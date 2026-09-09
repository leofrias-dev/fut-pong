import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.util.Random;


public class Main extends JFrame {

    private Cenario cenario;
    private Bola bola;
    private FisicaJogo fisicaJogo;
    private Timer timer;

    private boolean teclaW, teclaA, teclaS, teclaD;
    private boolean teclaEscPressionada;

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
    private int direcaoDesvioX = 0;
    private int direcaoDesvioY = 0;
    private int tempoRecuoParedeBot = 0;

    // Cada plano permanece por alguns segundos. Isso cria variedade sem
    // transformar o movimento do bot em uma sequência de decisões aleatórias.
    private final Random sorteioPartida = new Random();
    private boolean partidaPreparada = false;
    private double deslocamentoAtaqueBotY = 0;
    private int framesRestantesPlanoBot = 0;

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
        cenario.configurarAcaoMenuPausa(this::executarOpcaoPausa);
        add(cenario);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int c = e.getKeyCode();
                if (c == KeyEvent.VK_ESCAPE) {
                    // Segurar Esc não deve abrir e fechar o menu repetidamente.
                    if (!teclaEscPressionada && cenario.jogoRodando()) alternarPausa();
                    teclaEscPressionada = true;
                    return;
                }
                if (cenario.isPausado()) {
                    if (c == KeyEvent.VK_UP) cenario.selecionarOpcaoPausa(-1);
                    if (c == KeyEvent.VK_DOWN) cenario.selecionarOpcaoPausa(1);
                    if (c == KeyEvent.VK_ENTER) cenario.confirmarOpcaoPausa();
                    return;
                }
                if (fimDeJogo || !cenario.jogoRodando()) return;

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
                if (c == KeyEvent.VK_ESCAPE) teclaEscPressionada = false;
                if (c == KeyEvent.VK_W) teclaW = false;
                if (c == KeyEvent.VK_A) teclaA = false;
                if (c == KeyEvent.VK_S) teclaS = false;
                if (c == KeyEvent.VK_D) teclaD = false;
            }
        });

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                limparTeclas();
                teclaEscPressionada = false;
            }
        });

        timer = new Timer(16, e -> {
            if (!cenario.jogoRodando()) {
                partidaPreparada = false;
            }

            // Congela física, IA, relógio, pisões e contagem do fim de jogo.
            if (cenario.isPausado()) return;
            if (cenario.jogoRodando() && !partidaPreparada) {
                prepararNovaPartida();
                partidaPreparada = true;
            }
            if (cenario.jogoRodando()) {

                // Se o jogo NÃO acabou, roda a física normalmente
                if (!fimDeJogo) {
                    if (framesRestantesPlanoBot > 0) {
                        framesRestantesPlanoBot--;
                    } else {
                        sortearNovoPlanoBot();
                    }

                    int golsAntesDaFisica = cenario.golsP1 + cenario.golsBot;
                    if (fisicaJogo.atualizar()) {
                        tempoRecuoParedeBot = 35;
                    }
                    if (cenario.golsP1 + cenario.golsBot > golsAntesDaFisica) {
                        // A saída de bola só começa no próximo quadro. Nenhuma
                        // ação antiga do bot ou pisão atua sobre a bola resetada.
                        encerrarJogadaAposGol();
                        sortearNovoPlanoBot();
                        cenario.atualizarCronometro(periodoAtual, minutosVirtuais, segundosVirtuais, fimDeJogo);
                        cenario.repaint();
                        return;
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
                            sortearNovoPlanoBot();
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

                    int movimentoPlayerX = ((teclaD ? 1 : 0) - (teclaA ? 1 : 0)) * velocidadeSeuJogador;
                    int movimentoPlayerY = ((teclaS ? 1 : 0) - (teclaW ? 1 : 0)) * velocidadeSeuJogador;
                    fisicaJogo.moverJogadorComColisao(cenario.linhaEsquerda, cenario.linhaDireita,
                            movimentoPlayerX, movimentoPlayerY);

                    boolean bolaNaAreaEsq = (bola.x >= DimensoesJogo.AREA_ESQUERDA_X
                            && bola.x <= DimensoesJogo.AREA_ESQUERDA_X + DimensoesJogo.AREA_LARGURA
                            && bola.y >= DimensoesJogo.AREA_TOPO
                            && bola.y <= DimensoesJogo.AREA_TOPO + DimensoesJogo.AREA_ALTURA);
                    double centroBolaY = bola.y + bola.tamanho / 2.0;
                    double centroGoleiroEsquerdoY = cenario.goleiroEsquerda.y
                            + cenario.goleiroEsquerda.altura / 2.0;

                    if (bolaNaAreaEsq) {
                        if (cenario.goleiroEsquerda.x < bola.x) cenario.goleiroEsquerda.x += velocidadeGoleiro;
                        if (cenario.goleiroEsquerda.x > bola.x) cenario.goleiroEsquerda.x -= velocidadeGoleiro;

                        if (centroBolaY > centroGoleiroEsquerdoY) cenario.goleiroEsquerda.y += velocidadeGoleiro;
                        else if (centroBolaY < centroGoleiroEsquerdoY) cenario.goleiroEsquerda.y -= velocidadeGoleiro;
                    } else {
                        if (centroBolaY > centroGoleiroEsquerdoY && cenario.goleiroEsquerda.y < 420) cenario.goleiroEsquerda.y += velocidadeGoleiro;
                        else if (centroBolaY < centroGoleiroEsquerdoY && cenario.goleiroEsquerda.y > 195) cenario.goleiroEsquerda.y -= velocidadeGoleiro;

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
                    double centroGoleiroDireitoY = cenario.goleiroDireita.y
                            + cenario.goleiroDireita.altura / 2.0;

                    if (bolaNaAreaDir) {
                        if (cenario.goleiroDireita.x < bola.x) cenario.goleiroDireita.x += velocidadeGoleiro;
                        if (cenario.goleiroDireita.x > bola.x) cenario.goleiroDireita.x -= velocidadeGoleiro;

                        if (centroBolaY > centroGoleiroDireitoY) cenario.goleiroDireita.y += velocidadeGoleiro;
                        else if (centroBolaY < centroGoleiroDireitoY) cenario.goleiroDireita.y -= velocidadeGoleiro;
                    } else {
                        if (centroBolaY > centroGoleiroDireitoY && cenario.goleiroDireita.y < 420) cenario.goleiroDireita.y += velocidadeGoleiro;
                        else if (centroBolaY < centroGoleiroDireitoY && cenario.goleiroDireita.y > 195) cenario.goleiroDireita.y -= velocidadeGoleiro;

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

                    // O bot aborda a bola um pouco por cima ou por baixo. O lado,
                    // a distância e a duração desse plano mudam durante a partida.
                    alvoBolaY = Math.max(
                            DimensoesJogo.CAMPO_TOPO,
                            Math.min(DimensoesJogo.CAMPO_FUNDO - cenario.linhaDireita.altura,
                                    alvoBolaY + deslocamentoAtaqueBotY)
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
                            >= DimensoesJogo.AREA_DIREITA_X - 80
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
                        // Na defesa, o jogador de linha abre espaço antes de a bola
                        // entrar na área. Ele também sai da mesma faixa vertical do
                        // goleiro, evitando uma tabela infinita entre os dois.
                        alvoMovimentoX = DimensoesJogo.AREA_DIREITA_X
                                - cenario.linhaDireita.largura - 90;
                        double ladoLivreY = centroBolaY <= DimensoesJogo.CAMPO_MEIO_Y
                                ? centroBolaY + 90
                                : centroBolaY - 90;
                        alvoMovimentoY = Math.max(
                                DimensoesJogo.CAMPO_TOPO,
                                Math.min(DimensoesJogo.CAMPO_FUNDO - cenario.linhaDireita.altura,
                                        ladoLivreY - cenario.linhaDireita.altura / 2.0)
                        );
                        tempoDesvioBot = 0;
                    } else {
                        if (!jogadorBloqueiaCaminhoDoBot(alvoBolaX, alvoBolaY)) {
                            tempoDesvioBot = 0;
                        } else if (tempoDesvioBot <= 0) {
                            iniciarDesvioBot();
                        }
                    }

                    if (tempoRecuoParedeBot <= 0
                            && !goleiroDireitoTemPrioridade
                            && tempoDesvioBot > 0) {
                        // Contorna pela lateral quando o obstáculo está acima/abaixo,
                        // ou por cima/baixo quando os corpos estão lado a lado.
                        if (direcaoDesvioX != 0) {
                            alvoMovimentoX = cenario.linhaEsquerda.x
                                    + cenario.linhaEsquerda.largura / 2.0
                                    + direcaoDesvioX * ((cenario.linhaEsquerda.largura
                                    + cenario.linhaDireita.largura) / 2.0 + 25)
                                    - cenario.linhaDireita.largura / 2.0;
                            alvoMovimentoY = cenario.linhaEsquerda.y
                                    + (cenario.linhaEsquerda.altura - cenario.linhaDireita.altura) / 2.0;
                        } else {
                            alvoMovimentoY = cenario.linhaEsquerda.y
                                    + cenario.linhaEsquerda.altura / 2.0
                                    + direcaoDesvioY * ((cenario.linhaEsquerda.altura
                                    + cenario.linhaDireita.altura) / 2.0 + 25)
                                    - cenario.linhaDireita.altura / 2.0;
                            alvoMovimentoX = cenario.linhaEsquerda.x
                                    + (cenario.linhaEsquerda.largura - cenario.linhaDireita.largura) / 2.0;
                        }
                        alvoMovimentoX = Math.max(DimensoesJogo.CAMPO_ESQUERDA,
                                Math.min(DimensoesJogo.CAMPO_DIREITA - cenario.linhaDireita.largura,
                                        alvoMovimentoX));
                        alvoMovimentoY = Math.max(
                                DimensoesJogo.CAMPO_TOPO,
                                Math.min(DimensoesJogo.CAMPO_FUNDO - cenario.linhaDireita.altura,
                                        alvoMovimentoY)
                        );
                        velocidadeMovimentoBot = velocidadeLinhaDireita + 1;
                        tempoDesvioBot--;
                    }

                    moverBotEmDirecao(alvoMovimentoX, alvoMovimentoY, velocidadeMovimentoBot);

                    // Não deixa o bot continuar entrando na bola quando ela já está
                    // encostada em uma parede. Isso evita que ela fique prensada.
                    if (fisicaJogo.bolaPertoDaParede(bola)
                            && fisicaJogo.jogadorEncostaNaBola(cenario.linhaDireita, bola)) {
                        cenario.linhaDireita.x = oldBotX;
                        cenario.linhaDireita.y = oldBotY;
                        tempoRecuoParedeBot = 35;
                    }

                    double distBotBola = Math.hypot(
                            cenario.linhaDireita.x + cenario.linhaDireita.largura / 2.0 - bola.x - bola.tamanho / 2.0,
                            cenario.linhaDireita.y + cenario.linhaDireita.altura / 2.0 - bola.y - bola.tamanho / 2.0);
                    double distBotPlayer = Math.hypot(cenario.linhaDireita.x - cenario.linhaEsquerda.x, cenario.linhaDireita.y - cenario.linhaEsquerda.y);

                    boolean bolaNoCanto = (bola.x < 45 || bola.x > 735 || bola.y < 95 || bola.y > 565);

                    double velocidadeBola = Math.hypot(bola.velX, bola.velY);

                    if (bolaNoCanto && distBotBola < 60 && velocidadeBola < 2.0) {
                        bola.velX = (bola.x < 400) ? 5.0 : -5.0;
                        bola.velY = (bola.y < 300) ? 4.5 : -4.5;
                    }

                    if (cooldownChuteBot == 0 && !botChutando
                            && !goleiroDireitoTemPrioridade) {
                        boolean botPertoDaBolaNaParede = fisicaJogo.bolaPertoDaParede(bola)
                                && distBotBola < 65;

                        if (botPertoDaBolaNaParede) {
                            botChutando = true;
                            raioAuraBot = 0;
                            cooldownChuteBot = 90;
                            tempoRecuoParedeBot = 35;
                            executarPisaoEscapeParedeBot();
                        } else if ((!bolaNoCanto && distBotBola < 55) || distBotPlayer < 75) {
                            botChutando = true;
                            raioAuraBot = 0;
                            cooldownChuteBot = 75 + sorteioPartida.nextInt(36);
                            executarChuteBot();
                        }
                    }

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

                        voltarTelaInicial();
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
            fisicaJogo.moverJogadorComColisao(cenario.linhaDireita, cenario.linhaEsquerda,
                    (int) ((dxBot / distBot) * 40), (int) ((dyBot / distBot) * 40));
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
            fisicaJogo.moverJogadorComColisao(cenario.linhaEsquerda, cenario.linhaDireita,
                    (int) ((dxPlayer / distPlayer) * 40), (int) ((dyPlayer / distPlayer) * 40));
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

        fisicaJogo.moverJogadorComColisao(cenario.linhaDireita, cenario.linhaEsquerda,
                passoX, passoY);
    }

    private void prepararNovaPartida() {
        sortearNovoPlanoBot();
    }

    private void sortearNovoPlanoBot() {
        int ladoDaAbordagem = sorteioPartida.nextBoolean() ? 1 : -1;
        deslocamentoAtaqueBotY = ladoDaAbordagem * (14 + sorteioPartida.nextInt(17));

        // Entre aproximadamente 4 e 11 segundos reais antes de reconsiderar
        // a jogada. O plano é estável o bastante para parecer intencional.
        framesRestantesPlanoBot = 260 + sorteioPartida.nextInt(421);
    }

    private boolean jogadorBloqueiaCaminhoDoBot(double alvoX, double alvoY) {
        Jogador bot = cenario.linhaDireita;
        Jogador player = cenario.linhaEsquerda;
        if (Math.hypot(bot.x - player.x, bot.y - player.y) > 180) return false;

        // Mantém uma folga ao sair do desvio para não alternar de rota na quina.
        // Sem desvio ativo, o pequeno recuo permite deslizar encostado.
        double margem = tempoDesvioBot > 0 ? 4.0 : -0.01;
        Rectangle2D obstaculo = new Rectangle2D.Double(
                player.x - bot.largura - margem, player.y - bot.altura - margem,
                player.largura + bot.largura + 2 * margem, player.altura + bot.altura + 2 * margem);
        return obstaculo.intersectsLine(bot.x, bot.y, alvoX, alvoY);
    }

    private void iniciarDesvioBot() {
        double botCentroX = cenario.linhaDireita.x + cenario.linhaDireita.largura / 2.0;
        double playerCentroX = cenario.linhaEsquerda.x + cenario.linhaEsquerda.largura / 2.0;
        double botCentroY = cenario.linhaDireita.y + cenario.linhaDireita.altura / 2.0;
        double playerCentroY = cenario.linhaEsquerda.y + cenario.linhaEsquerda.altura / 2.0;
        double diferencaX = botCentroX - playerCentroX;
        double diferencaY = botCentroY - playerCentroY;
        direcaoDesvioX = 0;
        direcaoDesvioY = 0;

        if (Math.abs(diferencaY) / cenario.linhaDireita.altura
                > Math.abs(diferencaX) / cenario.linhaDireita.largura) {
            if (Math.abs(diferencaX) > 5) {
                direcaoDesvioX = diferencaX > 0 ? 1 : -1;
            } else {
                direcaoDesvioX = bola.x + bola.tamanho / 2.0 >= playerCentroX ? 1 : -1;
            }
            double espacoLateral = direcaoDesvioX > 0
                    ? DimensoesJogo.CAMPO_DIREITA - playerCentroX
                    : playerCentroX - DimensoesJogo.CAMPO_ESQUERDA;
            if (espacoLateral < 65) direcaoDesvioX *= -1;
            tempoDesvioBot = 55;
            return;
        }

        if (Math.abs(diferencaY) > 10) {
            // Se já está um pouco acima ou abaixo, continua por esse lado.
            direcaoDesvioY = diferencaY > 0 ? 1 : -1;
        } else {
            // Prefere o lado da bola, desde que haja espaço para passar.
            direcaoDesvioY = bola.y + bola.tamanho / 2.0 >= playerCentroY ? 1 : -1;
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

    private void alternarPausa() {
        limparTeclas();
        cenario.definirPausado(!cenario.isPausado());
        requestFocusInWindow();
    }

    private void executarOpcaoPausa(int opcao) {
        if (!cenario.isPausado()) return;
        switch (opcao) {
            case 0:
                alternarPausa();
                break;
            case 1:
                resetarEstadoDaPartida();
                cenario.reiniciarPartida();
                requestFocusInWindow();
                break;
            case 2:
                voltarTelaInicial();
                break;
            default:
                break;
        }
    }

    private void voltarTelaInicial() {
        resetarEstadoDaPartida();
        cenario.resetarParaMenu();
        requestFocusInWindow();
    }

    private void resetarEstadoDaPartida() {
        fimDeJogo = false;
        framesEsperaFimDeJogo = 0;
        periodoAtual = 1;
        minutosVirtuais = 0;
        segundosVirtuais = 0;
        acumuladorMilis = 0;
        alphaTextoTempo = 0.25f;
        partidaPreparada = false;
        limparTeclas();
        encerrarJogadaAposGol();
    }

    private void encerrarJogadaAposGol() {
        pisaAtivo = false;
        botChutando = false;
        raioAura = 0;
        raioAuraBot = 0;
        cooldownChuteBot = 0;
        tempoDesvioBot = 0;
        direcaoDesvioX = 0;
        direcaoDesvioY = 0;
        tempoRecuoParedeBot = 0;
    }

    private void limparTeclas() {
        teclaW = false;
        teclaA = false;
        teclaS = false;
        teclaD = false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
