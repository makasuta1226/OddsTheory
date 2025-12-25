package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.domain.Odds;
import com.example.demo.domain.UmarenOdds;

@Service
public class OddsJudgeService {

	private final TanshoJudgeService tanshoService = new TanshoJudgeService();
	private final FukushoJudgeService fukushoService = new FukushoJudgeService();
	private final UmarenJudgeService umarenService = new UmarenJudgeService();

	public Map<Integer, Double> judgeTansho(List<Odds> oddsList) {
		return tanshoService.judge(oddsList);
	}

	public Map<Integer, Double> judgeFukusho(List<Odds> oddsList) {
		return fukushoService.judge(oddsList);
	}

	public Map<Integer, Double> judgeUmaren(Map<Integer, List<UmarenOdds>> umarenMap) {
		return umarenService.judge(umarenMap);
	}
}
