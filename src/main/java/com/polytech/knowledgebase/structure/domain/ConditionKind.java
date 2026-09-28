package com.polytech.knowledgebase.structure.domain;
public enum ConditionKind {
    BEGINNER("experience"), EXPERIENCED("experience"), CALM_WATER("water"), WAVES("water"),
    LIGHT_WIND("wind"), STRONG_WIND("wind");
    private final String dimension;
    ConditionKind(String dimension) { this.dimension = dimension; }
    public String getDimension() { return dimension; }
}
