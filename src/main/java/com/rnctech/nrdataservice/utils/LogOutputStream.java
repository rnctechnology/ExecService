package com.rnctech.nrdataservice.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author zilin chen
 * @since 2020.10
 */

public abstract class LogOutputStream extends OutputStream {
	
  private static final int INTIAL_SIZE = 999;
  private static final int CR = 13;
  private static final int LF = 10;
  private final ByteArrayOutputStream buffer;
  private boolean skip;
  private final int level;

  public LogOutputStream() {
    this(INTIAL_SIZE);
  }

  public LogOutputStream(int level) {
    this.buffer = new ByteArrayOutputStream(132);
    this.skip = false;
    this.level = level;
  }

  @Override
  public void write(int cc) throws IOException {
    byte c = (byte) cc;
    if (c != 10 && c != 13) {
      this.buffer.write(cc);
    } else if (!this.skip) {
      this.processBuffer();
    }

    this.skip = c == 13;
  }

  @Override
  public void flush() throws IOException {
    if (this.buffer.size() > 0) {
      this.processBuffer();
    }

  }

  @Override
  public void close() throws IOException {
    if (this.buffer.size() > 0) {
      this.processBuffer();
    }

    super.close();
  }

  public int getMessageLevel() {
    return this.level;
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    int offset = off;
    int blockStartOffset = off;

    for (int remaining = len; remaining > 0; blockStartOffset = offset) {
      while (remaining > 0 && b[offset] != 10 && b[offset] != 13) {
        ++offset;
        --remaining;
      }

      int blockLength = offset - blockStartOffset;
      if (blockLength > 0) {
        this.buffer.write(b, blockStartOffset, blockLength);
      }

      while (remaining > 0 && (b[offset] == 10 || b[offset] == 13)) {
        this.write(b[offset]);
        ++offset;
        --remaining;
      }
    }

  }

  protected void processBuffer() {
    this.processLine(this.buffer.toString());
    this.buffer.reset();
  }

  protected void processLine(String line) {
    this.processLine(line, this.level);
  }

  protected abstract void processLine(String var1, int var2);
}
