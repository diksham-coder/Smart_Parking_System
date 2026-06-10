// Smart Parking Management System
// DSA: Stack (LIFO), Queue (FIFO), Linear Search, Bubble Sort
// OOP: Encapsulation, Inheritance, Abstraction, Polymorphism

import java.util.Scanner;

// STACK: array-backed LIFO, manages free slot IDs
class Stack {
    private int capacity;
    private String[] items;
    private int top;

    public Stack(int capacity) {
        this.capacity = capacity;
        this.items = new String[capacity];
        this.top = -1;
    }

    public void push(String item) { if (top < capacity - 1) items[++top] = item; }
    public String pop() {
        if (isEmpty()) return null;
        String item = items[top];
        items[top--] = null;
        return item;
    }
    public boolean isEmpty() { return top == -1; }
    public int size() { return top + 1; }
}

// QUEUE: circular array-backed FIFO, manages overflow vehicles
class Queue {
    private int capacity;
    private Vehicle[] items;
    private int front, rear, size;

    public Queue(int capacity) {
        this.capacity = capacity;
        this.items = new Vehicle[capacity];
        this.front = 0;
        this.rear = -1;
        this.size = 0;
    }

    public void enqueue(Vehicle item) {
        if (size == capacity) { System.out.println("[ERROR] Waiting queue full."); return; }
        rear = (rear + 1) % capacity;
        items[rear] = item;
        size++;
    }

    public Vehicle dequeue() {
        if (isEmpty()) return null;
        Vehicle item = items[front];
        items[front] = null;
        front = (front + 1) % capacity;
        size--;
        if (size == 0) { front = 0; rear = -1; }
        return item;
    }

    // Linear search: duplicate check in queue
    public boolean contains(String number) {
        if (isEmpty()) return false;
        for (int i = 0; i < size; i++) {
            int index = (front + i) % capacity;
            if (items[index] != null && items[index].getNumber().equals(number)) return true;
        }
        return false;
    }

    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }

    public String listAll() {
        if (isEmpty()) return "None";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            int index = (front + i) % capacity;
            if (items[index] != null) {
                sb.append(items[index].getNumber());
                if (i < size - 1) sb.append(", ");
            }
        }
        return sb.toString();
    }
}

// VEHICLE: abstract base — encapsulation + abstraction
abstract class Vehicle {
    private String number;
    private String vType;

    public Vehicle(String number, String vType) { this.number = number; this.vType = vType; }
    public String getNumber() { return number; }
    public String getType() { return vType; }
    public abstract int calculateFee(int hours); // abstract = abstraction
}

// CAR: inheritance + polymorphism
class Car extends Vehicle {
    private static final int BASE_FEE = 50, HOURLY_RATE = 20;
    public Car(String number) { super(number, "CAR"); }
    @Override
    public int calculateFee(int hours) { return BASE_FEE + (hours * HOURLY_RATE); }
}

// BIKE: inheritance + polymorphism
class Bike extends Vehicle {
    private static final int BASE_FEE = 20, HOURLY_RATE = 10;
    public Bike(String number) { super(number, "BIKE"); }
    @Override
    public int calculateFee(int hours) { return BASE_FEE + (hours * HOURLY_RATE); }
}

// PARKING SLOT: one physical space
class ParkingSlot {
    private String slotId, slotType;
    private boolean isOccupied;
    private Vehicle vehicle;

    public ParkingSlot(String slotId, String slotType) {
        this.slotId = slotId;
        this.slotType = slotType;
        this.isOccupied = false;
        this.vehicle = null;
    }

    public void assign(Vehicle v) { isOccupied = true; vehicle = v; }
    public void release() { isOccupied = false; vehicle = null; }
    public String getSlotId() { return slotId; }
    public String getSlotType() { return slotType; }
    public boolean getIsOccupied() { return isOccupied; }
    public Vehicle getVehicle() { return vehicle; }
    public String getStatus() { return "[" + slotId + ":" + (isOccupied ? "OCCP" : "FREE") + "]"; }
}

// HISTORY RECORD: immutable exit transaction
class HistoryRecord {
    private String number, type, slot;
    private int hours, fee;

    public HistoryRecord(String number, String type, String slot, int hours, int fee) {
        this.number = number; this.type = type; this.slot = slot;
        this.hours = hours; this.fee = fee;
    }

    public String getNumber() { return number; }
    public String getType() { return type; }
    public String getSlot() { return slot; }
    public int getHours() { return hours; }
    public int getFee() { return fee; }
}

// PARKING LOT: main controller
// DSA: Stack (slot pool), Queue (waiting list), Linear Search, Bubble Sort
class ParkingLot {
    private int totalCars, totalBikes;
    private ParkingSlot[] carSlots, bikeSlots;
    private Stack carStack, bikeStack;
    private Queue carQueue, bikeQueue;
    private int revenue; // encapsulated — only modified via exitVehicle()
    private HistoryRecord[] history;
    private int historyCount;

