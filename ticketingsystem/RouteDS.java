package ticketingsystem;

public class RouteDS {
    private int seatNum;
    private int seatsNum;
    private SeatCache seatCache;
    private SeatDS[] seats;

    public RouteDS(int coachNum, int seatNum, int stationNum) {
        this.seatNum = seatNum;
        seatsNum = coachNum * seatNum;
        seats = new SeatDS[seatsNum];
        for (int i = 0; i < seatsNum; i++)
            seats[i] = new SeatDS(stationNum);
        seatCache = new SeatCache(coachNum, seatNum, stationNum);
    }

    public final int[] buyTicket(int departure, int arrival) {
        int[] status;
        while (seatCache.countSeat(departure, arrival) > 0) {
            for (int i = 0; i < seatsNum; i++) {
                while (seats[i].isAvailable(departure, arrival)) {
                    status = seats[i].hold(departure, arrival);
                    if (status != null) {
                        seatCache.decCache(departure, arrival, status);
                        return new int[] { i / seatNum + 1, i % seatNum + 1 };
                    }
                }
            }
        }
        return null;
    }

    public final int inquiry(int departure, int arrival) {
        return seatCache.countSeat(departure, arrival);
    }

    public final boolean refund(Ticket ticket, int departure, int arrival) {
        int seatNo = (ticket.coach - 1) * seatNum + ticket.seat - 1;
        int[] status = seats[seatNo].unhold(departure, arrival);
        if (status != null) {
            seatCache.incCache(departure, arrival, status);
            return true;
        }
        return false;
    }
}