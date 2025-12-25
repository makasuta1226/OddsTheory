package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.domain.Odds;
import com.example.demo.dto.JudgeResultDto;

@Service
public class TotalJudgeService {

	// 合算ポイントの最大値（100%換算用）
	private static final double MAX_TOTAL_POINT = 100.0;

	/**
	 * 単勝・複勝・馬連を合算して全頭の％リストを生成
	 */
	public List<JudgeResultDto> judgeRanking(
			Map<Integer, Double> tanshoPointMap,
			Map<Integer, Double> fukushoPointMap,
			Map<Integer, Double> umarenPointMap,
			List<Odds> oddsList) {

		// ① 馬番ごとの合算ポイント
		Map<Integer, Double> totalPointMap = new HashMap<>();
		tanshoPointMap.forEach((uma, point) -> totalPointMap.merge(uma, point, Double::sum));
		fukushoPointMap.forEach((uma, point) -> totalPointMap.merge(uma, point, Double::sum));
		umarenPointMap.forEach((uma, point) -> totalPointMap.merge(uma, point, Double::sum));

		// ② DTO生成
		List<JudgeResultDto> resultList = new ArrayList<>();
		for (Odds o : oddsList) {
			// 取消馬は除外（オッズが存在しない場合）
			if (o.getTansho() == 0 && o.getFukuHigh() == 0) {
				continue;
			}

			double point = totalPointMap.getOrDefault(o.getUma(), 0.0);
			resultList.add(new JudgeResultDto(
					o.getUma(),
					o.getWaku(),
					o.getName(),
					point));
		}

		// ③ 降順ソート（％化は降順後でもOK）
		resultList.sort((a, b) -> Double.compare(b.getPoint(), a.getPoint()));

		// ④ MAX_TOTAL_POINT 基準で％化
		double scale = 0.975; // 調整倍率
		resultList.forEach(r -> {
			double percent = (r.getPoint() / MAX_TOTAL_POINT) * 100.0 * scale;
			if (percent > 90.0)
				percent = 90.0; // 上限ガード
			if (percent < 0)
				percent = 0; // 下限ガード
			r.setPercent(Math.round(percent * 10.0) / 10.0); // 小数点1桁
		});

		return resultList;
	}
}
