package poly.edu.quanlynhahang.service;

import poly.edu.quanlynhahang.entity.RestaurantTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure table-combination planner. It deliberately receives only tables which are already
 * available for the requested slot, so it never bypasses the reservation conflict checks.
 */
public class TableCombinationPlanner {
    private static final int MAX_COMBINED_TABLES = 4;
    private static final Pattern POSITION_NUMBER = Pattern.compile("(.*?)(\\d+)$");

    public Optional<List<RestaurantTable>> findBestCombination(List<RestaurantTable> availableTables, int guestCount) {
        List<RestaurantTable> candidates = availableTables.stream()
                .filter(table -> capacityOf(table) > 0)
                .sorted(Comparator.comparing(RestaurantTable::getId))
                .toList();
        if (candidates.stream().anyMatch(table -> capacityOf(table) >= guestCount)) {
            return Optional.empty();
        }
        Candidate best = null;
        for (int size = 2; size <= Math.min(MAX_COMBINED_TABLES, candidates.size()); size++) {
            best = search(candidates, guestCount, size, 0, new ArrayList<>(), best);
            if (best != null) {
                // Fewer tables always wins, so no larger combination can improve this result.
                break;
            }
        }
        return best == null ? Optional.empty() : Optional.of(best.tables());
    }

    private Candidate search(List<RestaurantTable> candidates, int guests, int targetSize, int start,
                             List<RestaurantTable> selected, Candidate best) {
        if (selected.size() == targetSize) {
            int totalCapacity = selected.stream().mapToInt(this::capacityOf).sum();
            if (totalCapacity < guests) {
                return best;
            }
            Candidate candidate = new Candidate(List.copyOf(selected), totalCapacity - guests, proximityPenalty(selected));
            return best == null || candidate.compareTo(best) < 0 ? candidate : best;
        }
        for (int index = start; index <= candidates.size() - (targetSize - selected.size()); index++) {
            selected.add(candidates.get(index));
            best = search(candidates, guests, targetSize, index + 1, selected, best);
            selected.removeLast();
        }
        return best;
    }

    private int capacityOf(RestaurantTable table) {
        return table.getMaxCapacity() != null ? table.getMaxCapacity()
                : (table.getCapacity() == null ? 0 : table.getCapacity());
    }

    private int proximityPenalty(List<RestaurantTable> tables) {
        int penalty = 0;
        for (int left = 0; left < tables.size(); left++) {
            for (int right = left + 1; right < tables.size(); right++) {
                penalty += positionDistance(tables.get(left).getPositionDescription(), tables.get(right).getPositionDescription());
            }
        }
        return penalty;
    }

    private int positionDistance(String first, String second) {
        if (first == null || second == null || first.isBlank() || second.isBlank()) return 10;
        String left = first.trim().toLowerCase(Locale.ROOT);
        String right = second.trim().toLowerCase(Locale.ROOT);
        if (left.equals(right)) return 0;
        Matcher leftMatcher = POSITION_NUMBER.matcher(left);
        Matcher rightMatcher = POSITION_NUMBER.matcher(right);
        if (leftMatcher.matches() && rightMatcher.matches() && leftMatcher.group(1).equals(rightMatcher.group(1))) {
            return Math.abs(Integer.parseInt(leftMatcher.group(2)) - Integer.parseInt(rightMatcher.group(2)));
        }
        return 10;
    }

    private record Candidate(List<RestaurantTable> tables, int surplus, int proximityPenalty)
            implements Comparable<Candidate> {
        @Override
        public int compareTo(Candidate other) {
            int result = Integer.compare(surplus, other.surplus);
            if (result != 0) return result;
            result = Integer.compare(proximityPenalty, other.proximityPenalty);
            if (result != 0) return result;
            for (int index = 0; index < tables.size(); index++) {
                result = Integer.compare(tables.get(index).getId(), other.tables.get(index).getId());
                if (result != 0) return result;
            }
            return 0;
        }
    }
}
