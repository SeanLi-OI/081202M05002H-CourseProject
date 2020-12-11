package ticketingsystem;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SeatDS {
    private AtomicInteger status;

    public SeatDS(int stationNum) {
        status = new AtomicInteger((1 << stationNum) - 1);
    }

    public boolean hold(int departure, int arrival, Status w) {
        do {
            w.oldStatus = status.get();
            if (((w.oldStatus>>departure) & ((1<<(arrival-departure))-1))!=((1<<(arrival-departure))-1))
                return false;
            w.newStatus = w.oldStatus & (~(((1 << (arrival - departure)) - 1) << departure));
        } while (!this.status.compareAndSet(w.oldStatus, w.newStatus));
        return true;
    }

    public boolean isAvailable(int departure, int arrival) {
        
        return ((status.get()>>departure) & ((1<<(arrival-departure))-1))==((1<<(arrival-departure))-1);
    }

    public boolean unhold(int departure, int arrival, Status w) {
        do {
            w.oldStatus = status.get();
            w.newStatus = w.oldStatus | (((1 << (arrival - departure)) - 1) << departure);
        } while (!this.status.compareAndSet(w.oldStatus, w.newStatus));
        return true;
    }
}