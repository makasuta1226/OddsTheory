package com.example.demo.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.domain.Odds;
import com.example.demo.domain.RaceInfo;
import com.example.demo.domain.UmarenOdds;
import com.example.demo.exception.ExcelInUseException;
import com.example.demo.exception.OddsLoadException;

@Service
public class OddsLoaderService {

	private static final Logger logger = LoggerFactory.getLogger(OddsLoaderService.class);
	private String lastWakuRaw = null;

	// -------------------------------------------------------------
	// Excel ファイル存在・読み込みチェック
	// -------------------------------------------------------------
	private void checkExcelFile(String excelPath) {
		File file = new File(excelPath);
		if (!file.exists()) {
			throw new OddsLoadException("オッズのExcelファイルが見つかりません");
		}
		if (!file.canRead()) {
			throw new OddsLoadException("オッズのExcelファイルを読み込めません");
		}
	}

	// -------------------------------------------------------------
	// オッズExcelが空かどうかチェック（A1判定）
	// -------------------------------------------------------------
	public boolean isOddsExcelEmpty(String excelPath) {
		checkExcelFile(excelPath);

		try (Workbook wb = WorkbookFactory.create(new FileInputStream(excelPath))) {

			Sheet tanFukuSheet = wb.getSheet("TanFuku");
			Sheet umaSheet = wb.getSheet("Uma");

			if (tanFukuSheet == null || umaSheet == null) {
				return true;
			}

			Cell tanFukuA1 = (tanFukuSheet.getRow(0) != null)
					? tanFukuSheet.getRow(0).getCell(0)
					: null;

			Cell umaA1 = (umaSheet.getRow(0) != null)
					? umaSheet.getRow(0).getCell(0)
					: null;

			if (tanFukuA1 == null || tanFukuA1.toString().trim().isEmpty()) {
				return true;
			}

			if (umaA1 == null || umaA1.toString().trim().isEmpty()) {
				return true;
			}

			return false;

		} catch (Exception e) {
			return true;
		}
	}

	// -------------------------------------------------------------
	// 単勝・複勝オッズ読み込み
	// -------------------------------------------------------------
	public List<Odds> loadTanFukuOdds(String excelPath) {
		checkExcelFile(excelPath);
		List<Odds> list = new ArrayList<>();

		try (Workbook wb = WorkbookFactory.create(new FileInputStream(excelPath))) {
			Sheet sheet = wb.getSheet("TanFuku");
			if (sheet == null) {
				throw new OddsLoadException("Excelに「TanFuku」シートが見つかりません");
			}

			int lastRow = sheet.getLastRowNum();
			if (lastRow == 0) {
				throw new OddsLoadException("「TanFuku」シートが空です");
			}
			int startRow = findStartRow(sheet, lastRow);

			for (int i = startRow; i <= lastRow; i++) {
				Row row = sheet.getRow(i);
				if (row == null)
					continue;

				String horseName = getString(row.getCell(2));
				if (horseName == null)
					break;

				Integer uma = extractInt(getString(row.getCell(1)));
				if (uma == null)
					continue;

				String wakuRaw = getString(row.getCell(0));
				Integer waku = extractInt(wakuRaw);
				if (waku == null) {
					waku = extractInt(lastWakuRaw);
				} else {
					lastWakuRaw = wakuRaw;
				}
				if (waku == null)
					continue;

				Odds o = new Odds();
				o.setWaku(waku);
				o.setUma(uma);
				o.setName(horseName);
				o.setJockey(getString(row.getCell(8)));
				o.setTansho(getDoubleSafe(row.getCell(3)));
				parseFukuOdds(o, getString(row.getCell(4)));

				list.add(o);
			}

		} catch (Exception e) {
			throw new OddsLoadException("単勝・複勝オッズの読み込みに失敗しました", e);
		}
		return list;
	}

