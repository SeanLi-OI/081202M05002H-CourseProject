package ticketingsystem;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SeatDS {
    private int status;
    public int[] cache;
    private int stationNum;

    private ReadWriteLock statusRwLock;
    private ReadWriteLock cacheRwLock[];

    public SeatDS(int stationNum) {
        cache = new int[stationNum];
        status = (1 << stationNum) - 1;
        Arrays.fill(cache, status);
        this.stationNum = stationNum;
        cacheRwLock = new ReentrantReadWriteLock[stationNum];
        Arrays.fill(cacheRwLock, new ReentrantReadWriteLock(true));
        statusRwLock = new ReentrantReadWriteLock(true);
    }

    public boolean hold(int departure, int arrival) {
        Lock writeLock = statusRwLock.writeLock();
        writeLock.lock();
        try {
            status = status & (~(((1 << (arrival - departure)) - 1) << departure));
        } finally {
            writeLock.unlock();
        }
        return updateCache();
    }

    public boolean isAvailable(int departure, int arrival) {
        int temp;
        Lock readLock = cacheRwLock[departure].readLock();
        readLock.lock();
        try {
            temp = cache[departure];
        } finally {
            readLock.unlock();
        }
        return (temp & (1 << arrival)) > 0;
    }

    public boolean unhold(int departure, int arrival) {
        Lock writeLock = statusRwLock.writeLock();
        writeLock.lock();
        try {
            status = status | (((1 << (arrival - departure)) - 1) << departure);
        } finally {
            writeLock.unlock();
        }
        return updateCache();
    }

    public boolean updateCache() {
        int temp;
        Lock readLock, writeLock;
        for (int i = 0, j; i < stationNum;) {
            j = i;
            readLock = statusRwLock.readLock();
            readLock.lock();
            try {
                temp = status;
            } finally {
                readLock.unlock();
            }
            while (j < stationNum && (temp & (1 << j)) > 0)
                j++;
            while (i <= j && i < stationNum) {
                writeLock = cacheRwLock[i].writeLock();
                writeLock.lock();
                try {
                    cache[i] = (cache[i] | ((((1 << (j - i + 1))) - 1) << i))
                            & (~(((1 << (stationNum - j)) - 1) << (j + 1)));
                } finally {
                    writeLock.unlock();
                }
                i++;
            }
        }
        return true;
    }

}