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

import org.asdfjkl.jerryfx.engine.Engine;

public class EngineController {

    private final Engine _engine;

    public EngineController(Engine engine) {
        _engine = engine;
    }

    public void sendCommand(String cmd) {
        _engine.sendCommand(cmd);
    }

    public void quit() {
        _engine.sendCommand("quit");
    }

    void setMultiPv(final int multiPv) {
        _engine.setMultiPv(multiPv);
    }

    public void activatePlayBlackMode() {
        _engine.activatePlayBlackMode();
    }

    public void activatePlayWhiteMode() {
        _engine.activatePlayWhiteMode();
    }

    public void activateAnalysisMode() {
        _engine.activateAnalysisMode();
    }

}
