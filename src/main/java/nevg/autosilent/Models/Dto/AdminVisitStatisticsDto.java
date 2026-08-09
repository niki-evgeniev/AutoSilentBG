package nevg.autosilent.Models.Dto;

import java.time.LocalDate;

public record AdminVisitStatisticsDto(
        long totalVisits,
        long todayVisits,
        LocalDate date
) {
}
