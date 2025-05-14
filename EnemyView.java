package com.example.newproject;

import javafx.animation.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.List;
import java.util.Objects;
//Tolga Geçgen_150120064,Metehan Çelik_150119709,Ali İhsan Çakmak_150122602
// This class represents the view of an enemy in the game, including its position, health bar, and movement.
public class EnemyView extends ImageView {
    public Enemy enemy;
    public Level level;
    public int cellSize;
    public Rectangle healthBar;
    public Timeline timeline;
    public GameController game;
    public double progress = 0.0;
    public double speed = 1.5;

    // Constructor for the EnemyView class.
    public EnemyView(Enemy enemy, Level level, int cellSize, GameController game) {
        this.enemy = enemy;
        this.level = level;
        this.cellSize = cellSize;
        this.game = game;

        // Set the initial position of the enemy.
        Image enemyImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/enemy.png")));
        setImage(enemyImg);
        setFitWidth(cellSize - 10);
        setFitHeight(cellSize - 10);

        updatePosition();

        // Create a health bar for the enemy.
        healthBar = new Rectangle(getX() + 5, getY() - 8, 30, 4);
        healthBar.setFill(Color.RED);
        healthBar.setStroke(Color.DARKRED);
        healthBar.setArcWidth(2);
        healthBar.setArcHeight(2);

        // Add the health bar to the game root.
        timeline = new Timeline(new KeyFrame(Duration.seconds(1.0 / 60), e -> moveSmooth()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // Update the position of the enemy based on its path index and progress.
    public void updatePosition() {
        List<int[]> path = level.pathCells;
        int idx = enemy.pathIndex;
        if (idx >= path.size() - 1) idx = path.size() - 2;
        int[] from = path.get(idx);
        int[] to = path.get(idx + 1);

        double x = (from[0] + (to[0] - from[0]) * progress) * cellSize + 5;
        double y = (from[1] + (to[1] - from[1]) * progress) * cellSize + 5;
        setX(x);
        setY(y);

        if (healthBar != null) {
            healthBar.setX(getX() + 5);
            healthBar.setY(getY() - 8);
            double ratio = Math.max(0, enemy.health / 30.0);
            healthBar.setWidth(30 * ratio);
        }
    }

    // Get the X and Y coordinates of the enemy.
    public double getCenterX() {
        return getX() + getFitWidth() / 2;
    }

    public double getCenterY() {
        return getY() + getFitHeight() / 2;
    }

    // Move the enemy smoothly along its path.
    public void moveSmooth() {
        List<int[]> path = level.pathCells;
        if (enemy.pathIndex >= path.size() - 1 || !enemy.alive) {
            timeline.stop();
            setVisible(false);
            if (healthBar != null) game.root.getChildren().remove(healthBar);
            game.activeEnemies.remove(this);
            if (enemy.alive) {
                enemy.alive = false;
                game.player.lives--;
                game.checkGameOver();
            }
            return;
        }

        int[] from = path.get(enemy.pathIndex);
        int[] to = path.get(enemy.pathIndex + 1);
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double distance = Math.sqrt(dx * dx + dy * dy);

        double delta = (speed / 60.0) / (distance == 0 ? 1 : distance);

        progress += delta;
        if (progress >= 1.0) {
            progress = 0.0;
            enemy.pathIndex++;
            if (enemy.pathIndex >= path.size() - 1) {
                timeline.stop();
                setVisible(false);
                if (healthBar != null) game.root.getChildren().remove(healthBar);
                game.activeEnemies.remove(this);
                enemy.alive = false;
                game.player.lives--;
                game.checkGameOver();
                return;
            }
        }

        updatePosition();
    }

    // Kill the enemy and create particles to simulate an explosion.
    public void kill() {
        timeline.stop();
        setVisible(false);
        if (healthBar != null) game.root.getChildren().remove(healthBar);
        enemy.alive = false;

        for (int i = 0; i < 20; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double dist = 10 + Math.random() * 20;
            double dx = Math.cos(angle) * dist;
            double dy = Math.sin(angle) * dist;
            Circle particle = new Circle(getCenterX(), getCenterY(), 2, Color.ORANGERED);
            game.root.getChildren().add(particle);

            TranslateTransition tt = new TranslateTransition(Duration.millis(500), particle);
            tt.setByX(dx);
            tt.setByY(dy);
            tt.setOnFinished(e -> game.root.getChildren().remove(particle));
            tt.play();
        }
        game.player.money += 10;
    }
}