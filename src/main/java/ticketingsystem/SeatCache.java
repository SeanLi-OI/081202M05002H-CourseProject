package ticketingsystem;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SeatCache {
    public int coachNum;
    public int seatNum;
    public int stationNum;
    public final int[][] cache;
    private final AtomicStampedReference<Integer> tag;
    private final ReadWriteLock cacheRwLock=new ReentrantReadWriteLock(true);
    private final Lock cacheReadLock=cacheRwLock.readLock();
    private final Lock cacheWriteLock=cacheRwLock.writeLock();

    public SeatCache(int coachNum, int seatNum, int stationNum) {
        cache = new int[2][];
        this.tag = new AtomicStampedReference<>(0, 0);
        this.stationNum = stationNum;
        for (int i = 0; i < 2; i++) {
            cache[i] = new int[stationNum * stationNum];
            Arrays.fill(cache[i], coachNum * seatNum);
        }
    }

    public int countSeat(int departure, int arrival) {
        int oldTimestamp, oldTag;
        int value;
        do {
            oldTimestamp = tag.getStamp();
            oldTag = tag.getReference();
            // cacheReadLock.lock();
            // try {
                value = cache[oldTag][departure * stationNum + arrival];
            // } finally {
            //     cacheReadLock.unlock();
            // }
        } while (!tag.compareAndSet(oldTag, oldTag, oldTimestamp, oldTimestamp));
        return value;
    }

    final public void updateCache(int departure, int arrival, int w[], boolean flag) {
        cacheWriteLock.lock();
        try {
            int oldTag = tag.getReference();
            int newTag = 1 - oldTag;
            int index = 0;
            int oldStatus = w[0];
            int newStatus = w[1];
            int temp;
            if (flag) {
                for (int i = 0; i < arrival; i++) {
                    index = index + i + 1;
                    temp = 1;
                    for (int j = i + 1; j < stationNum; j++) {
                        if  (((oldStatus & temp) != temp) &&((newStatus & temp) == temp))
                            cache[newTag][index] = ++cache[oldTag][index];
                        index++;
                        temp = (temp << 1) | 1;
                    }
                    oldStatus >>= 1;
                    newStatus >>= 1;
                }
            } else {
                for (int i = 0; i < arrival; i++) {
                    index = index + i + 1;
                    temp = 1;
                    for (int j = i + 1; j < stationNum; j++) {
                        if  (((oldStatus & temp) == temp) &&((newStatus & temp) != temp))
                            cache[newTag][index] = --cache[oldTag][index];
                        index++;
                        temp = (temp << 1) | 1;
                    }
                    oldStatus >>= 1;
                    newStatus >>= 1;
                }
            }
            tag.set(newTag, tag.getStamp() + 1);
        } finally {
            cacheWriteLock.unlock();
        }
    }
}