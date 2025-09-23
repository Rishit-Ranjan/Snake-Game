import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Random;
import java.util.ArrayList;
import java.io.*;
import javax.sound.sampled.*;

public class App extends JPanel implements ActionListener, KeyListener {
    private static class Tile {
        int x;
        int y;

        Tile(int x, int y) { this.x = x; this.y = y; }
    }

    private enum FoodType {
        NORMAL(Color.RED, 1),
        BONUS(Color.YELLOW, 3),
        SPEED_BOOST(Color.CYAN, 1),
        SHRINK(Color.MAGENTA, -2),
        GHOST(Color.WHITE, 1);

        final Color color;
        final int growthValue;

        FoodType(Color color, int growthValue) {
            this.color = color;
            this.growthValue = growthValue;
        }
    }

    private static class Food {
        Point location;
        FoodType type;

        Food(int x, int y, FoodType type) {
            this.location = new Point(x, y);
            this.type = type;
        }
    }

    private enum GameState {
        TITLE_SCREEN,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    private enum Difficulty {
        EASY, NORMAL, HARD
    }

    private final int BOARD_WIDTH = 35;
    private final int BOARD_HEIGHT = 35;
    private final int CELL_SIZE = 20;
    private final int MIN_GAME_SPEED = 40;      // Maximum speed (minimum delay)
    private final int BOOST_DURATION = 5000;    // 5 seconds for speed boost
    private final int BOOST_SPEED = 30;         // Delay for speed boost
    private final int GHOST_DURATION = 5000;    // 5 seconds for ghost mode
    private static final String HIGH_SCORE_FILE = "highscore.txt";

    private ArrayList<Tile> snake;
    private char direction;
    private Timer timer;
    private int currentGameSpeed;
    private int highScore = 0;
    private Random random;
    private Food currentFood;
    private boolean isBoosted = false;
    private Timer boostTimer;
    private boolean isGhostMode = false;
    private Timer ghostTimer;
    private long boostStartTime;
    private long ghostStartTime;
    private long pauseStartTime;
    private GameState gameState;
    private Difficulty selectedDifficulty;
    private boolean isWrapAroundMode;
    private int initialGameSpeed;
    private int speedIncrement;

    // UI Components
    private JButton easyButton, normalButton, hardButton;
    private JCheckBox wrapAroundCheckBox;
    private JButton restartButton;  // Add this line
    private JButton mainMenuButton;
    private Clip eatSound;
    private Clip gameOverSound;

