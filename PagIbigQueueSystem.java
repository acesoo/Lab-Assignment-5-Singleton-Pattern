package lab5;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class PagIbigQueueSystem {

    private final AtomicInteger currentNumber = new AtomicInteger(1);
    private final Object lock = new Object();
    private volatile boolean running = true;

    public void start() {
        System.out.println("=== PAG-IBIG CENTRALIZED QUEUING SYSTEM ===");
        System.out.println("Serving 3 Help Desk Stations\n");

        Thread monitor = new Thread(this::runMonitor);
        monitor.setDaemon(true);
        monitor.start();

        Thread[] stations = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int id = i + 1;
            stations[i] = new Thread(() -> stationLoop(id));
            stations[i].start();
        }

        // keep main thread alive until all stations exit
        try {
            for (Thread t : stations) t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running = false;
        }
    }

    private void stationLoop(int stationId) {
        Scanner sc = new Scanner(System.in);         

        while (running) {
            System.out.println("\n[Station " + stationId + "] Options:");
            System.out.println("1. Call Next Customer");
            System.out.println("2. Reset Queue Number");
            System.out.println("3. Exit Station");
            System.out.print("Choose (1-3): ");

            int choice = safeReadInt(sc);
            if (choice == -1) continue;               

            switch (choice) {
                case 1 -> {
                    int num = callNext();
                    System.out.println("[Station " + stationId + "] Now serving: QUEUE #" + num);
                }
                case 2 -> {
                    System.out.print("Enter new queue number: ");
                    int resetTo = safeReadInt(sc);
                    if (resetTo != -1) {
                        reset(resetTo);
                        System.out.println("[Station " + stationId + "] Queue reset to: " + resetTo);
                    }
                }
                case 3 -> {
                    System.out.println("[Station " + stationId + "] Shutting down…");
                    return;
                }
                default -> System.out.println("Invalid option – try again.");
            }

            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        }
    }

    private int callNext() {
        return currentNumber.getAndIncrement();
    }

    private void reset(int newNumber) {
        synchronized (lock) {
            currentNumber.set(newNumber);
            System.out.println("\n*** QUEUE RESET TO " + newNumber + " ***\n");
        }
    }

    private void runMonitor() {
        int last = -1;
        System.out.println("\n--- ONLINE QUEUE MONITOR (pagibig.gov.ph/queue) ---");

        while (running) {
            int cur = currentNumber.get();
            if (cur != last) {
                int serving = cur - 1;
                int waiting = Math.max(0, serving);
                System.out.printf("[LIVE] Now Serving: QUEUE #%03d   |  Waiting: %d%n",
                        serving, waiting);
                last = cur;
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
    }

    private int safeReadInt(Scanner sc) {
        try {
            return sc.nextInt();
        } catch (Exception e) {
            sc.nextLine();                     
            System.out.println("Please type a number.");
            return -1;
        }
    }
}