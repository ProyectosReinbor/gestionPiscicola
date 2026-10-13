package piscicola;

import java.util.*;

public final class Model {
    public static final String OWNER = "Piscicultor", KEEPER = "Encargado de la estación";
    public static class User {
        public String id, name, role, station, salt, hash;
        public boolean active = true;
    }
    public static class Pond {
        public String id, name, station, keeperId, species, system, purpose, state;
        public double size;
        @Override public String toString() { return name + " · " + station; }
    }
    public static class Lot {
        public String id, pondId, species, date, state = "Activo", reason = "";
        public int quantity, months = 6;
    }
    public static class Feeding {
        public String id, pondId, lotId, date, food, userId;
        public double kg;
    }
    public static class Notice {
        public String id, pondId, type, detail, date, userId, requestedState;
        public String status = "Pendiente";
    }
    public static class Audit {
        public String date, userId, action;
        public Audit(String userId, String action) { this.date=java.time.LocalDateTime.now().toString(); this.userId=userId; this.action=action; }
    }
    public static class Database {
        public int version = 1;
        public List<User> users = new ArrayList<>();
        public List<Pond> ponds = new ArrayList<>();
        public List<Lot> lots = new ArrayList<>();
        public List<Feeding> feedings = new ArrayList<>();
        public List<Notice> notices = new ArrayList<>();
        public List<Audit> audit = new ArrayList<>();
    }
}
