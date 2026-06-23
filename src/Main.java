import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Main extends JFrame {

    private Cenario cenario;
    private Bola bola;
    private Timer timer;

    private boolean teclaW = false;
    private boolean teclaA = false;
    private boolean teclaS = false;
    private boolean teclaD = false;

    private int velocidadeSeuJogador = 4;
    private int velocidadeGoleiroEsquerda = 2;
    private int velocidadeGoleiroDireita = 2;
    private int velocidadeLinhaDireita = 3;

    // Variáveis de controle para o estado de desvio do BOT
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
                int codigo = e.getKeyCode();
                if (codigo == KeyEvent.VK_W) teclaW = true;
                if (codigo == KeyEvent.VK_A) teclaA = true;
                if (codigo == KeyEvent.VK_S) teclaS = true;
                if (codigo == KeyEvent.VK_D) teclaD = true;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int codigo = e.getKeyCode();
                if (codigo == KeyEvent.VK_W) teclaW = false;
                if (codigo == KeyEvent.VK_A) teclaA = false;
                if (codigo == KeyEvent.VK_S) teclaS = false;
                if (codigo == KeyEvent.VK_D) teclaD = false;
            }
        });

        timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cenario.jogoRodando()) {
                    bola.mexer();

                    // Salva as posições ANTES do movimento atual para o sistema de física de barreira
                    int p1AntigoX = cenario.linhaEsquerda.x;
                    int p1AntigoY = cenario.linhaEsquerda.y;
                    int botAntigoX = cenario.linhaDireita.x;
                    int botAntigoY = cenario.linhaDireita.y;

                    // 1. MOVIMENTO DO SEU JOGADOR (P1)
                    if (teclaW && cenario.linhaEsquerda.y > 60)  cenario.linhaEsquerda.y -= velocidadeSeuJogador;
                    if (teclaS && cenario.linhaEsquerda.y < 540) cenario.linhaEsquerda.y += velocidadeSeuJogador;
                    if (teclaA && cenario.linhaEsquerda.x > 100) cenario.linhaEsquerda.x -= velocidadeSeuJogador;
                    if (teclaD && cenario.linhaEsquerda.x < 650) cenario.linhaEsquerda.x += velocidadeSeuJogador;

                    // 2. MOVIMENTO DOS GOLEIROS AUTOMÁTICOS
                    if (bola.y > cenario.goleiroEsquerda.y + 20 && cenario.goleiroEsquerda.y < 420) {
                        cenario.goleiroEsquerda.y += velocidadeGoleiroEsquerda;
                    } else if (bola.y < cenario.goleiroEsquerda.y + 20 && cenario.goleiroEsquerda.y > 195) {
                        cenario.goleiroEsquerda.y -= velocidadeGoleiroEsquerda;
                    }

                    if (bola.y > cenario.goleiroDireita.y + 20 && cenario.goleiroDireita.y < 420) {
                        cenario.goleiroDireita.y += velocidadeGoleiroDireita;
                    } else if (bola.y < cenario.goleiroDireita.y + 20 && cenario.goleiroDireita.y > 195) {
                        cenario.goleiroDireita.y -= velocidadeGoleiroDireita;
                    }

                    // 3. MOVIMENTO AUTOMÁTICO DO BOT DE LINHA
                    if (tempoDesvioBot > 0) {
                        // Move puramente na vertical para deslizar pelas costas do jogador
                        cenario.linhaDireita.y += direcaoDesvioY * (velocidadeLinhaDireita + 1);

                        // Garante que o desvio não jogue o bot para fora dos limites visuais do campo
                        if (cenario.linhaDireita.y < 60)  cenario.linhaDireita.y = 60;
                        if (cenario.linhaDireita.y > 540) cenario.linhaDireita.y = 540;

                        tempoDesvioBot--;
                    } else {
                        // Perseguição normal da bola (Eixo Y)
                        if (Math.abs(bola.y - cenario.linhaDireita.y) > 15) {
                            if (bola.y > cenario.linhaDireita.y && cenario.linhaDireita.y < 540) {
                                cenario.linhaDireita.y += velocidadeLinhaDireita;
                            } else if (bola.y < cenario.linhaDireita.y && cenario.linhaDireita.y > 60) {
                                cenario.linhaDireita.y -= velocidadeLinhaDireita;
                            }
                        }

                        // Perseguição normal da bola (Eixo X)
                        if (bola.x > cenario.linhaDireita.x + 10 && cenario.linhaDireita.x < 650) {
                            cenario.linhaDireita.x += (velocidadeLinhaDireita - 1);
                        } else if (bola.x < cenario.linhaDireita.x - 10 && cenario.linhaDireita.x > 100) {
                            cenario.linhaDireita.x -= (velocidadeLinhaDireita - 1);
                        }
                    }

                    // 4. SISTEMA DE COLISÃO DO CORPO E IA ANTI-PRENSADA
                    if (checarColisaoJogadores(cenario.linhaEsquerda, cenario.linhaDireita)) {
                        // Anula a penetração dos corpos voltando ao estado do frame anterior
                        cenario.linhaEsquerda.x = p1AntigoX;
                        cenario.linhaEsquerda.y = p1AntigoY;
                        cenario.linhaDireita.x = botAntigoX;
                        cenario.linhaDireita.y = botAntigoY;

                        if (tempoDesvioBot <= 0) {
                            tempoDesvioBot = 35; // Ativa a manobra por 35 frames

                            // Regra 1: Tenta ir para a direção contrária de onde o jogador está bloqueando
                            if (cenario.linhaDireita.y >= cenario.linhaEsquerda.y) {
                                direcaoDesvioY = 1;  // Vai para baixo
                            } else {
                                direcaoDesvioY = -1; // Vai para cima
                            }

                            // Regra 2: Se a direção escolhida colidir com as paredes do teto ou chão, inverte!
                            if (direcaoDesvioY == -1 && cenario.linhaDireita.y <= 65) {
                                direcaoDesvioY = 1;  // Se ia bater no teto, desvia por baixo
                            } else if (direcaoDesvioY == 1 && cenario.linhaDireita.y >= 535) {
                                direcaoDesvioY = -1; // Se ia bater no chão, desvia por cima
                            }
                        }
                    }

                    // 5. Colisões e checagem da bola
                    bola.verificarColisao(cenario.goleiroEsquerda);
                    bola.verificarColisao(cenario.linhaEsquerda);
                    bola.verificarColisao(cenario.goleiroDireita);
                    bola.verificarColisao(cenario.linhaDireita);

                    cenario.verificarGol();
                }
                cenario.repaint();
            }

            private boolean checarColisaoJogadores(Jogador j1, Jogador j2) {
                Rectangle r1 = new Rectangle(j1.x, j1.y, j1.largura, j1.altura);
                Rectangle r2 = new Rectangle(j2.x, j2.y, j2.largura, j2.altura);
                return r1.intersects(r2);
            }
        });
        timer.start();
    }

    public static void main(String[] args) {
        Main jogo = new Main();
        jogo.setVisible(true);
    }
}