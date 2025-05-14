package com.example.newproject;

import java.util.List;
//Tolga Geçgen_150120064,Metehan Çelik_150119709,Ali İhsan Çakmak_150122602
// This class represents a level in the game, including its dimensions, path cells, and waves of enemies.
public class Level {
    public int width, height;
    public List<int[]> pathCells;
    public List<Wave> waves;

    // Constructor to initialize the level with its dimensions, path cells, and waves.
    public Level(int width, int height, List<int[]> pathCells, List<Wave> waves) {
        this.width = width;
        this.height = height;
        this.pathCells = pathCells;
        this.waves = waves;
    }
}