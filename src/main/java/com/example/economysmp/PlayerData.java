package com.example.economysmp;

public class PlayerData {
    public double money;
    public long shards;
    public long playtime; // seconds

    public PlayerData(double money, long shards, long playtime) {
        this.money = money;
        this.shards = shards;
        this.playtime = playtime;
    }
}
