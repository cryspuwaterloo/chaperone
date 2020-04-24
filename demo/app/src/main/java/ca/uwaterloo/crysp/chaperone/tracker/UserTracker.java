package ca.uwaterloo.crysp.chaperone.tracker;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.uwaterloo.crysp.chaperone.tracker.helpers.Kalman1D;
import ca.uwaterloo.crysp.chaperone.tracker.settings.TrackerSettings;
import ca.uwaterloo.crysp.chaperone.tracker.settings.AcousticSettings;
import ca.uwaterloo.crysp.chaperone.tracker.helpers.MathHelper;

public class UserTracker {
    // Constants
    private final static int OFFSET_FRAME_NUMBER = 5; // Only use the estimation after N frames
    private final static float DIRECTION_TOLERANCE = 2;
    private final static float MAG_MAG = 1.15f; // magnification of magnitude;
    private final static float DISTANCE_BOUND = 5;
    private final static int MAX_DISTANCE_INDEX = 150; // 150 * 0.014 ~ 2m
    private final static int WINDOW_SIZE = 5;

    private float dUnit;
    private float tUnit;
    private TrackerSettings ts;
    private Kalman1D kalmanFilter;

    // History data
    private List<float[]> rawMagnitude;
    private List<float[]> diffMagnitude;
    private List<Boolean> wasObserved;
    private List<List<int[]>> historyCandidates;
    private List<Float> historyEstDistance;
    private List<Float> historyEstSpeed;
    private List<Float> historyObsDistance;

    private boolean firstFlag;
    private boolean noiseCancelling;
    private int firstOffset;
    private int continuousNoiseSlots;
    private int continuousNoObservations;



    private void initialize() {
        firstFlag = false;
        firstOffset = 0;
        continuousNoiseSlots = 0;
        continuousNoObservations = 0;
        rawMagnitude = new ArrayList<float[]>();
        diffMagnitude = new ArrayList<float[]>();
        wasObserved = new ArrayList<Boolean>();
        historyCandidates = new ArrayList<List<int[]>>();
        historyEstDistance = new ArrayList<Float>();
        historyEstSpeed = new ArrayList<Float>();
        historyObsDistance = new ArrayList<Float>();
    }

    public float getObservation(int index) {
        return historyObsDistance.get(index);
    }

    public float getEstimation(int index) {
        return historyEstDistance.get(index);
    }

    public List<List<int[]>> getHistoryCandidates() {
        return historyCandidates;
    }

    public List<float[]> getAllRawMagnitude() {
        return rawMagnitude;
    }

    public int getFirstOffset() {
        return firstOffset;
    }

    public int getMaxTimeIndex() {
        return historyEstDistance.size() - 1;
    }