    public App() {
        setPreferredSize(new Dimension(BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        
        initUI();

        // Load sounds - requires eat.wav and gameover.wav in the src folder
        //eatSound = loadSound("/eat.wav");
        //gameOverSound = loadSound("/gameover.wav");

        loadHighScore();
        gameState = GameState.TITLE_SCREEN;
        snake = new ArrayList<>();
    }

    private void initUI() {
        setLayout(null);

        // Title Screen Buttons
        easyButton = new JButton("Easy");
        styleButton(easyButton);
        easyButton.setBounds((BOARD_WIDTH * CELL_SIZE - 120) / 2, 180, 120, 40);
        easyButton.addActionListener(e -> startGame(Difficulty.EASY));
        add(easyButton);

        normalButton = new JButton("Normal");
        styleButton(normalButton);
        normalButton.setBounds((BOARD_WIDTH * CELL_SIZE - 120) / 2, 230, 120, 40);
        normalButton.addActionListener(e -> startGame(Difficulty.NORMAL));
        add(normalButton);

        hardButton = new JButton("Hard");
        styleButton(hardButton);
        hardButton.setBounds((BOARD_WIDTH * CELL_SIZE - 120) / 2, 280, 120, 40);
        hardButton.addActionListener(e -> startGame(Difficulty.HARD));
        add(hardButton);

        // Wrap-around mode checkbox
        wrapAroundCheckBox = new JCheckBox("Wrap Around Walls");
        wrapAroundCheckBox.setBounds((BOARD_WIDTH * CELL_SIZE - 150) / 2, 330, 150, 30);
        wrapAroundCheckBox.setBackground(getBackground());
        wrapAroundCheckBox.setForeground(Color.WHITE);
        wrapAroundCheckBox.setFont(new Font("Arial", Font.PLAIN, 14));
        wrapAroundCheckBox.setFocusPainted(false);
        add(wrapAroundCheckBox);

        // Add restart button
        restartButton = new JButton("Restart");
        restartButton.setBounds((BOARD_WIDTH * CELL_SIZE - 120) / 2,
                        (BOARD_HEIGHT * CELL_SIZE) / 2 + 50,
                        120, 40);
        styleButton(restartButton);
        restartButton.addActionListener(e -> startGame(selectedDifficulty));
        restartButton.setVisible(false);
        add(restartButton);

        // Add main menu button
        mainMenuButton = new JButton("Main Menu");
        mainMenuButton.setBounds((BOARD_WIDTH * CELL_SIZE - 120) / 2,
                        (BOARD_HEIGHT * CELL_SIZE) / 2 + 100,
                        120, 40);
        styleButton(mainMenuButton);
        mainMenuButton.addActionListener(e -> showTitleScreen());
        mainMenuButton.setVisible(false);
        add(mainMenuButton);
    }

    private void startGame(Difficulty difficulty) {
        this.selectedDifficulty = difficulty;
        this.isWrapAroundMode = wrapAroundCheckBox.isSelected();
        switch (difficulty) {
            case EASY:
                initialGameSpeed = 150;
                speedIncrement = 3;
                break;
            case NORMAL:
                initialGameSpeed = 100;
                speedIncrement = 5;
                break;
            case HARD:
                initialGameSpeed = 70;
                speedIncrement = 7;
                break;
        }
        
        easyButton.setVisible(false);
        normalButton.setVisible(false);
        hardButton.setVisible(false);
        wrapAroundCheckBox.setVisible(false);
        restartButton.setVisible(false);
        mainMenuButton.setVisible(false);

        gameState = GameState.PLAYING;
        initGame();
        requestFocusInWindow();
    }

    private void showTitleScreen() {
        gameState = GameState.TITLE_SCREEN;
        
        restartButton.setVisible(false);
        mainMenuButton.setVisible(false);

        easyButton.setVisible(true);
        normalButton.setVisible(true);
        hardButton.setVisible(true);
        wrapAroundCheckBox.setVisible(true);

        repaint();
        requestFocusInWindow();
    }

    private void initGame() {
        if (boostTimer != null && boostTimer.isRunning()) {
            boostTimer.stop();
        }
        if (ghostTimer != null && ghostTimer.isRunning()) {
            ghostTimer.stop();
        }
        isBoosted = false;
        isGhostMode = false;

        snake.clear();
        snake.add(new Tile(BOARD_WIDTH/2, BOARD_HEIGHT/2));
        direction = 'R';
        random = new Random();
        spawnFood();
        currentGameSpeed = initialGameSpeed;
        
        timer = new Timer(currentGameSpeed, this);
        timer.start();
    }

    private void loadHighScore() {
        try (BufferedReader reader = new BufferedReader(new FileReader(HIGH_SCORE_FILE))) {
            String line = reader.readLine();
            if (line != null) {
                highScore = Integer.parseInt(line.trim());
            }
        } catch (IOException | NumberFormatException e) {
            // File doesn't exist or is invalid, default high score to 0.
            highScore = 0;
        }
    }

    private void saveHighScore() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HIGH_SCORE_FILE))) {
            writer.write(String.valueOf(highScore));
        } catch (IOException e) {
            e.printStackTrace(); // Log error if saving fails.
        }
    }

    /*private Clip loadSound(String filePath) {
        try {
            // The leading "/" is important for getResource to find the file at the root of the classpath
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResource(filePath));
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | NullPointerException e) {
            System.err.println("Warning: Could not load sound file: " + filePath);
            // e.printStackTrace(); // Uncomment for detailed error diagnosis
            return null;
        }
    }*/

    private void playSound(Clip clip) {
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0); // Rewind to the beginning
            clip.start();
        }
    }

    private void spawnFood() {
        int x, y;
        FoodType type;

        do {
            x = random.nextInt(BOARD_WIDTH);
            y = random.nextInt(BOARD_HEIGHT);
        } while (isSnakeAt(x, y));

        double chance = random.nextDouble();
        if (snake.size() > 10 && !isWrapAroundMode && chance < 0.10) { // 10% for ghost food (only in no-wrap mode)
            type = FoodType.GHOST;
        } else if (snake.size() > 15 && chance < 0.20) { // 10% chance for shrink food
            type = FoodType.SHRINK;
        } else if (snake.size() > 5 && chance < 0.30) { // 10% chance for bonus food
            type = FoodType.BONUS;
        } else if (snake.size() > 10 && chance < 0.40) { // 10% chance for speed boost
            type = FoodType.SPEED_BOOST;
        } else {
            type = FoodType.NORMAL;
        }

        currentFood = new Food(x, y, type);
    }

    private void handleFoodEaten() {
        FoodType eatenFoodType = currentFood.type;

        // 1. Handle snake growth
        // The snake automatically grows by 1 because we don't remove the tail.
        // For bonus growth, add extra segments to the tail.
        if (eatenFoodType.growthValue > 1) {
            Tile tail = snake.get(snake.size() - 1);
            for (int i = 0; i < eatenFoodType.growthValue - 1; i++) {
                snake.add(new Tile(tail.x, tail.y));
            }
        } else if (eatenFoodType.growthValue < 0) {
            // Shrink the snake
            int shrinkAmount = Math.abs(eatenFoodType.growthValue);
            // We also need to counteract the implicit growth of 1 from not removing the tail.
            int totalToRemove = shrinkAmount + 1;
            for (int i = 0; i < totalToRemove && snake.size() > 3; i++) {
                snake.remove(snake.size() - 1);
            }
        }

        // 2. Handle speed boost
        if (eatenFoodType == FoodType.SPEED_BOOST && !isBoosted) {
            isBoosted = true;
            boostStartTime = System.currentTimeMillis();
            timer.setDelay(BOOST_SPEED);
            boostTimer = new Timer(BOOST_DURATION, e -> {
                timer.setDelay(currentGameSpeed); // Revert to normal game speed
                isBoosted = false;
            });
            boostTimer.setRepeats(false);
            boostTimer.start();
        }

        // 3. Handle Ghost Mode
        if (eatenFoodType == FoodType.GHOST && !isGhostMode) {
            isGhostMode = true;
            ghostStartTime = System.currentTimeMillis();
            ghostTimer = new Timer(GHOST_DURATION, e -> {
                isGhostMode = false;
            });
            ghostTimer.setRepeats(false);
            ghostTimer.start();
        }
    }

    private void togglePause() {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.PAUSED;
            pauseStartTime = System.currentTimeMillis();
            timer.stop();
            if (boostTimer != null && boostTimer.isRunning()) {
                boostTimer.stop();
            }
            if (ghostTimer != null && ghostTimer.isRunning()) {
                ghostTimer.stop();
            }
        } else if (gameState == GameState.PAUSED) {
            long pauseDuration = System.currentTimeMillis() - pauseStartTime;
            if (isBoosted) {
                boostStartTime += pauseDuration;
            }
            if (isGhostMode) {
                ghostStartTime += pauseDuration;
            }
            gameState = GameState.PLAYING;
            timer.start();
            if (boostTimer != null && isBoosted) {
                boostTimer.start();
            }
            if (ghostTimer != null && isGhostMode) {
                ghostTimer.start();
            }
            requestFocusInWindow();
        }
        repaint();
    }

    private void endGame() {
        gameState = GameState.GAME_OVER;
        timer.stop();
        playSound(gameOverSound);
        int currentScore = snake.size() - 1;
        if (currentScore > highScore) {
            highScore = currentScore;
            saveHighScore();
        }
        restartButton.setVisible(true);
        mainMenuButton.setVisible(true);
    }

    private void move() {
        if (gameState != GameState.PLAYING) return;

        Tile head = snake.get(0);
        Tile newHead = new Tile(head.x, head.y);

        switch (direction) {
            case 'U': newHead.y--; break;
            case 'D': newHead.y++; break;
            case 'L': newHead.x--; break;
            case 'R': newHead.x++; break;
        }

        // Handle wrap-around mode
        if (isWrapAroundMode) {
            if (newHead.x < 0) newHead.x = BOARD_WIDTH - 1;
            else if (newHead.x >= BOARD_WIDTH) newHead.x = 0;
            if (newHead.y < 0) newHead.y = BOARD_HEIGHT - 1;
            else if (newHead.y >= BOARD_HEIGHT) newHead.y = 0;
        }

        // Check for game over conditions
        // 1. Collision with self
        if (isSnakeAt(newHead.x, newHead.y)) {
            endGame();
            return;
        }

        // 2. Collision with walls (only if not in wrap-around mode)
        if (!isWrapAroundMode && !isGhostMode && (newHead.x < 0 || newHead.x >= BOARD_WIDTH || newHead.y < 0 || newHead.y >= BOARD_HEIGHT)) {
            endGame();
            return;
        }

        snake.add(0, newHead);

        // Check if food is eaten
        if (newHead.x == currentFood.location.x && newHead.y == currentFood.location.y) {
            playSound(eatSound);
            handleFoodEaten();
            spawnFood();
            // Increase permanent speed only if not boosted
            if (!isBoosted && currentGameSpeed > MIN_GAME_SPEED && speedIncrement > 0) {
                currentGameSpeed -= speedIncrement;
                timer.setDelay(currentGameSpeed);
            }
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    private boolean isSnakeAt(int x, int y) {
        for (Tile tile : snake) {
            if (tile.x == x && tile.y == y) {
                return true;
            }
        }
        return false;
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(80, 80, 80));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEtchedBorder());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (gameState == GameState.TITLE_SCREEN) {
            // Draw Title
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            FontMetrics titleMetrics = getFontMetrics(g2d.getFont());
            g2d.drawString("Snake", (getWidth() - titleMetrics.stringWidth("Snake")) / 2, 80);

            // Show high score on title screen
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics metrics = getFontMetrics(g.getFont());
            String highScoreMsg = "High Score: " + highScore;
            g.drawString(highScoreMsg,
                    (BOARD_WIDTH * CELL_SIZE - metrics.stringWidth(highScoreMsg)) / 2,
                    130);
            return;
        }
        
        // --- Draw Game Elements (if not on title screen) ---

        // Draw Score and High Score
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics metrics = getFontMetrics(g2d.getFont());
        String scoreText = "Score: " + (snake.size() - 1);
        g2d.drawString(scoreText, 10, 20);
        String highScoreText = "High Score: " + highScore;
        g2d.drawString(highScoreText, BOARD_WIDTH * CELL_SIZE - metrics.stringWidth(highScoreText) - 10, 20);

        // Draw snake head
        Color headColor = new Color(0, 220, 0); // Brighter green for head
        Color bodyColor = Color.GREEN;

        if (isGhostMode) {
            headColor = new Color(0, 220, 0, 150); // Semi-transparent
            bodyColor = new Color(0, 255, 0, 150); // Semi-transparent
        }

        Tile head = snake.get(0);
        g2d.setColor(headColor);
        g2d.fillRoundRect(head.x * CELL_SIZE, head.y * CELL_SIZE, CELL_SIZE, CELL_SIZE, 8, 8);
        
        // Draw snake
        g2d.setColor(bodyColor);
        for (int i = 1; i < snake.size(); i++) {
            Tile p = snake.get(i);
            g2d.fillRoundRect(p.x * CELL_SIZE, p.y * CELL_SIZE, CELL_SIZE, CELL_SIZE, 8, 8);
        }

        // Draw food
        g2d.setColor(currentFood.type.color);
        g2d.fillRoundRect(currentFood.location.x * CELL_SIZE, currentFood.location.y * CELL_SIZE, CELL_SIZE, CELL_SIZE, 12, 12);

        // Draw help text for pause
        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.setColor(new Color(255, 255, 255, 100)); // Semi-transparent white
            g2d.drawString("Press 'P' to Pause", 10, getHeight() - 10);
        }

        // Draw power-up timers
        drawPowerUpTimers(g2d);

        // Draw game over message
        if (gameState == GameState.GAME_OVER) {
            drawOverlay(g2d, "Game Over!");
        }

        // Draw paused message
        if (gameState == GameState.PAUSED) {
            drawOverlay(g2d, "Paused");
        }
    }

    private void drawPowerUpTimers(Graphics2D g2d) {
        if (gameState != GameState.PLAYING && gameState != GameState.PAUSED) return;

        int barY = getHeight() - 45;
        final int BAR_HEIGHT = 10;
        final int BAR_MAX_WIDTH = 150;
        final int BAR_X = (getWidth() - BAR_MAX_WIDTH) / 2;
        long now = (gameState == GameState.PAUSED) ? pauseStartTime : System.currentTimeMillis();

        if (isBoosted) {
            long elapsed = now - boostStartTime;
            if (elapsed < BOOST_DURATION) {
                double remainingRatio = 1.0 - (double)elapsed / BOOST_DURATION;
                int barWidth = (int)(BAR_MAX_WIDTH * remainingRatio);

                g2d.setColor(Color.DARK_GRAY);
                g2d.fillRect(BAR_X, barY, BAR_MAX_WIDTH, BAR_HEIGHT);
                g2d.setColor(FoodType.SPEED_BOOST.color);
                g2d.fillRect(BAR_X, barY, barWidth, BAR_HEIGHT);
                g2d.setColor(Color.WHITE);
                g2d.drawRect(BAR_X, barY, BAR_MAX_WIDTH, BAR_HEIGHT);
                
                barY -= (BAR_HEIGHT + 5); // Move next bar up
            }
        }

        if (isGhostMode) {
            long elapsed = now - ghostStartTime;
            if (elapsed < GHOST_DURATION) {
                double remainingRatio = 1.0 - (double)elapsed / GHOST_DURATION;
                int barWidth = (int)(BAR_MAX_WIDTH * remainingRatio);

                g2d.setColor(Color.DARK_GRAY);
                g2d.fillRect(BAR_X, barY, BAR_MAX_WIDTH, BAR_HEIGHT);
                g2d.setColor(FoodType.GHOST.color);
                g2d.fillRect(BAR_X, barY, barWidth, BAR_HEIGHT);
                g2d.setColor(Color.WHITE);
                g2d.drawRect(BAR_X, barY, BAR_MAX_WIDTH, BAR_HEIGHT);
            }
        }
    }

    private void drawOverlay(Graphics2D g2d, String text) {
        g2d.setColor(new Color(0, 0, 0, 150)); // Semi-transparent overlay
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        FontMetrics metrics = getFontMetrics(g2d.getFont());
        g2d.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, getHeight() / 2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // Press 'P' to pause/resume the game
        if (keyCode == KeyEvent.VK_P && (gameState == GameState.PLAYING || gameState == GameState.PAUSED)) {
            togglePause();
            return;
        }

        // Ignore movement keys if game is not running or is paused
        if (gameState != GameState.PLAYING) {
            return;
        }

        switch (keyCode) {
            case KeyEvent.VK_UP:
                if (direction != 'D') direction = 'U';
                break;
            case KeyEvent.VK_DOWN:
                if (direction != 'U') direction = 'D';
                break;
            case KeyEvent.VK_LEFT:
                if (direction != 'R') direction = 'L';
                break;
            case KeyEvent.VK_RIGHT:
                if (direction != 'L') direction = 'R';
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Snake Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new App());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
