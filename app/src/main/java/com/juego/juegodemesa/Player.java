package com.juego.juegodemesa;

public class Player {
    private String uid;
    private int taps;

    public Player() {}

    public Player(String uid, int taps) {
        this.uid = uid;
        this.taps = taps;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getTaps() {
        return taps;
    }

    public void setTaps(int taps) {
        this.taps = taps;
    }
}
