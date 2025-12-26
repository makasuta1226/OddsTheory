package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.domain.Odds;
import com.example.demo.domain.UmarenOdds;
import com.example.demo.dto.JudgeResultDto;
import com.example.demo.enums.RaceJudgeType;
import com.example.demo.exception.ExcelInUseException;
import com.example.demo.exception.OddsLoadException;
import com.example.demo.service.FukushoJudgeService;
import com.example.demo.service.OddsDebugService;
import com.example.demo.service.OddsLoaderService;
import com.example.demo.service.RaceJudgeService;
import com.example.demo.service.TanshoJudgeService;
import com.example.demo.service.TotalJudgeService;
import com.example.demo.service.UmarenJudgeService;

@Controller
public class JudgeController {

	private final TanshoJudgeService tanshoJudgeService;
	private final FukushoJudgeService fukushoJudgeService;
	private final UmarenJudgeService umarenJudgeService;
	private final TotalJudgeService totalJudgeService;
	private final RaceJudgeService raceJudgeService;
	private final OddsLoaderService oddsLoaderService;

	private final String excelPath = "C:/Users/zd3O05/pleiades/workspace/OddsTheory/odds.xlsx";

	private final OddsDebugService oddsDebugService;

	public JudgeController(
			TanshoJudgeService tanshoJudgeService,
			FukushoJudgeService fukushoJudgeService,
			UmarenJudgeService umarenJudgeService,
			TotalJudgeService totalJudgeService,
			RaceJudgeService raceJudgeService,
			OddsLoaderService oddsLoaderService,
			OddsDebugService oddsDebugService) {

		this.tanshoJudgeService = tanshoJudgeService;
		this.fukushoJudgeService = fukushoJudgeService;
		this.umarenJudgeService = umarenJudgeService;
		this.totalJudgeService = totalJudgeService;
		this.raceJudgeService = raceJudgeService;
		this.oddsLoaderService = oddsLoaderService;
		this.oddsDebugService = oddsDebugService;
	}

	// 判定開始
	@PostMapping("/judge/start")
	public String startJudge() {
		return "redirect:/judge/loading";
	}

	// ローディング画面
	@GetMapping("/judge/loading")
	public String loadingJudge() {
		return "judge/judge_loading";
	}

	// 判定結果表示
	@SuppressWarnings("unchecked")
	@GetMapping("/judge/result")
	public String resultJudge(HttpSession session, Model model) {

		try {
			// セッションからオッズデータを取得
			List<Odds> tanshoOdds = (List<Odds>) session.getAttribute("tanshoOdds");
			List<Odds> fukushoOdds = (List<Odds>) session.getAttribute("fukushoOdds");
			Map<Integer, List<UmarenOdds>> umarenOddsMap = (Map<Integer, List<UmarenOdds>>) session
					.getAttribute("umarenOdds");

			if (tanshoOdds == null || fukushoOdds == null || umarenOddsMap == null ||
					tanshoOdds.isEmpty() || fukushoOdds.isEmpty()) {
				return "redirect:/odds/import";
			}

			// セッションから取得後すぐ
			oddsDebugService.printTanshoFukusho(tanshoOdds);
			oddsDebugService.printUmarenWithRank(tanshoOdds, umarenOddsMap);

			// --- 判定処理 ---
			Map<Integer, Double> tanshoPointMap = tanshoJudgeService.judge(tanshoOdds);
			Map<Integer, Double> fukushoPointMap = fukushoJudgeService.judge(fukushoOdds);
			Map<Integer, Double> umarenPointMap = umarenJudgeService.judge(umarenOddsMap);

			// --- 総合判定（全頭分のポイントを統合） ---
			List<JudgeResultDto> judgeResults = totalJudgeService.judgeRanking(
					tanshoPointMap, fukushoPointMap, umarenPointMap, tanshoOdds);

			if (judgeResults.isEmpty()) {
				return "redirect:/odds/import";
			}

			// --- レース判定 ---
			List<UmarenOdds> umarenOddsList = umarenOddsMap.values().stream()
					.flatMap(List::stream)
					.collect(Collectors.toList());

			RaceJudgeType raceJudge = raceJudgeService.judge(judgeResults, umarenOddsList);

			// --- Viewへデータ渡し ---
			model.addAttribute("ranking", judgeResults); // 全頭分ポイント入り
			model.addAttribute("raceJudge", raceJudge);

			return "judge/result";

		} catch (OddsLoadException e) {
			model.addAttribute("errorMessage", e.getMessage());
			return "error/odds_error";
		} catch (Exception e) {
			model.addAttribute("errorMessage", "予期しないエラーが発生しました。");
			return "error/error";
		}
	}

	// 別レース判定
	@GetMapping("/judge/resetAndGo")
	public String resetAndGo(HttpSession session, Model model) {
		try {
			oddsLoaderService.clearOdds(excelPath);
			session.removeAttribute("tanshoOdds");
			session.removeAttribute("fukushoOdds");
			session.removeAttribute("umarenOdds");
			return "redirect:/odds/import";
		} catch (ExcelInUseException e) {
			model.addAttribute("excelWarning", "Excelファイルが開かれている為、再実行処理が出来ません。Excelを閉じてください");
			return "judge/result";
		}
	}

	// TOPへ戻る
	@GetMapping("/judge/clearAndTop")
	public String clearAndGoTop(HttpSession session, Model model) {
		try {
			oddsLoaderService.clearOdds(excelPath);
			session.removeAttribute("tanshoOdds");
			session.removeAttribute("fukushoOdds");
			session.removeAttribute("umarenOdds");
			return "redirect:/top";
		} catch (ExcelInUseException e) {
			model.addAttribute("excelWarning", "Excelファイルが開かれているため、オッズ削除ができません。Excelを閉じてください。");
			return "judge/result";
		}
	}
}
