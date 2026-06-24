import java.awt.Rectangle;

public class Bola {
    public double x = 385;
    public double y = 320;
    public double velX = 0;
    public double velY = 0;
    public final int tamanho = 20;

    // Valores limites baseados no design do seu cenário
    private final int LIMITE_ESQUERDA = 10;
    private final int LIMITE_DIREITA = 775;
    private final int LIMITE_TOPO = 60;
    private final int LIMITE_FUNDO = 600;

    public void mexer() {
        x += velX;
        y += velY;

        // Aplica o atrito gradual para a bola não rolar eternamente
        velX *= 0.99;
        velY *= 0.99;

        // --- COLISÃO E TRAVA NOS LIMITES SUPERIOR E INFERIOR ---
        if (y < LIMITE_TOPO) {
            y = LIMITE_TOPO;
            velY = -velY * 0.85; // Rebate com pequena perda de energia
        } else if (y + tamanho > LIMITE_FUNDO) {
            y = LIMITE_FUNDO - tamanho;
            velY = -velY * 0.85;
        }

        // --- COLISÃO E TRAVA NAS LATERAIS (FORA DA ZONA DE GOL) ---
        // A zona de gol vertical está entre 270 e 390
        boolean naDirecaoDoGol = (y >= 270 && y <= 390);

        if (!naDirecaoDoGol) {
            // Lado Esquerdo (Fundo de campo comum)
            if (x < LIMITE_ESQUERDA) {
                x = LIMITE_ESQUERDA;
                velX = -velX * 0.85;
            }
            // Lado Direito (Fundo de campo comum onde o Bot travou)
            if (x + tamanho > LIMITE_DIREITA) {
                x = LIMITE_DIREITA - tamanho;
                velX = -velX * 0.85;
            }
        } else {
            // Se estiver na direção do gol mas passar do limite externo da trave (fundo da rede)
            if (x < LIMITE_ESQUERDA - 10) {
                x = LIMITE_ESQUERDA - 10;
            }
            if (x + tamanho > LIMITE_DIREITA + 10) {
                x = LIMITE_DIREITA + 10 - tamanho;
            }
        }
    }

    public Rectangle getLimites() {
        return new Rectangle((int)x, (int)y, tamanho, tamanho);
    }
}