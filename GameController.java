package com.example.newproject;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.*;

//Tolga Geçgen_150120064,Metehan Çelik_150119709,Ali İhsan Çakmak_150122602
// Main class for the Tower Defense game, handling the game logic, UI, and player interactions.
public class GameController extends Application {
    public enum TowerType { SINGLE, LASER, TRIPLE, MISSILE }

    int cellSize = 40;
    Level level;
    Player player;
    int waveIdx = 0;
    List<Tower> towers = new ArrayList<>();
    List<EnemyView> activeEnemies = new ArrayList<>();
    TowerType selectedTowerType = TowerType.SINGLE;
    Label infoLabel = new Label();
    Pane root = new Pane();
    Circle rangeCircle = null;
    boolean gameOver = false;
    Timeline waitForEnemiesClearTimeline;
    Timeline nextWaveCountdownTimeline;
    Timeline infoTimeline;
    int nextWaveCountdown = 0;
    String[] levels = { level1txt(), level2txt(), level3txt(), level4txt(), level5txt() };
    int currentLevelIdx = 0;


     //Sets up and displays the start screen, and handles transition to the game screen.
    @Override
    public void start(Stage stage) {
        VBox startScreen = new VBox(20);
        startScreen.setAlignment(Pos.CENTER);

        BackgroundImage backgroundImage = new BackgroundImage(
                new Image(Objects.requireNonNull(getClass().getResource("/firstScreen.jpg")).toExternalForm()),
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
        );
        startScreen.setBackground(new Background(backgroundImage));

        Button playButton = new Button("PLAY");
        playButton.setStyle("-fx-font-size: 60px; -fx-background-color: #4CAF50; -fx-text-fill: white;");

        playButton.setOnAction(e -> {
            player = new Player(5, 100);
            BorderPane gameLayout = new BorderPane();

            BackgroundImage gameBackground = new BackgroundImage(
                    new Image(Objects.requireNonNull(getClass().getResource("/background.jpg")).toExternalForm()),
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(1000, 600, false, false, false, false)
            );
            gameLayout.setBackground(new Background(gameBackground));

            root.setLayoutX(50);
            root.setLayoutY(50);

            gameLayout.getChildren().add(root);
            VBox dataPanel = new VBox(10);
            dataPanel.setPadding(new Insets(10));
            dataPanel.getChildren().add(infoLabel);
            gameLayout.setRight(dataPanel);

            loadLevel(currentLevelIdx);
            Scene gameScene = new Scene(gameLayout, 1000, 600);
            stage.setScene(gameScene);
        });
        startScreen.getChildren().add(playButton);

        Scene startScene = new Scene(startScreen, 800, 600);
        stage.setScene(startScene);
        stage.setTitle("Tower Defense");
        stage.show();
    }

