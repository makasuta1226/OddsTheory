package com.example.demo.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.domain.Odds;
import com.example.demo.domain.UmarenOdds;

@Service
public class OddsDebugService {

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

		// 複勝出力
		System.out.println("===== 複勝 人気順 =====");
		fukushoSorted.forEach(o -> System.out.printf("複勝 人気:%d 馬番:%d オッズ:%.1f%n",
				o.getFukushoRank(), o.getUma(), o.getFukuHigh()));
	}

	// --- 馬連1番人気軸の全ペアオッズを出力 ---
	public void printUmaren(List<Odds> tanshoOdds, Map<Integer, List<UmarenOdds>> umarenOddsMap) {
		if (tanshoOdds == null || tanshoOdds.isEmpty() || umarenOddsMap == null || umarenOddsMap.isEmpty()) {
			System.out.println("馬連オッズがありません");
			return;
		}

		// 全UmarenOddsをフラットに
		List<UmarenOdds> allPairs = umarenOddsMap.values().stream()
				.flatMap(List::stream)
				.collect(Collectors.toList());

		// 単勝1番人気馬を軸にする
		Integer favoriteHorse = tanshoOdds.stream()
				.min((o1, o2) -> Double.compare(o1.getTansho(), o2.getTansho()))
				.map(Odds::getUma)
				.orElse(null);

		if (favoriteHorse != null) {
			// 軸馬1番人気の全ペア取得（取消は除外）
			List<UmarenOdds> favoritePairs = UmarenOdds.getPairsByAxis(allPairs, favoriteHorse);

			System.out.println("===== 馬連 1番人気軸: " + favoriteHorse + " =====");
			for (UmarenOdds u : favoritePairs) {
				System.out.printf("軸:%d 相手:%d オッズ:%.1f%n",
						u.getBaseHorseNo(),
						u.getPairHorseNo(),
						u.getOdds());
			}
		} else {
			System.out.println("単勝1番人気馬が特定できません");
		}
	}
}
