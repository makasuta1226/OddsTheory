package com.example.demo.domain;

public class Odds {

	/* ===== 単勝・複勝 ===== */
	private int waku;
	private int uma;
	private String name;
	private String jockey;
	private double tansho;
	private double fukuLow;
	private double fukuHigh;

	/* ===== ランキング ===== */
	private Integer tanshoRank;
	private Integer fukushoRank;
	private Integer umarenBestRank;

	/* ===== 判定用 ===== */
	public boolean isTansho() {
		return tansho > 0;
	}

	public boolean isFukusho() {
		return fukuLow > 0 || fukuHigh > 0;
	}

	/* ===== Getter / Setter ===== */
	public int getWaku() {
		return waku;
	}

	public void setWaku(int waku) {
		this.waku = waku;
	}

	public int getUma() {
		return uma;
	}

	public void setUma(int uma) {
		this.uma = uma;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getJockey() {
		return jockey;
	}

	public void setJockey(String jockey) {
		this.jockey = jockey;
	}

	public double getTansho() {
		return tansho;
	}

	public void setTansho(double tansho) {
		this.tansho = tansho;
	}

	public double getFukuLow() {
		return fukuLow;
	}

	public void setFukuLow(double fukuLow) {
		this.fukuLow = fukuLow;
	}

	public double getFukuHigh() {
		return fukuHigh;
	}

	public void setFukuHigh(double fukuHigh) {
		this.fukuHigh = fukuHigh;
	}

	public Integer getTanshoRank() {
		return tanshoRank;
	}

	public void setTanshoRank(Integer tanshoRank) {
		this.tanshoRank = tanshoRank;
	}

	public Integer getFukushoRank() {
		return fukushoRank;
	}

	public void setFukushoRank(Integer fukushoRank) {
		this.fukushoRank = fukushoRank;
	}

	public Integer getUmarenBestRank() {
		return umarenBestRank;
	}

	public void setUmarenBestRank(Integer umarenBestRank) {
		this.umarenBestRank = umarenBestRank;
	}

	/* ===== コンストラクタ ===== */
	public Odds(int waku, String name, int uma, double tansho, double fukuHigh) {
		this.waku = waku;
		this.name = name;
		this.uma = uma;
		this.tansho = tansho;
		this.fukuHigh = fukuHigh;
	}

	public Odds() {
	}
}
