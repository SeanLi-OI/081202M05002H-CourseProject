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

    private ReadWriteLock seatsRwLock[];
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
        seatsRwLock = new ReentrantReadWriteLock[coachNum * seatNum];
        cacheRwLock = new ReentrantReadWriteLock[stationNum * stationNum];
        cache = new int[stationNum * stationNum];
        Arrays.fill(cache, -1);
        Arrays.fill(seatsRwLock, new ReentrantReadWriteLock(true));
        Arrays.fill(cacheRwLock, new ReentrantReadWriteLock(true));
        tidRwLock = new ReentrantReadWriteLock(true);
    }

    public int[] buyTicket(int departure, int arrival) {
        int index = departure * stationNum + arrival;
        int count = 0;
        int i = 0;
        int tidNum = -1;
        int[] coachAndSeat = new int[2];
        int[] oldStatus = new int[stationNum];
        int[] newStatus = new int[stationNum];
        Lock readLock=cacheRwLock[index].readLock();
        Lock writeLock;
        readLock.lock();
        try {
            if (cache[index] == 0)
                return null;
        } finally {
            readLock.unlock();
        }

        for (; i < seats.length; i++) {
            boolean isAvailable;
            readLock=seatsRwLock[i].readLock();
            readLock.lock();
            try {
                isAvailable = seats[i].isAvailable(departure, arrival);
                if (isAvailable)
                    oldStatus = Arrays.copyOf(seats[i].cache, seats[i].cache.length);
            } finally {
                readLock.unlock();
            }
            if (isAvailable) {
                writeLock=seatsRwLock[i].writeLock();
                writeLock.lock();
                try {
                    isAvailable = seats[i].hold(departure, arrival);
                } finally {
                    writeLock.unlock();
                }
                if (isAvailable) {
                    writeLock=tidRwLock.writeLock();
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
        }
        if (tidNum == -1)
            return null;

        int value;
        readLock=cacheRwLock[index].readLock();
        readLock.lock();
        try {
            value = cache[index];
        } finally {
            readLock.unlock();
        }
        if (value != -1)
            count = value - 1;
        else
            for (int j = i + 1; j < seats.length; j++) {
                readLock=seatsRwLock[j].readLock();
                readLock.lock();
                try {
                    if (seats[j].isAvailable(departure, arrival))
                        count++;
                } finally {
                    readLock.unlock();
                }
            }

        readLock=seatsRwLock[i].readLock();
        readLock.lock();
        try {
            newStatus = Arrays.copyOf(seats[i].cache, seats[i].cache.length);
        } finally {
            readLock.unlock();
        }
        updateCache(oldStatus, newStatus, false);

        writeLock=cacheRwLock[index].writeLock();
        writeLock.lock();
        try {
            cache[index] = count;
        } finally {
            writeLock.unlock();
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
        if (value != -1)
            return value;
        int count = 0;
        for (int i = 0; i < seats.length; i++) {
            readLock = seatsRwLock[i].readLock();
            readLock.lock();
            try {
                if (seats[i].isAvailable(departure, arrival))
                    count++;
            } finally {
                readLock.unlock();
            }
        }
        return count;
    }

    public boolean refund(Ticket ticket, int departure, int arrival) {
        int seatNo = (ticket.coach - 1) * coachNum + ticket.seat - 1;
        int[] oldStatus = new int[stationNum];
        int[] newStatus = new int[stationNum];
        Lock readLock = seatsRwLock[seatNo].readLock();
        readLock.lock();
        try {
            oldStatus = Arrays.copyOf(seats[seatNo].cache, seats[seatNo].cache.length);
        } finally {
            readLock.unlock();
        }
        boolean isSuccess;
        Lock writeLock = seatsRwLock[seatNo].writeLock();
        writeLock.lock();
        try {
            isSuccess = seats[seatNo].unhold(departure, arrival);
        } finally {
            writeLock.unlock();
        }
        if (isSuccess) {
            readLock = seatsRwLock[seatNo].readLock();
            readLock.lock();
            try {
                newStatus = Arrays.copyOf(seats[seatNo].cache, seats[seatNo].cache.length);
            } finally {
                readLock.unlock();
            }
            updateCache(oldStatus, newStatus, true);
            return true;
        }
        return false;
    }

    private void updateCache(int[] oldStatus, int[] newStatus, boolean flag) {
        Lock readLock, writeLock;
        for (int i = 0; i < stationNum; i++) {
            int temp = i * stationNum, diff = (oldStatus[i] ^ newStatus[i]) >> (i + 1);
            for (int j = i + 1; j < stationNum; j++, diff >>= 1)
                if ((diff & 1) > 0) {
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
                            cache[temp + j] = (flag ? res + 1 : res - 1);
                        } finally {
                            writeLock.unlock();
                        }
                    }
                }
        }
    }
}