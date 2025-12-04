package ru.johncool.producerconsumer.producer;

import ru.johncool.producerconsumer.model.Task;

import java.util.concurrent.BlockingQueue;

public class TaskProducer implements Runnable {
  private final BlockingQueue<Task> queue;
  private final int producerId;
  private final int totalTasks;
  private volatile boolean running = true;

  public TaskProducer(BlockingQueue<Task> queue, int producerId, int totalTasks) {
    this.queue = queue;
    this.producerId = producerId;
    this.totalTasks = totalTasks;
  }

  @Override
  public void run() {
    try {
      for (int i = 0; i < totalTasks && running; i++) {
        // 1. Создай задачу
        Task task = new Task(i, "Задача номер " + i);
        // 2. Положи в очередь с помощью put()
        queue.put(task);
        // 3. Залогируй действие
        System.out.printf("Добавлена задача номер %s, продюссером с id %s", i, producerId);
      }
      System.out.println("Producer " + producerId + " завершил работу");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.out.println("Producer " + producerId + " прерван");
    }
  }

  public void stop() {
    running = false;
  }


}