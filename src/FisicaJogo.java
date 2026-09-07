import java.awt.Rectangle;

public class FisicaJogo {

    private static final double ELASTICIDADE_PAREDE = 0.88;

    private final Cenario cenario;
    private final Bola bola;
    private boolean botPrendeuBolaNaParede;

    public FisicaJogo(Cenario cenario, Bola bola) {
        this.cenario = cenario;
        this.bola = bola;
    }

    public boolean atualizar() {
        botPrendeuBolaNaParede = false;

        double maiorVelocidade = Math.max(Math.abs(bola.velX), Math.abs(bola.velY));
        int passosFisica = Math.max(4,
                (int) Math.ceil(maiorVelocidade / (bola.tamanho / 3.0)));

        for (int passo = 0; passo < passosFisica; passo++) {
            bola.x += bola.velX / passosFisica;
            bola.y += bola.velY / passosFisica;

            aplicarLimitesRetosDaBola();
            aplicarFisicaCurvaCenario();

            aplicarColisaoFisica(cenario.linhaEsquerda, 1.0);
            aplicarColisaoFisica(cenario.goleiroEsquerda, 1.1);
            aplicarColisaoFisica(cenario.goleiroDireita, 1.1);
            aplicarColisaoFisica(cenario.linhaDireita, 1.0);
            desprenderBolaEntreJogadores();

            // Uma colisão com jogador pode empurrar a bola para dentro da parede.
            aplicarLimitesRetosDaBola();
            aplicarFisicaCurvaCenario();
        }

        bola.aplicarArrasto();
        return botPrendeuBolaNaParede;
    }

    private void aplicarLimitesRetosDaBola() {
        if (bola.y < DimensoesJogo.CAMPO_TOPO) {
            bola.y = DimensoesJogo.CAMPO_TOPO;
            bola.velY = Math.abs(bola.velY) * ELASTICIDADE_PAREDE;
        } else if (bola.y > DimensoesJogo.CAMPO_FUNDO - bola.tamanho) {
            bola.y = DimensoesJogo.CAMPO_FUNDO - bola.tamanho;
            bola.velY = -Math.abs(bola.velY) * ELASTICIDADE_PAREDE;
        }

        double centroBolaY = bola.y + bola.tamanho / 2.0;
        boolean centroNaAberturaDoGol = centroBolaY > DimensoesJogo.ABERTURA_LATERAL_TOPO
                && centroBolaY < DimensoesJogo.ABERTURA_LATERAL_FUNDO;

        if (!centroNaAberturaDoGol) {
            if (bola.x < DimensoesJogo.CAMPO_ESQUERDA) {
                bola.x = DimensoesJogo.CAMPO_ESQUERDA;
                bola.velX = Math.abs(bola.velX) * ELASTICIDADE_PAREDE;
            } else if (bola.x > DimensoesJogo.CAMPO_DIREITA - bola.tamanho) {
                bola.x = DimensoesJogo.CAMPO_DIREITA - bola.tamanho;
                bola.velX = -Math.abs(bola.velX) * ELASTICIDADE_PAREDE;
            }
        }

        aplicarColisaoComTraves();
    }

    private void aplicarColisaoComTraves() {
        aplicarColisaoTrave(DimensoesJogo.CAMPO_ESQUERDA, DimensoesJogo.GOL_TOPO);
        aplicarColisaoTrave(DimensoesJogo.CAMPO_ESQUERDA, DimensoesJogo.GOL_FUNDO);
        aplicarColisaoTrave(DimensoesJogo.CAMPO_DIREITA, DimensoesJogo.GOL_TOPO);
        aplicarColisaoTrave(DimensoesJogo.CAMPO_DIREITA, DimensoesJogo.GOL_FUNDO);
    }

