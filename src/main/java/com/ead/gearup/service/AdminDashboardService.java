package com.ead.gearup.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.ead.gearup.dto.admin.AdminDashboardResponseDTO;
import com.ead.gearup.dto.admin.AdminStatsDTO;
import com.ead.gearup.dto.admin.MonthlyMetricDTO;
import com.ead.gearup.dto.admin.ProjectStatusDTO;
import com.ead.gearup.enums.AppointmentStatus;
import com.ead.gearup.enums.ProjectStatus;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.repository.AppointmentRepository;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.ProjectRepository;
import com.ead.gearup.repository.TaskRepository;
import com.ead.gearup.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final int MONTH_RANGE = 6;

    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public AdminDashboardResponseDTO getDashboard() {
        AdminStatsDTO stats = buildStats();
        List<ProjectStatusDTO> projectStatus = buildProjectStatus();
        List<MonthlyMetricDTO> customerRegistrations = buildMonthlyUserMetrics(UserRole.CUSTOMER);
        List<MonthlyMetricDTO> projectCompletions = buildMonthlyProjectMetrics(ProjectStatus.COMPLETED);

        return AdminDashboardResponseDTO.builder()
                .stats(stats)
                .projectStatus(projectStatus)
                .customerRegistrations(customerRegistrations)
                .projectCompletions(projectCompletions)
                .build();
    }

    private AdminStatsDTO buildStats() {
        long totalEmployees = employeeRepository.count();
        long activeProjects = projectRepository.countByStatus(ProjectStatus.IN_PROGRESS);
        long upcomingAppointments = appointmentRepository.countUpcomingAppointments(LocalDate.now(), List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS));
        long totalCustomers = customerRepository.count();
        long totalServices = taskRepository.count();
        return AdminStatsDTO.builder()
                .totalEmployees(totalEmployees)
                .activeProjects(activeProjects)
                .upcomingAppointments(upcomingAppointments)
                .totalCustomers(totalCustomers)
                .totalServices(totalServices)
                .build();
    }

    private List<ProjectStatusDTO> buildProjectStatus() {
        List<Object[]> raw = projectRepository.countProjectsByStatus();
        long total = raw.stream().mapToLong(row -> ((Number) row[1]).longValue()).sum();

        return raw.stream()
                .map(row -> {
                    String status = row[0].toString();
                    long count = ((Number) row[1]).longValue();
                    double percentage = total == 0 ? 0 : (count * 100.0) / total;
                    return ProjectStatusDTO.builder()
                            .status(status)
                            .count(count)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<MonthlyMetricDTO> buildMonthlyUserMetrics(UserRole role) {
        Map<YearMonth, Long> counts = toMonthCountMap(userRepository.countUsersByRoleGroupedByMonth(role.name(), monthWindowStart()));
        return buildMonthlyMetrics(counts);
    }

    private List<MonthlyMetricDTO> buildMonthlyProjectMetrics(ProjectStatus status) {
        Map<YearMonth, Long> counts = toMonthCountMap(projectRepository.countProjectsByMonthAndStatus(status.name(), monthWindowStart()));
        return buildMonthlyMetrics(counts);
    }

    private Map<YearMonth, Long> toMonthCountMap(List<Object[]> rows) {
        Map<YearMonth, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Timestamp timestamp = (Timestamp) row[0];
            YearMonth yearMonth = YearMonth.from(timestamp.toLocalDateTime());
            long count = ((Number) row[1]).longValue();
            result.put(yearMonth, count);
        }
        return result;
    }

    private List<MonthlyMetricDTO> buildMonthlyMetrics(Map<YearMonth, Long> counts) {
        List<YearMonth> window = IntStream.range(0, MONTH_RANGE)
                .mapToObj(i -> YearMonth.now().minusMonths(MONTH_RANGE - 1 - i))
                .collect(Collectors.toList());

        return window.stream()
                .map(month -> MonthlyMetricDTO.builder()
                        .month(month.format(MONTH_LABEL_FORMATTER))
                        .count(counts.getOrDefault(month, 0L))
                        .build())
                .collect(Collectors.toList());
    }

    private LocalDateTime monthWindowStart() {
        YearMonth earliest = YearMonth.now().minusMonths(MONTH_RANGE - 1);
        return earliest.atDay(1).atStartOfDay();
    }
}

