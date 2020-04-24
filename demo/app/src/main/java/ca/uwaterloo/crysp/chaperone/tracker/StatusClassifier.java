package ca.uwaterloo.crysp.chaperone.tracker;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ca.uwaterloo.crysp.chaperone.tracker.classifiers.dtc1;
import ca.uwaterloo.crysp.chaperone.tracker.classifiers.dtc2;
import ca.uwaterloo.crysp.chaperone.tracker.classifiers.dtc3;

public class StatusClassifier {
    private final static int DEPARTURE_SLOT_SIZE = 3;
    private final static int ABSENCE_SLOT_SIZE = 4;
    private int[] codedPattern0 = {200, 211, 201};
    private int[] codedPattern1 = {200, 0, 210};
    public final static int NO_SIGNAL = 0;
    public final static int DEPARTURE_SIGNAL = 1;
    public final static int ABSENCE_SIGNAL = 2;
    public final static int BOTH_SIGNAL = 3;

    private List<int[]> historyClassification;

    public StatusClassifier() {
        historyClassification = new ArrayList<>();
    }

    public int addFeatures(List<Float> features) {
        double[] featureArray = new double[features.size()];
        for(int i = 0; i < features.size(); ++i) {
            featureArray[i] = (double) features.get(i);
        }
        // Log.d("classifier", String.format("feature size: %d", featureArray.length));
        int[] results = {
                dtc1.predict(featureArray),
                dtc2.predict(featureArray),
                dtc3.predict(featureArray)
        };
        historyClassification.add(results);
        Log.d("classifier", String.format("classification result: %d, %d, %d",
                results[0], results[1], results[2]));
        if(historyClassification.size() < Math.max(DEPARTURE_SLOT_SIZE, ABSENCE_SLOT_SIZE)) return NO_SIGNAL;
        boolean f1 = DepartureDetection(getPatterns(historyClassification, DEPARTURE_SLOT_SIZE));
        boolean f2 = AbsenceDetection(getPatterns(historyClassification, ABSENCE_SLOT_SIZE));

        if(f1) {
            if(f2) return BOTH_SIGNAL;
            else return DEPARTURE_SIGNAL;
        } else {
            if(f2) return ABSENCE_SIGNAL;
            else return NO_SIGNAL;
        }
    }

    public boolean AbsenceDetection(int[][] patterns) {
        int score = 0;
        for(int[] pattern: patterns) {
            score += pattern[2];
        }
        if(score <= 0.4 * patterns.length && patterns[patterns.length - 1][2] == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean DepartureDetection(int[][] patterns) {
        int cur = encodePattern(patterns[patterns.length - 1]);
        boolean flag = true;
        for(int pt: codedPattern1) {
            if(pt == cur) flag = false;
        }
        if(flag) return false;
        int score = 0;
        for(int i = patterns.length - 2; i >= 0; --i) {
            cur = encodePattern(patterns[i]);
            for(int pt: codedPattern0) {
                if(pt == cur) {
                    score++;
                    break;
                }
            }
        }
        if(score >= patterns.length - 2 && patterns[patterns.length - 1][2] == 0) return true;
        else return false;
    }

    private int encodePattern(int[] pattern) {
        return pattern[0] * 100 + pattern[1] * 10 + pattern[2];
    }

    private int[][] getPatterns(List<int[]> patterns, int size) {
        int length = patterns.size();
        int[][] results = new int[size][patterns.get(0).length];
        for(int i = length - size; i < length; ++i) {
            results[i - length + size] = patterns.get(i);
        }
        return results;
    }
}
