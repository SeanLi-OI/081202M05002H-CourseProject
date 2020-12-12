package ticketingsystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TicketingDS implements TicketingSystem {
    private int routenum;
    private int coachnum;
    private int seatnum;
    private int stationnum;
    private int threadnum;

    private RouteDS[] rous;
    private static ConcurrentHashMap<Long, Ticket> hasAllot;
    private AtomicLong tidNum;

    public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
        this.routenum = (routenum == 0) ? 5 : routenum;
        this.coachnum = (coachnum == 0) ? 8 : coachnum;
        this.seatnum = (seatnum == 0) ? 100 : seatnum;
        this.stationnum = (stationnum == 0) ? 10 : stationnum;
        this.threadnum = (threadnum == 0) ? 16 : threadnum;
        this.rous = new RouteDS[routenum + 1];
        tidNum = new AtomicLong(0);
        for (int i = 1; i <= routenum; i++)
            this.rous[i] = new RouteDS(this.coachnum, this.seatnum, this.stationnum);
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
        if (x == y)
            return true;
        if (x == null || y == null)
            return false;

        return ((x.tid == y.tid) && (x.passenger.equals(y.passenger)) && (x.route == y.route) && (x.coach == y.coach)
                && (x.seat == y.seat) && (x.departure == y.departure) && (x.arrival == y.arrival));
    }

    public boolean checkRequest(int route, int departure, int arrival) {
        return route < 1 || route > this.routenum || departure < 1 || departure > this.stationnum || arrival < 1
                || arrival > this.stationnum || arrival <= departure;
    }
}
