import java.awt.Rectangle;

public class Bola {
    // Usamos double para permitir movimentos suaves e desaceleração gradual
    public double x = 385;
    public double y = 320;
    public double velX = 0; // Começa parada
    public double velY = 0; // Começa parada
    public int tamanho = 15;

    // Constante de atrito (0.98 faz a bola parar suavemente)
    private final double ATRITO = 0.985;

    public void mexer() {
        x += velX;
        y += velY;

        // Aplica o atrito (reduz a velocidade gradualmente)
        velX *= ATRITO;
        velY *= ATRITO;

        // Para a bola completamente se estiver muito devagar (evita micro-movimentos infinitos)
        if (Math.abs(velX) < 0.1) velX = 0;
        if (Math.abs(velY) < 0.1) velY = 0;

        // Colisões com paredes (quicar com perda de energia)
        if (y <= 60) { y = 60; velY = -velY * 0.8; }
        if (y >= 600 - tamanho) { y = 600 - tamanho; velY = -velY * 0.8; }
        if (x <= 10) { x = 10; velX = -velX * 0.8; }
        if (x >= 775 - tamanho) { x = 775 - tamanho; velX = -velX * 0.8; }
    }

    public Rectangle getLimites() {
        // Convertemos para int apenas para o sistema de colisão do Java
        return new Rectangle((int)x, (int)y, tamanho, tamanho);
    }

    // Método para receber "chute" ou impacto
    public void aplicarImpulso(double forcaX, double forcaY) {
        this.velX += forcaX;
        this.velY += forcaY;
    }
}