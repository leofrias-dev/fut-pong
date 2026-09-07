import java.awt.Rectangle;


public class Bola {
    private static final double FATOR_ARRASTO = 0.99;
    private static final double VELOCIDADE_MINIMA = 0.12;

    public double x = DimensoesJogo.BOLA_INICIAL_X;
    public double y = DimensoesJogo.BOLA_INICIAL_Y;
    public double velX = 0;
    public double velY = 0;
    public final int tamanho = 18;

    public void mexer() {
        x += velX;
        y += velY;

        aplicarArrasto();

        // --- COLISÃO E TRAVA NOS LIMITES SUPERIOR E INFERIOR ---
        if (y < DimensoesJogo.CAMPO_TOPO) {
            y = DimensoesJogo.CAMPO_TOPO;
            velY = -velY * 0.85; // Rebate com pequena perda de energia
        } else if (y + tamanho > DimensoesJogo.CAMPO_FUNDO) {
            y = DimensoesJogo.CAMPO_FUNDO - tamanho;
            velY = -velY * 0.85;
        }

        // --- COLISÃO E TRAVA NAS LATERAIS (FORA DA ZONA DE GOL) ---
        // A zona de gol vertical está entre 270 e 390
        boolean naDirecaoDoGol = (y >= DimensoesJogo.GOL_TOPO && y <= DimensoesJogo.GOL_FUNDO);

        if (!naDirecaoDoGol) {
            // Lado Esquerdo (Fundo de campo comum)
            if (x < DimensoesJogo.CAMPO_ESQUERDA) {
                x = DimensoesJogo.CAMPO_ESQUERDA;
                velX = -velX * 0.85;
            }
            // Lado Direito (Fundo de campo comum onde o Bot travou)
            if (x + tamanho > DimensoesJogo.CAMPO_DIREITA) {
                x = DimensoesJogo.CAMPO_DIREITA - tamanho;
                velX = -velX * 0.85;
            }
        } else {
            // Se estiver na direção do gol mas passar do limite externo da trave (fundo da rede)
            if (x < DimensoesJogo.CAMPO_ESQUERDA - 10) {
                x = DimensoesJogo.CAMPO_ESQUERDA - 10;
            }
            if (x + tamanho > DimensoesJogo.CAMPO_DIREITA + 10) {
                x = DimensoesJogo.CAMPO_DIREITA + 10 - tamanho;
            }
        }
    }

    public Rectangle getLimites() {
        return new Rectangle((int)x, (int)y, tamanho, tamanho);
    }

    public void aplicarArrasto() {
        velX *= FATOR_ARRASTO;
        velY *= FATOR_ARRASTO;

        // Evita que velocidades minúsculas mantenham a bola se mexendo para sempre.
        if (Math.hypot(velX, velY) < VELOCIDADE_MINIMA) {
            velX = 0;
            velY = 0;
        }
    }
}
