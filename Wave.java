package com.example.newproject;

//Tolga Geçgen_150120064,Metehan Çelik_150119709,Ali İhsan Çakmak_150122602
// This class represents a wave in the game, including the number of enemies, spawn delay, and start delay.
public class Wave {
    public int enemyCount;
    public double spawnDelay;
    public double startDelay;

    public Wave(int enemyCount, double spawnDelay, double startDelay) {
        this.enemyCount = enemyCount;
        this.spawnDelay = spawnDelay;
        this.startDelay = startDelay;
    }
}