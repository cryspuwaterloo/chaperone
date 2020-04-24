package ca.uwaterloo.crysp.chaperone.tracker.helpers;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MathHelper {

    public static float[] absSubtract(float[] current, float[] previous) throws Exception {
        if (current.length != previous.length) throw new Exception("the size of input data does not match");
        float[] results = new float[current.length];
        for(int i = 0; i < current.length; ++i) {
            results[i] = Math.abs(current[i] - previous[i]);
        }
        return results;
    }

    public static float[] flatten2d(float[][] values) {
        float[] flattened = new float[values.length*values[0].length];
        for(int i = 0; i < values.length; ++i) {
            System.arraycopy(values[i], 0, flattened, i * values[0].length, values[i].length);
        }
        return flattened;
    }

    public static float average(float[] values) {
        float result = 0.0f;
        for(float value: values) {
            result = result + value;
        }
        return result / values.length;
    }

    public static float average(float[][] values) {
        float result = 0.0f;
        for(float[] row: values) {
            for(float value: row)
            result = result + value;
        }
        return result / values.length / values[0].length;
    }

    public static float std(float[] values) {
        float mean = average(values);
        double result = 0.0;
        for(float value: values) {
            result += (value - mean) * (value - mean);
        }
        return (float) Math.sqrt(result / (values.length - 1));
    }

    public static float[] colStd(float[][] values) {
        float[] results = new float[values[0].length];
        Log.d("MathHelper", String.format("col_number=%d, row_number=%d,", values[0].length, values.length));
        try {
            float[][] tValues = Fast2DMat.transpose(values);
            for(int i = 0; i < tValues.length; ++i) {
                results[i] = std(tValues[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    public static float median(float[] values) {
        float[] sorted = new float[values.length];
        System.arraycopy(values,0, sorted,0, sorted.length);
        Arrays.sort(sorted);

        if (sorted.length % 2 == 0) {
            return (sorted[(sorted.length / 2) - 1] + sorted[sorted.length / 2]) / 2;
        } else {
            return sorted[sorted.length / 2];
        }
    }

    public static float median(List<Float> values) {
        float[] sorted = new float[values.size()];
        for(int i = 0; i < values.size(); ++i) {
            sorted[i] = values.get(i);
        }
        // System.arraycopy(values,0, sorted,0, sorted.length);
        Arrays.sort(sorted);

        if (sorted.length % 2 == 0) {
            return (sorted[(sorted.length / 2) - 1] + sorted[sorted.length / 2]) / 2;
        } else {
            return sorted[sorted.length / 2];
        }
    }

    public static List<Integer> upperMADIndex(float[] values, float multiplier) {
        float[] dev = new float[values.length];
        List<Integer> indices = new ArrayList<Integer>();
        float med = median(values);
        for(int i = 0; i < dev.length; ++i) {
            dev[i] = Math.abs(values[i] - med);
        }
        float mdev = median(dev);

        if (mdev < 1e-6f) {
            return indices;
        }
        for(int i = 0; i < dev.length; ++i) {
            if (dev[i] / mdev > multiplier) {
                indices.add(i);
            }
        }
        return indices;
    }

    public static float[] upperMAD(float[] values, float multiplier) {
        List<Integer> indices = upperMADIndex(values, multiplier);
        float[] results = new float[indices.size()];
        for(int i = 0; i < results.length; ++i) results[i] = values[indices.get(i)];
        return results;
    }
}

