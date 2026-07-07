package com.example.gatepass.scanner;

import android.graphics.Rect;

import com.example.gatepass.utils.Constants;
import com.example.gatepass.validation.Verhoeff;
import com.google.mlkit.vision.text.Text;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

public class AadhaarScannerManager {

    private final List<String> idHistory = new ArrayList<>();
    private final List<String> nameHistory = new ArrayList<>();

    private String lastConfirmedId = "";
    private String lastConfirmedName = "";

    public interface ScannerListener {
        void onStabilityRequired();
        void onVerified(String name, String id);
        void onScanning(String partialInfo);
    }

    public void processText(Text visionText, ScannerListener listener) {
        String detectedId = "";
        Text.Line idLine = null;

        List<Text.TextBlock> blocks = visionText.getTextBlocks();

        for (Text.TextBlock block : blocks) {
            for (Text.Line line : block.getLines()) {
                Matcher m = Constants.AADHAAR_PATTERN.matcher(line.getText());
                if (m.find()) {
                    String raw = m.group().replaceAll("[\\s-]", "");
                    if (raw.length() == 12 && Verhoeff.validateVerhoeff(raw)) {
                        detectedId = raw;
                        idLine = line;
                        break;
                    }
                }
            }
            if (!detectedId.isEmpty()) break;
        }

        if (detectedId.isEmpty()) {
            listener.onScanning("Scanning Aadhaar Card...");
            return;
        }

        String detectedName = findName(idLine, blocks);

        if (!detectedName.isEmpty()) {
            runConsensus(detectedId, detectedName, listener);
        } else {
            listener.onScanning("ID: " + formatAadhaar(detectedId) + "\nLooking for name...");
        }
    }

    private String findName(Text.Line idLine, List<Text.TextBlock> allBlocks) {
        Rect idRect = idLine.getBoundingBox();
        if (idRect == null) return "";

        List<ScoredLine> candidates = new ArrayList<>();

        for (Text.TextBlock block : allBlocks) {
            for (Text.Line line : block.getLines()) {
                if (line == idLine) continue;
                Rect r = line.getBoundingBox();
                if (r == null) continue;

                if (r.bottom > idRect.top + 20) continue;

                String text = line.getText().trim();
                int score = scoreAsName(text);

                if (score > 0) {
                    int dist = idRect.top - r.bottom;
                    candidates.add(new ScoredLine(text, score, dist));
                }
            }
        }

        if (candidates.isEmpty()) return "";

        candidates.sort((a, b) -> {
            if (b.score != a.score) return b.score - a.score;
            return a.distToId - b.distToId;
        });

        return candidates.get(0).text;
    }

    private int scoreAsName(String text) {
        if (text.isEmpty() || Constants.BLACKLIST_REGEX.matcher(text).find()) return 0;

        int score = 0;
        if (Constants.NAME_PATTERN.matcher(text).matches()) score += 3;
        if (text.contains(" ")) score += 1;
        if (Objects.equals(text, text.toUpperCase())) score += 1;

        return score;
    }

    private void runConsensus(String id, String name, ScannerListener listener) {
        idHistory.add(id);
        nameHistory.add(name);
        if (idHistory.size() > Constants.REQUIRED_CONSENSUS) {
            idHistory.remove(0);
            nameHistory.remove(0);
        }

        if (idHistory.size() == Constants.REQUIRED_CONSENSUS) {
            int idCount = Collections.frequency(idHistory, id);
            boolean namesStable = true;
            for (String n : nameHistory) {
                if (calculateSimilarity(name, n) < Constants.SIMILARITY_THRESHOLD) {
                    namesStable = false;
                    break;
                }
            }

            if (idCount == Constants.REQUIRED_CONSENSUS && namesStable) {
                // Final sanity check — reject OCR garbage before confirming.
                if (!isPlausibleName(name)) {
                    listener.onScanning("ID found. Waiting for clearer name...");
                    return;
                }
                lastConfirmedId = id;
                lastConfirmedName = name;
                listener.onVerified(name, id);
            } else {
                listener.onStabilityRequired();
            }
        }
    }

    // accepts ALL CAPS / Title Case / all lower, but rejects random mixed-case
    // garbage like "ELOzIOTLE panssyou pry" which usually means OCR misread something
    private boolean isPlausibleName(String name) {
        if (name == null || name.trim().isEmpty()) return false;

        // Length bounds — real names on Aadhaar are within this range.
        if (name.length() < 3 || name.length() > 60) return false;

        // Names never contain digits.
        if (name.matches(".*\\d.*")) return false;

        // Only allow letters, spaces, dots, and hyphens.
        if (!name.matches("[a-zA-Z .\\-]+")) return false;

        // At least one word must be 2+ chars and not a repeated-character string.
        String[] words = name.trim().split("\\s+");
        int plausibleWords = 0;
        for (String word : words) {
            if (word.length() >= 2 && !word.matches("(.)\\1+")) {
                plausibleWords++;
            }
        }
        if (plausibleWords < 1) return false;

        // Case-pattern check: real names are all-caps, all-lower, or Title Case.
        // Randomly mixed case (e.g. "ELOzIOTLE") is OCR garbage.
        boolean hasUpper = !name.equals(name.toLowerCase());
        boolean hasLower = !name.equals(name.toUpperCase());

        if (hasUpper && hasLower) {
            // Mixed case is only acceptable if every word starts with a capital
            // and the rest are lowercase — i.e. strict Title Case.
            boolean isTitleCase = true;
            for (String word : words) {
                if (word.length() > 0 && !Character.isUpperCase(word.charAt(0))) {
                    isTitleCase = false;
                    break;
                }
                // Check the rest of the word is lowercase.
                if (word.length() > 1 && !word.substring(1).equals(word.substring(1).toLowerCase())) {
                    isTitleCase = false;
                    break;
                }
            }
            if (!isTitleCase) return false;
        }

        return true;
    }

    private double calculateSimilarity(String s1, String s2) {
        int dist = levenshtein(s1.toLowerCase(), s2.toLowerCase());
        return 1.0 - ((double) dist / Math.max(s1.length(), s2.length()));
    }

    private int levenshtein(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) costs[j] = j;
                else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1))
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    public String formatAadhaar(String id) {
        if (id == null || id.length() != 12) return id;
        return id.substring(0, 4) + " " + id.substring(4, 8) + " " + id.substring(8, 12);
    }

    public String getLastConfirmedId() { return lastConfirmedId; }
    public String getLastConfirmedName() { return lastConfirmedName; }

    private static class ScoredLine {
        String text;
        int score;
        int distToId;

        ScoredLine(String text, int score, int distToId) {
            this.text = text;
            this.score = score;
            this.distToId = distToId;
        }
    }
}