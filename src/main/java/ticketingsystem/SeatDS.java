package ticketingsystem;

import java.util.concurrent.atomic.AtomicInteger;

public class SeatDS {
    private AtomicInteger status;

    public SeatDS(int stationNum) {
        status = new AtomicInteger((1 << stationNum) - 1);
    }

    public boolean hold(int departure, int arrival, Status w) {
        int oldStatus, newStatus;
        do {
            oldStatus = status.get();
            if (((oldStatus >> departure) & ((1 << (arrival - departure)) - 1)) != ((1 << (arrival - departure)) - 1))
                return false;
            newStatus = oldStatus & (~(((1 << (arrival - departure)) - 1) << departure));
        } while (!status.compareAndSet(oldStatus, newStatus));
        w = new Status(oldStatus, newStatus);
        return true;
    }

    public boolean isAvailable(int departure, int arrival) {

        return ((status.get() >> departure) & ((1 << (arrival - departure)) - 1)) == ((1 << (arrival - departure)) - 1);
    }

    public boolean unhold(int departure, int arrival, Status w) {
        int oldStatus, newStatus;
        do {
            oldStatus = status.get();
            newStatus = oldStatus | (((1 << (arrival - departure)) - 1) << departure);
        } while (!status.compareAndSet(oldStatus, newStatus));
        w = new Status(oldStatus, newStatus);
        return true;
    }
}