package org.asdfjkl.jerryfx.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class UciEngineProcess implements AutoCloseable {

    public static final int MAX_LINES = 100;
    private final Process _process;
    private final BufferedWriter _sender;
    private final BufferedReader _receiver;

    public UciEngineProcess(final File path)
            throws IOException {
        _process = Runtime.getRuntime().exec(path.getAbsolutePath());
        _receiver = new BufferedReader(new InputStreamReader(_process.getInputStream()));
        _sender = new BufferedWriter(new OutputStreamWriter(_process.getOutputStream()));
    }

    public void send(final String cmd)
            throws IOException {
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
            System.err.println("Error closing engine: " + e);
        }
        close(_sender);
        close(_receiver);
        _process.destroy();
    }

    // TODO: Need to check end of stream handling.
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
            System.out.println("Error closing stream: " + stream);
        }
    }

}
