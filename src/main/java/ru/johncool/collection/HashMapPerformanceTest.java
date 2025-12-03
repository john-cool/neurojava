package ru.johncool.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class HashMapPerformanceTest {
  private static final int NUM_ELEMENTS = 100000;
  private static final int NUM_GET_OPERATIONS = 10000;
  private static final Random RANDOM = new Random();
  private static final double MILLION = 1_000_000.0;
  private static final int INITIAL_CAPACITY = 16;
  private static final float ZERO_FIVE = 0.5f;
  private static final float ZERO_NINE = 0.9f;


  public static void main(String[] args) {
    System.out.println("=== Тест производительности HashMap с разными настройками ===");
    int optimalCapacity = calculateOptimalCapacity(100000, 0.75f);
    HashMap<Integer, Integer> mapTrulyOptimal = new HashMap<>(optimalCapacity, 0.75f);

    HashMap<Integer,Integer> mapDefaultLf = new HashMap<>();
    HashMap<Integer,Integer> mapZeroFiveLf  = new HashMap<>(INITIAL_CAPACITY, ZERO_FIVE);
    HashMap<Integer,Integer> mapZeroNineLf  = new HashMap<>(INITIAL_CAPACITY, ZERO_NINE);
    HashMap<Integer,Integer> mapTenThousandsCapacity  = new HashMap<>(NUM_GET_OPERATIONS);

    testEfficiencyHashMap(mapDefaultLf, "Default load factor");
    testEfficiencyHashMap(mapZeroFiveLf, "Zero point five load factor");
    testEfficiencyHashMap(mapZeroNineLf, "Zero point nine load factor");
    testEfficiencyHashMap(mapTenThousandsCapacity, "Ten thousands initial capacity");
    testEfficiencyHashMap(mapTrulyOptimal, "Truly optimal");
  }

  private static void testEfficiencyHashMap(Map<Integer, Integer> map, String description) {
    long startPutTest = System.nanoTime();
    for (int i = 0; i < NUM_ELEMENTS; i++) {
      map.put(i, i);
    }
    long endPutTest = System.nanoTime();
    double durationPut = (endPutTest - startPutTest) / MILLION;

    long startGetTest = System.nanoTime();
    for (int i = 0; i < NUM_GET_OPERATIONS; i++) {
      map.get(RANDOM.nextInt(NUM_ELEMENTS));
    }
    long endGetTest = System.nanoTime();
    double durationGet = (endGetTest - startGetTest) / MILLION;

    System.out.printf("HashMap: %s - Put: %sms, Get: %sms \n", description, durationPut, durationGet);
  }

  private static int calculateOptimalCapacity(int expectedSize, float loadFactor) {
    return (int)(expectedSize / loadFactor);
  }
}
