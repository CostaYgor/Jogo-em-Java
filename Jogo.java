import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Jogo extends JPanel implements KeyListener {
    private Jogador jogador;
    private List<Inimigo> inimigos;
    private List<Projetil> projeteis;
    private boolean emJogo;
    private Random random;

    public Jogo() {
        setFocusable(true);
        addKeyListener(this);
        emJogo = true;
        jogador = new Jogador(50, 300); // Nave à esquerda
        inimigos = new ArrayList<>();
        projeteis = new ArrayList<>();
        random = new Random();

        // Inicializa os inimigos à direita em menor número
        for (int i = 0; i < 3; i++) { // Reduzido para 3 inimigos
            for (int j = 0; j < 2; j++) { // Reduzido para 2 linhas
                inimigos.add(new Inimigo(700 + i * 50, 50 + j * 100));
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (emJogo) {
            jogador.desenhar(g);
            for (Inimigo inimigo : inimigos) {
                inimigo.desenhar(g);
            }
            for (Projetil projetil : projeteis) {
                projetil.desenhar(g);
            }
        } else {
            g.setColor(Color.WHITE);
            g.drawString("Fim de Jogo", 350, 300);
        }
    }

    public void atualizar() {
        if (emJogo) {
            jogador.atualizar();
            for (Inimigo inimigo : inimigos) {
                inimigo.atualizar();
            }
            for (Projetil projetil : projeteis) {
                projetil.atualizar();
            }
            verificarColisoes();
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
            if (inimigo.getX() < 50) {
                emJogo = false;
                break;
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            jogador.moverCima();
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            jogador.moverBaixo();
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            projeteis.add(new Projetil(jogador.getX() + 20, jogador.getY()));
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Space Invaders - Zumbis");
        Jogo jogo = new Jogo();
        frame.add(jogo);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        while (true) {
            jogo.atualizar();
            jogo.repaint();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Jogador {
    private int x, y;

    public Jogador(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void desenhar(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(x, y, 20, 50); // Nave vertical
    }

    public void atualizar() {
        // Lógica de atualização do jogador
    }

    public void moverCima() {
        y -= 10;
        if (y < 0) y = 0;
    }

    public void moverBaixo() {
        y += 10;
        if (y > 550) y = 550;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 20, 50);
    }
}

class Inimigo {
    private int x, y;
    private Random random;
    private int direcaoY; // Controla o movimento em zigue-zague

    public Inimigo(int x, int y) {
        this.x = x;
        this.y = y;
        this.random = new Random();
        this.direcaoY = random.nextInt(3) - 1; // -1, 0 ou 1 (movimento aleatório)
    }

    public void desenhar(Graphics g) {
        g.setColor(Color.RED);
        g.fillRect(x, y, 20, 30); // Inimigos menores
    }

    public void atualizar() {
        x -= 1; // Movimento mais lento para a esquerda

        // Movimento em zigue-zague
        y += direcaoY;
        if (y < 0 || y > 570) { // Inverte a direção ao atingir os limites da tela
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

    public Rectangle getBounds() {
        return new Rectangle(x, y, 20, 30);
    }
}

class Projetil {
    private int x, y;

    public Projetil(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void desenhar(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillRect(x, y, 10, 5); // Projétil horizontal
    }

    public void atualizar() {
        x += 5; // Movimento para a direita
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 10, 5);
    }
}