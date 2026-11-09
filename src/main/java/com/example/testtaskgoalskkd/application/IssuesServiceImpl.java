package com.example.testtaskgoalskkd.application;

import com.example.testtaskgoalskkd.domain.IssueView;
import com.example.testtaskgoalskkd.infrastructure.repository.DepartmentRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.OffsetPageRequest;
import com.example.testtaskgoalskkd.infrastructure.repository.RuleRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.RunProblemRepository;
import com.example.testtaskgoalskkd.infrastructure.repository.RunRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssuesServiceImpl implements IssuesService {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;
    private static final Set<String> SEVERITIES = Set.of("critical", "major", "minor");
    /** Заглушка для параметра IN, когда фильтр по подразделению не применяется. */
    private static final List<String> NO_UNITS = List.of("");

    private final RunRepository runRepository;
    private final DepartmentRepository departmentRepository;
    private final RuleRepository ruleRepository;
    private final RunProblemRepository runProblemRepository;

    public IssuesServiceImpl(
            RunRepository runRepository,
            DepartmentRepository departmentRepository,
            RuleRepository ruleRepository,
            RunProblemRepository runProblemRepository) {
        this.runRepository = runRepository;
        this.departmentRepository = departmentRepository;
        this.ruleRepository = ruleRepository;
        this.runProblemRepository = runProblemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public IssuesPage getIssues(
            String runId, String unitId, String ruleCode, String severity, Integer limit, Long offset) {
        if (runId == null || runId.isBlank()) {
            throw new ValidationException("Не указан обязательный параметр run_id");
        }
        int pageSize = limit == null ? DEFAULT_LIMIT : limit;
        if (pageSize < 1 || pageSize > MAX_LIMIT) {
            throw new ValidationException("Параметр limit должен быть от 1 до " + MAX_LIMIT);
        }
        long pageOffset = offset == null ? 0 : offset;
        if (pageOffset < 0) {
            throw new ValidationException("Параметр offset не может быть отрицательным");
        }
        if (severity != null && !SEVERITIES.contains(severity)) {
            throw new ValidationException("Параметр severity должен быть одним из: critical, major, minor");
        }

        if (!runRepository.existsById(runId)) {
            throw new NotFoundException("Прогон с ID " + runId + " не найден");
        }
        List<String> departmentIds = NO_UNITS;
        if (unitId != null) {
            if (!departmentRepository.existsById(unitId)) {
                throw new NotFoundException("Подразделение с ID " + unitId + " не найдено");
            }
            departmentIds = departmentRepository.findSubtreeIds(unitId);
        }
        if (ruleCode != null && !ruleRepository.existsByCode(ruleCode)) {
            throw new NotFoundException("Правило с кодом " + ruleCode + " не найдено");
        }

        Page<IssueView> page = runProblemRepository.findIssues(
                runId, unitId == null, departmentIds, ruleCode, severity,
                new OffsetPageRequest(pageOffset, pageSize));
        List<IssuesPage.Issue> items = page.getContent().stream()
                .map(v -> new IssuesPage.Issue(
                        v.id(), v.ruleCode(), v.severity(), v.goalId(),
                        v.unitId() == null ? null : new IssuesPage.UnitRef(v.unitId(), v.unitName())))
                .toList();
        return new IssuesPage(items, page.getTotalElements(), pageSize, pageOffset);
    }
}
