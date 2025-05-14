package com.example.newproject;

import javafx.animation.Timeline;
import javafx.scene.image.ImageView;

//Tolga Geçgen_150120064,Metehan Çelik_150119709,Ali İhsan Çakmak_150122602
// This class represents a tower in the game, including its position, type, price, and attack timeline.
public class Tower {
    public ImageView imageView;
    public int x, y;
    public GameController.TowerType type;
    public int price;
    public Timeline attackTimeline;

    public Tower(int x, int y, GameController.TowerType type, int price) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.price = price;
    }


}