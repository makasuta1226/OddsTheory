package com.example.demo.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.domain.Odds;

@Service
public class TanshoJudgeService {

	private static final double LAYER_WEIGHT = 2.0;
	private static final double BASE_WEIGHT = 2.0;
	private static final double TOP_FAVORITE_PENALTY = 2.0;

	/**
	 * 単勝判定
	 * ・断層：断層ポイント制（弱・中・強を合成）
	 * ・評価：断層ゾーン + 基本オッズ
	 */
	public Map<Integer, Double> judge(List<Odds> oddsList) {

		Map<Integer, Double> pointMap = new HashMap<>();

		List<Odds> sorted = oddsList.stream()
				.filter(o -> o.getTansho() > 0)
				.sorted(Comparator.comparingDouble(Odds::getTansho))
				.toList();

		if (sorted.size() < 5) {
			return pointMap;
		}

		// ===== 断層検出（第1・第2・第3…）=====
		List<Integer> layers = getLayerIndexes(sorted);

		int layer2 = layers.size() > 1 ? layers.get(1) : sorted.size() - 3;
		int layer3 = layers.size() > 2 ? layers.get(2) : sorted.size() - 1;

		// ===== 断層ゾーンポイント =====
		for (int i = 0; i < sorted.size(); i++) {

			Odds o = sorted.get(i);
			double p;

			if (i <= layer2) {
				int diff = layer2 - i;
				p = getLayerPoint(diff) * LAYER_WEIGHT;
			} else if (i <= layer3) {
				p = 5.0 * LAYER_WEIGHT;
			} else {
				p = 0.0;
			}

			pointMap.merge(o.getUma(), p, Double::sum);
		}

		// ===== 基本オッズポイント =====
		for (Odds o : sorted) {

			double base = getBasePoint(o.getTansho()) * BASE_WEIGHT;

			// 1番人気補正
			if (o == sorted.get(0)) {
				base *= TOP_FAVORITE_PENALTY;
			}

			pointMap.merge(o.getUma(), base, Double::sum);
		}

		return pointMap;
	}

	/**
	 * 断層インデックス取得（断層ポイント制・完全版）
	 *
	 * 1.4倍以上：+1（弱）
	 * 1.7倍以上：+2（中）
	 * 2.0倍以上：+3（強）
	 *
	 * 累積ポイントが3以上になった地点を
	 * 「強断層」として検出し、断層直前馬（i-1）を記録
	 */
	private List<Integer> getLayerIndexes(List<Odds> sorted) {

		List<Integer> layers = new ArrayList<>();
		int cliffPoint = 0;

		for (int i = 1; i < sorted.size(); i++) {

			double prev = sorted.get(i - 1).getTansho();
			double curr = sorted.get(i).getTansho();

			if (prev <= 0 || curr <= 0) {
				cliffPoint = 0;
				continue;
			}

			double ratio = curr / prev;

			// ---- 断層ポイント加算 ----
			if (ratio >= 2.0) {
				cliffPoint += 3; // 強
			} else if (ratio >= 1.7) {
				cliffPoint += 2; // 中
			} else if (ratio >= 1.4) {
				cliffPoint += 1; // 弱
			}

			// ---- 強断層成立 ----
			if (cliffPoint >= 3) {
				layers.add(i - 1); // 断層直前馬
				cliffPoint = 0;
			}
		}

		return layers;
	}

	/**
	 * 断層内順位ポイント
	 */
	private double getLayerPoint(int diff) {
		switch (diff) {
		case 0:
			return 2.5;
		case 1:
			return 2.0;
		case 2:
			return 1.5;
		case 3:
			return 1.0;
		case 4:
			return 0.5;
		default:
			return 0.0;
		}
	}

	/**
	 * 単勝オッズ基礎ポイント
	 */
	private double getBasePoint(double odds) {
		if (odds <= 1.4)
			return 0.87;
		if (odds <= 1.9)
			return 0.80;
		if (odds <= 2.4)
			return 0.65;
		if (odds <= 3.9)
			return 0.57;
		if (odds <= 4.9)
			return 0.47;
		if (odds <= 5.9)
			return 0.43;
		if (odds <= 6.9)
			return 0.34;
		if (odds <= 7.9)
			return 0.33;
		if (odds <= 8.9)
			return 0.29;
		if (odds <= 9.9)
			return 0.28;
		if (odds <= 10.9)
			return 0.26;
		if (odds <= 13.9)
			return 0.23;
		if (odds <= 16.9)
			return 0.21;
		if (odds <= 19.9)
			return 0.18;
		if (odds <= 24.9)
			return 0.15;
		if (odds <= 29.9)
			return 0.135;
		if (odds <= 39.9)
			return 0.11;
		if (odds <= 49.9)
			return 0.08;
		if (odds <= 69.9)
			return 0.043;
		if (odds <= 99.9)
			return 0.02;
		return 0.01;
	}
}