	// -------------------------------------------------------------
	// 馬連オッズ読み込み
	// -------------------------------------------------------------
	public List<UmarenOdds> loadUmarenOdds(String excelPath) {
		checkExcelFile(excelPath);
		List<UmarenOdds> list = new ArrayList<>();

		try (Workbook wb = WorkbookFactory.create(new FileInputStream(excelPath))) {
			Sheet sheet = wb.getSheet("Uma");
			if (sheet == null) {
				throw new OddsLoadException("Excelに「Uma」シートが見つかりません");
			}

			Integer currentBaseHorse = null;

			for (Row row : sheet) {
				if (row == null)
					continue;

				Cell a = row.getCell(0);
				Cell b = row.getCell(1);

				if (a != null && a.getCellType() == CellType.NUMERIC
						&& (b == null || b.getCellType() == CellType.BLANK)) {
					currentBaseHorse = (int) a.getNumericCellValue();
					continue;
				}

				if (currentBaseHorse == null)
					continue;

				if (a != null && b != null
						&& a.getCellType() == CellType.NUMERIC
						&& b.getCellType() == CellType.NUMERIC) {

					UmarenOdds u = new UmarenOdds();
					u.setBaseHorseNo(currentBaseHorse);
					u.setPairHorseNo((int) a.getNumericCellValue());
					u.setOdds(b.getNumericCellValue());
					u.checkCancelled(); // 取消判定のみ

					if (u.isCancelled()) {
						logger.info("Horse No. " + u.getPairHorseNo() + " is cancelled.");
					}

					list.add(u);
				}
			}

		} catch (Exception e) {
			throw new OddsLoadException("馬連オッズの読み込みに失敗しました", e);
		}

		return list;
	}

