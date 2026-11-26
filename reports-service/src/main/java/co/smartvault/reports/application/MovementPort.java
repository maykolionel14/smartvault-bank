package co.smartvault.reports.application;

import co.smartvault.reports.domain.Movement;
import java.util.List;

public interface MovementPort {
    List<Movement> recentForUser(String actor);
}
