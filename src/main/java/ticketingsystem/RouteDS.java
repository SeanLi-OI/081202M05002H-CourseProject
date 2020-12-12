package ticketingsystem;

public class RouteDS {
    private int coachNum;
    private int seatNum;
    private int seatsNum;
    private SeatCache seatCache;
    private SeatDS seats[];

    public RouteDS(int coachNum, int seatNum, int stationNum) {
        this.coachNum = coachNum;
        this.seatNum = seatNum;
        seatsNum = coachNum * seatNum;
        seats = new SeatDS[seatsNum];
        for (int i = 0; i < seatsNum; i++)
            seats[i] = new SeatDS(stationNum);
        seatCache = new SeatCache(coachNum, seatNum, stationNum);
    }

    public int[] buyTicket(int departure, int arrival) {
        int status[];
        while (seatCache.countSeat(departure, arrival) > 0) {
            for (int i = 0; i < seatsNum; i++) {
                while (seats[i].isAvailable(departure, arrival)) {
                    status = seats[i].hold(departure, arrival);
                    if (status != null) {
                        seatCache.updateCache(departure, arrival, status, false);
                        return new int[] { i / seatNum + 1, i % seatNum + 1 };
                    }
                }
            }
        }
        return null;
    }

    public int inquiry(int departure, int arrival) {
        return seatCache.countSeat(departure, arrival);
    }

    public boolean refund(Ticket ticket, int departure, int arrival) {
        int seatNo = (ticket.coach - 1) * coachNum + ticket.seat - 1;
        int status[] = seats[seatNo].unhold(departure, arrival);
        if (status != null) {
            seatCache.updateCache(departure, arrival, status, true);
            return true;
        }
        return false;
    }
}