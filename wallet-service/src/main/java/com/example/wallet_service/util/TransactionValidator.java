

package com.example.wallet_service.util;

public class TransactionValidator {

    /**
     * Validates a generic object, demonstrating Java's pattern matching for instanceof.
     * (Future versions of Java will enhance this with switch expressions)
     */
    public static boolean isValidTransactionData(Object data) {
        // Pattern Matching for instanceof (Java 16+)
        if (data instanceof String s && !s.trim().isEmpty()) {
            return true; // Valid string description
        } else if (data instanceof Long l && l > 0) {
            return true; // Valid positive ID
        } else if (data instanceof Number n) {
            // Can be expanded to check specific numeric constraints
            return n.doubleValue() > 0;
        }
        return false;
    }
}

