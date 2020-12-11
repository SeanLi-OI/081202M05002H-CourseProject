package ticketingsystem;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RouteDS {
    public int coachNum;
    public int seatNum;
    public int stationNum;
    public SeatDS[] seats;
    public AtomicInteger tid;
    public int[] cache;

    private ReadWriteLock cacheRwLock;

    public RouteDS(int coachNum, int seatNum, int stationNum) {
        this.coachNum = coachNum;
        this.seatNum = seatNum;
        this.stationNum = stationNum;
        seats = new SeatDS[coachNum * seatNum];
        for (int i = 0; i < coachNum * seatNum; i++)
            seats[i] = new SeatDS(stationNum);
        tid = new AtomicInteger(0);
        cache = new int[stationNum * stationNum];
        Arrays.fill(cache, coachNum * seatNum);
        cacheRwLock = new ReentrantReadWriteLock(true);
    }

    public int[] buyTicket(int departure, int arrival) {
        int index = departure * stationNum + arrival;
        int tidNum = -1;
        int[] coachAndSeat = new int[2];
        Lock readLock = cacheRwLock.readLock();
        readLock.lock();
        try {
            if (cache[index] == 0)
                return null;
        } finally {
            readLock.unlock();
        }
        Status w = new Status(0, 0);
        for (int i = 0; i < seats.length; i++) {
            if (seats[i].hold(departure, arrival, w)) {
                do {
                    tidNum = tid.get();
                } while (!tid.compareAndSet(tidNum, tidNum + 1));
                coachAndSeat = new int[] { i / seatNum + 1, i % seatNum + 1 };
                break;
            }
        }
        if (tidNum == -1)
            return null;

        Lock writeLock = cacheRwLock.writeLock();
        writeLock.lock();
        try {
            for (int i = 0; i < stationNum; i++) {
                int temp = i * stationNum;
                for (int j = i + 1; j < stationNum; j++)
                    if ((((w.oldStatus >> i) & ((1 << (j - i)) - 1)) == ((1 << (j - i)) - 1))
                            && (((w.newStatus >> i) & ((1 << (j - i)) - 1)) != ((1 << (j - i)) - 1))) 
                        cache[temp + j] --;
            }
        } finally {
            writeLock.unlock();
        }
        return new int[] { tidNum, coachAndSeat[0], coachAndSeat[1] };
    }

    public int inquiry(int departure, int arrival) {
        int index = departure * stationNum + arrival, value;
        Lock readLock = cacheRwLock.readLock();
        readLock.lock();
        try {
            value = cache[index];
        } finally {
            readLock.unlock();
        }
        return value;
    }

    public boolean refund(Ticket ticket, int departure, int arrival) {
        int seatNo = (ticket.coach - 1) * coachNum + ticket.seat - 1;
        Status w = new Status(0, 0);
        if (seats[seatNo].unhold(departure, arrival, w)) {
            Lock writeLock = cacheRwLock.writeLock();
            writeLock.lock();
            try {
                for (int i = 0; i < stationNum; i++) {
                    int temp = i * stationNum;
                    for (int j = i + 1; j < stationNum; j++)
                        if ((((w.oldStatus >> i) & ((1 << (j - i)) - 1)) != ((1 << (j - i)) - 1))
                                && (((w.newStatus >> i) & ((1 << (j - i)) - 1)) == ((1 << (j - i)) - 1))) 
                            cache[temp + j] ++;
                }
                return true;
            } finally {
                writeLock.unlock();
            }
        }
        return false;
    }
}