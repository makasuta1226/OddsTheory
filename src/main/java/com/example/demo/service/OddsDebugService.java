package com.example.demo.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.domain.Odds;
import com.example.demo.domain.UmarenOdds;

@Service
public class OddsDebugService {

	// =====================================================
	// 単勝・複勝オッズを人気順で出力
	// =====================================================
	public void printTanshoFukusho(List<Odds> oddsList) {
		if (oddsList == null || oddsList.isEmpty()) {
			System.out.println("単勝/複勝オッズがありません");
			return;
		}

		// --- 単勝 人気順 ---
		System.out.println("===== 単勝 人気順 =====");
		oddsList.stream()
				.filter(o -> o.getTansho() > 0)
				.sorted(Comparator.comparingDouble(Odds::getTansho))
				.forEach(o -> System.out.printf(
						"単勝 人気:%d 馬番:%d オッズ:%.1f%n",
						o.getTanshoRank(), o.getUma(), o.getTansho()));

		// --- 複勝 人気順 ---
		List<Odds> fukushoSorted = oddsList.stream()
				.filter(o -> o.getFukuHigh() > 0)
				.sorted(Comparator.comparingDouble(Odds::getFukuHigh))
				.collect(Collectors.toList());

		for (int i = 0; i < fukushoSorted.size(); i++) {
			fukushoSorted.get(i).setFukushoRank(i + 1);
		}

		System.out.println("===== 複勝 人気順 =====");
		fukushoSorted.forEach(o -> System.out.printf(
				"複勝 人気:%d 馬番:%d オッズ:%.1f%n",
				o.getFukushoRank(), o.getUma(), o.getFukuHigh()));
	}

	// =====================================================
	// 馬連 人気順（軸馬専用オッズ + 軸馬全ペア）
	// =====================================================
	public void printUmarenWithRank(Map<Integer, List<UmarenOdds>> umarenOddsMap) {

		if (umarenOddsMap == null || umarenOddsMap.isEmpty()) {
			System.out.println("馬連オッズがありません");
			return;
		}

		// --- 全馬連オッズ（取消除外） ---
		List<UmarenOdds> allPairs = umarenOddsMap.values().stream()
				.flatMap(List::stream)
				.filter(u -> !u.isCancelled())
				.collect(Collectors.toList());

		if (allPairs.size() < 2) {
			System.out.println("有効な馬連オッズが2件未満です");
			return;
		}

		// =================================================
		// ① 馬連1番人気（軸馬）決定
		// =================================================
		Integer axisHorse = decideUmarenAxisHorse(allPairs);
		if (axisHorse == null) {
			System.out.println("軸馬が特定できません");
			return;
		}

		// ★ ここで確定（以後変更なし）
		final Integer axis = axisHorse;

		// =================================================
		// ② 軸馬専用オッズ作成（断層起点）
		// =================================================
		UmarenOdds axisSpecial = createAxisSpecialOdds(allPairs, axis);

		// =================================================
		// ③ 軸馬全ペア（人気順）
		// =================================================
		List<UmarenOdds> axisPairs = allPairs.stream()
				.filter(u -> u.getBaseHorseNo().equals(axis)
						|| u.getPairHorseNo().equals(axis))
				.sorted(Comparator.comparingDouble(UmarenOdds::getOdds))
				.collect(Collectors.toList());

		// =================================================
		// ④ 最終リスト（軸馬専用 → 軸馬全ペア）
		// =================================================
		List<UmarenOdds> finalPairs = new ArrayList<>();
		if (axisSpecial != null) {
			finalPairs.add(axisSpecial); // ★ 断層計算の起点
		}
		finalPairs.addAll(axisPairs);

		// =================================================
		// 表示
		// =================================================
		System.out.println("===== 馬連 1番人気軸: " + axis + " =====");
		int rank = 1;
		for (UmarenOdds u : finalPairs) {
			System.out.printf(
					"人気:%d 馬番:%d オッズ:%.1f%n",
					rank++, u.getPairHorseNo(), u.getOdds());
		}
	}

	// =====================================================
	// 馬連1番人気（軸馬）決定
	// ・全馬連オッズ最小値＋次点の組み合わせ
	// ・共通する馬番号が軸馬
	// =====================================================
	private Integer decideUmarenAxisHorse(List<UmarenOdds> allPairs) {

		List<UmarenOdds> sorted = allPairs.stream()
				.sorted(Comparator.comparingDouble(UmarenOdds::getOdds))
				.collect(Collectors.toList());

		UmarenOdds top1 = sorted.get(0);
		UmarenOdds top2 = sorted.get(1);

		if (top1.getBaseHorseNo().equals(top2.getBaseHorseNo())
				|| top1.getBaseHorseNo().equals(top2.getPairHorseNo())) {
			return top1.getBaseHorseNo();
		}
		if (top1.getPairHorseNo().equals(top2.getBaseHorseNo())
				|| top1.getPairHorseNo().equals(top2.getPairHorseNo())) {
			return top1.getPairHorseNo();
		}

		return null;
	}

	// =====================================================
	// 軸馬専用オッズ（軸-軸）作成
	// ・馬連1番人気を構成する2頭の「相手同士」のオッズ
	// =====================================================
	private UmarenOdds createAxisSpecialOdds(
			List<UmarenOdds> allPairs,
			Integer axisHorse) {

		List<UmarenOdds> sorted = allPairs.stream()
				.sorted(Comparator.comparingDouble(UmarenOdds::getOdds))
				.collect(Collectors.toList());

		UmarenOdds top1 = sorted.get(0);
		UmarenOdds top2 = sorted.get(1);

		List<Integer> opponents = new ArrayList<>();
		for (UmarenOdds u : List.of(top1, top2)) {
			if (!u.getBaseHorseNo().equals(axisHorse)) {
				opponents.add(u.getBaseHorseNo());
			}
			if (!u.getPairHorseNo().equals(axisHorse)) {
				opponents.add(u.getPairHorseNo());
			}
		}

		if (opponents.size() < 2) {
			return null;
		}

		Integer h1 = opponents.get(0);
		Integer h2 = opponents.get(1);

		for (UmarenOdds u : allPairs) {
			if ((u.getBaseHorseNo().equals(h1) && u.getPairHorseNo().equals(h2))
					|| (u.getBaseHorseNo().equals(h2) && u.getPairHorseNo().equals(h1))) {

				UmarenOdds axisSpecial = new UmarenOdds();
				axisSpecial.setBaseHorseNo(axisHorse);
				axisSpecial.setPairHorseNo(axisHorse); // 軸-軸
				axisSpecial.setOdds(u.getOdds());
				axisSpecial.setCancelled(false);
				return axisSpecial;
			}
		}

		return null;
	}

	// =====================================================
	// Controller 呼び出し用（引数一致）
	// =====================================================
	public void printUmarenWithRank(
			List<Odds> tanshoOdds,
			Map<Integer, List<UmarenOdds>> umarenOddsMap) {

		// 今回は単勝は使わない（将来拡張用）
		// 中身は既存ロジックをそのまま流用
		printUmarenWithRank(umarenOddsMap);
	}

}