    private void aplicarColisaoTrave(double traveX, double traveY) {
        double raioBola = bola.tamanho / 2.0;
        double centroBolaX = bola.x + raioBola;
        double centroBolaY = bola.y + raioBola;
        double dx = centroBolaX - traveX;
        double dy = centroBolaY - traveY;
        double distancia = Math.hypot(dx, dy);
        double distanciaMinima = raioBola + DimensoesJogo.TRAVE_RAIO;

        if (distancia >= distanciaMinima) return;

        double normalX;
        double normalY;
        double penetracao;

        if (distancia < 0.0001) {
            double velocidade = Math.hypot(bola.velX, bola.velY);
            if (velocidade > 0.0001) {
                normalX = -bola.velX / velocidade;
                normalY = -bola.velY / velocidade;
            } else {
                normalX = traveX == DimensoesJogo.CAMPO_ESQUERDA ? 1 : -1;
                normalY = 0;
            }
            penetracao = distanciaMinima;
        } else {
            normalX = dx / distancia;
            normalY = dy / distancia;
            penetracao = distanciaMinima - distancia;
        }

        bola.x += normalX * (penetracao + 0.2);
        bola.y += normalY * (penetracao + 0.2);

        double velocidadeContraTrave = bola.velX * normalX + bola.velY * normalY;
        if (velocidadeContraTrave < 0) {
            double elasticidade = 0.90;
            double impulso = (1.0 + elasticidade) * velocidadeContraTrave;
            bola.velX -= impulso * normalX;
            bola.velY -= impulso * normalY;
        }
    }

    private void aplicarFisicaCurvaCenario() {
        int raioCurva = DimensoesJogo.RAIO_CANTO;
        double centroBolaX = bola.x + bola.tamanho / 2.0;
        double centroBolaY = bola.y + bola.tamanho / 2.0;

        verificarCantoRedondo(centroBolaX, centroBolaY,
                DimensoesJogo.CAMPO_ESQUERDA + raioCurva,
                DimensoesJogo.CAMPO_TOPO + raioCurva, raioCurva, "sup_esq");
        verificarCantoRedondo(centroBolaX, centroBolaY,
                DimensoesJogo.CAMPO_ESQUERDA + raioCurva,
                DimensoesJogo.CAMPO_FUNDO - raioCurva, raioCurva, "inf_esq");
        verificarCantoRedondo(centroBolaX, centroBolaY,
                DimensoesJogo.CAMPO_DIREITA - raioCurva,
                DimensoesJogo.CAMPO_TOPO + raioCurva, raioCurva, "sup_dir");
        verificarCantoRedondo(centroBolaX, centroBolaY,
                DimensoesJogo.CAMPO_DIREITA - raioCurva,
                DimensoesJogo.CAMPO_FUNDO - raioCurva, raioCurva, "inf_dir");
    }

    private void verificarCantoRedondo(double bx, double by, double cx,
                                       double cy, double raio, String canto) {
        double dx = bx - cx;
        double dy = by - cy;
        double distanciaDoCentroDaCurva = Math.hypot(dx, dy);
        double limiteColisao = raio - bola.tamanho / 2.0;

        if (distanciaDoCentroDaCurva <= limiteColisao) return;

        boolean naAreaDoCanto = false;
        if (canto.equals("sup_esq") && dx < 0 && dy < 0) naAreaDoCanto = true;
        if (canto.equals("inf_esq") && dx < 0 && dy > 0) naAreaDoCanto = true;
        if (canto.equals("sup_dir") && dx > 0 && dy < 0) naAreaDoCanto = true;
        if (canto.equals("inf_dir") && dx > 0 && dy > 0) naAreaDoCanto = true;

        if (!naAreaDoCanto) return;

        double angulo = Math.atan2(dy, dx);
        bola.x = cx + Math.cos(angulo) * limiteColisao - bola.tamanho / 2.0;
        bola.y = cy + Math.sin(angulo) * limiteColisao - bola.tamanho / 2.0;

        double dotProduct = bola.velX * Math.cos(angulo) + bola.velY * Math.sin(angulo);
        if (dotProduct > 0) {
            bola.velX = (bola.velX - 2 * dotProduct * Math.cos(angulo)) * 0.8;
            bola.velY = (bola.velY - 2 * dotProduct * Math.sin(angulo)) * 0.8;
        }
    }

