package ru.johncool.producerconsumer.consumer;

import ru.johncool.producerconsumer.model.Task;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class TaskConsumer implements Runnable {
  private final BlockingQueue<Task> queue;
  private final int consumerId;
  private volatile boolean running = true;
  private int tasksProcessed = 0;
  private static final Random RANDOM = new Random();

  // Конфигурируемые параметры
  private final long pollTimeoutMs;
  private final int minProcessingTimeMs;
  private final int maxProcessingTimeMs;

  // Метрики
  private long totalProcessingTime = 0;
  private long maxProcessingTime = 0;
  private long minProcessingTime = Long.MAX_VALUE;
  private long totalTaskLifetime = 0;

  // Конструктор с конфигурацией
  public TaskConsumer(BlockingQueue<Task> queue, int consumerId,
                      int minProcessingTimeMs, int maxProcessingTimeMs,
                      long pollTimeoutMs) {
    this.queue = queue;
    this.consumerId = consumerId;
    this.minProcessingTimeMs = minProcessingTimeMs;
    this.maxProcessingTimeMs = maxProcessingTimeMs;
    this.pollTimeoutMs = pollTimeoutMs;

    // Валидация параметров
    if (minProcessingTimeMs < 0 || maxProcessingTimeMs < minProcessingTimeMs) {
      throw new IllegalArgumentException("Некорректные параметры времени обработки");
    }
  }

  // Упрощенный конструктор с параметрами по умолчанию
  public TaskConsumer(BlockingQueue<Task> queue, int consumerId) {
    this(queue, consumerId, 100, 300, 100);
  }

  @Override
  public void run() {
    System.out.printf("[Consumer-%d] Запущен. Ожидаю задачи...%n", consumerId);
    System.out.printf("[Consumer-%d] Конфигурация: pollTimeout=%dms, processingTime=%d-%dms%n",
      consumerId, pollTimeoutMs, minProcessingTimeMs, maxProcessingTimeMs);

    try {
      while (running && !Thread.currentThread().isInterrupted()) {
        Task task = pollTask();

        if (task == null) {
          continue; // Таймаут, продолжаем
        }

        if (task.isPoisonPill()) {
          handlePoisonPill();
          break;
        }

        processTaskSafely(task);
      }
    } catch (Exception e) {
      System.out.printf("[Consumer-%d] Критическая ошибка: %s%n",
        consumerId, e.getMessage());
    } finally {
      printFinalStats();
    }
  }

  private Task pollTask() throws InterruptedException {
    try {
      return queue.poll(pollTimeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      System.out.printf("[Consumer-%d] Получено прерывание во время ожидания задачи%n",
        consumerId);
      Thread.currentThread().interrupt();
      throw e;
    }
  }

  private void handlePoisonPill() {
    System.out.printf("[Consumer-%d] Получил Poison Pill. Завершаю работу...%n",
      consumerId);
  }

  private void processTaskSafely(Task task) {
    try {
      long processingTime = processTask(task);
      updateMetrics(processingTime, System.currentTimeMillis() - task.getCreatedTime());
      tasksProcessed++;
    } catch (InterruptedException e) {
      System.out.printf("[Consumer-%d] Прервана обработка задачи ID=%d%n",
        consumerId, task.getId());
      Thread.currentThread().interrupt();
      throw new RuntimeException("Поток был прерван", e);
    } catch (Exception e) {
      System.out.printf("[Consumer-%d] Ошибка обработки задачи ID=%d: %s%n",
        consumerId, task.getId(), e.getMessage());
      // Здесь можно добавить логику для Dead Letter Queue
    }
  }

  private long processTask(Task task) throws InterruptedException {
    long startTime = System.currentTimeMillis();
    long taskAge = startTime - task.getCreatedTime();

    System.out.printf("[Consumer-%d] Начал задачу ID=%d (возраст: %dms)%n",
      consumerId, task.getId(), taskAge);

    // Имитация обработки с учетом конфигурации
    int processingTimeMs = minProcessingTimeMs +
      RANDOM.nextInt(maxProcessingTimeMs - minProcessingTimeMs + 1);
    Thread.sleep(processingTimeMs);

    long endTime = System.currentTimeMillis();
    long actualProcessingTime = endTime - startTime;

    System.out.printf("[Consumer-%d] Завершил ID=%d | " +
        "Обработка: %dms | Общее время: %dms%n",
      consumerId, task.getId(),
      actualProcessingTime, endTime - task.getCreatedTime());

    return actualProcessingTime;
  }

  private void updateMetrics(long processingTime, long taskLifetime) {
    totalProcessingTime += processingTime;
    totalTaskLifetime += taskLifetime;

    if (processingTime > maxProcessingTime) {
      maxProcessingTime = processingTime;
    }

    if (processingTime < minProcessingTime) {
      minProcessingTime = processingTime;
    }
  }

  private void printFinalStats() {
    System.out.println("\n" + "=".repeat(50));
    System.out.printf("[Consumer-%d] ФИНАЛЬНАЯ СТАТИСТИКА:%n", consumerId);
    System.out.printf("  Обработано задач: %d%n", tasksProcessed);

    if (tasksProcessed > 0) {
      System.out.printf("  Среднее время обработки: %.2fms%n", getAverageProcessingTime());
      System.out.printf("  Среднее время жизни задачи: %.2fms%n", getAverageTaskLifetime());
      System.out.printf("  Минимальное время обработки: %dms%n", minProcessingTime);
      System.out.printf("  Максимальное время обработки: %dms%n", maxProcessingTime);
    } else {
      System.out.println("  Нет обработанных задач для статистики");
    }
    System.out.println("=".repeat(50));
  }

  // Методы для получения метрик
  public double getAverageProcessingTime() {
    return tasksProcessed > 0 ? (double) totalProcessingTime / tasksProcessed : 0;
  }

  public double getAverageTaskLifetime() {
    return tasksProcessed > 0 ? (double) totalTaskLifetime / tasksProcessed : 0;
  }

  public long getMaxProcessingTime() {
    return maxProcessingTime;
  }

  public long getMinProcessingTime() {
    return tasksProcessed > 0 ? minProcessingTime : 0;
  }

  public void stop() {
    System.out.printf("[Consumer-%d] Получена команда остановки%n", consumerId);
    running = false;
  }

  public int getConsumerId() {
    return consumerId;
  }

  public int getTasksProcessed() {
    return tasksProcessed;
  }

  public boolean isRunning() {
    return running;
  }
}