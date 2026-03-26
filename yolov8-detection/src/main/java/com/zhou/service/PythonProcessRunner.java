package com.zhou.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class PythonProcessRunner {

    public String run(List<String> command, Duration timeout) throws IOException, InterruptedException {
        return run(command, timeout, null);
    }

    public String run(List<String> command, Duration timeout, Consumer<String> lineHandler) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        StringBuilder output = new StringBuilder();
        LastLineHolder lastLineHolder = new LastLineHolder();
        ExecutorService readerExecutor = Executors.newSingleThreadExecutor();
        try {
            Future<?> readerFuture = readerExecutor.submit(() -> {
                try (InputStreamReader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                    StringBuilder lineBuffer = new StringBuilder();
                    int ch;
                    while ((ch = reader.read()) != -1) {
                        char c = (char) ch;
                        if (c == '\n' || c == '\r') {
                            if (lineBuffer.length() > 0) {
                                String line = lineBuffer.toString();
                                if (!line.isBlank()) {
                                    lastLineHolder.value = line;
                                }
                                output.append(line).append(System.lineSeparator());
                                if (lineHandler != null) {
                                    lineHandler.accept(line);
                                }
                                lineBuffer.setLength(0);
                            }
                        } else {
                            lineBuffer.append(c);
                        }
                    }

                    if (lineBuffer.length() > 0) {
                        String line = lineBuffer.toString();
                        if (!line.isBlank()) {
                            lastLineHolder.value = line;
                        }
                        output.append(line).append(System.lineSeparator());
                        if (lineHandler != null) {
                            lineHandler.accept(line);
                        }
                    }
                } catch (IOException ignored) {
                }
            });

            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Python process timed out: " + String.join(" ", command));
            }

            try {
                readerFuture.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException | java.util.concurrent.TimeoutException ignored) {
            }

            if (process.exitValue() != 0) {
                throw new IllegalStateException("Python process failed: " + output);
            }

            return lastLineHolder.value == null ? output.toString().trim() : lastLineHolder.value;
        } finally {
            readerExecutor.shutdownNow();
        }
    }

    private static final class LastLineHolder {
        private String value;
    }
}