    private void aplicarColisaoFisica(Jogador jogador, double multiplicador) {
        double raioBola = bola.tamanho / 2.0;
        double centroBolaX = bola.x + raioBola;
        double centroBolaY = bola.y + raioBola;
        double pontoX = Math.max(jogador.x,
                Math.min(centroBolaX, jogador.x + jogador.largura));
        double pontoY = Math.max(jogador.y,
                Math.min(centroBolaY, jogador.y + jogador.altura));
        double dx = centroBolaX - pontoX;
        double dy = centroBolaY - pontoY;
        double distanciaQuadrada = dx * dx + dy * dy;

        if (distanciaQuadrada > raioBola * raioBola) return;

        double normalX;
        double normalY;
        double penetracao;

        if (distanciaQuadrada > 0.000001) {
            double distancia = Math.sqrt(distanciaQuadrada);
            normalX = dx / distancia;
            normalY = dy / distancia;
            penetracao = raioBola - distancia;
        } else {
            double distanciaEsquerda = centroBolaX - jogador.x;
            double distanciaDireita = jogador.x + jogador.largura - centroBolaX;
            double distanciaTopo = centroBolaY - jogador.y;
            double distanciaFundo = jogador.y + jogador.altura - centroBolaY;
            double menorDistancia = Math.min(
                    Math.min(distanciaEsquerda, distanciaDireita),
                    Math.min(distanciaTopo, distanciaFundo));

            if (menorDistancia == distanciaEsquerda) {
                normalX = -1;
                normalY = 0;
            } else if (menorDistancia == distanciaDireita) {
                normalX = 1;
                normalY = 0;
            } else if (menorDistancia == distanciaTopo) {
                normalX = 0;
                normalY = -1;
            } else {
                normalX = 0;
                normalY = 1;
            }
            penetracao = raioBola + menorDistancia;
        }

        bola.x += normalX * (penetracao + 0.5);
        bola.y += normalY * (penetracao + 0.5);

        double forcaRebote = 5.5 * multiplicador;
        double velocidadeParaFora = bola.velX * normalX + bola.velY * normalY;
        if (velocidadeParaFora < forcaRebote) {
            double impulso = forcaRebote - velocidadeParaFora;
            bola.velX += normalX * impulso;
            bola.velY += normalY * impulso;
        }

        desprenderBolaDaParede(jogador);
    }

