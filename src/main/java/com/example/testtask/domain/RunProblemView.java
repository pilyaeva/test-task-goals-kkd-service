package com.example.testtask.domain;

/**
 * Проблема прогона вместе с критичностью сработавшего правила.
 */
public record RunProblemView(String ruleId, String goalId, String severity) {
}
