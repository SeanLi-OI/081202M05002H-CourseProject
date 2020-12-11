package ticketingsystem;

import java.util.concurrent.ConcurrentHashMap;

public class TicketingDS implements TicketingSystem {
    public int routenum;
    public int coachnum;
    public int seatnum;
    public int stationnum;
    public int threadnum;

    public RouteDS[] rous;
    public static ConcurrentHashMap<Long, Boolean> hasAllot;

    public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
        this.routenum = (routenum == 0) ? 5 : routenum;
        this.coachnum = (coachnum == 0) ? 8 : coachnum;
        this.seatnum = (seatnum == 0) ? 100 : seatnum;
        this.stationnum = (stationnum == 0) ? 10 : stationnum;
        this.threadnum = (threadnum == 0) ? 16 : threadnum;
        this.rous = new RouteDS[routenum + 1];
        for (int i = 1; i <= routenum; i++)
            this.rous[i] = new RouteDS(this.coachnum, this.seatnum, this.stationnum);
        hasAllot = new ConcurrentHashMap<>(1 << 22);
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        if (checkRequest(route, departure, arrival))
            return null;
        int[] res = rous[route].buyTicket(departure - 1, arrival - 1);
        if (res != null) {
            Ticket ticket = new Ticket();
            ticket.tid = res[0] * this.routenum + route;
            ticket.coach = res[1];
            ticket.seat = res[2];
            ticket.passenger = passenger;
            ticket.route = route;
            ticket.departure = departure;
            ticket.arrival = arrival;
            hasAllot.put(ticket.tid, true);
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
        return hasAllot.remove(ticket.tid) != null
                ? rous[ticket.route].refund(ticket, ticket.departure - 1, ticket.arrival - 1)
                : false;
    }

    public boolean checkRequest(int route, int departure, int arrival) {
        return route < 1 || route > this.routenum || departure < 1 || departure >= this.stationnum || arrival <= 1
                || arrival > this.stationnum || arrival < departure;
    }
}
