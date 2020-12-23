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

import org.asdfjkl.jerryfx.lib.*;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.lang.System;

public class GameModel {

    public static final int MODE_ENTER_MOVES = 0;
    public static final int MODE_ANALYSIS = 1;
    public static final int MODE_PLAY_WHITE = 2;
    public static final int MODE_PLAY_BLACK = 3;
    public static final int MODE_GAME_ANALYSIS = 4;
    public static final int BOTH_PLAYERS = 5;
    public static final int MODE_PLAYOUT_POSITION = 7;
    Game game;
    private final ArrayList<StateChangeListener> stateChangeListeners = new ArrayList<>();
    private int currentMode;
    private int multiPv = 1;
    private boolean flipBoard = false;
    private boolean humanPlayerColor = CONSTANTS.WHITE;

    public boolean wasSaved = false;
    private int engineStrength = 20;
    private int engineThinkTimeSecs = 3;

    ArrayList<EngineDefinition> _engineDefinitions = new ArrayList<>();
    private EngineDefinition _activeEngineDefinition = null;

    private int gameAnalysisForPlayer = BOTH_PLAYERS;
    private int gameAnalysisThreshold = 500; // centipawns
    private int gameAnalysisThinkTime = 3000;

    private boolean gameAnalysisJustStarted = false;

    public String currentBestPv = "";
    public int currentBestEval = 0;
    public int currentMateInMoves = -1;
    public boolean currentIsMate = false;

    public String childBestPv = "";
    public int childBestEval = 0;
    public int childMateInMoves = -1;
    public boolean childIsMate = false;

    public boolean doNotNotifyAboutResult = false;

    public String lastSeenBestmove = "";

    public final Polyglot book;

    private Preferences prefs;

    private static final int modelVersion = 400;

    private final PgnDatabase pgnDatabase;
    public int currentPgnDatabaseIdx = 0;
    public File lastOpenedDirPath = null;
    public File lastSaveDirPath = null;

    public boolean openDatabaseOnNextDialog = false;

    private SearchPattern searchPattern;
    BoardStyle boardStyle;

    public GameModel() {
        this.game = new Game();
        Board b = new Board(true);

        pgnDatabase = new PgnDatabase();
        searchPattern = new SearchPattern();
        searchPattern.setSearchForHeader(true);
        boardStyle = new BoardStyle();

        this.game.getRootNode().setBoard(b);
        this.currentMode = MODE_ENTER_MOVES;

        String stockfishPath = getStockfishPath();
        String bookPath = getBookPath();

        EngineDefinition stockfish = new EngineDefinition();
        stockfish.setName("Stockfish (Internal)");
        if(stockfishPath != null) {
            stockfish.setPath(stockfishPath);
        }
        stockfish.setInternal(true);
        _engineDefinitions.add(stockfish);
        setActiveEngine(stockfish);

        book = new Polyglot();
        if(bookPath != null) {
            File file = new File(bookPath);
            if (file.exists()) {
                book.loadBook(file);
            }
        }

        restoreModel();
        restoreBoardStyle();
        restoreEngines();
    }

    private String getStockfishPath() {

            String os = System.getProperty("os.name").toLowerCase();
            //System.out.println(os);
            if(os.contains("win")) {

                String stockfishPath = "";
                String jarPath = "";
                String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
                File tmp = (new File(jarPath));
                if(tmp.getParentFile().exists()) {
                    File subEngine = new File(tmp.getParentFile(), "engine");
                    stockfishPath = new File(subEngine, "stockfish12.exe").getPath();
                    return stockfishPath;
                }
        }
        if(os.contains("linux")) {
                String stockfishPath = "";
                String jarPath = "";
                String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                //System.out.println(path);
                jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
                //System.out.println(jarPath);
                File tmp = (new File(jarPath));
                if(tmp.getParentFile().exists()) {
                    //System.out.println(tmp.getParentFile().getAbsolutePath());
                    if(tmp.getParentFile().getParentFile().exists()) {
                    File subEngine = new File(tmp.getParentFile().getParentFile(), "engine");
                    //System.out.println(subEngine.getPath());
                    stockfishPath = new File(subEngine, "stockfish_x64").getPath();
                    return stockfishPath;
                    }
                }

        }
        return null;
    }

