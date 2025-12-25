package com.example.demo.enums;

public enum RaceJudgeType {

	HONMEI("本命"), CHUANA("中穴"), ANA("穴"), MIOKURI("見送り");

	private final String label;

	RaceJudgeType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
