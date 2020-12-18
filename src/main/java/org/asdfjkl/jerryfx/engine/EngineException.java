package org.asdfjkl.jerryfx.engine;

import java.io.IOException;

public class EngineException extends RuntimeException {

    public EngineException(final String msg, final IOException e) {
        super(msg, e);
    }

}
