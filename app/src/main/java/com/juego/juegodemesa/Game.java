package com.juego.juegodemesa;

public class Game {
    private Player player1;
    private Player player2;
    private String status;
    private String winner;

    public Game() {}

    public Game(Player player1, Player player2, String status) {
        this.player1 = player1;
        this.player2 = player2;
        this.status = status;
        this.winner = null;
    }

    public Player getPlayer1() {
        return player1;
    }

    public void setPlayer1(Player player1) {
        this.player1 = player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }
}
