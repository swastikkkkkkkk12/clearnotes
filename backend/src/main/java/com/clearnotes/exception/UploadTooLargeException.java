package com.clearnotes.exception;

public class UploadTooLargeException extends RuntimeException {
    public UploadTooLargeException() { super("Audio file exceeds the 100 MB upload limit."); }
}
