package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.utils;

import java.math.BigDecimal;

public class NumberToWordsUtil {

    private static final String[] units = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"
    };

    private static final String[] teens = {
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
            "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] tens = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty",
            "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static final String[] thousands = {
            "", "Thousand", "Million", "Billion", "Trillion"
    };

    public static String convertToWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Zero Shillings Only";
        }

        long wholePart = amount.longValue();
        int cents = amount.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();

        StringBuilder result = new StringBuilder();

        if (wholePart == 0) {
            result.append("Zero");
        } else {
            result.append(convertWholeNumber(wholePart));
        }

        result.append(" Shilling");
        if (wholePart != 1) {
            result.append("s");
        }

        if (cents > 0) {
            result.append(" and ").append(convertWholeNumber(cents)).append(" Cent");
            if (cents != 1) {
                result.append("s");
            }
        }

        result.append(" Only");

        return result.toString();
    }

    private static String convertWholeNumber(long number) {
        if (number == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        int thousandIndex = 0;

        while (number > 0) {
            if (number % 1000 != 0) {
                StringBuilder groupResult = new StringBuilder();
                groupResult.append(convertHundreds((int) (number % 1000)));

                if (thousandIndex > 0) {
                    groupResult.append(" ").append(thousands[thousandIndex]);
                }

                if (result.length() > 0) {
                    groupResult.append(" ");
                }

                result.insert(0, groupResult.toString());
            }

            number /= 1000;
            thousandIndex++;
        }

        return result.toString().trim();
    }

    private static String convertHundreds(int number) {
        StringBuilder result = new StringBuilder();

        if (number >= 100) {
            result.append(units[number / 100]).append(" Hundred");
            number %= 100;
            if (number > 0) {
                result.append(" ");
            }
        }

        if (number >= 20) {
            result.append(tens[number / 10]);
            number %= 10;
            if (number > 0) {
                result.append(" ");
            }
        } else if (number >= 10) {
            result.append(teens[number - 10]);
            number = 0;
        }

        if (number > 0) {
            result.append(units[number]);
        }

        return result.toString();
    }
}