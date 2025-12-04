package ru.johncool.producerconsumer;

import ru.johncool.producerconsumer.model.Task;
import ru.johncool.producerconsumer.producer.TaskProducer;
import ru.johncool.producerconsumer.consumer.TaskConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ProducerConsumerBenchmark {
  private static final int QUEUE_CAPACITY = 200;
  private static final int NUM_PRODUCERS = 4;
  private static final int NUM_CONSUMERS = 3;
  private static final int TASKS_PER_PRODUCER = 100;
  private static final int CONSUMER_MIN_TIME_MS = 50;
  private static final int CONSUMER_MAX_TIME_MS = 250;
  private static final long CONSUMER_POLL_TIMEOUT_MS = 150;

  public static void main(String[] args) throws InterruptedException {
    System.out.println("=== БЕНЧМАРК PRODUCER-CONSUMER СИСТЕМЫ ===\n");

    long startTime = System.currentTimeMillis();

    // 1. Создаем очередь
    BlockingQueue<Task> taskQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    // 2. Создаем Consumer'ов и сохраняем ссылки для сбора метрик
    ExecutorService consumerExecutor = Executors.newFixedThreadPool(NUM_CONSUMERS);
    List<TaskConsumer> consumers = new ArrayList<>();

    for (int i = 0; i < NUM_CONSUMERS; i++) {
      TaskConsumer consumer = new TaskConsumer(
        taskQueue,
        i,
        CONSUMER_MIN_TIME_MS,
        CONSUMER_MAX_TIME_MS,
        CONSUMER_POLL_TIMEOUT_MS
      );
      consumers.add(consumer);
      consumerExecutor.submit(consumer);
    }

    // 3. Создаем и запускаем Producer'ов
    ExecutorService producerExecutor = Executors.newFixedThreadPool(NUM_PRODUCERS);
    for (int i = 0; i < NUM_PRODUCERS; i++) {
      producerExecutor.submit(new TaskProducer(taskQueue, i, TASKS_PER_PRODUCER));
    }

    // 4. Ждем завершения Producer'ов
    producerExecutor.shutdown();
    boolean producersFinished = producerExecutor.awaitTermination(2, TimeUnit.MINUTES);

    if (producersFinished) {
      System.out.println("\n✅ Все Producer'ы завершили работу");
      System.out.println("Всего задач создано: " + (NUM_PRODUCERS * TASKS_PER_PRODUCER));
    } else {
      System.out.println("\n⚠️ Producer'ы не завершились за отведенное время");
    }

    // 5. Отправляем Poison Pill для каждого Consumer'а
    System.out.println("\n📨 Отправляю Poison Pill для Consumer'ов...");
    for (int i = 0; i < NUM_CONSUMERS; i++) {
      taskQueue.put(Task.createPoisonPill());
    }

    // 6. Ждем завершения Consumer'ов
    consumerExecutor.shutdown();
    boolean consumersFinished = consumerExecutor.awaitTermination(1, TimeUnit.MINUTES);

    long totalTime = System.currentTimeMillis() - startTime;

    // 7. Выводим общую статистику
    printSystemStatistics(consumers, totalTime, producersFinished && consumersFinished);
  }

  private static void printSystemStatistics(List<TaskConsumer> consumers,
                                            long totalTimeMs, boolean success) {
    System.out.println("\n" + "=".repeat(60));
    System.out.println("📊 ОБЩАЯ СТАТИСТИКА СИСТЕМЫ");
    System.out.println("=".repeat(60));

    System.out.printf("Общее время работы: %.2f сек%n", totalTimeMs / 1000.0);
    System.out.printf("Статус завершения: %s%n", success ? "✅ УСПЕХ" : "⚠️ С ПРОБЛЕМАМИ");

    int totalTasksProcessed = 0;
    double totalAvgProcessingTime = 0;
    double totalAvgTaskLifetime = 0;

    System.out.println("\n📈 СТАТИСТИКА ПО CONSUMER'АМ:");
    System.out.println("-".repeat(60));

    for (TaskConsumer consumer : consumers) {
      int tasks = consumer.getTasksProcessed();
      totalTasksProcessed += tasks;

      System.out.printf("Consumer-%d: %d задач | ", consumer.getConsumerId(), tasks);
      System.out.printf("Ср. обработка: %.2fms | ", consumer.getAverageProcessingTime());
      System.out.printf("Ср. время жизни: %.2fms%n", consumer.getAverageTaskLifetime());

      totalAvgProcessingTime += consumer.getAverageProcessingTime();
      totalAvgTaskLifetime += consumer.getAverageTaskLifetime();
    }

    System.out.println("\n📊 ИТОГО ПО СИСТЕМЕ:");
    System.out.println("-".repeat(60));
    System.out.printf("Всего обработано задач: %d%n", totalTasksProcessed);

    if (consumers.size() > 0) {
      System.out.printf("Среднее время обработки: %.2fms%n",
        totalAvgProcessingTime / consumers.size());
      System.out.printf("Среднее время жизни задачи: %.2fms%n",
        totalAvgTaskLifetime / consumers.size());
      System.out.printf("Пропускная способность: %.2f задач/сек%n",
        totalTasksProcessed / (totalTimeMs / 1000.0));
    }

    System.out.println("=".repeat(60));
  }
}