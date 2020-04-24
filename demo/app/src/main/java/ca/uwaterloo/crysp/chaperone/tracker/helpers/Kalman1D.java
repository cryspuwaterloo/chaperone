package ca.uwaterloo.crysp.chaperone.tracker.helpers;

import java.util.ArrayList;
import java.util.List;

public class Kalman1D {
    private float dt;
    private float[][] F = {{1, 1}, {0, 1}};
    // private SimpleMatrix G;
    private float[][] H = {{1, 0}};
    private float[][] x0;
    private float[][] Q;
    private float[][] R;
    private List<float[][]> xHat;
    private List<float[][]> pHat;

    public float[] add(float obs) {
        float[][] ob = {{obs}};
        try {
            float[][] xm = Fast2DMat.mult(F, xHat.get(xHat.size() - 1));
            float[][] pm = Fast2DMat.plus(Fast2DMat.mult(Fast2DMat.mult(F, pHat.get(pHat.size() - 1)),
                    Fast2DMat.transpose(F)), Q);
            // SimpleMatrix pm = F.mult(pHat.get(pHat.size() - 1)).mult(F.transpose()).plus(Q);
            float[][] inverted = Fast2DMat.inv(Fast2DMat.plus(Fast2DMat.mult(Fast2DMat.mult(H, pm),
                    Fast2DMat.transpose(H)), R));
            // SimpleMatrix inverted = H.mult(pm).mult(H.transpose()).plus(R).invert();
            float[][] K = Fast2DMat.mult(Fast2DMat.mult(pm, Fast2DMat.transpose(H)), inverted);
            // SimpleMatrix K = pm.mult(H.transpose()).mult(inverted);
            float[][] xt = Fast2DMat.plus(xm, Fast2DMat.mult(K, Fast2DMat.minus(ob, Fast2DMat.mult(H, xm))));
            // SimpleMatrix xt = xm.plus(K.mult(ob.minus(H.mult(xm))));
            float[][] pt = Fast2DMat.mult(Fast2DMat.minus(Fast2DMat.identity2d(), Fast2DMat.mult(K, H)), pm);
            // SimpleMatrix pt = SimpleMatrix.identity(2).minus(K.mult(H)).mult(pm);
            xHat.add(xt.clone());
            pHat.add(pt.clone());
            float[] results = {xt[0][0], xt[1][0]};
            return results;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Kalman1D(float q, float r, float d0, float p0) {
        // float[][] arrayG = {{dt * dt / 2}, {dt}};
        // G = new SimpleMatrix(arrayG);
        x0 = new float[][]{{d0}, {0}};
        Q = new float[][]{{q, 0}, {0, q}};
        R = new float[][]{{r}};
        xHat = new ArrayList<float[][]>();
        xHat.add(x0.clone());
        float[][] arrayP0 = {{p0, 0}, {0, p0}};
        pHat = new ArrayList<float[][]>();
        pHat.add(arrayP0.clone());
    }
}
