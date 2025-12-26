package com.example.demo.service;

import java.util.ArrayList;
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
		final Integer axisHorse = tanshoOdds.stream()
				.min((o1, o2) -> Double.compare(o1.getTansho(), o2.getTansho()))
				.map(Odds::getUma)
				.orElse(null);

		if (axisHorse == null) {
			System.out.println("単勝1番人気馬が特定できません");
			return;
		}

		// 軸馬全組み合わせ（オッズ順）
		List<UmarenOdds> axisPairs = allPairs.stream()
				.filter(u -> axisHorse.equals(u.getBaseHorseNo()) || axisHorse.equals(u.getPairHorseNo()))
				.sorted((u1, u2) -> Double.compare(u1.getOdds(), u2.getOdds()))
				.collect(Collectors.toList());

		if (axisPairs.isEmpty()) {
			System.out.println("軸馬の組み合わせオッズがありません");
			return;
		}

		// --- 上位2頭の馬番号を取得（オッズ順で1位・2位）
		List<Integer> top2Opponents = axisPairs.stream()
				.map(u -> u.getBaseHorseNo().equals(axisHorse) ? u.getPairHorseNo() : u.getBaseHorseNo())
				.distinct()
				.limit(2)
				.collect(Collectors.toList());

		// --- 全体オッズから上位2頭の組み合わせを取得（軸馬基準）
		List<UmarenOdds> topPairs = new ArrayList<>();
		for (int oppNo : top2Opponents) {
			allPairs.stream()
					.filter(u -> (u.getBaseHorseNo().equals(axisHorse) && u.getPairHorseNo() == oppNo)
							|| (u.getPairHorseNo().equals(axisHorse) && u.getBaseHorseNo() == oppNo))
					.findFirst()
					.ifPresent(topPairs::add);
		}

		// --- 軸ペアリストの先頭にtopPairsを追加（重複除外）
		List<UmarenOdds> finalPairs = new ArrayList<>(topPairs);
		for (UmarenOdds u : axisPairs) {
			boolean exists = finalPairs.stream()
					.anyMatch(f -> (f.getBaseHorseNo().equals(u.getBaseHorseNo())
							&& f.getPairHorseNo().equals(u.getPairHorseNo()))
							|| (f.getBaseHorseNo().equals(u.getPairHorseNo())
									&& f.getPairHorseNo().equals(u.getBaseHorseNo())));
			if (!exists)
				finalPairs.add(u);
		}

		// --- 表示 ---
		System.out.println("===== 馬連 1番人気軸: " + axisHorse + " (人気順付き) =====");
		int rank = 1;
		for (UmarenOdds u : finalPairs) {
			int pairHorse = u.getBaseHorseNo().equals(axisHorse) ? u.getPairHorseNo() : u.getBaseHorseNo();
			// 人気1位の相手だけ空文字
			String pairStr = (rank == 1) ? "" : String.valueOf(pairHorse);
			System.out.printf("人気:%d 軸:%d 相手:%s オッズ:%.1f%n", rank++, axisHorse, pairStr, u.getOdds());
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