    private void desprenderBolaDaParede(Jogador jogador) {
        double centroBolaX = bola.x + bola.tamanho / 2.0;
        double centroBolaY = bola.y + bola.tamanho / 2.0;
        double centroJogadorX = jogador.x + jogador.largura / 2.0;
        double centroJogadorY = jogador.y + jogador.altura / 2.0;
        boolean centroNaAberturaDoGol = centroBolaY > DimensoesJogo.ABERTURA_LATERAL_TOPO
                && centroBolaY < DimensoesJogo.ABERTURA_LATERAL_FUNDO;
        boolean naParedeEsquerda = !centroNaAberturaDoGol
                && bola.x <= DimensoesJogo.CAMPO_ESQUERDA + 4;
        boolean naParedeDireita = !centroNaAberturaDoGol
                && bola.x + bola.tamanho >= DimensoesJogo.CAMPO_DIREITA - 4;

        if (naParedeEsquerda || naParedeDireita) {
            double direcaoVertical = centroBolaY < centroJogadorY ? -1 : 1;
            if (centroBolaY < DimensoesJogo.CAMPO_TOPO + 35) direcaoVertical = 1;
            if (centroBolaY > DimensoesJogo.CAMPO_FUNDO - 35) direcaoVertical = -1;

            bola.x = naParedeEsquerda
                    ? DimensoesJogo.CAMPO_ESQUERDA
                    : DimensoesJogo.CAMPO_DIREITA - bola.tamanho;
            bola.velX = 0;
            bola.velY = direcaoVertical * Math.max(8.0, Math.abs(bola.velY));
            if (jogador == cenario.linhaDireita) botPrendeuBolaNaParede = true;
            return;
        }

        boolean naParedeSuperior = bola.y <= DimensoesJogo.CAMPO_TOPO + 4;
        boolean naParedeInferior = bola.y + bola.tamanho >= DimensoesJogo.CAMPO_FUNDO - 4;
        if (naParedeSuperior || naParedeInferior) {
            double direcaoHorizontal = centroBolaX < centroJogadorX ? -1 : 1;
            if (centroBolaX < DimensoesJogo.CAMPO_ESQUERDA + 35) direcaoHorizontal = 1;
            if (centroBolaX > DimensoesJogo.CAMPO_DIREITA - 35) direcaoHorizontal = -1;

            bola.y = naParedeSuperior
                    ? DimensoesJogo.CAMPO_TOPO
                    : DimensoesJogo.CAMPO_FUNDO - bola.tamanho;
            bola.velX = direcaoHorizontal * Math.max(8.0, Math.abs(bola.velX));
            bola.velY = 0;
            if (jogador == cenario.linhaDireita) botPrendeuBolaNaParede = true;
        }
    }

    private void desprenderBolaEntreJogadores() {
        Jogador[] jogadores = {
                cenario.linhaEsquerda,
                cenario.goleiroEsquerda,
                cenario.goleiroDireita,
                cenario.linhaDireita
        };

        for (int i = 0; i < jogadores.length - 1; i++) {
            for (int k = i + 1; k < jogadores.length; k++) {
                if (tentarDesprenderBolaEntre(jogadores[i], jogadores[k])) return;
            }
        }
    }

    private boolean tentarDesprenderBolaEntre(Jogador jogadorA, Jogador jogadorB) {
        double margem = 6.0;
        if (!jogadorPertoDaBola(jogadorA, margem)
                || !jogadorPertoDaBola(jogadorB, margem)) return false;

        double bolaCentroX = bola.x + bola.tamanho / 2.0;
        double bolaCentroY = bola.y + bola.tamanho / 2.0;
        double centroAX = jogadorA.x + jogadorA.largura / 2.0;
        double centroAY = jogadorA.y + jogadorA.altura / 2.0;
        double centroBX = jogadorB.x + jogadorB.largura / 2.0;
        double centroBY = jogadorB.y + jogadorB.altura / 2.0;
        double bolaAteAX = centroAX - bolaCentroX;
        double bolaAteAY = centroAY - bolaCentroY;
        double bolaAteBX = centroBX - bolaCentroX;
        double bolaAteBY = centroBY - bolaCentroY;
        double distanciaA = Math.hypot(bolaAteAX, bolaAteAY);
        double distanciaB = Math.hypot(bolaAteBX, bolaAteBY);

        if (distanciaA < 0.0001 || distanciaB < 0.0001) return false;

        double cosseno = (bolaAteAX * bolaAteBX + bolaAteAY * bolaAteBY)
                / (distanciaA * distanciaB);
        if (cosseno > -0.15) return false;

        double eixoX = centroBX - centroAX;
        double eixoY = centroBY - centroAY;
        double tamanhoEixo = Math.hypot(eixoX, eixoY);
        if (tamanhoEixo < 0.0001) return false;

        double saidaX = -eixoY / tamanhoEixo;
        double saidaY = eixoX / tamanhoEixo;
        double campoCentroX = (DimensoesJogo.CAMPO_ESQUERDA
                + DimensoesJogo.CAMPO_DIREITA) / 2.0;
        double campoCentroY = (DimensoesJogo.CAMPO_TOPO
                + DimensoesJogo.CAMPO_FUNDO) / 2.0;
        double direcaoParaOCentro = saidaX * (campoCentroX - bolaCentroX)
                + saidaY * (campoCentroY - bolaCentroY);

        if (direcaoParaOCentro < 0) {
            saidaX *= -1;
            saidaY *= -1;
        }

        bola.x += saidaX * 2.5;
        bola.y += saidaY * 2.5;
        bola.velX = saidaX * 7.0;
        bola.velY = saidaY * 7.0;
        return true;
    }

