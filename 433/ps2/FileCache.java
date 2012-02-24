package syu;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static syu.Utils.*;

public class FileCache {

  protected ConcurrentHashMap<String,byte[]> map;
  protected int maxSize;
  protected AtomicInteger size;

  public byte[] get(String in) {
    return map.get(in);
  }

  public void put(String key, byte[] val) {
    map.put(key, val);
  }

  public FileCache(int maxSize) {
    this.maxSize = maxSize;
  }

  public boolean contains(String key) {
    return map.containsKey(key);
  }

  public boolean roomForFile(byte[] val) {
    return maxSize > (size.get() + val.length);
  }

}
