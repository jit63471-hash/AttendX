package com.attendx.util;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class FxUtil {
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static <T> void runAsync(ThrowingSupplier<T> backgroundTask, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return backgroundTask.get();
            }
        };

        if (onSuccess != null) {
            task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        }
        if (onError != null) {
            task.setOnFailed(e -> onError.accept(task.getException()));
        }

        executor.submit(task);
    }

    public static void runAsyncAction(ThrowingRunnable backgroundTask, Runnable onSuccess, Consumer<Throwable> onError) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                backgroundTask.run();
                return null;
            }
        };

        if (onSuccess != null) {
            task.setOnSucceeded(e -> onSuccess.run());
        }
        if (onError != null) {
            task.setOnFailed(e -> onError.accept(task.getException()));
        }

        executor.submit(task);
    }

    public static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private FxUtil() {}
}