    public ParkingLot(int totalCars, int totalBikes) {
        this.totalCars = totalCars;
        this.totalBikes = totalBikes;
        carSlots = new ParkingSlot[totalCars];
        bikeSlots = new ParkingSlot[totalBikes];

        for (int i = 0; i < totalCars; i++) carSlots[i] = new ParkingSlot("C" + (i + 1), "CAR");
        for (int i = 0; i < totalBikes; i++) bikeSlots[i] = new ParkingSlot("B" + (i + 1), "BIKE");

        carStack = new Stack(totalCars);
        bikeStack = new Stack(totalBikes);
        carQueue = new Queue(50);
        bikeQueue = new Queue(50);

        // push in reverse so C1/B1 is on top (first allocated)
        for (int i = totalCars - 1; i >= 0; i--) carStack.push(carSlots[i].getSlotId());
        for (int i = totalBikes - 1; i >= 0; i--) bikeStack.push(bikeSlots[i].getSlotId());

        revenue = 0;
        history = new HistoryRecord[200];
        historyCount = 0;
    }

    // Linear search by slot ID
    private ParkingSlot findSlotById(String slotId) {
        for (int i = 0; i < totalCars; i++) if (carSlots[i].getSlotId().equals(slotId)) return carSlots[i];
        for (int i = 0; i < totalBikes; i++) if (bikeSlots[i].getSlotId().equals(slotId)) return bikeSlots[i];
        return null;
    }

    // Linear search by vehicle number
    private ParkingSlot findSlotByVehicle(String number) {
        for (int i = 0; i < totalCars; i++) {
            ParkingSlot s = carSlots[i];
            if (s.getIsOccupied() && s.getVehicle().getNumber().equals(number)) return s;
        }
        for (int i = 0; i < totalBikes; i++) {
            ParkingSlot s = bikeSlots[i];
            if (s.getIsOccupied() && s.getVehicle().getNumber().equals(number)) return s;
        }
        return null;
    }

    // Bubble sort by numeric suffix of slot ID (e.g. C3 -> 3)
    private void bubbleSortSlots(ParkingSlot[] slots, int n) {
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                int a = Integer.parseInt(slots[j].getSlotId().substring(1));
                int b = Integer.parseInt(slots[j + 1].getSlotId().substring(1));
                if (a > b) { ParkingSlot temp = slots[j]; slots[j] = slots[j + 1]; slots[j + 1] = temp; }
            }
        }
    }

    private int countOccupied(ParkingSlot[] slots, int n) {
        int count = 0;
        for (int i = 0; i < n; i++) if (slots[i].getIsOccupied()) count++;
        return count;
    }

    // Feature 1: Park
    public void park(Vehicle vehicle) {
        String num = vehicle.getNumber();
        if (findSlotByVehicle(num) != null) { System.out.println("[ERROR] " + num + " is already parked."); return; }

        boolean isCar = vehicle.getType().equals("CAR");
        Stack stack = isCar ? carStack : bikeStack;
        Queue queue = isCar ? carQueue : bikeQueue;

        if (queue.contains(num)) { System.out.println("[ERROR] " + num + " is already in the waiting queue."); return; }

        if (!stack.isEmpty()) {
            String slotId = stack.pop();
            ParkingSlot slot = findSlotById(slotId);
            slot.assign(vehicle);
            System.out.println("[PARKED] " + num + " -> Slot " + slotId);
        } else {
            queue.enqueue(vehicle);
            System.out.println("[QUEUED] " + num + " | No free slot. Position: " + queue.size());
        }
    }

    // Feature 2: Exit — polymorphic fee, auto-assign from queue
    public void exitVehicle(String number, int hours) {
        ParkingSlot slot = findSlotByVehicle(number);
        if (slot == null) { System.out.println("[ERROR] " + number + " not found."); return; }

        Vehicle vehicle = slot.getVehicle();
        int fee = vehicle.calculateFee(hours); // runtime polymorphism
        revenue += fee;

        if (historyCount < 200)
            history[historyCount++] = new HistoryRecord(number, vehicle.getType(), slot.getSlotId(), hours, fee);

        slot.release();
        System.out.println("[EXIT] " + number + " | Slot " + slot.getSlotId() + " freed | Fee: Rs." + fee);

        boolean isCar = vehicle.getType().equals("CAR");
        Queue queue = isCar ? carQueue : bikeQueue;
        Stack stack = isCar ? carStack : bikeStack;

        if (!queue.isEmpty()) {
            Vehicle next = queue.dequeue();
            slot.assign(next);
            System.out.println("[AUTO] " + next.getNumber() + " assigned -> Slot " + slot.getSlotId());
        } else {
            stack.push(slot.getSlotId());
        }
    }

    // Feature 3: Search
    public void search(String number) {
        ParkingSlot slot = findSlotByVehicle(number);
        if (slot != null)
            System.out.println("[FOUND] " + number + " at Slot " + slot.getSlotId() + " (" + slot.getSlotType() + ")");
        else
            System.out.println("[MISS] " + number + " not found.");
    }

    // Feature 4: Slot map (Bubble Sort before display)
    public void slotMap() {
        bubbleSortSlots(carSlots, totalCars);
        bubbleSortSlots(bikeSlots, totalBikes);
        StringBuilder carRow = new StringBuilder("CAR  SLOTS: ");
        StringBuilder bikeRow = new StringBuilder("BIKE SLOTS: ");
        for (int i = 0; i < totalCars; i++) carRow.append(carSlots[i].getStatus()).append(" ");
        for (int i = 0; i < totalBikes; i++) bikeRow.append(bikeSlots[i].getStatus()).append(" ");
        System.out.println(carRow);
        System.out.println("Free Car Slots : " + (totalCars - countOccupied(carSlots, totalCars)) + "/" + totalCars);
        System.out.println(bikeRow);
        System.out.println("Free Bike Slots: " + (totalBikes - countOccupied(bikeSlots, totalBikes)) + "/" + totalBikes);
    }

    // Feature 5: Summary
    public void summary() {
        int cp = countOccupied(carSlots, totalCars);
        int bp = countOccupied(bikeSlots, totalBikes);
        System.out.println("Cars Parked : " + cp + " | Bikes Parked : " + bp);
        System.out.println("Cars Waiting: " + carQueue.size() + " | Bikes Waiting: " + bikeQueue.size());
        System.out.println("Total On-Site: " + (cp + bp));
        if (carQueue.size() > 0) System.out.println("Car Queue : [" + carQueue.listAll() + "]");
        if (bikeQueue.size() > 0) System.out.println("Bike Queue: [" + bikeQueue.listAll() + "]");
    }

    // Feature 6: Revenue (encapsulated — no public setter)
    public void revenue() { System.out.println("Total Revenue: Rs." + revenue); }

    // Feature 7: History
    public void showHistory() {
        if (historyCount == 0) { System.out.println("No records found."); return; }
        for (int i = 0; i < historyCount; i++) {
            HistoryRecord r = history[i];
            System.out.println("#" + (i + 1) + " | " + r.getNumber() + " [" + r.getType() + "]"
                + " | Slot: " + r.getSlot() + " | Hours: " + r.getHours() + " | Fee: Rs." + r.getFee());
        }
    }
}

