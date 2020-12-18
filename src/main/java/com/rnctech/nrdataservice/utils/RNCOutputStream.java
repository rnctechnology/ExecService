package com.rnctech.nrdataservice.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rnctech.nrdataservice.RNConsts;
import com.rnctech.nrdataservice.service.RNResult.ResultMessage;
import com.rnctech.nrdataservice.service.RNResult.Type;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author zilin chen
 * @since 2020.09
 */
public class RNCOutputStream extends OutputStream {
	
  Logger logger = LoggerFactory.getLogger(RNCOutputStream.class);
  private final int NEW_LINE_CHAR = '\n';
  private final int LINE_FEED_CHAR = '\r';

  private List<MessageOutputStream> resultMessageOutputs = new LinkedList<>();
  private MessageOutputStream currentOut;
  private List<String> resourceSearchPaths = Collections.synchronizedList(new LinkedList<String>());

  ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  private final OutputListener flushListener;
  private final OutputChangeListener changeListener;

  private int size = 0;
  private int lastCRIndex = -1;

  // change static var to set interpreter output limit
  // limit will be applied to all InterpreterOutput object.
  // so we can expect the consistent behavior
  public static int limit = RNConsts.API_OUTPUT_LIMIT;


public RNCOutputStream(OutputListener flushListener) {
    this.flushListener = flushListener;
    changeListener = null;
    clear();
  }

  public RNCOutputStream(OutputListener flushListener,
                           OutputChangeListener listener)
      throws IOException {
    this.flushListener = flushListener;
    this.changeListener = listener;
    clear();
  }

  public void setType(Type type) throws IOException {
    MessageOutputStream out = null;

    synchronized (resultMessageOutputs) {
      int index = resultMessageOutputs.size();
      MessageListener listener =
          createInterpreterResultMessageOutputListener(index);

      if (changeListener == null) {
        out = new MessageOutputStream(type, listener);
      } else {
        out = new MessageOutputStream(type, listener, changeListener);
      }
      out.setResourceSearchPaths(resourceSearchPaths);

      buffer.reset();
      size = 0;
      lastCRIndex = -1;

      if (currentOut != null) {
        currentOut.flush();
      }

      resultMessageOutputs.add(out);
      currentOut = out;
    }
  }

  public MessageListener createInterpreterResultMessageOutputListener(
      final int index) {

    return new MessageListener() {
      final int idx = index;

      @Override
      public void onAppend(MessageOutputStream out, byte[] line) {
        if (flushListener != null) {
          flushListener.onAppend(idx, out, line);
        }
      }

      @Override
      public void onUpdate(MessageOutputStream out) {
        if (flushListener != null) {
          flushListener.onUpdate(idx, out);
        }
      }
    };
  }

  public MessageOutputStream getCurrentOutput() {
    synchronized (resultMessageOutputs) {
      return currentOut;
    }
  }

  public MessageOutputStream getOutputAt(int index) {
    synchronized (resultMessageOutputs) {
      return resultMessageOutputs.get(index);
    }
  }

  public int size() {
    synchronized (resultMessageOutputs) {
      return resultMessageOutputs.size();
    }
  }

  public void clear() {
    size = 0;
    lastCRIndex = -1;
    truncated = false;
    buffer.reset();

    synchronized (resultMessageOutputs) {
      for (MessageOutputStream out : resultMessageOutputs) {
        out.clear();
        try {
          out.close();
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
        }
      }

      // clear all ResultMessages
      resultMessageOutputs.clear();
      currentOut = null;
      startOfTheNewLine = true;
      firstCharIsPercentSign = false;
      updateAllResultMessages();
    }
  }

  private void updateAllResultMessages() {
    if (flushListener != null) {
      flushListener.onUpdateAll(this);
    }
  }


  int previousChar = 0;
  boolean startOfTheNewLine = true;
  boolean firstCharIsPercentSign = false;

  boolean truncated = false;