    private String getBookPath() {
        String os = System.getProperty("os.name").toLowerCase();
            if(os.contains("win")) {
                String bookPath = "";
                String jarPath = "";
                String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
                File tmp = (new File(jarPath));
                if(tmp.getParentFile().exists()) {
                    File subBook = new File(tmp.getParentFile(), "book");
                    bookPath = new File(subBook, "varied.bin").getPath();
                    return bookPath;
                }
        }
        if(os.contains("linux")) {
                String bookPath = "";
                String jarPath = "";
                String path = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                jarPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
                File tmp = (new File(jarPath));
                if(tmp.getParentFile().exists()) {
                    if(tmp.getParentFile().getParentFile().exists()) {
                    File subBook = new File(tmp.getParentFile().getParentFile(), "book");
                    bookPath = new File(subBook, "varied.bin").getPath();
                    return bookPath;
                    }
                }
        }
        return null;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public int getGameAnalysisForPlayer() { return gameAnalysisForPlayer; }

    public void setGameAnalysisForPlayer(int player) { gameAnalysisForPlayer = player; }

    public int getGameAnalysisThreshold() { return gameAnalysisThreshold; }

    public void setGameAnalysisThreshold(int threshold) { gameAnalysisThreshold = threshold; }

    public void setGameAnalysisThinkTime(int thinktime) { gameAnalysisThinkTime = thinktime; }

    public int getGameAnalysisThinkTime() { return gameAnalysisThinkTime; }

    public void setMode(int mode) {
        this.currentMode = mode;
    }

    public int getMode() {
        return currentMode;
    }

    public void setComputerThinkTimeSecs(int secs) {
        engineThinkTimeSecs = secs;
    }

    public int getComputerThinkTimeSecs() {
        return engineThinkTimeSecs;
    }

    public int getEngineStrength() { return engineStrength; }

    public void setEngineStrength(int strength) { engineStrength = strength; }

    public int getMultiPv() {
        return multiPv;
    }

    public void setMultiPv(int multiPv) {
        if(multiPv < 1) {
            throw new IllegalArgumentException("setMultiPV: "+multiPv+ " but must be >= 1!");
        }
        if(multiPv > 4) {
            throw new IllegalArgumentException("setMultiPV: "+multiPv+ " but must be <= 4!");
        }
        this.multiPv = multiPv;
    }

    public void setFlipBoard(boolean flipBoard) {
        this.flipBoard = flipBoard;
    }

    public boolean getFlipBoard() {
        return flipBoard;
    }

    public void setHumanPlayerColor(boolean humanPlayerColor) {
        this.humanPlayerColor = humanPlayerColor;
    }

    public boolean getHumanPlayerColor() {
        return humanPlayerColor;
    }

    public boolean getGameAnalysisJustStarted() { return gameAnalysisJustStarted; }

    public void setGameAnalysisJustStarted(boolean val) { gameAnalysisJustStarted = val; }

    public PgnDatabase getPgnDatabase() {
        return pgnDatabase;
    }

    public SearchPattern getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(SearchPattern searchPattern) {
        this.searchPattern = searchPattern;
    }


    public void addListener(StateChangeListener toAdd) {
        stateChangeListeners.add(toAdd);
    }

    public void triggerStateChange() {
        for (StateChangeListener sl : stateChangeListeners)
            sl.stateChange();
    }

    public void saveModel() {

        prefs = Preferences.userRoot().node(this.getClass().getName());

        prefs.putInt("modelVersion",modelVersion);

        PgnPrinter printer = new PgnPrinter();
        String pgn = printer.printGame(getGame());
        prefs.put("currentGame", pgn);

    }

    public void saveBoardStyle() {
        prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.putInt("COLOR_STYLE", boardStyle.getColorStyle());
        prefs.putInt("PIECE_STYLE", boardStyle.getPieceStyle());
    }

    public void saveEngines() {

        prefs = Preferences.userRoot().node(this.getClass().getName());
        for(int i = 1; i < _engineDefinitions.size(); i++) {
            EngineDefinition engineDefinition = _engineDefinitions.get(i);
            String engineString = engineDefinition.writeToString();
            prefs.put("ENGINE"+i, engineString);
        }
        prefs.putInt("ACTIVE_ENGINE_IDX", _engineDefinitions.indexOf(getActiveEngine()));
    }

    private void restoreBoardStyle() {

        BoardStyle style = new BoardStyle();
        prefs = Preferences.userRoot().node(this.getClass().getName());
        int mVersion = prefs.getInt("modelVersion", 0);

        if(mVersion == modelVersion) {
            int colorStyle = prefs.getInt("COLOR_STYLE", BoardStyle.STYLE_BLUE);
            int pieceStyle = prefs.getInt("PIECE_STYLE", BoardStyle.PIECE_STYLE_MERIDA);
            style.setPieceStyle(pieceStyle);
            style.setColorStyle(colorStyle);
        }
        boardStyle = style;
    }

    private void restoreEngines() {

        prefs = Preferences.userRoot().node(this.getClass().getName());
        int mVersion = prefs.getInt("modelVersion", 0);

        if(mVersion == modelVersion) {

            for(int i=1;i<99;i++) {
                String engineString = prefs.get("ENGINE"+i, "");
                if(!engineString.isEmpty()) {
                    EngineDefinition engineDefinition = new EngineDefinition();
                    engineDefinition.restoreFromString(engineString);
                    _engineDefinitions.add(engineDefinition);
                }
            }
            int activeIdx = prefs.getInt("ACTIVE_ENGINE_IDX", 0);
            if(activeIdx < _engineDefinitions.size()) {
                setActiveEngine(_engineDefinitions.get(activeIdx));
            } else {
                setActiveEngine(_engineDefinitions.get(0));
            }
        }
    }

    public void saveScreenGeometry(ScreenGeometry g) {

        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());

        prefs.putDouble("WINDOW_POSITION_X", g.xOffset);
        prefs.putDouble("WINDOW_POSITION_Y", g.yOffset);
        prefs.putDouble("WINDOW_WIDTH", g.width);
        prefs.putDouble("WINDOW_HEIGHT", g.height);
        prefs.putDouble("MOVE_DIVIDER_RATIO", g.moveDividerRatio);
        prefs.putDouble("MAIN_DIVIDER_RATIO", g.mainDividerRatio);
    }

