package com.jobproj.api.common;

public class OwnerMismatchException extends RuntimeException {
  public OwnerMismatchException() { super("owner_mismatch"); }
  public OwnerMismatchException(String message) { super(message); }
}
