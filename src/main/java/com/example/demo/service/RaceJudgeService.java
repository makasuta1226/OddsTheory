package com.example.demo.service;

import java.util.List;
import java.util.OptionalDouble;

import org.springframework.stereotype.Service;

import com.example.demo.domain.UmarenOdds;
import com.example.demo.dto.JudgeResultDto;
import com.example.demo.enums.RaceJudgeType;

@Service
public class RaceJudgeService {

	/**
	 * 推奨％リストをもとに柔軟にレースタイプ判定
	 *
	 * @param judgeResults 推奨％を含む全馬DTOリスト
	 * @param umarenOdds 馬連オッズリスト
	 * @return RaceJudgeType 判定結果
	 */
	public RaceJudgeType judge(List<JudgeResultDto> judgeResults, List<UmarenOdds> umarenOdds) {

		if (judgeResults.isEmpty()) {
			return RaceJudgeType.MIOKURI;
		}

		// 推奨％の最大値
		OptionalDouble maxPercentOpt = judgeResults.stream()
				.mapToDouble(JudgeResultDto::getPercent)
				.max();
		double maxPercent = maxPercentOpt.orElse(0.0);

		// 推奨％40%以上の馬の数
		long over40Count = judgeResults.stream()
				.filter(r -> r.getPercent() >= 40.0)
				.count();

		// 推奨％70%以上の馬の数（本命レース判定用）
		long over70Count = judgeResults.stream()
				.filter(r -> r.getPercent() >= 70.0)
				.count();

		// 馬連上位オッズ
		double umarenTopOdds = umarenOdds.stream()
				.filter(u -> u.getOdds() > 0)
				.mapToDouble(UmarenOdds::getOdds)
				.min()
				.orElse(99.9);

		// --- 判定ロジック ---
		// 本命レース
		if (maxPercent >= 80.0 && over40Count <= 5 && over70Count >= 1 && umarenTopOdds <= 5.0) {
			return RaceJudgeType.HONMEI;
		}

		// 中穴レース
		if (maxPercent >= 70.0 && over40Count <= 7 && umarenTopOdds >= 5.0) {
			return RaceJudgeType.CHUANA;
		}

		// 穴レース
		if (maxPercent < 60.0 && over40Count >= 10) {
			return RaceJudgeType.ANA;
		}

		// 見送り
		return RaceJudgeType.MIOKURI;
	}
}
