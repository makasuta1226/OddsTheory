package com.example.demo.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.domain.Odds;
import com.example.demo.domain.UmarenOdds;

@Service
public class OddsRankService {

	/**
	 * 単勝オッズ順位を Odds にセット
	 * 低オッズ＝上位人気
	 */
	public void setTanshoRank(List<Odds> oddsList) {

		oddsList.stream()
				.filter(o -> o.getTansho() > 0)
				.sorted(Comparator.comparingDouble(Odds::getTansho))
				.forEachOrdered(new java.util.function.Consumer<Odds>() {

					int rank = 1;

					@Override
					public void accept(Odds o) {
						o.setTanshoRank(rank++);
					}
				});
	}

	/**
	 * 各馬が含まれる「馬連オッズの最上位順位」を Odds にセット
	 */
	public void setUmarenBestRank(
			List<Odds> oddsList,
			List<UmarenOdds> umarenOddsList) {

		// 馬番 → 最小順位
		Map<Integer, Integer> bestRankMap = new HashMap<>();

		umarenOddsList.stream()
				.filter(u -> !u.isCancelled())
				.sorted(Comparator.comparingDouble(UmarenOdds::getOdds))
				.forEachOrdered(new java.util.function.Consumer<UmarenOdds>() {

					int rank = 1;

					@Override
					public void accept(UmarenOdds u) {

						updateBestRank(bestRankMap, u.getBaseHorseNo(), rank);
						updateBestRank(bestRankMap, u.getPairHorseNo(), rank);
						rank++;
					}
				});

		// Odds に反映
		for (Odds o : oddsList) {
			o.setUmarenBestRank(bestRankMap.get(o.getUma()));
		}
	}

	private void updateBestRank(
			Map<Integer, Integer> map,
			int horseNo,
			int rank) {

		map.merge(horseNo, rank, Math::min);
	}
}
