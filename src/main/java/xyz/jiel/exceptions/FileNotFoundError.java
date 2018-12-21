package xyz.jiel.exceptions;

public class FileNotFoundError extends RuntimeException {
    public FileNotFoundError(String message) {
        super(message);
    }
}