    private boolean jogadorPertoDaBola(Jogador jogador, double margem) {
        double raioComMargem = bola.tamanho / 2.0 + margem;
        double centroBolaX = bola.x + bola.tamanho / 2.0;
        double centroBolaY = bola.y + bola.tamanho / 2.0;
        double pontoX = Math.max(jogador.x,
                Math.min(centroBolaX, jogador.x + jogador.largura));
        double pontoY = Math.max(jogador.y,
                Math.min(centroBolaY, jogador.y + jogador.altura));
        double dx = centroBolaX - pontoX;
        double dy = centroBolaY - pontoY;
        return dx * dx + dy * dy <= raioComMargem * raioComMargem;
    }

    public boolean isNaAreaProibida(Jogador jogador) {
        Rectangle rectJogador = new Rectangle(
                jogador.x, jogador.y, jogador.largura, jogador.altura);
        Rectangle areaEsquerda = new Rectangle(
                DimensoesJogo.AREA_ESQUERDA_X, DimensoesJogo.AREA_TOPO,
                DimensoesJogo.AREA_LARGURA, DimensoesJogo.AREA_ALTURA);
        Rectangle areaDireita = new Rectangle(
                DimensoesJogo.AREA_DIREITA_X, DimensoesJogo.AREA_TOPO,
                DimensoesJogo.AREA_LARGURA, DimensoesJogo.AREA_ALTURA);
        return rectJogador.intersects(areaEsquerda) || rectJogador.intersects(areaDireita);
    }

    public boolean estaForaDoCampo(Jogador jogador) {
        if (jogador.x < DimensoesJogo.CAMPO_ESQUERDA
                || jogador.x > DimensoesJogo.CAMPO_DIREITA - jogador.largura
                || jogador.y < DimensoesJogo.CAMPO_TOPO
                || jogador.y > DimensoesJogo.CAMPO_FUNDO - jogador.altura) {
            return true;
        }

        int raioCurva = DimensoesJogo.RAIO_CANTO;
        double centroX = jogador.x + jogador.largura / 2.0;
        double centroY = jogador.y + jogador.altura / 2.0;
        double limite = raioCurva - jogador.largura / 2.0;

        if (centroX < DimensoesJogo.CAMPO_ESQUERDA + raioCurva
                && centroY < DimensoesJogo.CAMPO_TOPO + raioCurva
                && Math.hypot(centroX - (DimensoesJogo.CAMPO_ESQUERDA + raioCurva),
                centroY - (DimensoesJogo.CAMPO_TOPO + raioCurva)) > limite) {
            ajustarJogadorNaCurva(jogador,
                    DimensoesJogo.CAMPO_ESQUERDA + raioCurva,
                    DimensoesJogo.CAMPO_TOPO + raioCurva, limite);
        } else if (centroX < DimensoesJogo.CAMPO_ESQUERDA + raioCurva
                && centroY > DimensoesJogo.CAMPO_FUNDO - raioCurva
                && Math.hypot(centroX - (DimensoesJogo.CAMPO_ESQUERDA + raioCurva),
                centroY - (DimensoesJogo.CAMPO_FUNDO - raioCurva)) > limite) {
            ajustarJogadorNaCurva(jogador,
                    DimensoesJogo.CAMPO_ESQUERDA + raioCurva,
                    DimensoesJogo.CAMPO_FUNDO - raioCurva, limite);
        } else if (centroX > DimensoesJogo.CAMPO_DIREITA - raioCurva
                && centroY < DimensoesJogo.CAMPO_TOPO + raioCurva
                && Math.hypot(centroX - (DimensoesJogo.CAMPO_DIREITA - raioCurva),
                centroY - (DimensoesJogo.CAMPO_TOPO + raioCurva)) > limite) {
            ajustarJogadorNaCurva(jogador,
                    DimensoesJogo.CAMPO_DIREITA - raioCurva,
                    DimensoesJogo.CAMPO_TOPO + raioCurva, limite);
        } else if (centroX > DimensoesJogo.CAMPO_DIREITA - raioCurva
                && centroY > DimensoesJogo.CAMPO_FUNDO - raioCurva
                && Math.hypot(centroX - (DimensoesJogo.CAMPO_DIREITA - raioCurva),
                centroY - (DimensoesJogo.CAMPO_FUNDO - raioCurva)) > limite) {
            ajustarJogadorNaCurva(jogador,
                    DimensoesJogo.CAMPO_DIREITA - raioCurva,
                    DimensoesJogo.CAMPO_FUNDO - raioCurva, limite);
        }

        return false;
    }

