package org.olo.bootstrap.registry;

import java.util.Comparator;

/**
 * Orders workflow definition versions for latest-resolution (semver-like {@code major.minor.patch}).
 */
final class WorkflowVersionOrder {

    private static final Comparator<String> COMPARATOR = WorkflowVersionOrder::compare;

    private WorkflowVersionOrder() {
    }

    static Comparator<String> comparator() {
        return COMPARATOR;
    }

    static int compare(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        if (normalizedLeft.equals(normalizedRight)) {
            return 0;
        }

        String[] leftParts = normalizedLeft.split("\\.");
        String[] rightParts = normalizedRight.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            String leftPart = index < leftParts.length ? leftParts[index] : "0";
            String rightPart = index < rightParts.length ? rightParts[index] : "0";
            Integer leftNumber = parseNumericPart(leftPart);
            Integer rightNumber = parseNumericPart(rightPart);
            if (leftNumber != null && rightNumber != null) {
                int numericCompare = Integer.compare(leftNumber, rightNumber);
                if (numericCompare != 0) {
                    return numericCompare;
                }
                continue;
            }
            int lexicalCompare = leftPart.compareTo(rightPart);
            if (lexicalCompare != 0) {
                return lexicalCompare;
            }
        }
        return normalizedLeft.compareTo(normalizedRight);
    }

    private static String normalize(String version) {
        if (version == null || version.isBlank()) {
            return "";
        }
        return version.trim();
    }

    private static Integer parseNumericPart(String part) {
        if (part.isEmpty()) {
            return 0;
        }
        for (int index = 0; index < part.length(); index++) {
            if (!Character.isDigit(part.charAt(index))) {
                return null;
            }
        }
        return Integer.parseInt(part);
    }
}