    public float add(float[] rawData) {
        float[] data = Arrays.copyOfRange(rawData, 0, MAX_DISTANCE_INDEX);

        rawMagnitude.add(data);

        if (!firstFlag) {
            if (!isRawNoise(data)) {
                firstFlag = true;
            }
            else {
                firstOffset = firstOffset + 1;
                System.out.println("first frame is noise");
            }
            return 0;
        }
        float[] delta = new float[data.length];
        try {
            delta = MathHelper.absSubtract(data, rawMagnitude.get(rawMagnitude.size() - 2));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO: Extract static objects

        if (noiseCancelling && (isRawNoise(data) || isDiffNoise(delta))) {
            diffMagnitude.add(new float[data.length]);
            continuousNoiseSlots = continuousNoiseSlots + 1;
            // System.out.println("noise!!");
            // TODO: Add log information later
        } else {
            diffMagnitude.add(delta);
            continuousNoiseSlots = 0;
        }
        // Candidate selection code
        long t0 = System.currentTimeMillis();
        List<int[]> candidates = detectCandidates(diffMagnitude.get(diffMagnitude.size() - 1));
        for(int[] candi: candidates) {
            Log.i("Candidate", String.format("cand: %d, %d, %d, %f", candi[0], candi[1],candi[2], delta[candi[2]]));
        }


        System.out.println(String.format("candidate detection time: %d ms", System.currentTimeMillis()- t0));
        historyCandidates.add(candidates);
        t0 = System.currentTimeMillis();
        float curDistance = estimateDistance(candidates);
        System.out.println(String.format("distance estimation time: %d ms", System.currentTimeMillis()- t0));
        System.out.println((rawMagnitude.size() - 1) + ": " + curDistance + "," + curDistance*dUnit + "," + historyObsDistance.get(historyObsDistance.size() - 1));
        return curDistance;
    }


    public UserTracker() {
        // read the default settings
        AcousticSettings as = new AcousticSettings();
        dUnit = as.getDistanceUnit();
        tUnit = as.getTimeUnit();
        ts = new TrackerSettings();
        noiseCancelling = true;

        kalmanFilter = new Kalman1D(ts.getKalmanQ(), ts.getKalmanR(), ts.getKalmanDefaultDistance(), 1.0f);
        initialize();
    }

    // =======================================================
    //  Distance index converter
    // =======================================================

    public float indexToDistance(int index) {
        return (float) index * dUnit;
    }

    public int distanceToIndex(float distance) {
        return (int) Math.floor(distance / dUnit);
    }

    // =======================================================
    //  Noise detection
    // =======================================================

    private boolean isRawNoise(float[] col) {
        return MathHelper.median(Arrays.copyOfRange(col, ts.getFarIndex(), col.length)) > ts.getFarRawNoise();
    }

    private boolean isDiffNoise(float[] col) {
        return MathHelper.average(Arrays.copyOfRange(col, ts.getFarIndex(), col.length)) > ts.getFarDiffNoise();
    }

    private boolean hasGlitch(float[] col) {
        int size = 0;
        for (float elem: col)
            if (elem > ts.getGlitchNoise()) size++;
        return size >= ts.getGlitchSize();
    }

    // =======================================================
    //  Candidate selection algorithms
    // =======================================================

    // candidate format: {startIndex, size, peakIndex}
    private List<int[]> detectCandidates(float[] col) {
        // exclude the direct transmission
        float[] trimmedCol = new float[col.length];
        System.arraycopy(col, 0, trimmedCol, 0, col.length);

        for(int i = 0; i < ts.getEdgeIndex(); ++i) trimmedCol[i] = 0.0f;
        for(int i = ts.getEdgeIndex(); i < ts.getStartIndex() + 1; ++ i)
            if (trimmedCol[i] < ts.getDirectHighMagnitude()) trimmedCol[i] = 0.0f;

        List<Integer> outliers = MathHelper.upperMADIndex(trimmedCol, ts.getOutlierMultiplier());
        List<int[]> candidates = new ArrayList<int[]>();
        // Filter out disqualified outliers
        List<Integer> filteredOutliers = filterOutliers(outliers, col);
        // clustering the outliers - 1D clustering
        int formerIndex = -1;
        for(int index: filteredOutliers) {
            if (candidates.size() == 0) {
                int[] tmp = {index, 1, index};
                candidates.add(tmp);
            } else if (index - formerIndex > ts.getClusterTolerance()) {
                int[] tmp = {index, 1, index};
                candidates.add(tmp);
            } else {
                int[] tmp = candidates.get(candidates.size() - 1);
                // update size and peak
                tmp[1] += index - formerIndex;
                if (trimmedCol[tmp[2]] < trimmedCol[index]) tmp[2] = index;
                candidates.set(candidates.size() - 1, tmp);
            }
            formerIndex = index;
        }
        return candidates;
    }

    private List<Integer> filterOutliers(List<Integer> outliers, float[] col) {
        List<Integer> results = new ArrayList<Integer>();
        if (outliers.size() == 0) return results;
        for (int elem: outliers) {
            float value = col[elem]; // choose the peak
            for (float[] pair: ts.getDistanceThreshold()) {
                if (elem <= distanceToIndex(pair[0])) {
                    if (value >= pair[1]) {
                        results.add(elem);

                    }
                    break;
                }
            }
        }
        return results;
    }

    private float estimateDistance(List<int[]> candidates) {
        // Check the previous distance and speed
        float estXp;
        float estVp;
        int observation = -1;
        float maxMagnitude = -1;
        int power = 0;
        int curDirection = 0;
        if (historyEstDistance.size() > 0) estXp = historyEstDistance.get(historyEstDistance.size() - 1);
        else estXp = ts.getKalmanDefaultDistance();
        if (historyEstSpeed.size() < OFFSET_FRAME_NUMBER) estVp = 0.0f;
        else estVp = historyEstSpeed.get(historyEstSpeed.size() - 1);
        if (historyEstSpeed.size() >= OFFSET_FRAME_NUMBER) {
            curDirection = speedToDirection(estVp);
            for(int i = historyEstSpeed.size() - 2; i > 0; --i) {
                int tmpDirection = speedToDirection(historyEstSpeed.get(i));
                if (tmpDirection == curDirection && curDirection != 0) power++;
                else break;
            }
        }
        for (int i = 0; i < candidates.size(); ++i) {
            int startIndex = candidates.get(i)[0];
            int size = candidates.get(i)[1];
            int peakIndex = candidates.get(i)[0];
            float discountedMaxMovement = Math.max((float)ts.getMaxMovementIndex() * (float)Math.pow(ts.getMovementDiscount(), power),
                    (float)ts.getMinMovementIndex());
            if(isValidMovement(curDirection, startIndex, startIndex + size, estXp + estVp,
                    (float)ts.getMaxMovementIndex(), discountedMaxMovement)) {
                float magn = diffMagnitude.get(diffMagnitude.size() - 1)[peakIndex];
                if (magn > maxMagnitude * MAG_MAG) {
                    observation = peakIndex;
                    maxMagnitude = magn;
                }
            }
        }
        float observed_distance;
        if (observation != -1) {
            observed_distance = (float)observation;
            continuousNoObservations = 0;
        } else if (historyEstDistance.size() == 0) {
            observed_distance = ts.getKalmanDefaultDistance();
        } else {
            observed_distance = historyEstDistance.get(historyEstDistance.size() - 1);
            continuousNoObservations++;
            System.out.println("No observation");
            if (continuousNoObservations > 2) {
                observed_distance = historyEstDistance.get(historyEstDistance.size() - 2);
            }
        }

        wasObserved.add(observation != -1);
        long t0 = System.currentTimeMillis();
        System.out.println(observed_distance);
        float[] results = kalmanFilter.add(observed_distance);
        System.out.println(String.format("Kalman time measurement: %d ms", System.currentTimeMillis()- t0));
        float resultDistance = results[0];
        float resultSpeed = results[1];
        if (historyEstDistance.size() > 1000) {
            System.out.println("Obs: " + observed_distance + " Est_d: " + resultDistance + " Est_v" + resultSpeed);
        }
        if (resultDistance < DISTANCE_BOUND) {
            resultDistance = DISTANCE_BOUND;
            resultSpeed = 0;
        }
        historyEstSpeed.add(resultSpeed);
        historyEstDistance.add(resultDistance);
        historyObsDistance.add(observed_distance);
        return resultDistance;
    }

    private boolean isValidMovement(int curVd, float moveSt, float moveEd, float estDist, float maxRange, float minRange) {
        if (curVd == 0) {
            return (Math.abs(moveSt - estDist) < maxRange) || (Math.abs(moveEd - estDist) < maxRange);
        } else if (curVd < 0) {
            return (moveEd > estDist - maxRange) && (moveSt < estDist + minRange);
        } else {
            return (moveEd > estDist - minRange) && (moveSt < estDist + maxRange);
        }
    }

    private int speedToDirection(float speed) {
        if (speed > DIRECTION_TOLERANCE) return 1;
        else if (speed < -DIRECTION_TOLERANCE) return -1;
        else return 0;
    }

    public int getMaxIndex() {
        return diffMagnitude.size() - 1;
    }

    // =======================================================
    //  Feature extraction
    // =======================================================

    public List<Float> extractFeatures() {
        float[][] rawTiles = selectRawTiles(10, 50);
        float[][] diffTiles = selectDiffTiles(10, 50);
        List<Float> features = new ArrayList<>();
        features.add(getWinEstDistance());
        features.add(getWinObsDistance());
        features.add(getWinEstDistanceDev());
        features.add(getWinObsDistanceDev());
        features.add(getWinObserved());
        features.add(getWinAverageEstSpeed());
        features.add(getWinAverageObsSpeed());
        features.add(getWinEstSpeed());
        features.add((float)getWinFluctuation());
        features.add(getAverageMagnitude(diffTiles));
        features.add(getMedianMagnitude(diffTiles));
        features.add(getTileHotRate(diffTiles, 200)); // TODO: define threshold in profile
        float[] stds = getStdMagnitude(rawTiles);
        // Log.d("UserTracker", String.format("size of std: %d", stds.length));
        for(float value: stds) {
            features.add(value);
        }
        return features;
    }

    public float getWinEstDistance() {
        float result = 0.0f;
        int length = historyEstDistance.size();
        for(int index = length - WINDOW_SIZE; index < length; ++index) {
            result += historyEstDistance.get(index);
        }
        return result/WINDOW_SIZE;
    }

    public float getWinEstDistanceDev() {
        float est = getWinEstDistance();
        float med = MathHelper.median(historyEstDistance);
        return (est - med) / med;
    }

    public float getWinObsDistance() {
        float result = 0.0f;
        int length = historyObsDistance.size();
        for(int index = length - WINDOW_SIZE; index < length; ++index) {
            result += historyObsDistance.get(index);
        }
        return result/WINDOW_SIZE;
    }

    public float getWinObsDistanceDev() {
        float obs = getWinObsDistance();
        float med = MathHelper.median(historyObsDistance);
        return (obs - med) / med;
    }

    public float getWinObserved() {
        for(int i = wasObserved.size() - WINDOW_SIZE; i < wasObserved.size(); ++i) {
            if(wasObserved.get(i)) {
                return 1;
            }
        }
        return 0;
    }

    public float getWinEstSpeed() {
        float result = 0.0f;
        int length = historyEstSpeed.size();
        for(int index = length - WINDOW_SIZE; index < length; ++index) {
            result += historyEstSpeed.get(index);
        }
        return result/WINDOW_SIZE;
    }

    public float getWinAverageEstSpeed() {
        int length = historyEstDistance.size();
        return (historyEstDistance.get(length - 1) - historyEstDistance.get(length - WINDOW_SIZE)) / WINDOW_SIZE;
    }

    public float getWinAverageObsSpeed() {
        int length = historyObsDistance.size();
        return (historyObsDistance.get(length - 1) - historyObsDistance.get(length - WINDOW_SIZE)) / WINDOW_SIZE;
    }

    public int getWinFluctuation() {
        int length = historyEstSpeed.size();
        int prevDirection = speedToDirection(historyEstSpeed.get(length - WINDOW_SIZE));
        int counter = 0;
        for(int i = length - WINDOW_SIZE + 1; i < length; ++i) {
            int curDirection = speedToDirection(historyEstSpeed.get(i));
            if (curDirection * prevDirection < 0) {
                counter++;
            }
            prevDirection = curDirection;
        }
        return counter;
    }

    public float[][] selectRawTiles(int st, int ed) {
        float[][] selected = new float[WINDOW_SIZE][ed - st + 1];
        int length = rawMagnitude.size();
        for(int i = length - WINDOW_SIZE; i < length; ++i) {
            System.arraycopy(rawMagnitude.get(i), st, selected[i - length + WINDOW_SIZE], 0, ed - st + 1);
        }
        return selected;
    }

    public float[][] selectDiffTiles(int st, int ed) {
        float[][] selected = new float[WINDOW_SIZE][ed - st + 1];
        int length = diffMagnitude.size();
        for(int i = length - WINDOW_SIZE; i < length; ++i) {
            System.arraycopy(rawMagnitude.get(i), st, selected[i - length + WINDOW_SIZE], 0, ed - st + 1);
        }
        return selected;
    }

    public float getAverageMagnitude(float[][] selected) {
        return MathHelper.average(selected);
    }

    public float getMedianMagnitude(float[][] selected) {
        return MathHelper.average(MathHelper.flatten2d(selected));
    }

    public float getTileHotRate(float[][] selected, float threshold) {
        int count = 0;
        for(float[] row: selected) {
            for(float value: row) {
                if (value > threshold) {
                    count++;
                }
            }
        }
        return (float)count / selected.length / selected[0].length;
    }

    public float[] getStdMagnitude(float[][] selected) {
        return MathHelper.colStd(selected);
    }


    public void disableNoiseDetection() {
        Log.i("User Tracker", "Disable Noise Detection!");
        noiseCancelling = false;
    }

    public void enableNoiseDetection() {
        Log.i("User Tracker", "Enable Noise Detection!");
        noiseCancelling = true;
    }
}