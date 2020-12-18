package com.rnctech.nrdataservice.utils;

import org.slf4j.Logger;

import java.io.IOException;

/**
 * @author zilin chen
 * @since 2020.09
 */
public class RNCLogOutputStream extends LogOutputStream {
	
  private Logger logger;
  volatile RNCOutputStream execOutput;

  public RNCLogOutputStream(Logger logger) {
    this.logger = logger;
  }

  public RNCOutputStream getExecOutput() {
    return execOutput;
  }

  public void setExecOutput(RNCOutputStream execOutput) {
    this.execOutput = execOutput;
  }

  @Override
  public void write(int b) throws IOException {
    super.write(b);
    if (execOutput != null) {
      execOutput.write(b);
    }
  }

  @Override
  public void write(byte [] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public void write(byte [] b, int off, int len) throws IOException {
    for (int i = off; i < len; i++) {
      write(b[i]);
    }
  }

  @Override
  protected void processLine(String s, int i) {
    logger.debug("Exec output:" + s);
  }

  @Override
  public void close() throws IOException {
    super.close();
    if (execOutput != null) {
      execOutput.close();
    }
  }

  @Override
  public void flush() throws IOException {
    super.flush();
    if (execOutput != null) {
      execOutput.flush();
    }
  }

}