    private void ajustarJogadorNaCurva(Jogador jogador, double cx,
                                        double cy, double limite) {
        double centroX = jogador.x + jogador.largura / 2.0;
        double centroY = jogador.y + jogador.altura / 2.0;
        double angulo = Math.atan2(centroY - cy, centroX - cx);
        jogador.x = (int) (cx + Math.cos(angulo) * limite - jogador.largura / 2.0);
        jogador.y = (int) (cy + Math.sin(angulo) * limite - jogador.altura / 2.0);
    }

    public void limitarGoleiroNaArea(Jogador goleiro, int limiteEsquerdo,
                                      int limiteDireito) {
        int maximoX = limiteDireito - goleiro.largura;
        int maximoY = DimensoesJogo.AREA_TOPO + DimensoesJogo.AREA_ALTURA
                - goleiro.altura;
        goleiro.x = Math.max(limiteEsquerdo, Math.min(maximoX, goleiro.x));
        goleiro.y = Math.max(DimensoesJogo.AREA_TOPO, Math.min(maximoY, goleiro.y));
    }

    public boolean bolaPertoDaParede(Bola b) {
        double centroBolaY = b.y + b.tamanho / 2.0;
        boolean centroNaAberturaDoGol = centroBolaY > DimensoesJogo.ABERTURA_LATERAL_TOPO
                && centroBolaY < DimensoesJogo.ABERTURA_LATERAL_FUNDO;
        boolean pertoDaLateral = !centroNaAberturaDoGol
                && (b.x <= DimensoesJogo.CAMPO_ESQUERDA + 4
                || b.x + b.tamanho >= DimensoesJogo.CAMPO_DIREITA - 4);
        boolean pertoDaHorizontal = b.y <= DimensoesJogo.CAMPO_TOPO + 4
                || b.y + b.tamanho >= DimensoesJogo.CAMPO_FUNDO - 4;
        return pertoDaLateral || pertoDaHorizontal;
    }

    public boolean jogadorEncostaNaBola(Jogador jogador, Bola b) {
        Rectangle areaDaBola = new Rectangle(
                (int) Math.floor(b.x) - 2,
                (int) Math.floor(b.y) - 2,
                b.tamanho + 4,
                b.tamanho + 4);
        Rectangle areaDoJogador = new Rectangle(
                jogador.x, jogador.y, jogador.largura, jogador.altura);
        return areaDaBola.intersects(areaDoJogador);
    }

    public boolean checarColisaoJogadores(Jogador jogadorA, Jogador jogadorB) {
        Rectangle areaA = new Rectangle(
                jogadorA.x, jogadorA.y, jogadorA.largura, jogadorA.altura);
        Rectangle areaB = new Rectangle(
                jogadorB.x, jogadorB.y, jogadorB.largura, jogadorB.altura);
        return areaA.intersects(areaB);
    }
}
