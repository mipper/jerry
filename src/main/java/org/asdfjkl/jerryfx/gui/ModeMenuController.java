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

package org.asdfjkl.jerryfx.gui;

import org.asdfjkl.jerryfx.lib.Board;
import org.asdfjkl.jerryfx.lib.CONSTANTS;
import org.asdfjkl.jerryfx.lib.GameNode;
import org.asdfjkl.jerryfx.lib.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ModeMenuController implements StateChangeListener {

    private static final Logger _logger = LoggerFactory.getLogger(ModeMenuController.class);

    private final GameModel gameModel;
    private final EngineOutputView engineOutputView;
    private EngineController engineController;

    public ModeMenuController(GameModel gameModel, EngineOutputView engineOutputView) {
        this.gameModel = gameModel;
        this.engineOutputView = engineOutputView;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    public void handleEngineInfo(String s) {
        if(s.startsWith("INFO")) {
            //"INFO |Stockfish 12 (Level MAX)|145.081 kn/s||(#0)  23. Be7#||||"
            String[] sSplit = s.split("\\|");
            if(getGameModel().getGame().getCurrentNode().getBoard().isCheckmate() && sSplit.length > 1) {
                String sTemp = "|" + sSplit[1] + "|||(#0)|";
                this.engineOutputView.setText(sTemp);
            } else {
                this.engineOutputView.setText(s.substring(5));
            }
        }
        else if(s.startsWith("BESTMOVE")) {
            handleBestMove(s);
        }
        else {
            _logger.info("Ignoring response: {}", s);
        }
    }

    public void setEngineController(EngineController engineController) {
        this.engineController = engineController;
    }

    public void activateAnalysisMode() {
        getGameModel().lastSeenBestmove = "";
        // first change gamestate and reset engine
        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        String cmdEngine = getGameModel().activeEngine.getPath();
        engineController.sendCommand("start "+cmdEngine);
        engineController.sendCommand("uci");
        engineController.sendCommand("ucinewgame");
        for(EngineOption enOpt : getGameModel().activeEngine.options) {
            if(enOpt.isNotDefault()) {
                engineController.sendCommand(enOpt.toUciCommand());
            }
        }
        if(getGameModel().activeEngine.supportsMultiPV()) {
            engineController.sendCommand("setoption name MultiPV value " + getGameModel().getMultiPv());
        }
        getGameModel().setMode(GameModel.MODE_ANALYSIS);
        getGameModel().triggerStateChange();
    }

    public void activateEnterMovesMode() {
        getGameModel().lastSeenBestmove = "";
//        engineController.sendCommand("stop");
//        engineController.sendCommand("quit");
        getGameModel().setMode(GameModel.MODE_ENTER_MOVES);
        getGameModel().triggerStateChange();
    }

    private void handleStateChangeAnalysis() {

        String fen = getGameModel().getGame().getCurrentNode().getBoard().fen();
        engineController.sendCommand("stop");
        engineController.sendCommand("position fen "+fen);
        engineController.sendCommand("go infinite");

    }

    private void handleStateChangeGameAnalysis() {

        boolean continueAnalysis = true;

        boolean parentIsRoot = (getGameModel().getGame().getCurrentNode().getParent() == getGameModel().getGame().getRootNode());
        if(!parentIsRoot) {
            // if the current position is in the opening book,
            // we stop the analysis
            long zobrist = getGameModel().getGame().getCurrentNode().getBoard().getZobrist();
            if(getGameModel().book.inBook(zobrist)) {
                getGameModel().getGame().getCurrentNode().setComment("last book move");
                continueAnalysis = false;
            } else {
                // otherwise continue the analysis
                if (getGameModel().getGameAnalysisJustStarted()) {
                    getGameModel().setGameAnalysisJustStarted(false);
                } else {
                    getGameModel().getGame().goToParent();
                }
                String fen = getGameModel().getGame().getCurrentNode().getBoard().fen();
                engineController.sendCommand("stop");
                engineController.sendCommand("position fen " + fen);
                //System.out.println("go movetime "  + (gameModel.getGameAnalysisThinkTime() * 1000));
                engineController.sendCommand("go movetime " + (getGameModel().getGameAnalysisThinkTime() * 1000));
            }
        } else {
            continueAnalysis = false;
        }

        if(!continueAnalysis) {
            // we are at the root or found a book move
            getGameModel().getGame().setTreeWasChanged(true);
            activateEnterMovesMode();
            //FlatAlert alert = new FlatAlert(Alert.AlertType.INFORMATION);
            DialogSimpleAlert dlg = new DialogSimpleAlert();
            dlg.show("     The analysis is finished.     ");
        }
    }

    public void activatePlayoutPositionMode() {
        getGameModel().lastSeenBestmove = "";
        // first change gamestate and reset engine
        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        String cmdEngine = getGameModel().activeEngine.getPath();
        engineController.sendCommand("start "+cmdEngine);
        engineController.sendCommand("uci");
        engineController.sendCommand("ucinewgame");
        for(EngineOption enOpt : getGameModel().activeEngine.options) {
            if(enOpt.isNotDefault()) {
                engineController.sendCommand(enOpt.toUciCommand());
            }
        }
        // trigger statechange
        getGameModel().setMode(GameModel.MODE_PLAYOUT_POSITION);
        getGameModel().setFlipBoard(false);
        getGameModel().triggerStateChange();
    }

    public void activateGameAnalysisMode() {

        getGameModel().lastSeenBestmove = "";

        getGameModel().getGame().removeAllComments();
        getGameModel().getGame().removeAllVariants();
        getGameModel().getGame().removeAllAnnotations();
        getGameModel().getGame().setTreeWasChanged(true);

        engineController.sendCommand("stop");
        engineController.sendCommand("quit");
        String cmdEngine = getGameModel().activeEngine.getPath();
        engineController.sendCommand("start "+cmdEngine);
        engineController.sendCommand("uci");
        engineController.sendCommand("ucinewgame");
        if(getGameModel().activeEngine.supportsMultiPV()) {
            engineController.sendCommand("setoption name MultiPV value 1");
        }
        getGameModel().setFlipBoard(false);
        getGameModel().getGame().goToRoot();
        getGameModel().getGame().goToLeaf();
        if(getGameModel().getGame().getCurrentNode().getBoard().isCheckmate()) {
            getGameModel().currentIsMate = true;
            getGameModel().currentMateInMoves = 0;
        }

        getGameModel().setGameAnalysisJustStarted(true);
        getGameModel().triggerStateChange();

    }

    public void handleStateChangePlayWhiteOrBlack() {
        // first check if we can apply a bookmove
        long zobrist = getGameModel().getGame().getCurrentNode().getBoard().getZobrist();
        ArrayList<String> uciMoves0 = getGameModel().book.findMoves(zobrist);
        if(getGameModel().book.inBook(zobrist)) {
            ArrayList<String> uciMoves = getGameModel().book.findMoves(zobrist);
            int idx = (int) (Math.random() * uciMoves.size());
            handleBestMove("BESTMOVE|"+uciMoves.get(idx));
        } else {
            String fen = getGameModel().getGame().getCurrentNode().getBoard().fen();
            engineController.sendCommand("stop");
            engineController.sendCommand("position fen "+fen);
            engineController.sendCommand("go movetime "+(getGameModel().getComputerThinkTimeSecs() * 1000));
        }
    }

    public void handleStateChangePlayoutPosition() {

        String fen = getGameModel().getGame().getCurrentNode().getBoard().fen();
        engineController.sendCommand("stop");
        engineController.sendCommand("position fen "+fen);
        engineController.sendCommand("go movetime "+(getGameModel().getComputerThinkTimeSecs() * 1000));
    }

    private void addBestPv(String[] uciMoves) {
        //String[] uciMoves = gameModel.currentBestPv.split(" ");
        GameNode currentNode = getGameModel().getGame().getCurrentNode();

        for (String uciMove : uciMoves) {
            try {
                GameNode next = new GameNode();
                Board board = currentNode.getBoard().makeCopy();
                Move m = new Move(uciMove);
                if(!board.isLegal(m)) {
                    break;
                }
                board.apply(m);
                next.setMove(m);
                next.setBoard(board);
                next.setParent(currentNode);
                // to avoid bugs when incoherent information is
                // given/received by the engine, do not add lines that already exist
                if (currentNode.getVariations().size() > 0) {
                    String mUciChild0 = currentNode.getVariation(0).getMove().getUci();
                    if (mUciChild0.equals(uciMove)) {
                        break;
                    }
                }
                currentNode.addVariation(next);
                currentNode = next;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }


    public void editEngines() {
        getGameModel().setMode(GameModel.MODE_ENTER_MOVES);
        getGameModel().triggerStateChange();
        DialogEngines dlg = new DialogEngines();
        ArrayList<Engine> enginesCopy = new ArrayList<>();
        for(Engine engine : getGameModel().engines) {
            enginesCopy.add(engine);
        }
        int selectedIdx = getGameModel().engines.indexOf(getGameModel().activeEngine);
        if(selectedIdx < 0) {
            selectedIdx = 0;
        }
        boolean accepted = dlg.show(enginesCopy, selectedIdx);
        if(accepted) {
            //List<Engine> engineList = dlg.engineList
            ArrayList<Engine> engineList = new ArrayList<>(dlg.engineList);
            Engine selectedEngine = dlg.engineList.get(dlg.selectedIndex);
            getGameModel().engines = engineList;
            getGameModel().activeEngine = selectedEngine;
            getGameModel().triggerStateChange();
        }
    }

    public void handleBestMove(String bestmove) {
        //System.out.println("handling bestmove: "+bestmove);
        int mode = getGameModel().getMode();

        if(mode == GameModel.MODE_ENTER_MOVES) {
            return;
        }

        if(getGameModel().lastSeenBestmove.equals(bestmove)) {
            return;
        }

        String[] bestmoveItems = bestmove.split("\\|");

        if (mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK  || mode == GameModel.MODE_PLAYOUT_POSITION) {

            getGameModel().lastSeenBestmove = bestmove;
            // todo: catch Exceptions!
            String uci = bestmoveItems[1].split(" ")[0];
            Move m = new Move(uci);
            Board b = getGameModel().getGame().getCurrentNode().getBoard();
            if (b.isLegal(m)) {
                if(mode == GameModel.MODE_PLAY_WHITE && b.turn == CONSTANTS.BLACK) {
                    getGameModel().getGame().applyMove(m);
                    getGameModel().triggerStateChange();
                }
                if(mode == GameModel.MODE_PLAY_BLACK && b.turn == CONSTANTS.WHITE) {
                    getGameModel().getGame().applyMove(m);
                    getGameModel().triggerStateChange();
                }
                if(mode == GameModel.MODE_PLAYOUT_POSITION) {
                    getGameModel().getGame().applyMove(m);
                    getGameModel().triggerStateChange();
                }
            }
        }
        if(mode == GameModel.MODE_GAME_ANALYSIS) {

            getGameModel().lastSeenBestmove = bestmove;

            // first update information for current node
            getGameModel().childBestPv = getGameModel().currentBestPv;
            getGameModel().childBestEval = getGameModel().currentBestEval;
            getGameModel().childIsMate = getGameModel().currentIsMate;
            getGameModel().childMateInMoves = getGameModel().currentMateInMoves;

            getGameModel().currentBestPv = bestmoveItems[3];
            getGameModel().currentBestEval = Integer.parseInt(bestmoveItems[2]);
            getGameModel().currentIsMate = bestmoveItems[4].equals("true");
            // some engines, like arasan report 0.00 in mate position with nullmove
            // thus check manually
            //if(gameModel.getGame().getCurrentNode().getBoard().isCheckmate()) {
            //    gameModel.currentIsMate = true;
            //}

            /*
            System.out.println("");
            System.out.println("handle best move:");
            System.out.println(gameModel.getGame().getCurrentNode().getBoard().toString());
            System.out.println(gameModel.getGame().getCurrentNode().getBoard().fen());
            System.out.println("best eval: "+gameModel.currentBestEval);
            System.out.println("with move: "+gameModel.currentBestPv);
            System.out.println("child eva: "+gameModel.childBestEval);
            System.out.println("");
            */

            getGameModel().currentMateInMoves = Integer.parseInt(bestmoveItems[5]);

            if(!getGameModel().getGame().getCurrentNode().isLeaf()) {
                // completely skip that for black or white, if
                // that was chosen in the analysis
                boolean turn = getGameModel().getGame().getCurrentNode().getBoard().turn;
                if ((getGameModel().getGameAnalysisForPlayer() == GameModel.BOTH_PLAYERS)
                        || (getGameModel().getGameAnalysisForPlayer() == CONSTANTS.IWHITE && turn == CONSTANTS.WHITE)
                        || (getGameModel().getGameAnalysisForPlayer() == CONSTANTS.IBLACK && turn == CONSTANTS.BLACK)) {

                    // error, if currentNode is leaf -> nextMove will throw exception

                    if (!getGameModel().currentIsMate && !getGameModel().childIsMate) {
                        boolean wMistake = turn == CONSTANTS.WHITE && ((getGameModel().currentBestEval - getGameModel().childBestEval) >= getGameModel()
                                .getGameAnalysisThreshold());
                        boolean bMistake = turn == CONSTANTS.BLACK && ((getGameModel().currentBestEval - getGameModel().childBestEval) <= -(getGameModel()
                                                                                                                                                    .getGameAnalysisThreshold()));

                        //System.out.println("threshold: "+gameModel.getGameAnalysisThreshold());
                        //System.out.println("mistake  : " + (gameModel.currentBestEval - gameModel.childBestEval));

                        if (wMistake || bMistake) {
                            String uci = bestmoveItems[1].split(" ")[0];
                            String nextMove = getGameModel().getGame().getCurrentNode().getVariation(0).getMove().getUci();
                            String[] pvMoves = getGameModel().currentBestPv.split(" ");
                            // if the bestmove returned by the engine is different
                            // than the best suggested pv line, it means that e.g. the
                            // engine took a book move, but did not gave a pv evaluation
                            // in such a case, do not add the best pv line, as it is probably
                            // not a valid pv line for the current node
                            // we also do not want to add the same line as the child
                            if (!uci.equals(nextMove) && pvMoves.length > 0 && pvMoves[0].equals(uci)) {

                                addBestPv(pvMoves);

                                NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                                DecimalFormat decim = (DecimalFormat) nf;
                                decim.applyPattern("0.00");
                                String sCurrentBest = decim.format(getGameModel().currentBestEval / 100.0);
                                String sChildBest = decim.format(getGameModel().childBestEval / 100.0);

                                ArrayList<GameNode> vars = getGameModel().getGame().getCurrentNode().getVariations();
                                if (vars != null && vars.size() > 1) {
                                    GameNode child0 = getGameModel().getGame().getCurrentNode().getVariation(0);
                                    child0.setComment(sChildBest);
                                    GameNode child1 = getGameModel().getGame().getCurrentNode().getVariation(1);
                                    child1.setComment(sCurrentBest);
                                }
                            }
                        }
                    }

                    if (getGameModel().currentIsMate && !getGameModel().childIsMate) {
                        // the current player missed a mate
                        String[] pvMoves = getGameModel().currentBestPv.split(" ");
                        addBestPv(pvMoves);

                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                        DecimalFormat decim = (DecimalFormat) nf;
                        decim.applyPattern("0.00");
                        String sChildBest = decim.format(getGameModel().childBestEval / 100.0);

                        String sCurrentBest = "";
                        if (turn == CONSTANTS.WHITE) {
                            sCurrentBest = "#" + (Math.abs(getGameModel().currentMateInMoves));
                        } else {
                            sCurrentBest = "#-" + (Math.abs(getGameModel().currentMateInMoves));
                        }

                        ArrayList<GameNode> vars = getGameModel().getGame().getCurrentNode().getVariations();
                        if (vars != null && vars.size() > 1) {
                            GameNode child0 = getGameModel().getGame().getCurrentNode().getVariation(0);
                            child0.setComment(sChildBest);
                            GameNode child1 = getGameModel().getGame().getCurrentNode().getVariation(1);
                            child1.setComment(sCurrentBest);
                        }
                    }

                    if (!getGameModel().currentIsMate && getGameModel().childIsMate) {
                        // the current player  moved into a mate
                        String[] pvMoves = getGameModel().currentBestPv.split(" ");
                        addBestPv(pvMoves);

                        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
                        DecimalFormat decim = (DecimalFormat) nf;
                        decim.applyPattern("0.00");
                        String sCurrentBest = decim.format(getGameModel().currentBestEval / 100.0);

                        String sChildBest = "";
                        if (turn == CONSTANTS.WHITE) {
                            sChildBest = "#-" + (Math.abs(getGameModel().childMateInMoves));
                        } else {
                            sChildBest = "#" + (Math.abs(getGameModel().childMateInMoves));
                        }

                        ArrayList<GameNode> vars = getGameModel().getGame().getCurrentNode().getVariations();
                        if (vars != null && vars.size() > 1) {
                            GameNode child0 = getGameModel().getGame().getCurrentNode().getVariation(0);
                            child0.setComment(sChildBest);
                            GameNode child1 = getGameModel().getGame().getCurrentNode().getVariation(1);
                            child1.setComment(sCurrentBest);
                        }
                    }

                    if (getGameModel().currentIsMate && getGameModel().childIsMate) {
                        // the current player had a mate, but instead of executing it, he moved into a mate
                        // but we also want to skip the situation where the board position is checkmate
                        if ((getGameModel().currentMateInMoves >= 0 && getGameModel().childMateInMoves >= 0) &&
                            getGameModel().childMateInMoves != 0) {

                            String[] pvMoves = getGameModel().currentBestPv.split(" ");
                            addBestPv(pvMoves);

                            String sCurrentBest = "";
                            String sChildBest = "";
                            if (turn == CONSTANTS.WHITE) {
                                sCurrentBest = "#" + (Math.abs(getGameModel().currentMateInMoves));
                                sChildBest = "#-" + (Math.abs(getGameModel().childMateInMoves));
                            } else {
                                sCurrentBest = "#-" + (Math.abs(getGameModel().currentMateInMoves));
                                sChildBest = "#" + (Math.abs(getGameModel().childMateInMoves));
                            }

                            ArrayList<GameNode> vars = getGameModel().getGame().getCurrentNode().getVariations();
                            if (vars != null && vars.size() > 1) {
                                GameNode child0 = getGameModel().getGame().getCurrentNode().getVariation(0);
                                child0.setComment(sChildBest);
                                GameNode child1 = getGameModel().getGame().getCurrentNode().getVariation(1);
                                child1.setComment(sCurrentBest);
                            }
                        }
                    }
                }
            }
            getGameModel().getGame().setTreeWasChanged(true);
            getGameModel().triggerStateChange();

        }
    }

    @Override
    public void stateChange() {
        int mode = getGameModel().getMode();
        Board board = getGameModel().getGame().getCurrentNode().getBoard();
        boolean turn = board.turn;

        boolean isCheckmate = board.isCheckmate();
        boolean isStalemate = board.isStalemate();
        boolean isThreefoldRepetition = getGameModel().getGame().isThreefoldRepetition();

        boolean abort = false;

        // if we change from e.g. play white to enter moves, the state change would trigger
        // the notification again in enter moves mode after the state change. thus,
        // also check if
        if((isCheckmate || isStalemate || isThreefoldRepetition) && !getGameModel().doNotNotifyAboutResult) {

            String message = "";
            if(isCheckmate) {
                message = "     Checkmate.     ";
            }
            if(isStalemate) {
                message = "     Stalemate.     ";
            }
            if(isThreefoldRepetition) {
                message = "Draw (Threefold Repetition)";
            }
            if(mode != GameModel.MODE_GAME_ANALYSIS) {
                DialogSimpleAlert dlg = new DialogSimpleAlert();
                dlg.show(message);
            }

            if(mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK || mode == GameModel.MODE_PLAYOUT_POSITION) {
                abort = true;
            }
        }

        if(abort) {
            // if we change from e.g. play white to enter moves, the state change would trigger
            // the notification again in enter moves mode after the state change. thus
            // set a marker here, that for the next state change we ignore
            // the result notification
            getGameModel().doNotNotifyAboutResult = true;
            getGameModel().setMode(GameModel.MODE_ENTER_MOVES);
            getGameModel().triggerStateChange();
        } else {
            getGameModel().doNotNotifyAboutResult = false;
            if (mode == GameModel.MODE_ANALYSIS) {
                handleStateChangeAnalysis();
            }
            if (mode == GameModel.MODE_GAME_ANALYSIS) {
                handleStateChangeGameAnalysis();
            }
            if (mode == GameModel.MODE_PLAYOUT_POSITION) {
                handleStateChangePlayoutPosition();
            }
            if ((mode == GameModel.MODE_PLAY_WHITE || mode == GameModel.MODE_PLAY_BLACK)
                    && turn != getGameModel().getHumanPlayerColor()) {
                handleStateChangePlayWhiteOrBlack();
            }
        }
    }

}
