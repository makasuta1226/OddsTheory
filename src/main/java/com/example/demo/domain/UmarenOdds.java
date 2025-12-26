package com.example.demo.domain;

import java.util.List;
import java.util.stream.Collectors;

public class UmarenOdds {

	private Integer baseHorseNo; // 軸馬
	private Integer pairHorseNo; // 相手馬
	private Double odds; // 馬連オッズ
	private boolean cancelled; // 取消馬

	private Integer axisRank; // 軸馬のランキング（必要に応じて）
	private Integer pairRank; // 相手馬のランキング（必要に応じて）

	/* ===== 判定用 ===== */
	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public void checkCancelled() {
		cancelled = (odds == null || odds <= 0);
	}

	/* ===== Getter / Setter ===== */
	public Integer getBaseHorseNo() {
		return baseHorseNo;
	}

	public void setBaseHorseNo(Integer baseHorseNo) {
		this.baseHorseNo = baseHorseNo;
	}

	public Integer getPairHorseNo() {
		return pairHorseNo;
	}

	public void setPairHorseNo(Integer pairHorseNo) {
		this.pairHorseNo = pairHorseNo;
	}

	public Double getOdds() {
		return odds;
	}

	public void setOdds(Double odds) {
		this.odds = odds;
	}

	public Integer getAxisRank() {
		return axisRank;
	}

	public void setAxisRank(Integer axisRank) {
		this.axisRank = axisRank;
	}

	public Integer getPairRank() {
		return pairRank;
	}

	public void setPairRank(Integer pairRank) {
		this.pairRank = pairRank;
	}

	/* ===== 補助メソッド ===== */

	/**
	 * baseHorseNo と pairHorseNo を入れ替える
	 * 軸馬を強制的に base に揃えたい場合に使用
	 */
	public void swapBasePair() {
		Integer temp = this.baseHorseNo;
		this.baseHorseNo = this.pairHorseNo;
		this.pairHorseNo = temp;
	}

	/**
	 * 指定した軸馬に対応する全てのペア（取消馬を除外）を取得
	 * @param allOdds 全UmarenOddsリスト
	 * @param axisHorseNo 軸馬番号
	 * @return 指定軸馬の有効な全ペアをオッズ昇順でソートして返す
	 */
	public static List<UmarenOdds> getPairsByAxis(List<UmarenOdds> allOdds, Integer axisHorseNo) {
		return allOdds.stream()
				.filter(u -> axisHorseNo.equals(u.getBaseHorseNo()) && !u.isCancelled())
				.sorted((u1, u2) -> Double.compare(u1.getOdds(), u2.getOdds()))
				.collect(Collectors.toList());
	}
}
