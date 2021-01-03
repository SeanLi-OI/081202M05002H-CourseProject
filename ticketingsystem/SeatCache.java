package ticketingsystem;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SeatCache {
    private int stationNum;
    private final int[][] cache;
    private final AtomicStampedReference<Integer> tag;
    private final ReadWriteLock cacheRwLock = new ReentrantReadWriteLock(true);
    private final Lock cacheWriteLock = cacheRwLock.writeLock();

    public SeatCache(int coachNum, int seatNum, int stationNum) {
        cache = new int[2][];
        this.tag = new AtomicStampedReference<>(0, 0);
        this.stationNum = stationNum;
        for (int i = 0; i < 2; i++) {
            cache[i] = new int[stationNum * stationNum];
            Arrays.fill(cache[i], coachNum * seatNum);
        }
    }

    public final int countSeat(int departure, int arrival) {
        int oldTimestamp;
        int oldTag;
        int value;
        do {
            oldTimestamp = tag.getStamp();
            oldTag = tag.getReference();
            value = cache[oldTag][departure * stationNum + arrival];
        } while (!tag.compareAndSet(oldTag, oldTag, oldTimestamp, oldTimestamp));
        return value;
    }

    public final void incCache(int departure, int arrival, int[] w) {
        int index = 0;
        int oldStatus = w[0];
        int newStatus = w[1];
        int temp;
        cacheWriteLock.lock();
        try {
            int oldTag = tag.getReference();
            int newTag = 1 - oldTag;
            for (int i = 0, k = (1 << (departure + 1)) - 1; i < departure; i++, k >>= 1) {
                index = index + departure + 1;
                temp = k;
                for (int j = departure + 1; j < stationNum; j++) {
                    if (((oldStatus & temp) != temp) && ((newStatus & temp) == temp))
                        cache[newTag][index] = ++cache[oldTag][index];
                    index++;
                    temp = (temp << 1) | 1;
                }
                oldStatus >>= 1;
                newStatus >>= 1;
            }
            for (int i = departure; i < arrival; i++) {
                index = index + i + 1;
                temp = 1;
                for (int j = i + 1; j < stationNum; j++) {
                    if (((oldStatus & temp) != temp) && ((newStatus & temp) == temp))
                        cache[newTag][index] = ++cache[oldTag][index];
                    index++;
                    temp = (temp << 1) | 1;
                }
                oldStatus >>= 1;
                newStatus >>= 1;
            }
            tag.set(newTag, tag.getStamp() + 1);
        } finally {
            cacheWriteLock.unlock();
        }
    }

    public final void decCache(int departure, int arrival, int[] w) {
        int index = 0;
        int oldStatus = w[0];
        int newStatus = w[1];
        int temp;
        cacheWriteLock.lock();
        try {
            int oldTag = tag.getReference();
            int newTag = 1 - oldTag;
            for (int i = 0, k = (1 << (departure + 1)) - 1; i < departure; i++, k >>= 1) {
                index = index + departure + 1;
                temp = k;
                for (int j = departure + 1; j < stationNum; j++) {
                    if (((oldStatus & temp) == temp) && ((newStatus & temp) != temp))
                        cache[newTag][index] = --cache[oldTag][index];
                    index++;
                    temp = (temp << 1) | 1;
                }
                oldStatus >>= 1;
                newStatus >>= 1;
            }
            for (int i = departure; i < arrival; i++) {
                index = index + i + 1;
                temp = 1;
                for (int j = i + 1; j < stationNum; j++) {
                    if (((oldStatus & temp) == temp) && ((newStatus & temp) != temp))
                        cache[newTag][index] = --cache[oldTag][index];
                    index++;
                    temp = (temp << 1) | 1;
                }
                oldStatus >>= 1;
                newStatus >>= 1;
            }
            tag.set(newTag, tag.getStamp() + 1);
        } finally {
            cacheWriteLock.unlock();
        }
    }
}