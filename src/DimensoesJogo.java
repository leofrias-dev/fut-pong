public final class DimensoesJogo {

    private DimensoesJogo() {
        // Classe utilitária: não deve ser instanciada.
    }

    public static final int TELA_LARGURA = 1200;
    public static final int TELA_ALTURA = 800;

    public static final int CAMPO_ESQUERDA = 10;
    public static final int CAMPO_DIREITA = 775;
    public static final int CAMPO_TOPO = 60;
    public static final int CAMPO_FUNDO = 600;
    public static final int CAMPO_LARGURA = CAMPO_DIREITA - CAMPO_ESQUERDA;
    public static final int CAMPO_ALTURA = CAMPO_FUNDO - CAMPO_TOPO;
    public static final int CAMPO_MEIO_X = 392;
    public static final int RAIO_CANTO = 50;

    public static final int GOL_TOPO = 270;
    public static final int GOL_FUNDO = 390;
    public static final int GOL_ALTURA = GOL_FUNDO - GOL_TOPO;
    public static final int GOL_PROFUNDIDADE = 45;
    public static final int TRAVE_RAIO = 3;

    // A abertura física e o gol desenhado precisam ter exatamente o mesmo tamanho.
    public static final int ABERTURA_LATERAL_TOPO = GOL_TOPO;
    public static final int ABERTURA_LATERAL_FUNDO = GOL_FUNDO;

    public static final int AREA_TOPO = 195;
    public static final int AREA_ALTURA = 270;
    public static final int AREA_LARGURA = 90;
    public static final int AREA_ESQUERDA_X = CAMPO_ESQUERDA;
    public static final int AREA_DIREITA_X = CAMPO_DIREITA - AREA_LARGURA;

    public static final int BOLA_INICIAL_X = 385;
    public static final int BOLA_INICIAL_Y = 320;

    public static final int GOLEIRO_ESQUERDA_INICIAL_X = 30;
    public static final int GOLEIRO_DIREITA_INICIAL_X = 740;
    public static final int JOGADOR_ESQUERDA_INICIAL_X = 200;
    public static final int JOGADOR_DIREITA_INICIAL_X = 570;
    public static final int JOGADORES_INICIAL_Y = 280;

    public static final int BOTAO_JOGAR_X = 312;
    public static final int BOTAO_JOGAR_Y = 280;
    public static final int BOTAO_JOGAR_LARGURA = 160;
    public static final int BOTAO_JOGAR_ALTURA = 50;
}
