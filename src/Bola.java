import java.awt.Rectangle;

public class Bola {
    public int x = 390;
    public int y = 300;
    public int tamanho = 15;
    public int velocidadeX = 4;
    public int velocidadeY = 4; // Corrigido para o padrão com "e"

    public void mexer() {
        x += velocidadeX;
        y += velocidadeY;

        // Teto e Chão
        if (y <= 60) { y = 60; velocidadeY = -velocidadeY; }
        if (y >= 600 - tamanho) { y = 600 - tamanho; velocidadeY = -velocidadeY; }

        // Paredes externas temporárias (Gols)
        if (x <= 10) { x = 10; velocidadeX = -velocidadeX; }
        if (x >= 775 - tamanho) { x = 775 - tamanho; velocidadeX = -velocidadeX; }
    }

    public Rectangle getLimites() {
        return new Rectangle(x, y, tamanho, tamanho);
    }

    // 💥 SISTEMA DE COLISÃO ANTITUNELAMENTO (Impede a bola de atravessar os bonecos)
    public void verificarColisao(Jogador jogador) {
        if (this.getLimites().intersects(jogador.getLimites())) {

            // Centraliza os pontos para saber quem está à esquerda de quem
            int centroBolaX = this.x + (this.tamanho / 2);
            int centroJogadorX = jogador.x + (jogador.largura / 2);

            int centroBolaY = this.y + (this.tamanho / 2);
            int centroJogadorY = jogador.y + (jogador.altura / 2);

            // Calcula a sobreposição (o quanto um entrou para dentro do outro)
            int sobreposicaoX = (this.tamanho / 2) + (jogador.largura / 2) - Math.abs(centroBolaX - centroJogadorX);
            int sobreposicaoY = (this.tamanho / 2) + (jogador.altura / 2) - Math.abs(centroBolaY - centroJogadorY);

            // Se o menor ponto de entrada foi pelo lado esquerdo/direito (Eixo X)
            if (sobreposicaoX < sobreposicaoY) {
                if (centroBolaX < centroJogadorX) {
                    // Bola bateu no lado ESQUERDO do boneco -> Empurra para fora e rebate para a esquerda
                    this.x = jogador.x - this.tamanho;
                    if (velocidadeX > 0) velocidadeX = -velocidadeX;
                } else {
                    // Bola bateu no lado DIREITO do boneco -> Empurra para fora e rebate para a direita
                    this.x = jogador.x + jogador.largura;
                    if (velocidadeX < 0) velocidadeX = -velocidadeX;
                }
            }
            // Se o impacto foi por cima ou por baixo (Eixo Y)
            else {
                if (centroBolaY < centroJogadorY) {
                    // Bola bateu na CABEÇA do boneco -> Empurra para cima e rebate para cima
                    this.y = jogador.y - this.tamanho;
                    if (velocidadeY > 0) velocidadeY = -velocidadeY;
                } else {
                    // Bola bateu nos PÉS do boneco -> Empurra para baixo e rebate para baixo
                    this.y = jogador.y + jogador.altura;
                    if (velocidadeY < 0) velocidadeY = -velocidadeY;
                }
            }
        }
    }
}