    //Loads the specified level, resets game state, and initializes the UI for the level.
    void loadLevel(int idx) {
        if (infoTimeline != null) { infoTimeline.stop(); infoTimeline = null; }
        if (nextWaveCountdownTimeline != null) { nextWaveCountdownTimeline.stop(); nextWaveCountdownTimeline = null; }
        if (waitForEnemiesClearTimeline != null) { waitForEnemiesClearTimeline.stop(); waitForEnemiesClearTimeline = null; }

        for (Tower t : new ArrayList<>(towers)) {
            if (t.attackTimeline != null) t.attackTimeline.stop();
            root.getChildren().remove(t.imageView);
        }
        towers.clear();

        for (EnemyView ev : new ArrayList<>(activeEnemies)) {
            ev.timeline.stop();
            root.getChildren().remove(ev);
            if (ev.healthBar != null) root.getChildren().remove(ev.healthBar);
        }
        activeEnemies.clear();

        root.getChildren().clear();
        infoLabel.setText("");

        waveIdx = 0;
        nextWaveCountdown = 0;
        gameOver = false;

        try {
            level = loadLevelFromString(levels[idx]);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        root.setPrefSize(level.width * cellSize + 220, level.height * cellSize);
        drawGridAndPath();
        addTowerSelectionPanel();
        addInfoLabel();

        root.setOnMouseClicked(this::handleMouseClick);
        startWave();
        startInfoUpdater();
    }

    //Draws the grid and path for the level, and adds the path cells to the UI.
    void drawGridAndPath() {
        for (int row = 0; row < level.height; row++) {
            for (int col = 0; col < level.width; col++) {
                Rectangle rect = new Rectangle(col * cellSize, row * cellSize, cellSize, cellSize);
                rect.setFill(Math.random() > 0.5 ? Color.web("#ffe066") : Color.web("#ffd166"));
                rect.setStroke(Color.GOLDENROD);
                root.getChildren().add(rect);
            }
        }
        for (int[] cell : level.pathCells) {
            Rectangle rect = new Rectangle(cell[0] * cellSize, cell[1] * cellSize, cellSize, cellSize);
            rect.setFill(Color.LIGHTGRAY);
            rect.setStroke(Color.GOLDENROD);
            root.getChildren().add(rect);
        }
    }

    //Adds the tower selection panel to the UI, allowing players to choose different tower types.
    void addTowerSelectionPanel() {
        VBox panel = new VBox(8);
        panel.setLayoutX(level.width * cellSize + 20);
        panel.setLayoutY(20);

        Button singleBtn = new Button("Single Shot ($50)");
        Button laserBtn = new Button("Laser ($120)");
        Button tripleBtn = new Button("Triple Shot ($150)");
        Button missileBtn = new Button("Missile ($200)");

        singleBtn.setOnAction(e -> selectedTowerType = TowerType.SINGLE);
        laserBtn.setOnAction(e -> selectedTowerType = TowerType.LASER);
        tripleBtn.setOnAction(e -> selectedTowerType = TowerType.TRIPLE);
        missileBtn.setOnAction(e -> selectedTowerType = TowerType.MISSILE);

        panel.getChildren().addAll(singleBtn, laserBtn, tripleBtn, missileBtn);
        root.getChildren().add(panel);
    }

    //Adds the info label to the UI, displaying player stats and wave information.
    void addInfoLabel() {
        infoLabel.setStyle("-fx-font-size: 16px; -fx-background-color: #fffbe6;");
        infoLabel.setLayoutX(level.width * cellSize + 20);
        infoLabel.setLayoutY(200);
        root.getChildren().add(infoLabel);
    }

    //Starts a timeline to update the info label at regular intervals.
    void startInfoUpdater() {
        if (infoTimeline != null) infoTimeline.stop();
        infoTimeline = new Timeline(
                new KeyFrame(Duration.millis(200), e -> updateInfo())
        );
        infoTimeline.setCycleCount(Animation.INDEFINITE);
        infoTimeline.play();
    }

    //Returns the image name for the specified tower type.
    private String getTowerImageName(TowerType type) {
        switch (type) {
            case SINGLE: return "/tower_single.png";
            case LASER: return "/tower_laser.png";
            case TRIPLE: return "/tower_triple.png";
            case MISSILE: return "/tower_missile.png";
            default: return "/tower.png";
        }
    }

    // Handles mouse clicks on the game area, allowing players to place or interact with towers.
    void handleMouseClick(MouseEvent event) {
        int col = (int) (event.getX() / cellSize);
        int row = (int) (event.getY() / cellSize);

        if (col >= level.width || row >= level.height || col < 0 || row < 0 || gameOver) return;

        Optional<Tower> clickedTowerOpt = towers.stream()
                .filter(t -> t.x == col && t.y == row)
                .findFirst();

        if (clickedTowerOpt.isPresent()) {
            handleExistingTowerClick(clickedTowerOpt.get(), event);
        } else {
            handlePlaceNewTower(col, row);
        }
    }

    // Loads a level from a string representation, parsing the width, height, path cells, and wave data.
    Level loadLevelFromString(String txt) throws Exception {
        String[] lines = txt.split("\n");
        int width = 0, height = 0;
        List<int[]> path = new ArrayList<>();
        List<Wave> waves = new ArrayList<>();
        boolean waveSection = false;

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("WIDTH:")) width = Integer.parseInt(line.split(":")[1]);
            else if (line.startsWith("HEIGHT:")) height = Integer.parseInt(line.split(":")[1]);
            else if (line.equals("WAVE_DATA:")) waveSection = true;
            else if (!waveSection && !line.isEmpty() && !line.startsWith("//")) {
                String[] parts = line.split(",");
                path.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
            } else if (waveSection && !line.isEmpty() && !line.startsWith("//")) {
                String[] parts = line.split(",");
                waves.add(new Wave(
                        Integer.parseInt(parts[0].trim()),
                        Double.parseDouble(parts[1].trim()),
                        parts.length > 2 ? Double.parseDouble(parts[2].trim()) : 2.0
                ));
            }
        }
        return new Level(width, height, path, waves);
    }

    // Returns the price for the specified tower type.
    int getPriceForType(TowerType type) {
        switch (type) {
            case SINGLE: return 50;
            case LASER: return 120;
            case TRIPLE: return 150;
            case MISSILE: return 200;
        }
        return 50;
    }

    // Returns the range for the specified tower type.
    double getRangeForType(TowerType type) {
        switch (type) {
            case SINGLE: return 2.5 * cellSize;
            case LASER: return 2.5 * cellSize;
            case TRIPLE: return 2.2 * cellSize;
            case MISSILE: return 3.0 * cellSize;
        }
        return 2.5 * cellSize;
    }


