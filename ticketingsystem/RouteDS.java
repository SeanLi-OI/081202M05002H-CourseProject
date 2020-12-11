package ticketingsystem;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RouteDS {
    public int coachNum;
    public int seatNum;
    public int stationNum;
    public SeatDS[] seats;
    public int tid;
    public int[] cache;

    private ReadWriteLock cacheRwLock[];
    private ReadWriteLock tidRwLock;

    public RouteDS(int coachNum, int seatNum, int stationNum) {
        this.coachNum = coachNum;
        this.seatNum = seatNum;
        this.stationNum = stationNum;
        seats = new SeatDS[coachNum * seatNum];
        for (int i = 0; i < coachNum * seatNum; i++)
            seats[i] = new SeatDS(stationNum);
        tid = 0;
        cacheRwLock = new ReentrantReadWriteLock[stationNum * stationNum];
        cache = new int[stationNum * stationNum];
        Arrays.fill(cache, coachNum * seatNum);
        Arrays.fill(cacheRwLock, new ReentrantReadWriteLock(true));
        tidRwLock = new ReentrantReadWriteLock(true);
    }

    public int[] buyTicket(int departure, int arrival) {
        int index = departure * stationNum + arrival;
        int count = 0;
        int tidNum = -1;
        int[] coachAndSeat = new int[2];
        Lock readLock = cacheRwLock[index].readLock();
        Lock writeLock;
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
                writeLock = tidRwLock.writeLock();
                writeLock.lock();
                try {
                    tidNum = tid++;
                } finally {
                    writeLock.unlock();
                }
                coachAndSeat = new int[] { i / seatNum + 1, i % seatNum + 1 };
                break;
            }
        }
        if (tidNum == -1)
            return null;

        for (int i = 0; i < stationNum; i++) {
            int temp = i * stationNum;
            for (int j = i + 1; j < stationNum; j++)
                if ((((w.oldStatus >> i) & ((1 << (j - i)) - 1)) == ((1 << (j - i)) - 1))
                        && (((w.newStatus >> i) & ((1 << (j - i)) - 1)) != ((1 << (j - i)) - 1))) {
                    int res;
                    readLock = cacheRwLock[temp + j].readLock();
                    readLock.lock();
                    try {
                        res = cache[temp + j];
                    } finally {
                        readLock.unlock();
                    }
                    if (res != -1) {
                        writeLock = cacheRwLock[temp + j].writeLock();
                        writeLock.lock();
                        try {
                            cache[temp + j] = res - 1;
                        } finally {
                            writeLock.unlock();
                        }
                    }
                }
        }

        return new int[] { tidNum, coachAndSeat[0], coachAndSeat[1] };
    }

    public int inquiry(int departure, int arrival) {
        int index = departure * stationNum + arrival, value;
        Lock readLock = cacheRwLock[index].readLock();
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
        Lock readLock, writeLock;
        Status w = new Status(0, 0);
        if (seats[seatNo].unhold(departure, arrival, w)) {
            for (int i = 0; i < stationNum; i++) {
                int temp = i * stationNum;
                for (int j = i + 1; j < stationNum; j++)
                    if ((((w.oldStatus >> i) & ((1 << (j - i)) - 1)) != ((1 << (j - i)) - 1))
                            && (((w.newStatus >> i) & ((1 << (j - i)) - 1)) == ((1 << (j - i)) - 1))) {
                        int res;
                        readLock = cacheRwLock[temp + j].readLock();
                        readLock.lock();
                        try {
                            res = cache[temp + j];
                        } finally {
                            readLock.unlock();
                        }
                        if (res != -1) {
                            writeLock = cacheRwLock[temp + j].writeLock();
                            writeLock.lock();
                            try {
                                cache[temp + j] = res + 1;
                            } finally {
                                writeLock.unlock();
                            }
                        }
                    }
            }
            return true;
        }
        return false;
    }
}