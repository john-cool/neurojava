package ru.johncool.producerconsumer.model;

public final class Task {
  private final int id;
  private final String data;
  private final long createdTime;

  public Task(int id, String data) {
    this.id = id;
    this.data = data;
    this.createdTime = System.currentTimeMillis();
  }

  public boolean isPoisonPill() {
    return this.id == -1;
  }

  public static Task createPoisonPill() {
    return new Task(-1, "POISON_PILL");
  }

  public int getId() {
    return id;
  }

  public String getData() {
    return data;
  }

  public long getCreatedTime() {
    return createdTime;
  }

  @Override
  public String toString() {
    return "Task{" +
      "id=" + id +
      ", data='" + data + '\'' +
      ", createdTime=" + createdTime +
      '}';
  }
}
