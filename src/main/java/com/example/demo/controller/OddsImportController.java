package com.example.demo.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import com.example.demo.domain.Odds;
import com.example.demo.domain.RaceInfo;
import com.example.demo.domain.UmarenOdds;
import com.example.demo.service.OddsLoaderService;
import com.example.demo.service.OddsRankService;

@Controller
public class OddsImportController {

	private static final Logger logger = LoggerFactory.getLogger(OddsImportController.class);

	@Autowired
	private OddsLoaderService service;

	@Autowired
	private OddsRankService oddsRankService;

	@GetMapping("/odds/import")
	public String importTop() {
		return "odds/import";
	}

	@PostMapping("/odds/read")
	public String readOdds() {
		return "odds/import_loading";
	}

	@GetMapping("/odds/load_exec")
	public ModelAndView execLoad(HttpSession session) {

		ModelAndView mv = new ModelAndView();
		String excelPath = "C:/Users/zd3O05/pleiades/workspace/OddsTheory/odds.xlsx";

		try {
			// A1チェック（最優先）
			if (service.isOddsExcelEmpty(excelPath)) {
				mv.setViewName("error/odds_error");
				mv.addObject("errorTitle", "❌ オッズ読み込みエラー");
				mv.addObject("errorMessage", "オッズデータがありません");
				return mv;
			}

			// 単勝・複勝
			List<Odds> tanFukuList = service.loadTanFukuOdds(excelPath);

			// 馬連
			List<UmarenOdds> umarenOddsList = service.loadUmarenOdds(excelPath);

			oddsRankService.setTanshoRank(tanFukuList);
			oddsRankService.setUmarenBestRank(tanFukuList, umarenOddsList);

			if (tanFukuList.isEmpty() || umarenOddsList.isEmpty()) {
				mv.setViewName("error/odds_error");
				mv.addObject("errorTitle", "❌ オッズ読み込みエラー");
				mv.addObject("errorMessage", "オッズデータがありません");
				return mv;
			}

			Map<Integer, List<UmarenOdds>> umarenMap = umarenOddsList.stream()
					.collect(Collectors.groupingBy(
							UmarenOdds::getBaseHorseNo,
							LinkedHashMap::new,
							Collectors.toList()));

			session.setAttribute("tanshoOdds", tanFukuList);
			session.setAttribute("fukushoOdds", tanFukuList);
			session.setAttribute("umarenOdds", umarenMap);

			// レース情報
			RaceInfo raceInfo = service.loadRaceInfo(excelPath);

			// 表示用ヘッダ
			String raceHeader = generateRaceHeader(raceInfo);

			mv.addObject("tanFukuList", tanFukuList);
			mv.addObject("umarenMap", umarenMap);
			mv.addObject("race", raceInfo);
			mv.addObject("raceHeader", raceHeader);

			mv.setViewName("odds/view");

		} catch (Exception e) {
			logger.error("オッズ読み込み失敗", e);
			mv.setViewName("error/odds_error");
			mv.addObject("errorTitle", "❌ オッズ読み込みエラー");
			mv.addObject("errorMessage", "オッズデータがありません");
		}

		return mv;
	}

	private String generateRaceHeader(RaceInfo raceInfo) {
		if (raceInfo == null)
			return "";

		StringBuilder sb = new StringBuilder();

		// 競馬場名（最優先で設定）
		if (raceInfo.getPlace() != null && !raceInfo.getPlace().isBlank()) {
			sb.append(raceInfo.getPlace()).append(" ");
		}

		// レース番号（1レース → 1R）
		if (raceInfo.getRaceNo() != null && !raceInfo.getRaceNo().isBlank()) {
			sb.append(raceInfo.getRaceNo().replace("レース", "R"));
		}

		// ★ 全角スペース1つ
		sb.append("　");

		// レース名（特別レースやG1等）
		if (raceInfo.getRaceName() != null && !raceInfo.getRaceName().isBlank()) {
			String raceName = raceInfo.getRaceName();

			// 格付けを検出して【 】で囲む
			String[] grades = { "GⅠ", "GⅡ", "GⅢ", "リステッド" };
			for (String grade : grades) {
				if (raceName.endsWith(grade)) {
					raceName = raceName.substring(0, raceName.length() - grade.length())
							+ "【" + grade + "】";
					break;
				}
			}

			sb.append(raceName);
		}

		// ★ 全角スペース2つ
		sb.append("　　");

		// 発走時刻（必ず「発走時刻」を付ける）
		if (raceInfo.getStartTime() != null && !raceInfo.getStartTime().isBlank()) {
			sb.append(" 発走時刻 ").append(raceInfo.getStartTime());
		}

		return sb.toString();
	}

}
