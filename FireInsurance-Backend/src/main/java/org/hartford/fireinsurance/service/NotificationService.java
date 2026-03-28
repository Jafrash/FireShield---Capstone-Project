package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.NotificationDTO;
import org.hartford.fireinsurance.model.*;
import org.hartford.fireinsurance.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final UnderwriterRepository underwriterRepository;
    private final SurveyorRepository surveyorRepository;
    private final ClaimRepository claimRepository;
    private final InspectionRepository inspectionRepository;
    private final ClaimInspectionRepository claimInspectionRepository;

    public NotificationService(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            UnderwriterRepository underwriterRepository,
            SurveyorRepository surveyorRepository,
            ClaimRepository claimRepository,
            InspectionRepository inspectionRepository,
            ClaimInspectionRepository claimInspectionRepository
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.underwriterRepository = underwriterRepository;
        this.surveyorRepository = surveyorRepository;
        this.claimRepository = claimRepository;
        this.inspectionRepository = inspectionRepository;
        this.claimInspectionRepository = claimInspectionRepository;
    }

    public List<NotificationDTO> getNotificationsForUser(String username, int limit) {
        if (limit <= 0) {
            limit = 20;
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String role = user.getRole() != null ? user.getRole().toUpperCase(Locale.ROOT) : "";

        List<NotificationDTO> notifications = switch (role) {
            case "CUSTOMER" -> buildCustomerNotifications(username);
            case "UNDERWRITER" -> buildUnderwriterNotifications(username);
            case "SURVEYOR" -> buildSurveyorNotifications(username);
            case "ADMIN" -> buildAdminNotifications();
            case "SIU_INVESTIGATOR" -> buildSiuNotifications(username);
            default -> new ArrayList<>();
        };

        return notifications.stream()
                .sorted(Comparator.comparing(NotificationDTO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<NotificationDTO> buildCustomerNotifications(String username) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Customer profile not found."));

        return claimRepository.findByCustomer(customer).stream()
                .map(claim -> new NotificationDTO(
                        "claim-" + claim.getClaimId() + "-" + safeStatus(claim.getStatus()),
                        "Claim update",
                        "Claim #" + claim.getClaimId() + " is currently " + prettyStatus(claim.getStatus()),
                        "CLAIM",
                        safeStatus(claim.getStatus()),
                        "/customer/claims",
                        claim.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private List<NotificationDTO> buildUnderwriterNotifications(String username) {
        Underwriter underwriter = underwriterRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Underwriter profile not found."));

        List<NotificationDTO> notifications = new ArrayList<>();

        notifications.addAll(claimRepository.findByUnderwriter(underwriter).stream()
                .map(claim -> new NotificationDTO(
                        "uw-claim-" + claim.getClaimId() + "-" + safeStatus(claim.getStatus()),
                        "Assigned claim",
                        "Claim #" + claim.getClaimId() + " is " + prettyStatus(claim.getStatus()),
                        "CLAIM",
                        safeStatus(claim.getStatus()),
                        "/underwriter/claims",
                        claim.getCreatedAt()
                ))
                .collect(Collectors.toList()));

        return notifications;
    }

    private List<NotificationDTO> buildSurveyorNotifications(String username) {
        Surveyor surveyor = surveyorRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Surveyor profile not found."));

        List<NotificationDTO> notifications = new ArrayList<>();

        notifications.addAll(inspectionRepository.findBySurveyor(surveyor).stream()
                .map(inspection -> new NotificationDTO(
                        "prop-inspection-" + inspection.getInspectionId() + "-" + safeStatus(inspection.getStatus()),
                        "Property inspection",
                        "Property inspection #" + inspection.getInspectionId() + " is " + prettyStatus(inspection.getStatus()),
                        "PROPERTY_INSPECTION",
                        safeStatus(inspection.getStatus()),
                        "/surveyor/property-inspections",
                        inspection.getInspectionDate()
                ))
                .collect(Collectors.toList()));

        notifications.addAll(claimInspectionRepository.findBySurveyor(surveyor).stream()
                .map(inspection -> new NotificationDTO(
                        "claim-inspection-" + inspection.getClaimInspectionId() + "-" + safeStatus(inspection.getStatus()),
                        "Claim inspection",
                        "Claim inspection #" + inspection.getClaimInspectionId() + " is " + prettyStatus(inspection.getStatus()),
                        "CLAIM_INSPECTION",
                        safeStatus(inspection.getStatus()),
                        "/surveyor/claim-inspections",
                        inspection.getInspectionDate()
                ))
                .collect(Collectors.toList()));

        return notifications;
    }

    private List<NotificationDTO> buildAdminNotifications() {
        List<NotificationDTO> notifications = new ArrayList<>();

        notifications.addAll(claimRepository.findAll().stream()
                .map(claim -> new NotificationDTO(
                        "admin-claim-" + claim.getClaimId() + "-" + safeStatus(claim.getStatus()),
                        "Claim workflow",
                        "Claim #" + claim.getClaimId() + " is " + prettyStatus(claim.getStatus()),
                        "CLAIM",
                        safeStatus(claim.getStatus()),
                        "/admin/claims",
                        claim.getCreatedAt()
                ))
                .collect(Collectors.toList()));

        notifications.addAll(inspectionRepository.findAll().stream()
                .map(inspection -> new NotificationDTO(
                        "admin-prop-inspection-" + inspection.getInspectionId() + "-" + safeStatus(inspection.getStatus()),
                        "Property inspection workflow",
                        "Property inspection #" + inspection.getInspectionId() + " is " + prettyStatus(inspection.getStatus()),
                        "PROPERTY_INSPECTION",
                        safeStatus(inspection.getStatus()),
                        "/admin/property-inspections",
                        inspection.getInspectionDate()
                ))
                .collect(Collectors.toList()));

        notifications.addAll(claimInspectionRepository.findAll().stream()
                .map(inspection -> new NotificationDTO(
                        "admin-claim-inspection-" + inspection.getClaimInspectionId() + "-" + safeStatus(inspection.getStatus()),
                        "Claim inspection workflow",
                        "Claim inspection #" + inspection.getClaimInspectionId() + " is " + prettyStatus(inspection.getStatus()),
                        "CLAIM_INSPECTION",
                        safeStatus(inspection.getStatus()),
                        "/admin/claim-inspections",
                        inspection.getInspectionDate()
                ))
                .collect(Collectors.toList()));

        notifications.addAll(userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt() != null)
                .map(user -> new NotificationDTO(
                        "admin-user-" + user.getId() + "-" + safeStatus(user.getRole()),
                        "New user registered",
                        user.getUsername() + " joined as " + (user.getRole() != null ? user.getRole().toUpperCase(Locale.ROOT) : "USER"),
                        "USER",
                        safeStatus(user.getRole()),
                        "/admin/customers",
                        user.getCreatedAt()
                ))
                .collect(Collectors.toList()));

        return notifications;
    }

    private List<NotificationDTO> buildSiuNotifications(String username) {
        List<NotificationDTO> notifications = new ArrayList<>();

        // SIU investigators care about high-risk claims
        notifications.addAll(claimRepository.findAll().stream()
                .filter(claim -> RiskLevel.HIGH.equals(claim.getRiskLevel()) || (claim.getFraudScore() != null && claim.getFraudScore() > 70))
                .map(claim -> new NotificationDTO(
                        "siu-risk-" + claim.getClaimId(),
                        "High Risk Claim Detected",
                        "Claim #" + claim.getClaimId() + " requires immediate SIU review (Score: " + (claim.getFraudScore() != null ? claim.getFraudScore() : "Pending") + ")",
                        "CLAIM",
                        "URGENT",
                        "/siu/dashboard",
                        claim.getCreatedAt()
                ))
                .collect(Collectors.toList()));

        return notifications;
    }

    private String safeStatus(Enum<?> status) {
        return status != null ? status.name() : "UNKNOWN";
    }

    private String safeStatus(String status) {
        return status != null && !status.isBlank() ? status : "UNKNOWN";
    }

    private String prettyStatus(Enum<?> status) {
        if (status == null) {
            return "updated";
        }
        return status.name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}