	// -------------------------------------------------------------
	// レース情報読み込み
	// -------------------------------------------------------------
	public RaceInfo loadRaceInfo(String excelPath) {
		RaceInfo raceInfo = new RaceInfo();

		try (Workbook wb = WorkbookFactory.create(new FileInputStream(excelPath))) {
			Sheet sheet = wb.getSheetAt(0);
			boolean inBody = false;

			for (Row row : sheet) {
				for (Cell cell : row) {
					if (cell.getCellType() != CellType.STRING)
						continue;
					String text = cell.getStringCellValue().trim();
					if (text.isEmpty())
						continue;

					if (text.contains("ここから本文")) {
						inBody = true;
						continue;
					}
					if (!inBody)
						continue;

					if (raceInfo.getPlace() == null) {
						String place = extractPlace(text);
						if (place != null)
							raceInfo.setPlace(place);
					}

					if (raceInfo.getRaceNo() == null && text.matches("\\d+レース|\\d+R")) {
						raceInfo.setRaceNo(text.replace("レース", "R"));
					}

					if (raceInfo.getStartTime() == null && text.contains("発走時刻")) {
						String time = text.replace("発走時刻", "").replace("：", "").trim();
						raceInfo.setStartTime(time);
					}

					if (raceInfo.getRaceName() == null && isRaceNameCandidate(text)) {
						raceInfo.setRaceName(text);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return raceInfo;
	}

	// -------------------------------------------------------------
	// 判定後に Excel を安全に完全クリア
	// -------------------------------------------------------------
	public void clearOdds(String excelPath) {
		File file = new File(excelPath);

		if (!file.exists() || !file.canWrite()) {
			throw new ExcelInUseException("Excelファイルが存在しないか、書き込みできません");
		}

		File tempFile = null;
		try {
			tempFile = File.createTempFile("odds_temp_", ".xlsx");
			Files.copy(file.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

			try (Workbook wb = WorkbookFactory.create(new FileInputStream(tempFile))) {
				// TanFuku を空に
				Sheet tanFukuSheet = wb.getSheet("TanFuku");
				if (tanFukuSheet != null) {
					int lastRow = tanFukuSheet.getLastRowNum();
					for (int i = lastRow; i >= 0; i--) {
						Row row = tanFukuSheet.getRow(i);
						if (row != null)
							tanFukuSheet.removeRow(row);
					}
				}

				// Uma を空に
				Sheet umarenSheet = wb.getSheet("Uma");
				if (umarenSheet != null) {
					int lastRow = umarenSheet.getLastRowNum();
					for (int i = lastRow; i >= 0; i--) {
						Row row = umarenSheet.getRow(i);
						if (row != null)
							umarenSheet.removeRow(row);
					}
				}

				try (FileOutputStream fos = new FileOutputStream(tempFile)) {
					wb.write(fos);
				}
			}

			// 元ファイルに上書き
			Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			logger.info("Excelのオッズシートを安全にクリアしました");

			// セル選択をA1にリセット
			resetSelectionToA1(excelPath);

		} catch (Exception e) {
			throw new ExcelInUseException("Excelファイルを閉じてから再度お試しください", e);
		} finally {
			if (tempFile != null && tempFile.exists())
				tempFile.delete();
		}
	}

	// -------------------------------------------------------------
	// Excelで選択をA1セルにリセット
	// -------------------------------------------------------------
	public void resetSelectionToA1(String excelPath) {
		try (Workbook wb = WorkbookFactory.create(new FileInputStream(excelPath))) {
			Sheet tanFukuSheet = wb.getSheet("TanFuku");
			if (tanFukuSheet != null) {
				tanFukuSheet.setActiveCell(new CellAddress("A1"));
				wb.setActiveSheet(wb.getSheetIndex(tanFukuSheet));
			}

			Sheet umaSheet = wb.getSheet("Uma");
			if (umaSheet != null) {
				umaSheet.setActiveCell(new CellAddress("A1"));
				umaSheet.setSelected(false);
			}

			try (FileOutputStream fos = new FileOutputStream(excelPath)) {
				wb.write(fos);
			}
		} catch (Exception e) {
			throw new ExcelInUseException("Excelファイルにアクセスできません", e);
		}
	}

	// -------------------------------------------------------------
	// Utility
	// -------------------------------------------------------------
	private int findStartRow(Sheet sheet, int lastRow) {
		for (int i = 0; i <= lastRow; i++) {
			Row row = sheet.getRow(i);
			if (row == null)
				continue;
			if ("馬名".equals(getString(row.getCell(2))))
				return i + 1;
		}
		throw new OddsLoadException("馬名が見つかりません");
	}

	private String getString(Cell cell) {
		if (cell == null)
			return null;
		try {
			if (cell.getCellType() == CellType.STRING) {
				String s = cell.getStringCellValue();
				return (s == null || s.isBlank()) ? null : s.trim();
			}
			if (cell.getCellType() == CellType.NUMERIC) {
				return String.valueOf((int) cell.getNumericCellValue());
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private Integer extractInt(String s) {
		if (s == null)
			return null;
		String num = s.replaceAll("[^0-9]", "");
		return num.isEmpty() ? null : Integer.parseInt(num);
	}

	private double getDoubleSafe(Cell cell) {
		try {
			if (cell == null)
				return 0;
			if (cell.getCellType() == CellType.NUMERIC)
				return cell.getNumericCellValue();
			if (cell.getCellType() == CellType.STRING)
				return Double.parseDouble(cell.getStringCellValue().trim());
		} catch (Exception ignored) {
		}
		return 0;
	}

	private void parseFukuOdds(Odds o, String fuku) {
		if (fuku == null || !fuku.matches("[0-9.]+\\s*-\\s*[0-9.]+"))
			return;
		String[] sp = fuku.split("-");
		o.setFukuLow(parseDouble(sp[0]));
		o.setFukuHigh(parseDouble(sp[1]));
	}

	private double parseDouble(String s) {
		try {
			return Double.parseDouble(s.trim());
		} catch (Exception e) {
			return 0;
		}
	}

	private String extractPlace(String text) {
		String[] places = { "中山", "東京", "阪神", "京都", "中京", "札幌", "函館", "福島", "新潟", "小倉" };
		for (String p : places)
			if (text.matches(".*" + p + ".*"))
				return p;
		return null;
	}

	private boolean isRaceNameCandidate(String text) {
		return text.matches(
				".*(GⅠ|GⅡ|GⅢ|ステークス|特別|新馬"
						+ "|歳未勝利|歳以上未勝利|歳1勝クラス|歳以上1勝クラス"
						+ "|歳2勝クラス|歳以上2勝クラス|歳3勝クラス|歳以上3勝クラス).*");
	}
}
