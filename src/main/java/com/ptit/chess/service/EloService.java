package com.ptit.chess.service;

import com.ptit.chess.entity.Player;
import org.springframework.stereotype.Service;

@Service
public class EloService {

    private static final int K_FACTOR = 32;

    public void updateRatings(Player whitePlayer, Player blackPlayer, double whiteScore) {
        double whiteExpected = 1.0 / (1.0 + Math.pow(10, (blackPlayer.getElo() - whitePlayer.getElo()) / 400.0));
        double blackExpected = 1.0 / (1.0 + Math.pow(10, (whitePlayer.getElo() - blackPlayer.getElo()) / 400.0));

        double blackScore = 1.0 - whiteScore;

        int newWhiteElo = (int) Math.round(whitePlayer.getElo() + K_FACTOR * (whiteScore - whiteExpected));
        int newBlackElo = (int) Math.round(blackPlayer.getElo() + K_FACTOR * (blackScore - blackExpected));

        whitePlayer.setElo(Math.max(100, newWhiteElo)); // Minimum rating 100
        blackPlayer.setElo(Math.max(100, newBlackElo));

        if (whiteScore == 1.0) {
            whitePlayer.setWinCount(whitePlayer.getWinCount() + 1);
            blackPlayer.setLossCount(blackPlayer.getLossCount() + 1);
        } else if (whiteScore == 0.0) {
            blackPlayer.setWinCount(blackPlayer.getWinCount() + 1);
            whitePlayer.setLossCount(whitePlayer.getLossCount() + 1);
        } else {
            whitePlayer.setDrawCount(whitePlayer.getDrawCount() + 1);
            blackPlayer.setDrawCount(blackPlayer.getDrawCount() + 1);
        }
    }
}
