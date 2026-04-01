package com.iispl.ingestion;

import com.iispl.enums.TransactionType;
import com.iispl.exception.AdapterException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

import java.math.BigDecimal;

/**
 * Shared parsing utilities used by all TransactionAdapter implementations.
 *
 * Every method throws AdapterException (with source + row context) on bad input
 * so callers can stop the batch with a clear error message.
 */
public final class AdapterUtil {

	private AdapterUtil() {
		// utility class — no instances
	}

	// -------------------------------------------------------------------------
	// requireCell — mandatory field guard
	// -------------------------------------------------------------------------

	/**
	 * Asserts that a cell is present and non-blank. Call this before readString()
	 * for every mandatory column.
	 *
	 * @param cell      the POI cell (may be null if the column is missing)
	 * @param source    source system name for the error message (e.g. "CBS")
	 * @param fieldName column / field name for the error message
	 * @param rowNumber 1-based row number for the error message
	 * @throws AdapterException if the cell is null or blank
	 */
	public static void requireCell(Cell cell, String source, String fieldName, int rowNumber) throws AdapterException {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			throw new AdapterException(source, rowNumber, "Required field '" + fieldName + "' is missing or blank");
		}
		if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty()) {
			throw new AdapterException(source, rowNumber, "Required field '" + fieldName + "' must not be empty");
		}
	}

	// -------------------------------------------------------------------------
	// readString — safe string extraction
	// -------------------------------------------------------------------------

	/**
	 * Reads a cell as a trimmed String regardless of its POI cell type. Returns
	 * {@code null} when the cell is null or blank (use for optional columns).
	 *
	 * @param cell the POI cell to read (may be null)
	 * @return trimmed string value, or null
	 */
	public static String readString(Cell cell) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		return switch (cell.getCellType()) {
		case STRING -> cell.getStringCellValue().trim();
		case NUMERIC -> {
			// handles cells that Excel stores as numbers (e.g. account numbers)
			double d = cell.getNumericCellValue();
			if (d == Math.floor(d) && !Double.isInfinite(d)) {
				yield String.valueOf((long) d);
			}
			yield String.valueOf(d);
		}
		case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
		case FORMULA ->
			cell.getCachedFormulaResultType() == CellType.NUMERIC ? String.valueOf((long) cell.getNumericCellValue())
					: cell.getStringCellValue().trim();
		default -> null;
		};
	}

	// -------------------------------------------------------------------------
	// readAmount — BigDecimal from a numeric cell
	// -------------------------------------------------------------------------

	/**
	 * Reads a mandatory numeric cell as a {@link BigDecimal}.
	 *
	 * @param cell      the POI cell expected to contain a number
	 * @param source    source system name for the error message
	 * @param rowNumber 1-based row number for the error message
	 * @return BigDecimal value (never null)
	 * @throws AdapterException if the cell is absent, blank, or non-numeric
	 */
	public static BigDecimal readAmount(Cell cell, String source, int rowNumber) throws AdapterException {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			throw new AdapterException(source, rowNumber, "Required field 'amount' is missing or blank");
		}
		try {
			return switch (cell.getCellType()) {
			case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
			case STRING -> new BigDecimal(cell.getStringCellValue().trim());
			case FORMULA -> BigDecimal.valueOf(cell.getNumericCellValue());
			default -> throw new AdapterException(source, rowNumber,
					"Field 'amount' has unexpected cell type: " + cell.getCellType());
			};
		} catch (NumberFormatException e) {
			throw new AdapterException(source, rowNumber, "Field 'amount' is not a valid number: " + readString(cell),
					e);
		}
	}

	// -------------------------------------------------------------------------
	// readTransactionType — enum from a string cell
	// -------------------------------------------------------------------------

	/**
	 * Reads a mandatory string cell and maps it to a {@link TransactionType} enum
	 * constant. The comparison is case-insensitive and whitespace-tolerant.
	 *
	 * @param cell      the POI cell expected to contain e.g. "CREDIT" or "DEBIT"
	 * @param source    source system name for the error message
	 * @param rowNumber 1-based row number for the error message
	 * @return matching TransactionType (never null)
	 * @throws AdapterException if the cell is absent, blank, or does not match any
	 *                          constant
	 */
	public static TransactionType readTransactionType(Cell cell, String source, int rowNumber) throws AdapterException {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			throw new AdapterException(source, rowNumber, "Required field 'txnType' is missing or blank");
		}
		String raw = readString(cell);
		if (raw == null || raw.isEmpty()) {
			throw new AdapterException(source, rowNumber, "Required field 'txnType' must not be empty");
		}
		try {
			return TransactionType.valueOf(raw.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new AdapterException(source, rowNumber, "Unknown transaction type '" + raw + "'. "
					+ "Expected one of: " + java.util.Arrays.toString(TransactionType.values()), e);
		}
	}
}