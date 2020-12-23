package org.asdfjkl.jerryfx.engine;

import org.asdfjkl.jerryfx.gui.EngineOption;
import org.asdfjkl.jerryfx.gui.GameModel;
import org.asdfjkl.jerryfx.lib.CONSTANTS;

import java.io.File;

public class Engine {

    private final UciEngineProcess _engineProcess;
    private final GameModel _gameModel;

    public Engine(final UciEngineProcess engineProcess, final GameModel gameModel) {
        _engineProcess = engineProcess;
        _gameModel = gameModel;
    }

    public void setMultiPv(final int multiPv) {
        _gameModel.setMultiPv(multiPv);
        sendCommand("setoption name MultiPV value " + multiPv);
        _gameModel.triggerStateChange();
    }

    public void sendCommand(final String cmd) {
        _engineProcess.acceptCommand(cmd);
    }

    public void activatePlayWhiteMode() {
        _gameModel.lastSeenBestmove = "";
        _engineProcess.start(_gameModel.getActiveEngine().getFile());
        sendCommand("ucinewgame");
        for(EngineOption enOpt : _gameModel.getActiveEngine().options) {
            if(enOpt.isNotDefault()) {
                sendCommand(enOpt.toUciCommand());
            }
        }
        if(_gameModel.getActiveEngine().isInternal()) {
            sendCommand("setoption name Skill Level value " + _gameModel.getEngineStrength());
        }
        // trigger statechange
        _gameModel.setMode(GameModel.MODE_PLAY_WHITE);
        _gameModel.setFlipBoard(false);
        _gameModel.setHumanPlayerColor(CONSTANTS.WHITE);
        _gameModel.triggerStateChange();
    }

    public GameModel getGameModel() {
        return _gameModel;
    }

    public void activatePlayBlackMode() {
        getGameModel().lastSeenBestmove = "";
        _engineProcess.start(getGameModel().getActiveEngine().getFile());
        sendCommand("ucinewgame");
        for(EngineOption enOpt : getGameModel().getActiveEngine().options) {
            if(enOpt.isNotDefault()) {
                sendCommand(enOpt.toUciCommand());
            }
        }
        if(getGameModel().getActiveEngine().isInternal()) {
            sendCommand("setoption name Skill Level value " + getGameModel().getEngineStrength());
        }
        // trigger statechange
        getGameModel().setMode(GameModel.MODE_PLAY_BLACK);
        getGameModel().setFlipBoard(true);
        getGameModel().setHumanPlayerColor(CONSTANTS.BLACK);
        getGameModel().triggerStateChange();
    }

    public void activateAnalysisMode() {
        getGameModel().lastSeenBestmove = "";
        // first change gamestate and reset engine
        sendCommand("stop");
        sendCommand("quit");
        String cmdEngine = getGameModel().getActiveEngine().getPath();
        sendCommand("start "+cmdEngine);
        sendCommand("uci");
        sendCommand("ucinewgame");
        for(EngineOption enOpt : getGameModel().getActiveEngine().options) {
            if(enOpt.isNotDefault()) {
                sendCommand(enOpt.toUciCommand());
            }
        }
        if(getGameModel().getActiveEngine().supportsMultiPV()) {
            sendCommand("setoption name MultiPV value " + getGameModel().getMultiPv());
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

    public void quit() {
        sendCommand("quit");
    }

}
