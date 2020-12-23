/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.asdfjkl.jerryfx.engine;

import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.CONSTANTS;
import org.asdfjkl.jerryfx.lib.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EngineState {

    private static final Logger _logger = LoggerFactory.getLogger(EngineState.class);
    private static final int MAX_PV = 4;

    private static final Pattern READYOK = Pattern.compile("readok");
    private static final Pattern SCORECP = Pattern.compile("score\\scp\\s-{0,1}(\\d)+");
    private static final Pattern NPS = Pattern.compile("nps\\s(\\d)+");
    private static final Pattern SELDEPTH = Pattern.compile("seldepth\\s(\\d)+");
    private static final Pattern DEPTH = Pattern.compile("depth\\s(\\d)+");
    private static final Pattern MATE = Pattern.compile("score\\smate\\s-{0,1}(\\d)+");
    private static final Pattern CURRMOVENUMBER = Pattern.compile("currmovenumber\\s(\\d)+");
    private static final Pattern CURRMOVE = Pattern.compile("currmove\\s[a-z]\\d[a-z]\\d[a-z]{0,1}");
    private static final Pattern BESTMOVE = Pattern.compile("bestmove\\s[a-z]\\d[a-z]\\d[a-z]{0,1}");
    private static final Pattern PV = Pattern.compile("pv(\\s[a-z]\\d[a-z]\\d[a-z]{0,1})+");
    private static final Pattern POS = Pattern.compile("position\\s");
    private static final Pattern IDNAME = Pattern.compile("id\\sname ([^\n]+)");
    private static final Pattern MOVE = Pattern.compile("\\s[a-z]\\d[a-z]\\d([a-z]{0,1})\\s");
    private static final Pattern MOVES = Pattern.compile("\\s[a-z]\\d[a-z]\\d([a-z]{0,1})");
    private static final Pattern MULTIPV = Pattern.compile("multipv\\s(\\d)+");

    private String id = "";
    private int strength = -1;
    //int currentFullmoveNumber = 0; // store fullmove from uci output, not from game
    private int fullMoveNumber = 1;
//    private int halfmoves = 0;
    private String currentMove = "";
    private int nps = 0;
    private int selDepth = -1;
    private int depth = -1;

    ArrayList<String> pvList;
    private final ArrayList<String> pvSan;
    final ArrayList<Integer> score;
    final ArrayList<Integer> mate;
    final ArrayList<Boolean> seesMate;

    private boolean turn = CONSTANTS.WHITE;
    private String fen = "";

    int nrPvLines = 1;

    String bestmove = "";

    public EngineState() {
        pvList = new ArrayList<>();
        pvSan = new ArrayList<>();
        score = new ArrayList<>();
        mate = new ArrayList<>();
        seesMate = new ArrayList<>();
        for (int i = 0; i < MAX_PV; i++) {
            pvSan.add("");
            score.add(0);
            mate.add(0);
            seesMate.add(false);
        }
    }

    public void clear() {
        id = "";
        setStrength(-1);
        fullMoveNumber = 1;
//        setHalfmoves(0);
        currentMove = "";
        nps = 0;
        selDepth = -1;
        depth = -1;

        pvList.clear();
        pvSan.clear();
        score.clear();
        mate.clear();
        seesMate.clear();

        turn = CONSTANTS.WHITE;

        fen = "";
        nrPvLines = 1;

        bestmove = "";

        for (int i = 0; i < MAX_PV; i++) {
            pvSan.add("");
            score.add(0);
            mate.add(0);
            seesMate.add(false);
        }
    }

    public void setFen(String fen) {
        // update turn
        if (!fen.isEmpty()) {
            Board board = new Board(fen);
            this.turn = board.turn;
            this.fen = fen;
//            this.setHalfmoves(board.halfmoveClock);
            this.fullMoveNumber = board.fullmoveNumber;
        }
    }

    public void processEngineResponse(final List<String> response) {
        for(String line: response) {
            if (!line.isEmpty()) {
                _logger.debug("Received: {}", line);
                //lastString = line;
                // todo: instead of directly setting bestmove,
                // try updating engine info
                if(line.startsWith("bestmove")) {
                    bestmove = "BESTMOVE|" + line.substring(9)
                                           + "|" + score.get(0)
                                           + "|" + String.join(" ", pvList)
                                           + "|" + seesMate.get(0)
                                           + "|" + mate.get(0);
                } else {
                    update(line);
                }
            }
        }
    }

    public void update(String engineFeedback) {
        int multiPv = 0;
        String[] lines = engineFeedback.split("\n");
        for (String line : lines) {
            Matcher matchPVIdx = MULTIPV.matcher(line);
            if (matchPVIdx.find()) {
                String sMultiPV = matchPVIdx.group();
                multiPv = Integer.parseInt(sMultiPV.substring(8)) - 1;
            }

            // update score value. need to be careful about
            // - vs +, since engine reports always positive values
            Matcher matchScoreCP = SCORECP.matcher(line);
            if (matchScoreCP.find()) {
                String sScore = matchScoreCP.group();
                Integer dScore = Integer.parseInt(sScore.substring(9));
                if (this.turn == CONSTANTS.BLACK) {
                    score.set(multiPv, -dScore);
                }
                else {
                    score.set(multiPv, dScore);
                }
                seesMate.set(multiPv, false);
            }

            Matcher matchNps = NPS.matcher(line);
            if (matchNps.find()) {
                String sNps = matchNps.group();
                nps = Integer.parseInt(sNps.substring(4));
            }

            Matcher matchSelDepth = SELDEPTH.matcher(line);
            if (matchSelDepth.find()) {
                String sSelDepth = matchSelDepth.group();
                selDepth = Integer.parseInt(sSelDepth.substring(9));
            }

            Matcher matchDepth = DEPTH.matcher(line);
            if (matchDepth.find()) {
                String sDepth = matchDepth.group();
                depth = Integer.parseInt(sDepth.substring(6));
            }

            Matcher matchMate = MATE.matcher(line);
            if (matchMate.find()) {
                String sMate = matchMate.group();
                mate.set(multiPv, Integer.parseInt(sMate.substring(11)));
                seesMate.set(multiPv, true);
            }

            Matcher matchCurrMove = CURRMOVE.matcher(line);
            if (matchCurrMove.find()) {
                String sCurrMove = matchCurrMove.group();
                currentMove = sCurrMove.substring(9);
            }

            Matcher matchPV = PV.matcher(line);
            if (matchPV.find()) {
                String sMoves = matchPV.group().substring(3);
                pvList = new ArrayList<>(Arrays.asList(sMoves.split(" ")));
                updateSan(multiPv);
            }

            Matcher matchId = IDNAME.matcher(line);
            if (matchId.find()) {
                id = matchId.group().substring(8).split("\n")[0];
            }
        }
    }

    private void updateSan(int multiPvIndex) {
        if (pvList.size() > 0 && !fen.isEmpty()) {
            pvSan.set(multiPvIndex, "");
            Board b = new Board(fen);
            boolean whiteMoves = true;
            int moveNo = fullMoveNumber;
            if (turn == CONSTANTS.BLACK) {
                whiteMoves = false;
                pvSan.set(multiPvIndex, pvSan.get(multiPvIndex) + moveNo + ". ...");
            }
            for (String moveUci : pvList) {
                try {
                    Move mi = new Move(moveUci);
                    String san = b.san(mi);
                    if (whiteMoves) {
                        pvSan.set(multiPvIndex, pvSan.get(multiPvIndex) + " " + moveNo + ". " + san);
                    }
                    else {
                        pvSan.set(multiPvIndex, pvSan.get(multiPvIndex) + " " + san);
                        moveNo++;
                    }
                    whiteMoves = !whiteMoves;
                    b.apply(mi);
                }
                catch (IllegalArgumentException e) {
                    _logger.info("So what happened here?", e);
                }
            }
        }
    }

    @Override
    public String toString() {

        // id (Level MAX) | current Move + depth  |  nps | eval+line pv1 | .. pv2 | ...pv3 | ...pv4
        StringBuilder outStr = new StringBuilder();
        outStr.append("|");

        if (!id.isEmpty()) {
            if (getStrength() >= 0) {
                outStr.append(id);
                if (id.contains("Stockfish") && getStrength() == 20) {
                    outStr.append(" (Level MAX)");
                }
                else {
                    outStr.append(" (Level ").append(getStrength()).append(")");
                }
            }
            else {
                outStr.append(id);
            }
        }

        outStr.append("|");

        if (nps != 0) {
            outStr.append(nps / 1000.0d).append(" kn/s");
        }

        outStr.append("|");

        if (!this.currentMove.isEmpty()) {
            outStr.append(currentMove);
            outStr.append(" (depth ").append(depth).append("/").append(selDepth).append(")");
        }

        outStr.append("|");

        for (int i = 0; i < 4; i++) {
            if (i < nrPvLines) {
                if (seesMate.get(i)) {
                    int nrMates = mate.get(i);
                    // if it is black's turn, we need to invert
                    // the #mates, since the evaluation is always
                    // from the side that is moving - but the GUI
                    // always writes + if white mates, and - if black mates
                    if (turn == CONSTANTS.BLACK) {
                        nrMates = -nrMates;
                    }
                    // if it is black's turn, and the engine
                    outStr.append("(#").append(nrMates).append(") ");
                }
                else {
                    //if(this->score != 0.0) {
                    DecimalFormat df = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.US));
                    String floatScore = df.format(score.get(i) / 100.0);
                    outStr.append("(").append(floatScore).append(") ");
                }
                if (!pvSan.get(i).isEmpty()) {
                    outStr.append(pvSan.get(i));
                }
            }
            outStr.append(("|"));
        }
        return outStr.toString();
    }

    private int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

//    public void setHalfmoves(int halfmoves) {
//        this.halfmoves = halfmoves;
//    }

}