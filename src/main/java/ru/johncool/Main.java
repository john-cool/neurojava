package ru.johncool;

import java.util.Scanner;

public class Main {
  public static void main(String[] args) {
    System.out.println("Привет, мир! Я изучаю Java с ментором.");

    Scanner sc = new Scanner(System.in);

    System.out.println("Как тебя зовут?");
    String name = sc.nextLine();

    System.out.println("Привет, " + name + " Добро пожаловать в мир Java!");

  }
}