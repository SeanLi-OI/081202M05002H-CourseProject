package ticketingsystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TicketingDS implements TicketingSystem {
    private int routeNum;
    private int coachNum;
    private int seatNum;
    private int stationNum;
    private int threadNum;

    private RouteDS[] rous;
    private static ConcurrentHashMap<Long, Ticket> hasAllot;
    private AtomicLong tidNum;
    private int seatNums;

    public TicketingDS(int routeNum, int coachNum, int seatNum, int stationNum, int threadNum) {
        this.routeNum = (routeNum == 0) ? 5 : routeNum;
        this.coachNum = (coachNum == 0) ? 8 : coachNum;
        this.seatNum = (seatNum == 0) ? 100 : seatNum;
        this.stationNum = (stationNum == 0) ? 10 : stationNum;
        this.threadNum = (threadNum == 0) ? 16 : threadNum;
        rous = new RouteDS[routeNum + 1];
        tidNum = new AtomicLong(0);
        for (int i = 1; i <= routeNum; i++)
            rous[i] = new RouteDS(coachNum, seatNum, stationNum);
        hasAllot = new ConcurrentHashMap<>();
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        if (checkRequest(route, departure, arrival))
            return null;
        int[] res = rous[route].buyTicket(departure - 1, arrival - 1);
        if (res != null) {
            Ticket ticket = new Ticket();
            ticket.tid = tidNum.incrementAndGet();
            ticket.coach = res[0];
            ticket.seat = res[1];
            ticket.passenger = passenger;
            ticket.route = route;
            ticket.departure = departure;
            ticket.arrival = arrival;
            hasAllot.put(ticket.tid, ticket);
            return ticket;
        }
        return null;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        return checkRequest(route, departure, arrival) ? -1 : rous[route].inquiry(departure - 1, arrival - 1);
    }

    @Override
    public boolean refundTicket(Ticket ticket) {
        if (!hasAllot.containsKey(ticket.tid) || !ticketEquals(ticket, hasAllot.get(ticket.tid)))
            return false;
        hasAllot.remove(ticket.tid, ticket);
        return rous[ticket.route].refund(ticket, ticket.departure - 1, ticket.arrival - 1);
    }

    private final boolean ticketEquals(Ticket x, Ticket y) {
        if (x == null || y == null)
            return false;
        return ((x.tid == y.tid) && (x.passenger.equals(y.passenger)) && (x.route == y.route) && (x.coach == y.coach)
                && (x.seat == y.seat) && (x.departure == y.departure) && (x.arrival == y.arrival));
    }

    public boolean checkRequest(int route, int departure, int arrival) {
        return route < 1 || route > this.routeNum || departure < 1 || departure > this.stationNum || arrival < 1
                || arrival > this.stationNum || arrival <= departure;
    }
}