// Handles the click event for existing towers, allowing players to drag and drop them to new locations or sell them.
    private void handleExistingTowerClick(Tower tower, MouseEvent initialEvent){
        tower.imageView.setOnMouseDragged(dragEvent -> {
            double newX = dragEvent.getSceneX() - root.getLayoutX() - tower.imageView.getFitWidth() / 2;
            double newY = dragEvent.getSceneY() - root.getLayoutY() - tower.imageView.getFitHeight() / 2;
            tower.imageView.setX(newX);
            tower.imageView.setY(newY);
        });

        tower.imageView.setOnMouseReleased(releaseEvent -> {
            tower.imageView.setOnMouseDragged(null);
            tower.imageView.setOnMouseReleased(null);

            double releaseX = releaseEvent.getSceneX() - root.getLayoutX();
            double releaseY = releaseEvent.getSceneY() - root.getLayoutY();

            int newCol = (int) (releaseX / cellSize);
            int newRow = (int) (releaseY / cellSize);

            if (newCol < 0 || newRow < 0 || newCol >= level.width || newRow >= level.height) {
                sellTower(tower);
            } else {
                boolean isPath = level.pathCells.stream().anyMatch(cell -> cell[0] == newCol && cell[1] == newRow);
                boolean hasTower = towers.stream().anyMatch(t -> t.x == newCol && t.y == newRow && t != tower);

                if (!isPath && !hasTower) {
                    tower.x = newCol;
                    tower.y = newRow;
                    tower.imageView.setX(newCol * cellSize + (cellSize - tower.imageView.getFitWidth()) / 2.0);
                    tower.imageView.setY(newRow * cellSize + (cellSize - tower.imageView.getFitHeight()) / 2.0);
                } else {
                    tower.imageView.setX(tower.x * cellSize + (cellSize - tower.imageView.getFitWidth()) / 2.0);
                    tower.imageView.setY(tower.y * cellSize + (cellSize - tower.imageView.getFitHeight()) / 2.0);
                }
            }
        });
    }

    // Sells the specified tower, removing it from the game and updating the player's money.
    private void sellTower(Tower tower) {
        if (tower.attackTimeline != null) {
            tower.attackTimeline.stop();
        }
        towers.remove(tower);
        root.getChildren().remove(tower.imageView);
        player.money += tower.price;
        updateInfo();
    }

    // Handles the placement of a new tower, checking if the location is valid and updating the game state accordingly.
    private void handlePlaceNewTower(int col, int row) {
        boolean isPath = level.pathCells.stream().anyMatch(cell -> cell[0] == col && cell[1] == row);
        boolean hasTower = towers.stream().anyMatch(t -> t.x == col && t.y == row);

        int price = getPriceForType(selectedTowerType);
        if (!isPath && !hasTower && player.money >= price) {
            Tower tower = new Tower(col, row, selectedTowerType, price);

            Image TowerImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream(getTowerImageName(selectedTowerType))));
            ImageView kuleView = new ImageView(TowerImg);
            kuleView.setFitWidth(cellSize - 12);
            kuleView.setFitHeight(cellSize - 12);
            kuleView.setX(col * cellSize + (cellSize - kuleView.getFitWidth()) / 2.0);
            kuleView.setY(row * cellSize + (cellSize - kuleView.getFitHeight()) / 2.0);
            root.getChildren().add(kuleView);

            tower.imageView = kuleView;
            towers.add(tower);
            player.money -= price;

            startTowerAttack(tower);
            showRangeCircle(col, row, getRangeForType(selectedTowerType));
            updateInfo();
        }
    }

    // Displays a range circle around the specified tower, indicating its attack range.
    void showRangeCircle(int col, int row, double radius) {
        if (rangeCircle != null) root.getChildren().remove(rangeCircle);
        rangeCircle = new Circle(
                col * cellSize + cellSize / 2.0,
                row * cellSize + cellSize / 2.0,
                radius
        );
        rangeCircle.setStroke(Color.RED);
        rangeCircle.setFill(Color.rgb(255, 0, 0, 0.08));
        rangeCircle.setMouseTransparent(true);
        root.getChildren().add(rangeCircle);

        PauseTransition pt = new PauseTransition(Duration.seconds(1));
        pt.setOnFinished(e -> { root.getChildren().remove(rangeCircle); rangeCircle = null; });
        pt.play();
    }

    // Starts the wave countdown and spawns enemies for the current wave.
    void startWave() {
        if (nextWaveCountdownTimeline != null) {
            nextWaveCountdownTimeline.stop();
            nextWaveCountdownTimeline = null;
        }

        if (waveIdx >= level.waves.size()) {
            handleLevelCompletion();
            return;
        }

        if (gameOver) return;

        if (!activeEnemies.isEmpty()) {
            waitForEnemiesClearTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(0.5), e -> {
                        if (activeEnemies.isEmpty()) {
                            waitForEnemiesClearTimeline.stop();
                            startWave();
                        }
                    })
            );
            waitForEnemiesClearTimeline.setCycleCount(Animation.INDEFINITE);
            waitForEnemiesClearTimeline.play();
            return;
        }

        Wave wave = level.waves.get(waveIdx);

        if (waveIdx > 0 && wave.startDelay > 0) {
            nextWaveCountdown = (int) wave.startDelay;
            nextWaveCountdownTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(1), e -> {
                        nextWaveCountdown--;
                        updateInfo();
                        if (nextWaveCountdown <= 0) {
                            nextWaveCountdownTimeline.stop();
                            nextWaveCountdownTimeline = null;
                            spawnWaveEnemies(wave);
                        }
                    })
            );
            nextWaveCountdownTimeline.setCycleCount(nextWaveCountdown);
            nextWaveCountdownTimeline.play();
        } else {
            spawnWaveEnemies(wave);
        }
    }

    // Spawns enemies for the current wave, using a timeline to control the spawn rate.
    void spawnWaveEnemies(Wave wave) {
        Timeline spawnTimeline = new Timeline();
        spawnTimeline.setCycleCount(wave.enemyCount);
        spawnTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(wave.spawnDelay), e -> spawnEnemy()));

        spawnTimeline.setOnFinished(e -> {
            waveIdx++;
            if (waveIdx < level.waves.size()) {
                startWave();
            } else {
                handleLevelCompletion();
            }
        });

        spawnTimeline.play();
    }

    // Spawns a single enemy, creating an EnemyView and adding it to the game.
    void spawnEnemy() {
        Enemy enemy = new Enemy();
        EnemyView enemyView = new EnemyView(enemy, level, cellSize, this);
        activeEnemies.add(enemyView);
        root.getChildren().addAll(enemyView, enemyView.healthBar);
    }

    // Removes an enemy from the game, stopping its timeline and removing it from the UI.
    void removeEnemy(EnemyView ev) {
        activeEnemies.remove(ev);
    }

    // Handles the completion of a level, checking if all enemies are cleared and proceeding to the next level or ending the game.
    private void handleLevelCompletion() {
        if (activeEnemies.isEmpty()) {
            proceedToNextLevelOrEndGame();
        } else {
            if (waitForEnemiesClearTimeline != null) {
                waitForEnemiesClearTimeline.stop();
            }
            waitForEnemiesClearTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(0.5), e -> {
                        if (activeEnemies.isEmpty()) {
                            if (waitForEnemiesClearTimeline != null) {
                                waitForEnemiesClearTimeline.stop();
                            }
                            proceedToNextLevelOrEndGame();
                        }
                    })
            );
            waitForEnemiesClearTimeline.setCycleCount(Animation.INDEFINITE);
            waitForEnemiesClearTimeline.play();
        }
    }

    // Updates the info label with the current player stats and wave information.
    void updateInfo() {
        if (player == null) return;
        if (gameOver) return;

        int shownWave = Math.min(waveIdx, level.waves.size());

        String nextWaveText = (nextWaveCountdown > 0)
                ? "\nNext wave(" + (shownWave+1) + "): " + nextWaveCountdown + " sn"
                : "";

        infoLabel.setText(
                "Level: " + (currentLevelIdx + 1) + "/" + levels.length +
                        "\nLives: " + player.lives +
                        "\nMoney: " + player.money +
                        "\nWave: " + shownWave + "/" + level.waves.size() +
                        nextWaveText
        );
    }

    // Starts the attack for the specified tower type, using a timeline to control the attack rate.
    void startTowerAttack(Tower tower) {
        switch (tower.type) {
            case SINGLE: startSingleShotAttack(tower); break;
            case LASER: startLaserAttack(tower); break;
            case TRIPLE: startTripleShotAttack(tower); break;
            case MISSILE: startMissileAttack(tower); break;
        }
    }

    // Starts the single shot attack
    void startSingleShotAttack(Tower tower) {
        Timeline attackTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0.7), e -> {
                    double range = getRangeForType(TowerType.SINGLE);
                    EnemyView target = findNearestEnemy(tower, range);
                    if (target != null) {
                        target.enemy.health -= 10;
                        drawShot(tower, target, Color.RED);
                        if (target.enemy.health <= 0) {
                            target.kill();
                            activeEnemies.remove(target);
                        }
                    }
                })
        );
        attackTimeline.setCycleCount(Animation.INDEFINITE);
        attackTimeline.play();
        tower.attackTimeline = attackTimeline;
    }


    // Starts the laser attack
    void startLaserAttack(Tower tower) {
        Timeline attackTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0.2), e -> {
                    double range = getRangeForType(TowerType.LASER);
                    EnemyView target = findNearestEnemy(tower, range);
                    if (target != null) {
                        target.enemy.health -= 3;
                        drawShot(tower, target, Color.DEEPPINK);
                        if (target.enemy.health <= 0) {
                            target.kill();
                            activeEnemies.remove(target);
                        }
                    }
                })
        );
        attackTimeline.setCycleCount(Animation.INDEFINITE);
        attackTimeline.play();
        tower.attackTimeline = attackTimeline;
    }

    // Finds the nearest enemy within the specified range of the tower.
    EnemyView findNearestEnemy(Tower tower, double range) {
        double tx = tower.x * cellSize + cellSize / 2.0;
        double ty = tower.y * cellSize + cellSize / 2.0;
        EnemyView nearest = null;
        double minDist = Double.MAX_VALUE;
        for (EnemyView ev : new ArrayList<>(activeEnemies)) {
            if (!ev.isVisible()) continue;
            double dx = ev.getCenterX() - tx;
            double dy = ev.getCenterY() - ty;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist <= range && dist < minDist) {
                minDist = dist;
                nearest = ev;
            }
        }
        return nearest;
    }


    // Finds the nearest enemies within the specified range of the tower, up to the specified count.
    List<EnemyView> findNearestEnemies(Tower tower, double range, int count) {
        double tx = tower.x * cellSize + cellSize / 2.0;
        double ty = tower.y * cellSize + cellSize / 2.0;
        List<EnemyView> inRange = new ArrayList<>();
        for (EnemyView ev : new ArrayList<>(activeEnemies)) {
            if (!ev.isVisible()) continue;
            double dx = ev.getCenterX() - tx;
            double dy = ev.getCenterY() - ty;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist <= range) inRange.add(ev);
        }
        inRange.sort(Comparator.comparingInt(a -> a.enemy.pathIndex));
        return inRange.subList(0, Math.min(count, inRange.size()));
    }

    // Draws a shot line from the tower to the target enemy, using a specified color.
    void drawShot(Tower tower, EnemyView target, Color color) {
        double tx = tower.x * cellSize + cellSize / 2.0;
        double ty = tower.y * cellSize + cellSize / 2.0;
        Line shot = new Line(tx, ty, target.getCenterX(), target.getCenterY());
        shot.setStroke(color);
        shot.setStrokeWidth(2);
        root.getChildren().add(shot);
        PauseTransition pt = new PauseTransition(Duration.millis(120));
        pt.setOnFinished(ev -> root.getChildren().remove(shot));
        pt.play();
    }

    // Starts the triple shot attack
    void startTripleShotAttack(Tower tower) {
        Timeline attackTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1.0), e -> {
                    double range = getRangeForType(TowerType.TRIPLE);
                    List<EnemyView> targets = findNearestEnemies(tower, range, 3);
                    for (EnemyView target : targets) {
                        target.enemy.health -= 13;
                        drawShot(tower, target, Color.ORANGE);
                        if (target.enemy.health <= 0) {
                            target.kill();
                            activeEnemies.remove(target);
                        }
                    }
                })
        );
        attackTimeline.setCycleCount(Animation.INDEFINITE);
        attackTimeline.play();
        tower.attackTimeline = attackTimeline;
    }

    // Starts the missile attack
    void startMissileAttack(Tower tower) {
        Timeline attackTimeline = new Timeline(
                new KeyFrame(Duration.seconds(2.0), e -> {
                    double range = getRangeForType(TowerType.MISSILE);
                    EnemyView target = findNearestEnemy(tower, range);
                    if (target != null) {
                        Circle explosion = new Circle(
                                target.getCenterX(), target.getCenterY(), cellSize * 1.2, Color.rgb(255, 100, 0, 0.3)
                        );
                        root.getChildren().add(explosion);
                        PauseTransition pt = new PauseTransition(Duration.millis(350));
                        pt.setOnFinished(ev -> root.getChildren().remove(explosion));
                        pt.play();

                        for (EnemyView ev : new ArrayList<>(activeEnemies)) {
                            double dx = ev.getCenterX() - target.getCenterX();
                            double dy = ev.getCenterY() - target.getCenterY();
                            if (Math.sqrt(dx * dx + dy * dy) <= cellSize * 1.2) {
                                ev.enemy.health -= 30;
                                if (ev.enemy.health <= 0) {
                                    ev.kill();
                                    activeEnemies.remove(ev);
                                }
                            }
                        }
                    }
                })
        );
        attackTimeline.setCycleCount(Animation.INDEFINITE);
        attackTimeline.play();
        tower.attackTimeline = attackTimeline;
    }


    // Checks if the game is over, and if so, displays the game over screen.
    void showGameOverScreen() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: black;");

        ImageView bg = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/gameover.jpg"))));
        bg.setPreserveRatio(false);
        bg.setFitWidth(1000);
        bg.setFitHeight(600);

        Label gameOverLabel = new Label("Game Over");
        gameOverLabel.setStyle("-fx-font-size: 64px; -fx-text-fill: white; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, black, 8, 0, 2, 2);");

        Button tryAgainBtn = new Button("Try Again");
        tryAgainBtn.setStyle("-fx-font-size: 32px; -fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-weight: bold;");
        tryAgainBtn.setOnAction(e -> {
            player = new Player(5, 100);
            if (infoTimeline != null) { infoTimeline.stop(); infoTimeline = null; }
            if (nextWaveCountdownTimeline != null) { nextWaveCountdownTimeline.stop(); nextWaveCountdownTimeline = null; }
            if (waitForEnemiesClearTimeline != null) { waitForEnemiesClearTimeline.stop(); waitForEnemiesClearTimeline = null; }
            for (EnemyView ev : new ArrayList<>(activeEnemies)) {
                ev.timeline.stop();
                root.getChildren().remove(ev);
                if (ev.healthBar != null) root.getChildren().remove(ev.healthBar);
            }
            activeEnemies.clear();
            waveIdx = 0;
            currentLevelIdx = 0;
            Stage stage = (Stage) overlay.getScene().getWindow();
            start(stage);
        });

        VBox vbox = new VBox(40, gameOverLabel, tryAgainBtn);
        vbox.setAlignment(Pos.CENTER);

        overlay.getChildren().addAll(bg, vbox);
        StackPane.setAlignment(vbox, Pos.CENTER);

        Scene gameOverScene = new Scene(overlay, 1000, 600);
        Stage stage = (Stage) root.getScene().getWindow();
        stage.setScene(gameOverScene);
    }

    // Displays the level passed screen, allowing players to proceed to the next level.
    void showLevelPassedScreen() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: black;");

        ImageView bg = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/next.jpg"))));
        bg.setPreserveRatio(false);
        bg.setFitWidth(1000);
        bg.setFitHeight(600);

        Label levelPassedLabel = new Label("You passed this level");
        levelPassedLabel.setStyle("-fx-font-size: 64px; -fx-text-fill: white; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, black, 8, 0, 2, 2);");

        Button nextLevelBtn = new Button("Next Level");
        nextLevelBtn.setStyle("-fx-font-size: 32px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        nextLevelBtn.setOnAction(e -> {
            Stage stage = (Stage) overlay.getScene().getWindow();
            currentLevelIdx++;
            loadLevel(currentLevelIdx);

            BorderPane gameLayout = new BorderPane();
            BackgroundImage gameBackground = new BackgroundImage(
                    new Image(Objects.requireNonNull(getClass().getResource("/background.jpg")).toExternalForm()),
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(1000, 600, false, false, false, false)
            );
            gameLayout.setBackground(new Background(gameBackground));
            root.setLayoutX(50);
            root.setLayoutY(50);
            gameLayout.getChildren().add(root);
            VBox dataPanel = new VBox(10);
            dataPanel.setPadding(new Insets(10));
            dataPanel.getChildren().add(infoLabel);
            gameLayout.setRight(dataPanel);

            Scene gameScene = new Scene(gameLayout, 1000, 600);
            stage.setScene(gameScene);
        });

        VBox vbox = new VBox(40, levelPassedLabel, nextLevelBtn);
        vbox.setAlignment(Pos.CENTER);

        overlay.getChildren().addAll(bg, vbox);
        StackPane.setAlignment(vbox, Pos.CENTER);

        Scene levelPassedScene = new Scene(overlay, 1000, 600);
        Stage stage = (Stage) root.getScene().getWindow();
        stage.setScene(levelPassedScene);
    }

    // Displays the "You Won" screen, allowing players to return to the main menu.
    void showYouWonScreen() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: black;");

        ImageView bg = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/won.jpg"))));
        bg.setPreserveRatio(false);
        bg.setFitWidth(1000);
        bg.setFitHeight(600);

        Label youWonLabel = new Label("YOU WON");
        youWonLabel.setStyle("-fx-font-size: 64px; -fx-text-fill: white; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, black, 8, 0, 2, 2);");

        Button mainMenuBtn = new Button("Main Menu");
        mainMenuBtn.setStyle("-fx-font-size: 32px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        mainMenuBtn.setOnAction(e -> {
            player = new Player(5, 100);
            currentLevelIdx = 0;
            Stage stage = (Stage) overlay.getScene().getWindow();
            start(stage);
        });

        VBox vbox = new VBox(40, youWonLabel, mainMenuBtn);
        vbox.setAlignment(Pos.CENTER);

        overlay.getChildren().addAll(bg, vbox);
        StackPane.setAlignment(vbox, Pos.CENTER);

        Scene youWonScene = new Scene(overlay, 1000, 600);
        Stage stage = (Stage) root.getScene().getWindow();
        stage.setScene(youWonScene);
    }

    // Proceeds to the next level or ends the game if all levels are completed.
    private void proceedToNextLevelOrEndGame() {
        if (currentLevelIdx < levels.length - 1) {
            showLevelPassedScreen();
        } else {
            showYouWonScreen();
        }
    }

    // Handles the game over state, stopping all timelines and displaying the game over screen.
    void checkGameOver() {
        if (player.lives <= 0 && !gameOver) {
            gameOver = true;
            infoLabel.setText("Game Over!\nYour all lives finished.");
            for (Tower t : towers) {
                if (t.attackTimeline != null) t.attackTimeline.stop();
            }
            for (EnemyView ev : new ArrayList<>(activeEnemies)) {
                ev.timeline.stop();
            }
            if (nextWaveCountdownTimeline != null) nextWaveCountdownTimeline.stop();
            if (waitForEnemiesClearTimeline != null) waitForEnemiesClearTimeline.stop();

            showGameOverScreen();
        }
    }


    // Returns the string representation of the level data for each level.
    static String level1txt() {
        return """
    WIDTH:10
    HEIGHT:10
    0,2
    1,2
    2,2
    3,2
    3,3
    3,4
    3,5
    4,5
    5,5
    6,5
    7,5
    8,5
    9,5
    WAVE_DATA:
    5, 1, 2
    8, 0.5, 5
    12, 0.3, 5
    20, 0.3, 5
    """;
    }

    static String level2txt() {
        return """
    WIDTH:10
    HEIGHT:10
    2,0
    2,1
    2,2
    2,3
    3,3
    4,3
    5,3
    5,2
    5,1
    6,1
    7,1
    7,2
    7,3
    7,4
    7,5
    7,6
    7,7
    8,7
    9,7
    WAVE_DATA:
    5, 1, 2
    8, 0.5, 5
    12, 0.3, 5
    20, 0.3, 5
    """;
    }

    static String level3txt() {
        return """
    WIDTH:10
    HEIGHT:10
    0,5
    1,5
    2,5
    2,4
    2,3
    3,3
    4,3
    4,4
    4,5
    4,6
    4,7
    5,7
    6,7
    7,7
    7,6
    7,5
    7,4
    8,4
    9,4
    WAVE_DATA:
    5, 1, 2
    8, 0.5, 5
    12, 0.3, 5
    20, 0.3, 5
    """;
    }

    static String level4txt() {
        return """
    WIDTH:15
    HEIGHT:15
    0,2
    1,2
    2,2
    3,2
    3,3
    3,4
    3,5
    3,6
    3,7
    3,8
    4,8
    5,8
    6,8
    6,7
    6,6
    6,5
    6,4
    7,4
    8,4
    9,4
    9,5
    9,6
    9,7
    9,8
    9,9
    9,10
    8,10
    7,10
    6,10
    6,11
    6,12
    7,12
    8,12
    9,12
    10,12
    11,12
    12,12
    12,11
    12,10
    12,9
    12,8
    13,8
    14,8
    WAVE_DATA:
    5, 1, 2
    8, 0.5, 5
    12, 0.3, 5
    20, 0.3, 5
    """;
    }

    static String level5txt() {
        return """
    WIDTH:15
    HEIGHT:15
    3,0
    3,1
    3,2
    3,3
    2,3
    1,3
    1,4
    1,5
    1,6
    2,6
    3,6
    4,6
    5,6
    6,6
    6,7
    6,8
    5,8
    4,8
    4,9
    4,10
    5,10
    6,10
    7,10
    8,10
    9,10
    9,9
    9,8
    9,7
    9,6
    9,5
    9,4
    9,3
    10,3
    11,3
    12,3
    12,4
    12,5
    12,6
    12,7
    12,8
    12,9
    12,10
    12,11
    12,12
    13,12
    14,12
    WAVE_DATA:
    5, 1, 2
    8, 0.5, 5
    12, 0.3, 5
    20, 0.3, 5
    """;
    }

}