// ENTRY POINT: menu-driven main loop
public class SmartParkingSystem {

    static Vehicle createVehicle(String number, String choice) {
        if (choice.equals("1")) return new Car(number);
        if (choice.equals("2")) return new Bike(number);
        return null;
    }

    static void displayMenu() {
        System.out.println("\n" + "=".repeat(48));
        System.out.println("      SMART PARKING MANAGEMENT SYSTEM");
        System.out.println("      Cars: 10 Slots  |  Bikes: 5 Slots");
        System.out.println("=".repeat(48));
        System.out.println(" 1. Park Vehicle     5. Summary");
        System.out.println(" 2. Exit Vehicle     6. Revenue");
        System.out.println(" 3. Search Vehicle   7. History");
        System.out.println(" 4. Slot Map         8. Quit");
        System.out.println("-".repeat(48));
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ParkingLot lot = new ParkingLot(10, 5);

        while (true) {
            displayMenu();
            System.out.print("Enter choice (1-8): ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Vehicle number: ");
                    String number = sc.nextLine().trim().toUpperCase();
                    System.out.print("Type (1=Car, 2=Bike): ");
                    String vType = sc.nextLine().trim();
                    Vehicle v = createVehicle(number, vType);
                    if (v != null) lot.park(v);
                    else System.out.println("[ERROR] Invalid type. Enter 1 or 2.");
                    break;

                case "2":
                    System.out.print("Vehicle number: ");
                    String exitNum = sc.nextLine().trim().toUpperCase();
                    System.out.print("Hours parked: ");
                    try {
                        int hours = Integer.parseInt(sc.nextLine().trim());
                        if (hours < 1) System.out.println("[ERROR] Hours must be at least 1.");
                        else lot.exitVehicle(exitNum, hours);
                    } catch (NumberFormatException e) {
                        System.out.println("[ERROR] Enter a valid number.");
                    }
                    break;

                case "3":
                    System.out.print("Vehicle number: ");
                    lot.search(sc.nextLine().trim().toUpperCase());
                    break;

                case "4": lot.slotMap(); break;
                case "5": lot.summary(); break;
                case "6": lot.revenue(); break;
                case "7": lot.showHistory(); break;

                case "8":
                    System.out.println("Final revenue:");
                    lot.revenue();
                    System.out.println("Goodbye!");
                    sc.close();
                    return;

                default:
                    System.out.println("[ERROR] Enter a number from 1 to 8.");
            }
        }
    }
}
