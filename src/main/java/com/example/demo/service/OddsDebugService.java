package com.example.demo.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.domain.Odds;
import com.example.demo.domain.UmarenOdds;

@Service
public class OddsDebugService {

	// --- 馬連1番人気軸の全ペアオッズを出力（相手上位2頭の組み合わせを先頭にセット） ---
	public void printUmarenWithRank(List<Odds> tanshoOdds, Map<Integer, List<UmarenOdds>> umarenOddsMap) {
		if (tanshoOdds == null || tanshoOdds.isEmpty() || umarenOddsMap == null || umarenOddsMap.isEmpty()) {
			System.out.println("馬連オッズがありません");
			return;
		}

		// 全UmarenOddsをフラット化＆取消除外
		List<UmarenOdds> allPairs = umarenOddsMap.values().stream()
				.flatMap(List::stream)
				.filter(u -> !u.isCancelled())
				.collect(Collectors.toList());

		if (allPairs.size() < 2) {
			System.out.println("有効な馬連オッズが2件未満です");
			return;
		}

		// 単勝1番人気馬を軸に特定
		Integer favoriteHorse = tanshoOdds.stream()
				.min((o1, o2) -> Double.compare(o1.getTansho(), o2.getTansho()))
				.map(Odds::getUma)
				.orElse(null);

		if (favoriteHorse == null) {
			System.out.println("単勝1番人気馬が特定できません");
			return;
		}

		// 軸馬1番人気の全ペアを抽出（軸左・右どちらでも対応）
		final Integer axisHorse = favoriteHorse;
		List<UmarenOdds> axisPairs = allPairs.stream()
				.filter(u -> axisHorse.equals(u.getBaseHorseNo()) || axisHorse.equals(u.getPairHorseNo()))
				.sorted((u1, u2) -> Double.compare(u1.getOdds(), u2.getOdds()))
				.collect(Collectors.toList());

		// --- ⑤ 相手上位2頭の組み合わせオッズを抽出して先頭にセット ---
		// 相手1位と相手2位を特定（軸馬とのオッズで昇順に取得）
		List<Integer> top2Opponents = axisPairs.stream()
				.map(u -> u.getBaseHorseNo().equals(axisHorse) ? u.getPairHorseNo() : u.getBaseHorseNo())
				.distinct()
				.limit(2)
				.collect(Collectors.toList());

		List<UmarenOdds> topPairs = allPairs.stream()
				.filter(u -> (top2Opponents.contains(u.getBaseHorseNo()) && top2Opponents.contains(u.getPairHorseNo())))
				.sorted((u1, u2) -> Double.compare(u1.getOdds(), u2.getOdds()))
				.collect(Collectors.toList());

		// 軸ペアリストの先頭にtopPairsを挿入（重複は除外）
		topPairs.forEach(tp -> {
			boolean exists = axisPairs.stream()
					.anyMatch(ap -> (ap.getBaseHorseNo().equals(tp.getBaseHorseNo())
							&& ap.getPairHorseNo().equals(tp.getPairHorseNo())) ||
							(ap.getBaseHorseNo().equals(tp.getPairHorseNo())
									&& ap.getPairHorseNo().equals(tp.getBaseHorseNo())));
			if (!exists) {
				axisPairs.add(0, tp);
			}
		});

		// --- 表示 ---
		System.out.println("===== 馬連 1番人気軸: " + axisHorse + " (人気順付き) =====");
		int rank = 1;
		for (UmarenOdds u : axisPairs) {
			int pairHorse = u.getBaseHorseNo().equals(axisHorse) ? u.getPairHorseNo() : u.getBaseHorseNo();
			System.out.printf("人気:%d 軸:%d 相手:%d オッズ:%.1f%n", rank++, axisHorse, pairHorse, u.getOdds());
		}
	}

	// --- 単勝・複勝オッズを人気順で出力 ---
	public void printTanshoFukusho(List<Odds> oddsList) {
		if (oddsList == null || oddsList.isEmpty()) {
			System.out.println("単勝/複勝オッズがありません");
			return;
		}

		// 単勝人気順
		System.out.println("===== 単勝 人気順 =====");
		oddsList.stream()
				.filter(o -> o.getTansho() > 0)
				.sorted((o1, o2) -> Double.compare(o1.getTansho(), o2.getTansho()))
				.forEach(o -> System.out.printf("単勝 人気:%d 馬番:%d オッズ:%.1f%n",
						o.getTanshoRank(), o.getUma(), o.getTansho()));

		// 複勝人気順
		List<Odds> fukushoSorted = oddsList.stream()
				.filter(o -> o.getFukuHigh() > 0)
				.sorted((o1, o2) -> Double.compare(o1.getFukuHigh(), o2.getFukuHigh()))
				.collect(Collectors.toList());

		// 複勝ランク付け
		for (int i = 0; i < fukushoSorted.size(); i++) {
			fukushoSorted.get(i).setFukushoRank(i + 1);
		}

		System.out.println("===== 複勝 人気順 =====");
		fukushoSorted.forEach(o -> System.out.printf("複勝 人気:%d 馬番:%d オッズ:%.1f%n",
				o.getFukushoRank(), o.getUma(), o.getFukuHigh()));
	}
}