    public ScreenGeometry restoreScreenGeometry() {

        prefs = Preferences.userRoot().node(this.getClass().getName());
        int mVersion = prefs.getInt("modelVersion", 0);

        ScreenGeometry g = new ScreenGeometry(-1.0, -1.0, -1.0,-1.0,
                ScreenGeometry.DEFAULT_MOVE_DIVIDER_RATIO,
                ScreenGeometry.DEFAULT_MAIN_DIVIDER_RATIO);

        if(mVersion == modelVersion) {

            g.xOffset = prefs.getDouble("WINDOW_POSITION_X", -1.0);
            g.yOffset = prefs.getDouble("WINDOW_POSITION_Y", -1.0);
            g.width = prefs.getDouble("WINDOW_WIDTH", -1.0);
            g.height = prefs.getDouble("WINDOW_HEIGHT", -1.0);
            g.moveDividerRatio = prefs.getDouble("MOVE_DIVIDER_RATIO",
                    ScreenGeometry.DEFAULT_MOVE_DIVIDER_RATIO);
            g.mainDividerRatio = prefs.getDouble("MAIN_DIVIDER_RATIO",
                    ScreenGeometry.DEFAULT_MAIN_DIVIDER_RATIO);

        }
        return g;

    }

    private void restoreModel() {

        prefs = Preferences.userRoot().node(this.getClass().getName());
        int mVersion = prefs.getInt("modelVersion", 0);

        //intln(mVersion);
        if(mVersion == modelVersion) {
            PgnReader reader = new PgnReader();

            String pgn = prefs.get("currentGame", "");

            if(!pgn.isEmpty()) {
                Game g = reader.readGame(pgn);
                PgnPrinter p = new PgnPrinter();
                if (g.getRootNode().getBoard().isConsistent()) {
                    setGame(g);
                    g.setTreeWasChanged(true);
                    //triggerStateChange();
                }
            }
        }
    }

    public EngineDefinition getActiveEngine() {
        return _activeEngineDefinition;
    }

    public void setActiveEngine(EngineDefinition activeEngineDefinition) {
        this._activeEngineDefinition = activeEngineDefinition;
    }

}



