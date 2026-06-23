import java.awt.Rectangle;

public class Jogador {
    public int x;
    public int y;
    public int largura = 25;
    public int altura = 50;
    public int velocidade = 5;
    public String lado;

    public Jogador(int inicioX, int inicioY, String lado) {
        this.x = inicioX;
        this.y = inicioY;
        this.lado = lado;
    }

    public void subir() {
        if (y > 60) y -= velocidade;
    }

    public void descer() {
        if (y < 600 - altura) y += velocidade;
    }

    public void esquerda() {
        if (lado.equals("esquerda") && x > 10) {
            x -= velocidade;
        } else if (lado.equals("direita") && x > 90) {
            x -= Math.min(velocidade, x - 90);
        }
    }

    public void direita() {
        if (lado.equals("esquerda") && x < 695 - largura) {
            x += velocidade;
        } else if (lado.equals("direita") && x < 775 - largura) {
            x += velocidade;
        }
    }

    // 🤖 1. IA DO GOLEIRO
    public void atualizarIA(int bolaY, int bolaX) {
        int centroGoleiro = this.y + (this.altura / 2);
        int velocidadeGoleiro = 2; // Garantindo o nome correto aqui

        if (bolaY < centroGoleiro && y > 230) {
            y -= velocidadeGoleiro;
        } else if (bolaY > centroGoleiro && y < 430 - altura) {
            y += velocidadeGoleiro; // 🛠️ CORRIGIDO: Agora com "e"!
        }
    }

    // 🧠 2. IA DO JOGADOR DE LINHA (CORRIGIDA: Sem guardar caixão!)
    public void atualizarIALinha(int bolaX, int bolaY) {
        int centroJogadorY = this.y + (this.altura / 2);
        int centroJogadorX = this.x + (this.largura / 2);
        int velIA = 4;

        // 🎯 COMPORTAMENTO PARA A IA DO LADO DIREITO
        if (lado.equals("direita")) {
            // Se a bola estiver no campo de ataque dela (Esquerda), ela avança, mas respeita o meio de campo
            if (bolaX < 400) {
                // Segue o Y da bola
                if (bolaY < centroJogadorY && y > 60) y -= velIA;
                else if (bolaY > centroJogadorY && y < 600 - altura) y += velIA;

                // Avança no X para atacar, mas não passa do meio de campo (400)
                if (x > 410) x -= velIA;
            }
            // Se a bola estiver no campo de defesa dela (Direita), ela vai caçar a bola de verdade
            else {
                if (bolaY < centroJogadorY && y > 60) y -= velIA;
                else if (bolaY > centroJogadorY && y < 600 - altura) y += velIA;

                // Persegue a bola no X, mas sem entrar na área do próprio goleiro (limite 690)
                if (bolaX < centroJogadorX && x > 410) x -= velIA;
                else if (bolaX > centroJogadorX && x < 690) x += velIA;
            }
        }

        // 🛡️ COMPORTAMENTO PARA A IA DO LADO ESQUERDO (Caso o sorteio mude seu lado)
        else if (lado.equals("esquerda")) {
            // Se a bola estiver no campo de ataque dela (Direita)
            if (bolaX > 400) {
                if (bolaY < centroJogadorY && y > 60) y -= velIA;
                else if (bolaY > centroJogadorY && y < 600 - altura) y += velIA;

                // Avança até o meio de campo no máximo (400)
                if (x < 360) x += velIA;
            }
            // Se a bola estiver na defesa dela (Esquerda)
            else {
                if (bolaY < centroJogadorY && y > 60) y -= velIA;
                else if (bolaY > centroJogadorY && y < 600 - altura) y += velIA;

                // Persegue a bola no X, respeitando a linha do seu goleiro (limite 90)
                if (bolaX > centroJogadorX && x < 360) x += velIA;
                else if (bolaX < centroJogadorX && x > 90) x -= velIA;
            }
        }
    }

    public Rectangle getLimites() {
        return new Rectangle(x, y, largura, altura);
    }
}