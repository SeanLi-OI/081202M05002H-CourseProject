package ticketingsystem;

import java.util.Arrays;

public class RouteDS {
    public int coachNum;
    public int seatNum;
    public int stationNum;
    public SeatDS[] seats;
    public int tid;
    public Object lockObject;
    public int[] cache;

    public RouteDS(int coachnum, int seatnum, int stationnum) {
        this.coachNum = coachnum;
        this.seatNum = seatnum;
        this.stationNum = stationnum;
        this.seats = new SeatDS[coachnum * seatnum];
        for (int i = 0; i < coachnum * seatnum; i++) {
            this.seats[i] = new SeatDS(stationnum);
        }
        tid = 0;
        lockObject = new Object();
        this.cache = new int[stationnum * stationnum];
        Arrays.fill(cache, -1);
    }

    public int[] buyTicket(int departure, int arrival) {
        int index = departure * this.stationNum + arrival;
        int count = 0;
        int i = 0;
        int tidNum = -1;
        int[] coachAndSeat = new int[2];
        int[] oldStatus = new int[this.stationNum];
        int[] newStatus = new int[this.stationNum];
        synchronized (lockObject) {
            if (this.cache[index] == 0)
                return null;
            for (; i < this.seats.length; i++)
                if (this.seats[i].isAvailable(departure, arrival)) {
                    oldStatus = Arrays.copyOf(seats[i].cache, seats[i].cache.length);
                    if (this.seats[i].hold(departure, arrival)) {
                        tidNum = tid++;
                        coachAndSeat = new int[] { i / this.seatNum + 1, i % this.seatNum + 1 };
                        break;
                    }
                }
            if (tidNum == -1)
                return null;
            int value = this.cache[index];
            if (value != -1)
                count = value - 1;
            else
                for (int j = i + 1; j < this.seats.length; j++)
                    if (this.seats[j].isAvailable(departure, arrival))
                        count++;
            updateCache(oldStatus, newStatus, this.seats[i], false);
            this.cache[index] = count;
        }
        return new int[] { tidNum, coachAndSeat[0], coachAndSeat[1] };
    }

    public int inquiry(int departure, int arrival) {
        int index = departure * this.stationNum + arrival;
        int value = this.cache[index];
        if (value != -1)
            return value;
        int count = 0;
        for (int i = 0; i < this.seats.length; i++)
            if (this.seats[i].isAvailable(departure, arrival))
                count++;
        return count;
    }

    public boolean refund(Ticket ticket, int departure, int arrival) {
        int seatNo = (ticket.coach - 1) * this.coachNum + ticket.seat - 1;
        int[] oldStatus = new int[this.stationNum];
        int[] newStatus = new int[this.stationNum];
        synchronized (lockObject) {
            oldStatus = Arrays.copyOf(seats[seatNo].cache, seats[seatNo].cache.length);
            if (this.seats[seatNo].unhold(departure, arrival)) {
                updateCache(oldStatus, newStatus, this.seats[seatNo], true);
                return true;
            }
        }
        return false;
    }

    private void updateCache(int[] oldStatus, int[] newStatus, SeatDS seat, boolean flag) {
        newStatus = Arrays.copyOf(seat.cache, seat.cache.length);
        for (int i = 0; i < this.stationNum; i++) {
            int temp = i * this.stationNum, diff = (oldStatus[i] ^ newStatus[i]) >> (i + 1);
            for (int j = i + 1; j < this.stationNum; j++, diff >>= 1)
                if ((diff & 1) > 0) {
                    int res = this.cache[temp + j];
                    if (res != -1)
                        this.cache[temp + j] = (flag ? res + 1 : res - 1);
                }
        }
    }
}