  @Override
  public void write(int b) throws IOException {
    MessageOutputStream out;
    if (truncated) {
      return;
    }

    synchronized (resultMessageOutputs) {
      currentOut = getCurrentOutput();

      if (++size > limit) {
        if (b == NEW_LINE_CHAR && currentOut != null) {
          Type type = currentOut.getType();
          if (type == Type.TEXT || type == Type.TABLE) {
            setType(Type.HTML);
            getCurrentOutput().write(ResultMessage.getExceedsLimitSizeMessage(limit,
                "RN_INTERPRETER_OUTPUT_LIMIT").getData().getBytes());
            truncated = true;
            return;
          }
        }
      }

      if (b == LINE_FEED_CHAR) {
        if (lastCRIndex == -1) {
          lastCRIndex = size;
        }
        // reset size to index of last carriage return
        size = lastCRIndex;
      }

      if (startOfTheNewLine) {
        if (b == '%') {
          startOfTheNewLine = false;
          firstCharIsPercentSign = true;
          buffer.write(b);
          previousChar = b;
          return;
        } else if (b != NEW_LINE_CHAR) {
          startOfTheNewLine = false;
        }
      }

      if (b == NEW_LINE_CHAR) {
        if (currentOut != null && currentOut.getType() == Type.TABLE) {
          if (previousChar == NEW_LINE_CHAR) {
            startOfTheNewLine = true;
            return;
          }
        } else {
          startOfTheNewLine = true;
        }
      }

      boolean flushBuffer = false;
      if (firstCharIsPercentSign) {
        if (b == ' ' || b == NEW_LINE_CHAR || b == '\t') {
          firstCharIsPercentSign = false;
          String displaySystem = buffer.toString();
          for (Type type : Type.values()) {
            if (displaySystem.equals('%' + type.name().toLowerCase())) {
              // new type detected
              setType(type);
              previousChar = b;
              return;
            }
          }
          // not a defined display system
          flushBuffer = true;
        } else {
          buffer.write(b);
          previousChar = b;
          return;
        }
      }

      out = getCurrentOutputForWriting();

      if (flushBuffer) {
        out.write(buffer.toByteArray());
        buffer.reset();
      }
      out.write(b);
      previousChar = b;
    }
  }

  private MessageOutputStream getCurrentOutputForWriting() throws IOException {
    synchronized (resultMessageOutputs) {
      MessageOutputStream out = getCurrentOutput();
      if (out == null) {
        // add text type result message
        setType(Type.TEXT);
        out = getCurrentOutput();
      }
      return out;
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

  /**
   * @param file
   * @throws IOException
   */
  public void write(File file) throws IOException {
    MessageOutputStream out = getCurrentOutputForWriting();
    out.write(file);
  }

  public void write(String string) throws IOException {
    if (string.startsWith("%") && !startOfTheNewLine) {
      // prepend "\n" if it starts with another type of output and startOfTheNewLine is false
      write(("\n" + string).getBytes());
    } else {
      write(string.getBytes());
    }
  }

  /**
   * write contents in the resource file in the classpath
   * @param url
   * @throws IOException
   */
  public void write(URL url) throws IOException {
    MessageOutputStream out = getCurrentOutputForWriting();
    out.write(url);
  }

  public void addResourceSearchPath(String path) {
    resourceSearchPaths.add(path);
  }

  public void writeResource(String resourceName) throws IOException {
    MessageOutputStream out = getCurrentOutputForWriting();
    out.writeResource(resourceName);
  }

  public List<ResultMessage> toResultMessages() throws IOException {
    List<ResultMessage> list = new LinkedList<>();
    synchronized (resultMessageOutputs) {
      for (MessageOutputStream out : resultMessageOutputs) {
        list.add(out.toResultMessage());
      }
    }
    return list;
  }

  public void flush() throws IOException {
    MessageOutputStream out = getCurrentOutput();
    if (out != null) {
      out.flush();
    }
  }

  public byte[] toByteArray() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    synchronized (resultMessageOutputs) {
      for (MessageOutputStream m : resultMessageOutputs) {
        out.write(m.toByteArray());
      }
    }

    return out.toByteArray();
  }

  @Override
  public void close() throws IOException {
    synchronized (resultMessageOutputs) {
      for (MessageOutputStream out : resultMessageOutputs) {
        out.close();
      }
    }
  }

public List<ResultMessage> toResultMessage() throws IOException {
	List<ResultMessage> msgs = new ArrayList<ResultMessage>();

    synchronized (resultMessageOutputs) {
      for (MessageOutputStream m : resultMessageOutputs) {
        msgs.add(new ResultMessage(Type.TEXT, new String(m.toByteArray())));
      }
    }
	return msgs;
}
}


