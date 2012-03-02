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
  public static final int DEFAULTSIZE = 5000;

  public byte[] get(String key) {
      pc(" got " + key);
    return map.get(key);
  }

  public void put(String key, byte[] val) {
    pc(" added " + key);
    map.put(key, val);
  }

  public FileCache(int maxSize) {
    this.maxSize = maxSize;
    map = new ConcurrentHashMap<String,byte[]>();
    size = new AtomicInteger(0);
  }

  public boolean contains(String key) {
    return map.containsKey(key);
  }

  public boolean roomForFile(byte[] val) {
    return maxSize > (size.get() + val.length);
  }

}
