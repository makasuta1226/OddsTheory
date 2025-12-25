package com.example.demo.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.domain.UmarenOdds;

@Service
public class UmarenJudgeService {

	// ===== 重み =====
	private static final double LAYER_WEIGHT = 1.5;
	private static final double BASE_WEIGHT = 1.5;
	private static final double AXIS_CONFIDENCE_BONUS = 3.0; // 軸信頼度断層
	private static final double REVERSAL_BONUS = 2.0;

	private static final int MAX_LAYER_COUNT = 3;

	public Map<Integer, Double> judge(Map<Integer, List<UmarenOdds>> umarenOddsMap) {

		Map<Integer, Double> pointMap = new HashMap<>();
		if (umarenOddsMap == null || umarenOddsMap.isEmpty()) {
			return pointMap;
		}

		// ===== 全馬連オッズを1つのリストにまとめる =====
		List<UmarenOdds> allPairs = new ArrayList<>();
		for (List<UmarenOdds> list : umarenOddsMap.values()) {
			list.stream().filter(u -> !u.isCancelled()).forEach(allPairs::add);
		}

		// ===== 馬連人気順（オッズが低い順）でソート =====
		allPairs.sort(Comparator.comparingDouble(UmarenOdds::getOdds));

		// ここで人気順に順位をセット（1番人気が1位など）
		for (int rank = 0; rank < allPairs.size(); rank++) {
			UmarenOdds u = allPairs.get(rank);
			u.setPairRank(rank + 1); // 1番人気がrank=1
		}

		// ===== 軸ごとに判定 =====
		for (Map.Entry<Integer, List<UmarenOdds>> entry : umarenOddsMap.entrySet()) {
			int axisUma = entry.getKey();
			List<UmarenOdds> axisPairs = entry.getValue();

			List<UmarenOdds> sorted = axisPairs.stream()
					.filter(u -> !u.isCancelled())
					.sorted(Comparator.comparingDouble(UmarenOdds::getOdds))
					.toList();

			if (sorted.size() < 2)
				continue;

			// ===== 軸信頼度ボーナス =====
			UmarenOdds ab = sorted.get(0);
			UmarenOdds ac = sorted.get(1);

			Double bcOdds = findBCOdds(umarenOddsMap, ab.getPairHorseNo(), ac.getPairHorseNo());
			if (bcOdds != null && ac.getOdds() > 0) {
				double axisRatio = bcOdds / ac.getOdds();
				if (axisRatio >= 2.0) {
					pointMap.merge(axisUma, AXIS_CONFIDENCE_BONUS, Double::sum);
				}
			}

			// ===== 断層ポイント =====
			List<Integer> layerIndexes = getStrongLayerIndexes(sorted);
			for (int layerNo = 0; layerNo < layerIndexes.size(); layerNo++) {
				if (layerNo >= MAX_LAYER_COUNT)
					break;

				int cliffIndex = layerIndexes.get(layerNo);
				for (int i = 0; i <= cliffIndex; i++) {
					int diff = cliffIndex - i;
					double p = getLayerPoint(diff) * LAYER_WEIGHT;

					UmarenOdds u = sorted.get(i);
					if (isReversalHorse(u)) {
						p *= REVERSAL_BONUS;
					}

					pointMap.merge(u.getPairHorseNo(), p, Double::sum);
				}
			}

			// ===== ベースポイント =====
			for (UmarenOdds u : sorted) {
				double base = getBasePoint(u.getOdds()) * BASE_WEIGHT;
				pointMap.merge(u.getPairHorseNo(), base, Double::sum);
			}
		}

		return pointMap;
	}

	// ===== B–C オッズ取得（相手1 × 相手2） =====
	private Double findBCOdds(Map<Integer, List<UmarenOdds>> map, int b, int c) {
		List<UmarenOdds> list = map.get(b);
		if (list == null)
			return null;

		return list.stream()
				.filter(u -> !u.isCancelled())
				.filter(u -> u.getPairHorseNo() == c)
				.map(UmarenOdds::getOdds)
				.findFirst()
				.orElse(null);
	}

	// ===== 通常断層（ratio + 合成） =====
	private List<Integer> getStrongLayerIndexes(List<UmarenOdds> sorted) {
		List<Integer> layers = new ArrayList<>();
		double composite = 1.0;

		for (int i = 1; i < sorted.size(); i++) {
			double prev = sorted.get(i - 1).getOdds();
			double curr = sorted.get(i).getOdds();
			if (prev <= 0 || curr <= 0) {
				composite = 1.0;
				continue;
			}

			double ratio = curr / prev;
			composite *= ratio;

			if (ratio >= 2.0 || composite >= 2.0) {
				layers.add(i - 1);
				composite = 1.0;
			}
		}
		return layers;
	}

	// ===== 断層内順位ポイント =====
	private double getLayerPoint(int diff) {
		return switch (diff) {
		case 0 -> 2.5;
		case 1 -> 2.0;
		case 2 -> 1.5;
		case 3 -> 1.0;
		case 4 -> 0.5;
		default -> 0.0;
		};
	}

	// ===== 逆転現象（相手人気が軸より上） =====
	private boolean isReversalHorse(UmarenOdds u) {
		return u.getPairRank() != null
				&& u.getAxisRank() != null
				&& u.getPairRank() < u.getAxisRank();
	}

	// ===== 馬連ベースポイント =====
	private double getBasePoint(double odds) {
		if (odds <= 3.0)
			return 0.8;
		if (odds <= 6.0)
			return 0.7;
		if (odds <= 9.9)
			return 0.6;
		if (odds <= 12.9)
			return 0.5;
		if (odds <= 29.9)
			return 0.45;
		if (odds <= 39.9)
			return 0.4;
		if (odds <= 49.9)
			return 0.35;
		if (odds <= 59.9)
			return 0.3;
		if (odds <= 69.9)
			return 0.25;
		if (odds <= 79.9)
			return 0.2;
		if (odds <= 89.9)
			return 0.15;
		if (odds <= 99.9)
			return 0.1;
		return 0.05;
	}
}
