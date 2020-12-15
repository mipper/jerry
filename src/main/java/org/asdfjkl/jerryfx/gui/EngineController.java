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

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.asdfjkl.jerryfx.engine.UciEngineProcess;
import org.asdfjkl.jerryfx.lib.CONSTANTS;

import java.io.IOException;

public class EngineController {

    final UciEngineProcess _engineProcess;

    public EngineController(ModeMenuController modeMenuController) {

        _engineProcess = new UciEngineProcess(modeMenuController);
    }

    public void sendCommand(String cmd) {
//            _engineProcess.send(cmd);
            _engineProcess.acceptCommand(cmd);
//            cmdQueue.put(cmd);
    }

    public void activatePlayWhiteMode(final GameModel gameModel)
            throws IOException {
        gameModel.lastSeenBestmove = "";
        // first change gamestate and reset engine
//        sendCommand("stop");
//        sendCommand("quit");
//        String cmdEngine = gameModel.activeEngine.getPath();
        _engineProcess.start(gameModel.activeEngine.getFile());
//        sendCommand("start "+cmdEngine);
//        sendCommand("uci");
        sendCommand("ucinewgame");
        for(EngineOption enOpt : gameModel.activeEngine.options) {
            if(enOpt.isNotDefault()) {
                sendCommand(enOpt.toUciCommand());
            }
        }
        if(gameModel.activeEngine.isInternal()) {
            sendCommand("setoption name Skill Level value " + gameModel.getEngineStrength());
        }
        // trigger statechange
        gameModel.setMode(GameModel.MODE_PLAY_WHITE);
        gameModel.setFlipBoard(false);
        gameModel.setHumanPlayerColor(CONSTANTS.WHITE);
        gameModel.triggerStateChange();
    }

    public void activatePlayBlackMode(final GameModel gameModel) {
        gameModel.lastSeenBestmove = "";
        // first change gamestate and reset engine
        sendCommand("stop");
        sendCommand("quit");
        String cmdEngine = gameModel.activeEngine.getPath();
        sendCommand("start "+cmdEngine);
        sendCommand("uci");
        sendCommand("ucinewgame");
        for(EngineOption enOpt : gameModel.activeEngine.options) {
            if(enOpt.isNotDefault()) {
                sendCommand(enOpt.toUciCommand());
            }
        }
        if(gameModel.activeEngine.isInternal()) {
            sendCommand("setoption name Skill Level value " + gameModel.getEngineStrength());
        }
        // trigger statechange
        gameModel.setMode(GameModel.MODE_PLAY_BLACK);
        gameModel.setFlipBoard(true);
        gameModel.setHumanPlayerColor(CONSTANTS.BLACK);
        gameModel.triggerStateChange();
    }

}
