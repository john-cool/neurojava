package ru.johncool.collection;

import java.util.*;

public class ListPerformanceTest {

  private static final Random RANDOM = new Random();
  private static final int OPERATION_SIZE = 10000;
  private static final int SIZE = 100000;
  private static final int INS_OPERATIONS_SIZE = 1000;
  private static final double MILLION = 1_000_000.0;

  public static void main(String[] args) {

    System.out.println("=== Тест сравнение скорости работы с коллекциями ArrayList и LinkedList ===");

    // 1. Время добавления 100_000 элементов в конец
    testAddToEnd(new LinkedList<>(), "LinkedList");
    testAddToEnd(new ArrayList<>(), "ArrayList");

    // 2. Время доступа к случайному элементу ArrayList
    ArrayList<Integer> filledArrayList = new ArrayList<>();
    LinkedList<Integer> filledLinkedList = new LinkedList<>();
    fillList(filledArrayList, SIZE);
    fillList(filledLinkedList, SIZE);

    testGetByIndex(filledArrayList, "ArrayList");
    testGetByIndex(filledLinkedList, "LinkedList");

    // 3. Время вставки в середину списка ArrayList
    testInsertInMiddle(filledArrayList, "ArrayList");
    testInsertInMiddle(filledLinkedList, "LinkedList");

  }

  private static void testAddToEnd(List<Integer> list, String listName) {
    long startTestTime = System.nanoTime();
    for (int i = 0; i < SIZE; i++) {
      list.add(i);
    }
    long endTestTime = System.nanoTime();
    printResult(startTestTime, endTestTime, "testAddToEnd", listName);
  }

  private static void testGetByIndex(List<Integer> list, String listName) {
    long beginTestGetByIndex = System.nanoTime();
    for (int i = 0; i < OPERATION_SIZE; i++) {
      int index = RANDOM.nextInt(list.size());
      Integer number = list.get(index);
    }
    long endTestGetByIndex = System.nanoTime();
    printResult(beginTestGetByIndex, endTestGetByIndex, "testGetByIndex", listName);
  }
  private static void testInsertInMiddle(List<Integer> list, String listName) {
    long beginTestGetByIndex = System.nanoTime();

    if (list instanceof LinkedList) {
      // Для LinkedList используем итератор для эффективной вставки
      LinkedList<Integer> linkedList = (LinkedList<Integer>) list;
      int middleIndex = list.size() / 2;
      ListIterator<Integer> iterator = linkedList.listIterator(middleIndex);

      for (int i = 0; i < INS_OPERATIONS_SIZE; i++) {
        iterator.add(i);
      }
    } else {
      // Для ArrayList используем обычную вставку
      int middleIndex = list.size() / 2;
      for (int i = 0; i < INS_OPERATIONS_SIZE; i++) {
        list.add(middleIndex + i, i); // Смещаем индекс т.к. список растет
      }
    }

    long endTestGetByIndex = System.nanoTime();
    printResult(beginTestGetByIndex, endTestGetByIndex, "testInsertInMiddle", listName);
  }

  private static void printResult(long startTestTime, long endTestTime, String testName, String listName) {
    long durationNanos = endTestTime - startTestTime;
    double durationMillis = durationNanos / MILLION;
    System.out.printf("Тест %s для %s: %.3f мс%n", testName, listName, durationMillis);
  }

  // Вспомогательный метод для заполнения списков
  private static void fillList(List<Integer> list, int count) {
    for (int i = 0; i < count; i++) {
      list.add(i);
    }
  }
}