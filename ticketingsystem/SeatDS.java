package ticketingsystem;

import java.util.Arrays;

public class SeatDS {
    private int status;
    public int[] cache;
    private int stationNum;

    public SeatDS(int stationNum) {
        cache = new int[stationNum];
        status = (1 << stationNum) - 1;
        Arrays.fill(cache, status);
        this.stationNum = stationNum;
    }

    public boolean hold(int departure, int arrival) {
        status = status & (~(((1 << (arrival - departure)) - 1) << departure));
        return updateCache();
    }

    public boolean isAvailable(int departure, int arrival) {
        return (cache[departure] & (1 << arrival)) > 0;
    }

    public boolean unhold(int departure, int arrival) {
        status = status | (((1 << (arrival - departure)) - 1) << departure);
        return updateCache();
    }

    public boolean updateCache() {

        for (int i = 0, j; i < stationNum;) {
            j = i;
            while (j < stationNum && (status & (1 << j)) > 0)
                j++;
            while (i <= j && i < stationNum) {
                cache[i] = (cache[i] | ((((1 << (j - i + 1))) - 1) << i))
                        & (~(((1 << (stationNum - j)) - 1) << (j + 1)));
                i++;
            }
        }
        return true;
    }

}