package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.domain.Odds;
import com.example.demo.domain.UmarenOdds;
import com.example.demo.service.OddsJudgeService;
import com.example.demo.service.OddsLoaderService;

@Controller
public class OddsController {

	@Autowired
	private OddsJudgeService oddsJudgeService;

	@Autowired
	private OddsLoaderService oddsLoaderService;

	// Excelファイルのパス
	private final String excelPath = "C:/Users/zd3O05/pleiades/workspace/OddsTheory/odds.xlsx";

	@GetMapping("/odds/judge")
	public String judgeOdds(Model model) {

		try {
			// --- Excelから単勝・複勝オッズを読み込む ---
			List<Odds> oddsList = oddsLoaderService.loadTanFukuOdds(excelPath);
			if (oddsList == null || oddsList.isEmpty()) {
				model.addAttribute("errorMessage", "単勝・複勝オッズが存在しません。");
				return "odds_error";
			}

			// --- Excelから馬連オッズを読み込む ---
			List<UmarenOdds> umarenList = oddsLoaderService.loadUmarenOdds(excelPath);
			if (umarenList == null || umarenList.isEmpty()) {
				model.addAttribute("errorMessage", "馬連オッズが存在しません。");
				return "odds_error";
			}

			Map<Integer, List<UmarenOdds>> umarenMap = umarenList.stream()
					.collect(Collectors.groupingBy(UmarenOdds::getBaseHorseNo));

			// --- 判定 ---
			Map<Integer, Double> tanshoPoints = oddsJudgeService.judgeTansho(oddsList);
			Map<Integer, Double> fukushoPoints = oddsJudgeService.judgeFukusho(oddsList);
			Map<Integer, Double> umarenPoints = oddsJudgeService.judgeUmaren(umarenMap);

			// --- 推奨％（最大値85％＋相対バランス補正） ---
			Map<Integer, Double> tanshoPercent = calcPercent(tanshoPoints, 85.0);
			Map<Integer, Double> fukushoPercent = calcPercent(fukushoPoints, 85.0);
			Map<Integer, Double> umarenPercent = calcPercent(umarenPoints, 85.0);

			// --- Modelにセット ---
			model.addAttribute("tanshoPoints", tanshoPoints);
			model.addAttribute("fukushoPoints", fukushoPoints);
			model.addAttribute("umarenPoints", umarenPoints);

			model.addAttribute("tanshoPercent", tanshoPercent);
			model.addAttribute("fukushoPercent", fukushoPercent);
			model.addAttribute("umarenPercent", umarenPercent);

			return "odds_judge"; // 正常画面

		} catch (Exception e) {
			// 読み込み失敗時はエラー画面に遷移
			model.addAttribute("errorTitle", "❌ オッズの読み込みに失敗しました");
			model.addAttribute("errorMessage", "オッズが貼り付けられていないか、データが不正です。もう一度オッズを貼り付けて再試行してください。");
			return "odds_error";
		}
	}

	/**
	 * 推奨％を計算（最大値 maxPercent以内 + 他馬との相対バランス補正）
	 */
	private Map<Integer, Double> calcPercent(Map<Integer, Double> pointMap, double maxPercent) {
		Map<Integer, Double> percentMap = new HashMap<>();

		if (pointMap == null || pointMap.isEmpty()) {
			return percentMap;
		}

		// --- 合計ポイントを計算 ---
		double total = 0.0;
		for (Double value : pointMap.values()) {
			if (value != null) {
				total += value.doubleValue();
			}
		}
		if (total <= 0.0) {
			total = 1.0;
		}

		// --- 現状の最大ポイント比率 ---
		double currentMaxPercent = 0.0;
		for (Double value : pointMap.values()) {
			if (value != null) {
				double p = (value.doubleValue() / total) * 100.0;
				if (p > currentMaxPercent) {
					currentMaxPercent = p;
				}
			}
		}
		if (currentMaxPercent <= 0.0) {
			currentMaxPercent = 1.0;
		}

		double multiplier = maxPercent / currentMaxPercent;

		// --- パーセント計算 ---
		for (Map.Entry<Integer, Double> e : pointMap.entrySet()) {
			Double val = e.getValue();
			double percent = 0.0;
			if (val != null && val > 0.0) {
				percent = (val.doubleValue() / total) * 100.0 * multiplier;
			}
			if (percent < 0.0) {
				percent = 0.0;
			}
			percentMap.put(e.getKey(), percent);
		}

		return percentMap;
	}

}
