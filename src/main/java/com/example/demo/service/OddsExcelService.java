package com.example.demo.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import com.example.demo.exception.ExcelInUseException;

@Service
public class OddsExcelService {

	private final String excelPath = "C:/Users/zd3O05/pleiades/workspace/OddsTheory/odds.xlsx";

	public void resetTanFukuSheet() throws ExcelInUseException {
		clearSheet("TanFuku");
	}

	public void resetUmaSheet() throws ExcelInUseException {
		clearSheet("Uma");
	}

	private void clearSheet(String sheetName) throws ExcelInUseException {
		File file = new File(excelPath);
		if (!file.exists() || !file.canRead()) {
			throw new ExcelInUseException("Excelファイルを開けません。閉じて再度お試しください");
		}

		try (FileInputStream fis = new FileInputStream(file);
				Workbook wb = WorkbookFactory.create(fis)) {

			Sheet sheet = wb.getSheet(sheetName);
			if (sheet != null) {
				int lastRow = sheet.getLastRowNum();
				for (int i = sheet.getFirstRowNum(); i <= lastRow; i++) {
					if (sheet.getRow(i) != null) {
						sheet.removeRow(sheet.getRow(i));
					}
				}
			}

			// 上書き保存
			try (FileOutputStream fos = new FileOutputStream(file)) {
				wb.write(fos);
			}

		} catch (IOException e) {
			throw new ExcelInUseException("Excelファイルを開けません（使用中の可能性があります）");
		} catch (Exception e) {
			throw new ExcelInUseException("シートのクリアに失敗しました");
		}
	}
}
