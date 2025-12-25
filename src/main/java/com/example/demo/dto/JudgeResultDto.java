package com.example.demo.dto;

public class JudgeResultDto {

	private int uma;
	private int waku;
	private String name;
	private double point;
	private double percent;
	private String rankType; // "本命", "中穴", "穴"

	public JudgeResultDto(int uma, int waku, String name, double point) {
		this.uma = uma;
		this.waku = waku;
		this.name = name;
		this.point = point;
	}

	// --- Getter / Setter ---
	public int getUma() {
		return uma;
	}

	public void setUma(int uma) {
		this.uma = uma;
	}

	public int getWaku() {
		return waku;
	}

	public void setWaku(int waku) {
		this.waku = waku;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getPoint() {
		return point;
	}

	public void setPoint(double point) {
		this.point = point;
	}

	public double getPercent() {
		return percent;
	}

	public void setPercent(double percent) {
		this.percent = percent;
	}

	public String getRankType() {
		return rankType;
	}

	public void setRankType(String rankType) {
		this.rankType = rankType;
	}
}
