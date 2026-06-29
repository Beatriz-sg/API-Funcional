package com.app.confeitaria.docelivery.util;

/**
 * Validates Brazilian CPF numbers using the official two-digit verification algorithm.
 *
 * Usage:
 *   if (!CpfValidator.isValid(cpf)) {
 *       return ResponseEntity.badRequest().body("CPF inválido.");
 *   }
 */
public final class CpfValidator {

    private CpfValidator() {}

    /**
     * Returns true only for mathematically valid CPF values.
     * Strips dots and hyphens before validation.
     * Rejects null, wrong length, and all-same-digit sequences.
     */
    public static boolean isValid(String cpf) {
        if (cpf == null) return false;

        // Strip formatting characters
        String digits = cpf.replaceAll("[.\\-]", "");

        // Must be exactly 11 digits
        if (!digits.matches("\\d{11}")) return false;

        // Reject sequences like 00000000000 … 99999999999
        if (digits.chars().distinct().count() == 1) return false;

        // Validate first check digit
        if (!checkDigit(digits, 9)) return false;

        // Validate second check digit
        return checkDigit(digits, 10);
    }

    /**
     * Computes one CPF check digit and compares it to the actual digit at {@code position}.
     *
     * @param digits   11-digit string (digits only)
     * @param position 9 for the first check digit, 10 for the second
     */
    private static boolean checkDigit(String digits, int position) {
        int sum = 0;
        int weight = position + 1; // weight starts at 10 for position 9, 11 for position 10

        for (int i = 0; i < position; i++) {
            sum += (digits.charAt(i) - '0') * (weight - i);
        }

        int remainder = sum % 11;
        int expected = (remainder < 2) ? 0 : (11 - remainder);
        int actual = digits.charAt(position) - '0';

        return expected == actual;
    }
}
