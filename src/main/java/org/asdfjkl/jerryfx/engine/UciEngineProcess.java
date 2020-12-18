package org.asdfjkl.jerryfx.engine;

import javafx.application.Platform;
import org.asdfjkl.jerryfx.gui.EngineThread;
import org.asdfjkl.jerryfx.gui.ModeMenuController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class UciEngineProcess implements AutoCloseable {

    private static final Logger _logger = LoggerFactory.getLogger(UciEngineProcess.class);
    public static final int MAX_LINES = 100;
    private final ModeMenuController _modeMenuController;

    private Process _process;
    private BufferedWriter _sender;
    private BufferedReader _receiver;
    private EngineThread engineThread;
    final BlockingQueue<String> cmdQueue = new LinkedBlockingQueue<>();

    public UciEngineProcess(final ModeMenuController modeMenuController) {
        _modeMenuController = modeMenuController;
    }

    public void start(final File path) {
        startEngineProcess(path);
        _receiver = new BufferedReader(new InputStreamReader(_process.getInputStream()));
        _sender = new BufferedWriter(new OutputStreamWriter(_process.getOutputStream()));
        final AtomicReference<String> count = new AtomicReference<>();
        //cmdQueue = new LinkedBlockingQueue<String>();
        engineThread = new EngineThread(this);
        engineThread.stringProperty().addListener((observable, oldValue, newValue) -> {
            _logger.debug("Listener: {}, {}->{}", observable, oldValue, newValue);
            if (count.getAndSet(newValue) == null) {
                Platform.runLater(() -> {
                    String value = count.getAndSet(null);
                    _modeMenuController.handleEngineInfo(value);
                });
            }
        });
        engineThread.start();
    }

    public BlockingQueue<String> getCommandQueue() {
        return cmdQueue;
    }

    public void acceptCommand(final String cmd) {
        try {
            _logger.debug("Accept command: {}", cmd);
            cmdQueue.put(cmd);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void send(final String cmd)
            throws IOException {
        _logger.debug("Sending: {}", cmd);
        _sender.write(cmd + "\n");
        _sender.flush();
    }

    /**
     * Blocking command that expects a reply.
     *
     * @param cmd UCI command to send.
     *
     * @return A list of the lines the engine sent back.
     *
     * @throws IOException
     */
    public List<String> sendSynchronous(final String cmd)
            throws IOException {
        send(cmd);
        return receiveEntireResponse();
    }

    public List<String> receive()
            throws IOException {
        List<String> res = new ArrayList<>();
        while (_receiver.ready() && res.size() < MAX_LINES) {
            String line = _receiver.readLine();
            if (!line.isEmpty()) {
                res.add(line);
            }
        }
        return res;
    }

    @Override
    public void close() {
        try {
            send("quit");
        }
        catch (IOException e) {
            _logger.warn("Error closing engine: " + e);
        }
        close(_sender);
        close(_receiver);
        _process.destroy();
    }

    // TODO: Need to check end of stream handling.
    private void startEngineProcess(final File path) {
        try {
            _process = Runtime.getRuntime().exec(path.getAbsolutePath());
        }
        catch (IOException e) {
            throw new EngineException("Error starting engine: " + path, e);
        }
    }

    private List<String> receiveEntireResponse()
            throws IOException {
        List<String> res = new ArrayList<>();
        String line;
        do {
            if (_receiver.ready()) {
                line = _receiver.readLine();
                if (line == null || line.endsWith("ok")) {
                    return res;
                }
                if (!line.isEmpty()) {
                    res.add(line);
                }
            }
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt();
                return res;
            }
        } while (res.size() < MAX_LINES);
        return res;
    }

    private void close(final Closeable stream) {
        try {
            stream.close();
        }
        catch (IOException e) {
            _logger.info("Error closing stream: " + stream);
        }
    }

}
