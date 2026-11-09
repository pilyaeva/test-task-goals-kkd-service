package com.example.testtaskgoalskkd.domain.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Сценарии из specs/design.md, раздел «Алгоритмы» → «Тестирование». Без БД и Spring.
 */
class QualityRuleEngineTest {

    static final LocalDate SNAPSHOT = LocalDate.parse("2026-10-26");
    static final String OK_OWNER = "E-001";

    final QualityRuleEngine engine = new QualityRuleEngine();

    static KeyResultSnapshot kr(String weight, LocalDate updated) {
        return new KeyResultSnapshot(weight == null ? null : new BigDecimal(weight), updated);
    }

    static KeyResultSnapshot freshKr(String weight) {
        return kr(weight, SNAPSHOT.minusDays(1));
    }

    static GoalSnapshot goal(String id, String owner, String status, KeyResultSnapshot... krs) {
        return new GoalSnapshot(id, owner, status, List.of(krs));
    }

    static GoalSnapshot active(String id, String owner, KeyResultSnapshot... krs) {
        return goal(id, owner, "ACTIVE", krs);
    }

    List<DetectedProblem> check(GoalSnapshot... goals) {
        return engine.check(List.of(goals), SNAPSHOT);
    }

    // ---- успешные сценарии ----

    @Test
    void correctGoalHasNoProblems() {
        assertThat(check(active("G-1", OK_OWNER, freshKr("50"), freshKr("50")))).isEmpty();
    }

    @Test
    void eachRuleFiresOnItsOwnGoal() {
        List<DetectedProblem> result = check(
                active("G-1", null, freshKr("100")),
                active("G-2", OK_OWNER),
                active("G-3", OK_OWNER, freshKr("90")),
                active("G-4", OK_OWNER, kr("100", SNAPSHOT.minusDays(46))));

        assertThat(result).extracting(DetectedProblem::ruleCode, DetectedProblem::goalId).containsExactly(
                tuple("GOAL_NO_OWNER", "G-1"),
                tuple("GOAL_NO_KR", "G-2"),
                tuple("KR_WEIGHTS_SUM", "G-3"),
                tuple("KR_STALE_PROGRESS", "G-4"));
    }

    @Test
    void oneGoalCanViolateSeveralRules() {
        List<DetectedProblem> result = check(active("G-1", null, kr("90", SNAPSHOT.minusDays(46))));

        assertThat(result).extracting(DetectedProblem::ruleCode)
                .containsExactly("GOAL_NO_OWNER", "KR_WEIGHTS_SUM", "KR_STALE_PROGRESS");
        assertThat(result).extracting(DetectedProblem::goalId).containsOnly("G-1");
    }

    @Test
    void resultIsOrderedByRuleThenGoalId() {
        List<DetectedProblem> result = check(
                active("G-2", null, freshKr("90")),
                active("G-1", null, freshKr("90")));

        assertThat(result).extracting(DetectedProblem::ruleCode, DetectedProblem::goalId).containsExactly(
                tuple("GOAL_NO_OWNER", "G-1"),
                tuple("GOAL_NO_OWNER", "G-2"),
                tuple("KR_WEIGHTS_SUM", "G-1"),
                tuple("KR_WEIGHTS_SUM", "G-2"));
    }

    // ---- краевые сценарии ----

    @Test
    void exactly45DaysIsNotStale() {
        assertThat(check(active("G-1", OK_OWNER, kr("100", SNAPSHOT.minusDays(45))))).isEmpty();
    }

    @Test
    void fortySixDaysIsStale() {
        assertThat(check(active("G-1", OK_OWNER, kr("100", SNAPSHOT.minusDays(46)))))
                .containsExactly(new DetectedProblem("KR_STALE_PROGRESS", "G-1"));
    }

    @Test
    void missingUpdateDateIsStale() {
        assertThat(check(active("G-1", OK_OWNER, kr("100", null))))
                .containsExactly(new DetectedProblem("KR_STALE_PROGRESS", "G-1"));
    }

    @Test
    void updateDateAfterSnapshotIsNotStale() {
        assertThat(check(active("G-1", OK_OWNER, kr("100", SNAPSHOT.plusDays(6))))).isEmpty();
    }

    @Test
    void severalStaleKeyResultsGiveOneProblemPerGoal() {
        List<DetectedProblem> result = check(active("G-1", OK_OWNER,
                kr("50", SNAPSHOT.minusDays(100)), kr("50", SNAPSHOT.minusDays(90))));

        assertThat(result).containsExactly(new DetectedProblem("KR_STALE_PROGRESS", "G-1"));
    }

    @Test
    void fractionalWeightsSummingTo100AreValid() {
        assertThat(check(active("G-1", OK_OWNER, freshKr("33.33"), freshKr("33.33"), freshKr("33.34")))).isEmpty();
        assertThat(check(active("G-1", OK_OWNER, freshKr("100.00")))).isEmpty();
    }

    @Test
    void weightsSlightlyBelow100AreInvalid() {
        assertThat(check(active("G-1", OK_OWNER, freshKr("50"), freshKr("49.99"))))
                .containsExactly(new DetectedProblem("KR_WEIGHTS_SUM", "G-1"));
    }

    @Test
    void nullWeightCountsAsZero() {
        assertThat(check(active("G-1", OK_OWNER, freshKr("100"), freshKr(null)))).isEmpty();
        assertThat(check(active("G-1", OK_OWNER, freshKr(null), freshKr(null))))
                .containsExactly(new DetectedProblem("KR_WEIGHTS_SUM", "G-1"));
    }

    @Test
    void weightsOver100AreInvalid() {
        assertThat(check(active("G-1", OK_OWNER, freshKr("60"), freshKr("50"))))
                .containsExactly(new DetectedProblem("KR_WEIGHTS_SUM", "G-1"));
    }

    @Test
    void goalWithoutKeyResultsOnlyViolatesGoalNoKr() {
        assertThat(check(active("G-1", OK_OWNER)))
                .containsExactly(new DetectedProblem("GOAL_NO_KR", "G-1"));
    }

    @Test
    void blankOwnerIsMissingOwner() {
        assertThat(check(active("G-1", "", freshKr("100")), active("G-2", "  ", freshKr("100"))))
                .containsExactly(
                        new DetectedProblem("GOAL_NO_OWNER", "G-1"),
                        new DetectedProblem("GOAL_NO_OWNER", "G-2"));
    }

    @Test
    void inactiveGoalsAreSkipped() {
        KeyResultSnapshot bad = kr("10", SNAPSHOT.minusDays(200));

        assertThat(check(
                goal("G-1", null, "DRAFT", bad),
                goal("G-2", null, "DONE"),
                goal("G-3", null, "CANCELLED", bad))).isEmpty();
    }

    @Test
    void emptyInputGivesEmptyResult() {
        assertThat(engine.check(List.of(), SNAPSHOT)).isEmpty();
    }

    @Test
    void exposesRuleCodesInExecutionOrder() {
        assertThat(engine.ruleCodes())
                .containsExactly("GOAL_NO_OWNER", "GOAL_NO_KR", "KR_WEIGHTS_SUM", "KR_STALE_PROGRESS");
    }
}
