import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Jogo extends JPanel implements KeyListener, Runnable {
    private Jogador jogador;
    private List<Inimigo> inimigos;
    private List<Projetil> projeteis;
    private boolean emJogo;
    private boolean noMenu; // Controla se o jogo está no menu
    private Random random;
    private int larguraTela;
    private int alturaTela;
    private int maxInimigos = 5; // Número máximo de inimigos na tela
    private Set<Integer> teclasPressionadas = new HashSet<>(); // Armazena as teclas pressionadas

    // Controle de disparos
    private int disparosRealizados = 0; // Contador de disparos
    private long ultimoDisparo = 0; // Tempo do último disparo
    private boolean emRecarga = false; // Estado de recarga
    private long tempoRecarga = 0; // Tempo de início da recarga

    // Imagem de fundo
    private Image imagemFundo;

    public Jogo() {
        setFocusable(true);
        addKeyListener(this);
        emJogo = false; // O jogo começa no menu
        noMenu = true; // Inicia no menu
        random = new Random();
        projeteis = new ArrayList<>();
        inimigos = new ArrayList<>();

        // Obtém o tamanho da tela
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        larguraTela = (int) screenSize.getWidth();
        alturaTela = (int) screenSize.getHeight();

        // Carrega a imagem de fundo
        try {
            File file = new File("rua.png");
            if (file.exists()) {
                System.out.println("Arquivo encontrado: " + file.getAbsolutePath());
                imagemFundo = new ImageIcon(file.getAbsolutePath()).getImage();
                if (imagemFundo != null) {
                    System.out.println("Imagem de fundo carregada com sucesso! Largura: " + imagemFundo.getWidth(this) + ", Altura: " + imagemFundo.getHeight(this));
                } else {
                    System.out.println("Erro: A imagem de fundo é nula.");
                }
            } else {
                System.out.println("Erro: Arquivo rua.png não encontrado.");
                System.out.println("Caminho procurado: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar a imagem de fundo: " + e.getMessage());
        }

        // Configura a janela maximizada
        JFrame frame = new JFrame("Last Days");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(larguraTela - 100, alturaTela - 100); // Janela um pouco menor que a tela
        frame.setLocationRelativeTo(null); // Centraliza a janela
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximiza a janela
        frame.add(this);
        frame.setVisible(true);

        // Inicializa o jogador com o spritesheet
        int larguraJogador = 131; // Largura do jogador (frame)
        int alturaJogador = 124; // Altura do jogador (frame)
        jogador = new Jogador(larguraTela / 20, alturaTela / 2 - alturaJogador / 2, larguraJogador, alturaJogador, "jogador.png");

        // Inicia a thread de atualização do jogo
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Desenha a imagem de fundo redimensionada
        if (imagemFundo != null) {
            g.drawImage(imagemFundo, 0, 0, getWidth(), getHeight(), this);
        } else {
            System.out.println("Imagem de fundo é nula no paintComponent.");
        }

        if (noMenu) {
            // Exibe o menu inicial
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("Aperte espaço para começar", larguraTela / 2 - 200, alturaTela / 2);
        } else if (emJogo) {
            // Exibe o jogo
            jogador.desenhar(g);
            for (Inimigo inimigo : inimigos) {
                inimigo.desenhar(g);
            }
            for (Projetil projetil : projeteis) {
                projetil.desenhar(g);
            }

            // Exibe mensagem de recarga
            if (emRecarga) {
                g.setColor(Color.RED);
                g.drawString("Recarregando...", 20, 20);
            }

            // Exibe contador de munição acima do jogador
            g.setColor(Color.WHITE);
            g.drawString("Munição: " + (7 - disparosRealizados), jogador.getX(), jogador.getY() - 10);
        } else {
            // Exibe mensagem de fim de jogo
            g.setColor(Color.WHITE);
            g.drawString("Fim de Jogo", larguraTela / 2 - 50, alturaTela / 2);
        }
    }

    public void atualizar() {
        if (emJogo) {
            // Verifica as teclas pressionadas e executa as ações correspondentes
            int direcao = -1; // -1 indica que não está se movendo
            if (teclasPressionadas.contains(KeyEvent.VK_UP)) {
                jogador.moverCima();
                direcao = KeyEvent.VK_UP;
            }
            if (teclasPressionadas.contains(KeyEvent.VK_DOWN)) {
                jogador.moverBaixo();
                direcao = KeyEvent.VK_DOWN;
            }

            // Verifica se o jogador está atirando
            boolean atirando = teclasPressionadas.contains(KeyEvent.VK_SPACE);

            // Atualiza o frame do jogador
            jogador.atualizarFrame(direcao, atirando);

            // Disparar projéteis
            if (atirando) {
                disparar();
            }

            // Atualiza inimigos e projéteis
            for (Inimigo inimigo : inimigos) {
                inimigo.atualizar();
            }
            for (Projetil projetil : projeteis) {
                projetil.atualizar();
            }

            // Verifica colisões e spawn de inimigos
            verificarColisoes();
            spawnInimigos();
            removerInimigosForaDaTela();
            verificarRecarga();
        }
    }

    private void disparar() {
        long tempoAtual = System.currentTimeMillis();

        // Verifica se o jogador pode disparar
        if (!emRecarga && tempoAtual - ultimoDisparo >= 500) { // 0,5 segundos entre disparos
            projeteis.add(new Projetil(jogador.getX() + jogador.getLargura(), jogador.getY() + jogador.getAltura() / 2, 10, 10));
            ultimoDisparo = tempoAtual;
            disparosRealizados++;

            // Inicia a recarga após 7 disparos
            if (disparosRealizados >= 7) {
                emRecarga = true;
                tempoRecarga = tempoAtual;
                disparosRealizados = 0; // Reinicia o contador de disparos
            }
        }
    }

    private void verificarRecarga() {
        if (emRecarga) {
            long tempoAtual = System.currentTimeMillis();

            // Verifica se a recarga terminou
            if (tempoAtual - tempoRecarga >= 2000) { // 2 segundos de recarga
                emRecarga = false;
            }
        }
    }

    private void verificarColisoes() {
        // Verifica colisões entre projéteis e inimigos
        for (Projetil projetil : new ArrayList<>(projeteis)) {
            for (Inimigo inimigo : new ArrayList<>(inimigos)) {
                if (projetil.getBounds().intersects(inimigo.getBounds())) {
                    projeteis.remove(projetil);
                    inimigos.remove(inimigo);
                    break;
                }
            }
        }

        // Verifica se os inimigos chegaram ao jogador
        for (Inimigo inimigo : inimigos) {
            if (inimigo.getX() < larguraTela / 20) {
                emJogo = false;
                break;
            }
        }
    }

    private void spawnInimigos() {
        if (inimigos.size() < maxInimigos) {
            int larguraInimigo = 50; // Largura do inimigo
            int alturaInimigo = 50; // Altura do inimigo

            // Gera uma posição Y aleatória para o inimigo
            int y = random.nextInt(alturaTela - alturaInimigo);

            // Verifica se há espaço para spawnar o inimigo sem sobrepor outros
            boolean podeSpawnar = true;
            Rectangle novoInimigo = new Rectangle(larguraTela, y, larguraInimigo, alturaInimigo);
            for (Inimigo inimigo : inimigos) {
                if (novoInimigo.intersects(inimigo.getBounds())) {
                    podeSpawnar = false;
                    break;
                }
            }

            // Spawna o inimigo se houver espaço
            if (podeSpawnar) {
                inimigos.add(new Inimigo(larguraTela, y, larguraInimigo, alturaInimigo));
            }
        }
    }

    private void removerInimigosForaDaTela() {
        // Remove inimigos que saíram da tela
        inimigos.removeIf(inimigo -> inimigo.getX() + inimigo.getLargura() < 0);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (noMenu && e.getKeyCode() == KeyEvent.VK_SPACE) {
            // Sai do menu e inicia o jogo
            noMenu = false;
            emJogo = true;
        } else if (emJogo) {
            // Adiciona a tecla pressionada ao conjunto
            teclasPressionadas.add(e.getKeyCode());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Remove a tecla liberada do conjunto
        teclasPressionadas.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void run() {
        // Loop de atualização do jogo
        while (true) {
            if (emJogo || noMenu) {
                atualizar();
                repaint();
            }
            try {
                Thread.sleep(10); // Controla a taxa de atualização
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Jogo(); // Inicia o jogo
    }
}

class Jogador {
    private BufferedImage spritesheet; // Imagem completa do spritesheet
    private BufferedImage[] frames; // Array para armazenar os frames
    private int frameAtual = 4; // Frame atual da animação (inicia parado, frame 4)
    private int larguraFrame, alturaFrame; // Largura e altura de cada frame
    private int x, y; // Posição do jogador
    private boolean atirando = false; // Indica se o jogador está atirando

    public Jogador(int x, int y, int larguraFrame, int alturaFrame, String caminhoSpritesheet) {
        this.x = x;
        this.y = y;
        this.larguraFrame = larguraFrame;
        this.alturaFrame = alturaFrame;

        try {
            // Carrega o spritesheet
            spritesheet = ImageIO.read(getClass().getResource(caminhoSpritesheet));
            if (spritesheet == null) {
                System.out.println("Erro: Spritesheet não carregado. Verifique o caminho: " + caminhoSpritesheet);
                return;
            }
            System.out.println("Spritesheet carregado com sucesso!");

            // Verifica o tamanho do spritesheet
            int larguraSpritesheet = spritesheet.getWidth();
            int alturaSpritesheet = spritesheet.getHeight();
            System.out.println("Tamanho do spritesheet: " + larguraSpritesheet + "x" + alturaSpritesheet);

            // Divide o spritesheet em frames
            int colunas = 3; // Número de colunas no spritesheet
            int linhas = 3; // Número de linhas no spritesheet
            frames = new BufferedImage[colunas * linhas]; // Total de frames

            for (int linha = 0; linha < linhas; linha++) {
                for (int coluna = 0; coluna < colunas; coluna++) {
                    int frameX = coluna * larguraFrame;
                    int frameY = linha * alturaFrame;

                    // Verifica se as coordenadas estão dentro dos limites do spritesheet
                    if (frameX + larguraFrame <= larguraSpritesheet && frameY + alturaFrame <= alturaSpritesheet) {
                        int index = linha * colunas + coluna;
                        frames[index] = spritesheet.getSubimage(frameX, frameY, larguraFrame, alturaFrame);
                        System.out.println("Frame " + index + " extraído com sucesso: " + frames[index]);
                    } else {
                        System.out.println("Erro: Frame " + (linha * colunas + coluna) + " está fora dos limites do spritesheet.");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar o spritesheet: " + e.getMessage());
        }
    }

    public void desenhar(Graphics g) {
        if (frames == null) {
            System.out.println("Erro: Frames não foram carregados.");
            return;
        }

        if (frameAtual < 0 || frameAtual >= frames.length || frames[frameAtual] == null) {
            System.out.println("Erro: Frame atual é nulo ou inválido. frameAtual = " + frameAtual);
            return;
        }

        // Desenha o frame atual
        g.drawImage(frames[frameAtual], x, y, null);
        System.out.println("Desenhando frame " + frameAtual + " na posição (" + x + ", " + y + ")");
    }

    // Método para atualizar o frame da animação
    public void atualizarFrame(int direcao, boolean atirando) {
        this.atirando = atirando;

        // Define o frame padrão como "parado" (frame 4)
        frameAtual = 4;

        if (atirando) {
            // Atirando
            switch (direcao) {
                case KeyEvent.VK_UP:
                    frameAtual = 7; // Atirando enquanto anda para cima
                    break;
                case KeyEvent.VK_DOWN:
                    frameAtual = 2; // Atirando enquanto anda para baixo
                    break;
                default:
                    frameAtual = 6; // Atirando parado
                    break;
            }
        } else {
            // Não está atirando
            switch (direcao) {
                case KeyEvent.VK_UP:
                    frameAtual = 1; // Andando para cima
                    break;
                case KeyEvent.VK_DOWN:
                    frameAtual = 3; // Andando para baixo
                    break;
            }
        }

        // Verifica se o frameAtual está dentro dos limites
        if (frameAtual < 0 || frameAtual >= frames.length) {
            System.out.println("Erro: frameAtual fora dos limites. frameAtual = " + frameAtual);
            frameAtual = 4; // Define o frame "parado" como fallback
        }
    }

    public void moverCima() {
        y -= 10;
        if (y < 0) y = 0;
    }

    public void moverBaixo() {
        y += 10;
        if (y > Toolkit.getDefaultToolkit().getScreenSize().height - alturaFrame) {
            y = Toolkit.getDefaultToolkit().getScreenSize().height - alturaFrame;
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getLargura() {
        return larguraFrame;
    }

    public int getAltura() {
        return alturaFrame;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, larguraFrame, alturaFrame);
    }
}

class Inimigo {
    private int x, y;
    private int largura, altura;
    private Random random;
    private int direcaoY; // Controla o movimento em zigue-zague

    public Inimigo(int x, int y, int largura, int altura) {
        this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
        this.random = new Random();
        this.direcaoY = random.nextInt(3) - 1; // -1, 0 ou 1 (movimento aleatório)
    }

    public void desenhar(Graphics g) {
        g.setColor(Color.RED);
        g.fillRect(x, y, largura, altura); // Inimigos menores
    }

    public void atualizar() {
        x -= 1; // Movimento mais lento para a esquerda

        // Movimento em zigue-zague
        y += direcaoY;
        if (y < 0 || y > Toolkit.getDefaultToolkit().getScreenSize().height - altura) { // Inverte a direção ao atingir os limites da tela
            direcaoY *= -1;
        }

        // Aleatoriedade no movimento
        if (random.nextInt(100) < 5) { // 5% de chance de mudar a direção
            direcaoY = random.nextInt(3) - 1;
        }
    }

    public int getX() {
        return x;
    }

    public int getLargura() {
        return largura;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, largura, altura);
    }
}

class Projetil {
    private int x, y;
    private int largura, altura;

    public Projetil(int x, int y, int largura, int altura) {
        this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
    }

    public void desenhar(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillRect(x, y, largura, altura); // Projétil horizontal
    }

    public void atualizar() {
        x += 5; // Movimento para a direita
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, largura, altura);
    }
}