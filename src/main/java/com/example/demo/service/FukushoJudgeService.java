package com.example.demo.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.domain.Odds;

@Service
public class FukushoJudgeService {

	// ===== 重み =====
	private static final double LAYER_WEIGHT = 2.0;
	private static final double BASE_WEIGHT = 2.0;
	private static final double REVERSAL_BONUS = 4.0;

	// ===== 最大で見る断層数 =====
	private static final int MAX_LAYER_COUNT = 3;

	public Map<Integer, Double> judge(List<Odds> oddsList) {
		Map<Integer, Double> pointMap = new HashMap<>();

		if (oddsList == null || oddsList.isEmpty()) {
			return pointMap;
		}

		// ===== 複勝人気順に並べる =====
		List<Odds> sorted = oddsList.stream()
				.filter(o -> o.getFukuHigh() > 0)
				.sorted(Comparator.comparingDouble(Odds::getFukuHigh))
				.toList();

		// 人気順をセット（画面表示用）
		for (int i = 0; i < sorted.size(); i++) {
			sorted.get(i).setFukushoRank(i + 1);
		}

		if (sorted.size() < 5) {
			return pointMap;
		}

		// ===== 断層インデックス取得（強断層のみ） =====
		List<Integer> layerIndexes = getStrongLayerIndexes(sorted);

		// ===== 断層ポイント（最大3断層） =====
		for (int layerNo = 0; layerNo < layerIndexes.size(); layerNo++) {
			if (layerNo >= MAX_LAYER_COUNT)
				break;

			int cliffIndex = layerIndexes.get(layerNo);

			for (int i = 0; i <= cliffIndex; i++) {
				int diff = cliffIndex - i;
				double p = getLayerPoint(diff) * LAYER_WEIGHT;

				Odds o = sorted.get(i);

				// 逆転現象（複勝＞単勝人気）
				if (isReversalHorse(o)) {
					p *= REVERSAL_BONUS;
				}

				pointMap.merge(o.getUma(), p, Double::sum);
			}
		}

		// ===== ベースポイント =====
		for (Odds o : sorted) {
			double base = getBasePoint(o.getFukuHigh()) * BASE_WEIGHT;
			pointMap.merge(o.getUma(), base, Double::sum);
		}

		return pointMap;
	}

	/**
	 * 強断層インデックス抽出
	 * ・ratio >= 2.0
	 * ・合成比 >= 2.0
	 */
	private List<Integer> getStrongLayerIndexes(List<Odds> sorted) {
		List<Integer> layers = new ArrayList<>();
		double composite = 1.0;

		for (int i = 1; i < sorted.size(); i++) {
			double prev = sorted.get(i - 1).getFukuHigh();
			double curr = sorted.get(i).getFukuHigh();

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

	/**
	 * 断層内順位ポイント
	 */
	private double getLayerPoint(int diff) {
		return switch (diff) {
		case 0 -> 3.0;
		case 1 -> 2.5;
		case 2 -> 2.0;
		case 3 -> 1.5;
		case 4 -> 1.0;
		default -> 0.0;
		};
	}

	/**
	 * 単勝より複勝が上位 → 逆転現象
	 */
	private boolean isReversalHorse(Odds o) {
		return o.getFukushoRank() != null
				&& o.getTanshoRank() != null
				&& o.getFukushoRank() < o.getTanshoRank();
	}

	/**
	 * 複勝ベースポイント
	 */
	private double getBasePoint(double fukuHigh) {
		if (fukuHigh <= 1.4)
			return 0.897;
		if (fukuHigh <= 1.9)
			return 0.785;
		if (fukuHigh <= 2.9)
			return 0.65;
		if (fukuHigh <= 3.9)
			return 0.56;
		if (fukuHigh <= 4.9)
			return 0.503;
		if (fukuHigh <= 6.9)
			return 0.424;
		if (fukuHigh <= 9.9)
			return 0.328;
		if (fukuHigh <= 14.9)
			return 0.264;
		if (fukuHigh <= 19.9)
			return 0.207;
		if (fukuHigh <= 29.9)
			return 0.149;
		if (fukuHigh <= 49.9)
			return 0.118;
		if (fukuHigh <= 99.9)
			return 0.06;
		return 0.019;
	}
}
