package ru.johncool.collection;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentHashMapPerformanceTest {
  private static final int NUM_ELEMENTS = 100000;
  private static final int NUM_GET_OPERATIONS = 10000;
  private static final int NUM_THREADS = 10;
  private static final Random RANDOM = new Random();
  private static final double MILLION = 1_000_000.0;

  public static void main(String[] args) throws InterruptedException {
    System.out.println("=== Сравнение HashMap и ConcurrentHashMap ===");

    // Однопоточные тесты
    testSingleThreaded(new HashMap<>(), "HashMap (single-threaded)");
    testSingleThreaded(new ConcurrentHashMap<>(), "ConcurrentHashMap (single-threaded)");

    // Многопоточные тесты (без конкуренции)
    testMultiThreaded(new HashMap<>(), "HashMap (multi-threaded)");
    testMultiThreaded(new ConcurrentHashMap<>(), "ConcurrentHashMap (multi-threaded)");
    testMultiThreaded(Collections.synchronizedMap(new HashMap<>()), "SynchronizedMap (multi-threaded)");

    // Многопоточные тесты (с конкуренцией)
    testMultiThreadedWithContention(new ConcurrentHashMap<>(), "ConcurrentHashMap");
    testMultiThreadedWithContention(Collections.synchronizedMap(new HashMap<>()), "SynchronizedMap");

    // Тест многопоточного чтения
    testMultiThreadedReads(new HashMap<>(), "HashMap");
    testMultiThreadedReads(new ConcurrentHashMap<>(), "ConcurrentHashMap");

    // Тест атомарных операций
    testAtomicOperations();
  }


  private static void testSingleThreaded(Map<Integer, Integer> map, String description) {
    // Измерение put и get операций в одном потоке
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

  private static void testMultiThreaded(Map<Integer, Integer> map, String description) throws InterruptedException {
    int operationsPerThread = NUM_ELEMENTS / NUM_THREADS;

    ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
    CountDownLatch startLatch = new CountDownLatch(1);    // Для одновременного старта
    CountDownLatch finishLatch = new CountDownLatch(NUM_THREADS); // Для ожидания завершения

    long startTime = System.nanoTime();
    for (int i = 0; i < NUM_THREADS; i++) {
      final int thredId = i;
      executor.submit(() -> {
        try {
          startLatch.await();

          int startKey = thredId * operationsPerThread;
          int endKey = startKey + operationsPerThread;

          for (int key = startKey; key < endKey; key++) {
            map.put(key, key);
          }

          finishLatch.countDown();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }
    // Запускаем все потоки одновременно
    startLatch.countDown();

    // Ждем завершения всех потоков
    finishLatch.await();
    executor.shutdown();

    long endTime = System.nanoTime();
    double duration = (endTime - startTime) / MILLION;
    System.out.printf("%s - Time: %.3f ms, Size: %d%n", description, duration, map.size());
  }

  private static void testAtomicOperations() {
    ConcurrentHashMap<String, AtomicInteger> concurrentMap = new ConcurrentHashMap<>();

    // Тест computeIfAbsent - атомарная операция
    long start = System.nanoTime();
    for (int i = 0; i < NUM_ELEMENTS; i++) {
      concurrentMap.computeIfAbsent("key", k -> new AtomicInteger()).incrementAndGet();
    }
    long end = System.nanoTime();
    System.out.printf("ConcurrentHashMap computeIfAbsent: %.3f ms%n", (end - start) / MILLION);
  }

  private static void testMultiThreadedReads(Map<Integer, Integer> map, String description)
    throws InterruptedException {

    // Сначала заполним map данными
    for (int i = 0; i < NUM_ELEMENTS; i++) {
      map.put(i, i);
    }

    ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(NUM_THREADS);

    long startTime = System.nanoTime();

    for (int i = 0; i < NUM_THREADS; i++) {
      executor.submit(() -> {
        try {
          startLatch.await();

          // Многопоточное чтение
          for (int j = 0; j < NUM_GET_OPERATIONS / NUM_THREADS; j++) {
            int key = RANDOM.nextInt(NUM_ELEMENTS);
            map.get(key);
          }

          finishLatch.countDown();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }

    startLatch.countDown();
    finishLatch.await();
    executor.shutdown();

    long endTime = System.nanoTime();
    double duration = (endTime - startTime) / MILLION;
    System.out.printf("%s (reads only) - Time: %.3f ms%n", description, duration);
  }

  private static void testMultiThreadedWithContention(Map<Integer, Integer> map, String description)
    throws InterruptedException {

    ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(NUM_THREADS);

    long startTime = System.nanoTime();

    for (int i = 0; i < NUM_THREADS; i++) {
      executor.submit(() -> {
        try {
          startLatch.await();

          // ВСЕ потоки работают с одними и теми же ключами!
          // Высокая конкуренция за ресурсы
          for (int j = 0; j < NUM_ELEMENTS / NUM_THREADS; j++) {
            int key = RANDOM.nextInt(1000); // Только 1000 разных ключей!
            map.put(key, key);
          }

          finishLatch.countDown();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }

    startLatch.countDown();
    finishLatch.await();
    executor.shutdown();

    long endTime = System.nanoTime();
    double duration = (endTime - startTime) / MILLION;
    System.out.printf("%s (with contention) - Time: %.3f ms, Size: %d%n",
      description, duration, map.size());
  }
}