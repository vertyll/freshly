package com.vertyll.freshly.lang.logging;

import java.util.ArrayList;
import java.util.List;

public class RecordingUseCaseLogger implements UseCaseLogger {
    private final List<String> messages = new ArrayList<>();

    @Override
    public void debug(String message, Object... args) {
        messages.add(message);
    }

    @Override
    public void info(String message, Object... args) {
        messages.add(message);
    }

    @Override
    public void warn(String message, Object... args) {
        messages.add(message);
    }

    @Override
    public void error(String message, Object... args) {
        messages.add(message);
    }

    public List<String> messages() {
        return List.copyOf(messages);
    }
}
