package ca.uwaterloo.crysp.chaperone.tracker.helpers;

public class Fast2DMat {
    public static float[][] mult(float[][] a, float[][] b) throws Exception {
        int aRow = a.length;
        int bRow = b.length;
        if(aRow > 2 || aRow <= 0 || bRow > 2 || bRow <= 0) {
            throw new Exception("the input is not 2d matrix");
        }
        int aCol = a[0].length;
        int bCol = b[0].length;
        if(aCol > 2 || aCol <= 0 || bCol > 2 || bCol <= 0) {
            throw new Exception("the input is not 2d matrix");
        }
        //float [][]result;
        if (aRow == 1 && aCol == 2 && bRow == 2 && bCol == 1) {
            float [][]result = {{a[0][0]*b[0][0] + a[0][1]*b[1][0]}};
            return result;
        } else if (aRow == 1 && aCol == 1 && bRow == 1 && bCol == 1) {
            float [][]result = {{a[0][0]*b[0][0]}};
            return result;
        } else if (aRow == 1 && aCol == 1 && bRow == 1 && bCol == 2) {
            float[][] result = {{a[0][0]*b[0][0], a[0][0]*b[0][1]}};
            return result;
        } else if (aRow == 2 && aCol == 1 && bRow == 1 && bCol == 2) {
            float[][] result = {{a[0][0]*b[0][0], a[0][0]*b[0][1]},
                    {a[1][0]*b[0][0], a[1][0]*b[0][1]}};
            return result;
        } else if (aRow == 2 && aCol == 2 && bRow == 2 && bCol == 1) {
            float[][] result = {{a[0][0]*b[0][0] + a[0][1]*b[1][0]},
                    {a[1][0]*b[0][0] + a[1][1]*b[1][0]}};
            return result;
        } else if (aRow == 2 && aCol == 1 && bRow == 1 && bCol == 1) {
            float[][] result = {{a[0][0]*b[0][0]}, {a[1][0]*b[0][0]}};
            return result;
        } else if (aRow == 1 && aCol == 2 && bRow == 2 && bCol == 2) {
            float[][] result = {{a[0][0]*b[0][0] + a[0][1]*b[1][0], a[0][0]*b[0][1] + a[0][1]*b[1][1]}};
            return result;
        } else if (aRow == 2 && aCol == 2 && bRow == 2 && bCol == 2) {
            float [][]result = {{a[0][0]*b[0][0] + a[0][1]*b[1][0], a[0][0]*b[0][1] + a[0][1]*b[1][1]},
                    {a[1][0]*b[0][0] + a[1][1]*b[1][0], a[1][0]*b[0][1] + a[1][1]*b[1][1]}};
            return result;
        } else throw new Exception("matrices do not match");
    }

    public static float[][] transpose(float[][] a) throws Exception {
        int aRow = a.length;
        if(aRow <= 0) throw new Exception("the input is not matrix");
        int aCol = a[0].length;
        if(aCol <= 0) throw new Exception("the input is not matrix");
        float[][] result = new float[aCol][aRow];
        for(int i = 0; i < aRow; ++i) {
            for (int j = 0; j < aCol; ++j) {
                result[j][i] = a[i][j];
            }
        }
        return result;
    }

    public static float[][] plus(float[][] a, float[][] b) throws Exception {
        int aRow = a.length;
        int bRow = b.length;
        if (aRow > 2 || aRow <= 0 || bRow > 2 || bRow <= 0) {
            throw new Exception("the input is not 2d matrix");
        }
        int aCol = a[0].length;
        int bCol = b[0].length;
        if (aCol > 2 || aCol <= 0 || bCol > 2 || bCol <= 0) {
            throw new Exception("the input is not 2d matrix");
        }
        if (aRow == 1 && aCol == 2 && bRow == 1 && bCol == 2) {
            float[][] result = {{a[0][0] + b[0][0], a[0][1] + b[0][1]}};
            return result;
        } else if (aRow == 1 && aCol == 1 && bRow == 1 && bCol == 1) {
            float[][] result = {{a[0][0] + b[0][0]}};
            return result;
        } else if (aRow == 2 && aCol == 1 && bRow == 2 && bCol == 1) {
            float[][] result = {{a[0][0] + b[0][0]}, {a[1][0] + b[1][0]}};
            return result;
        } else if (aRow == 2 && aCol == 2 && bRow == 2 && bCol == 2) {
            float[][] result = {{a[0][0] + b[0][0], a[0][1] + b[0][1]},
                    {a[1][0] + b[1][0], a[1][1] + b[1][1]}};
            return result;
        } else throw new Exception(String.format("matrices do not match: a(%d, %d), b(%d, %d)",
                aRow, aCol, bRow, bCol));
    }

    public static float[][] minus(float[][] a, float[][] b) throws Exception {
        int aRow = a.length;
        int bRow = b.length;
        if (aRow > 2 || aRow <= 0 || bRow > 2 || bRow <= 0) {
            throw new Exception("the input is not 2d matrix");
        }
        int aCol = a[0].length;
        int bCol = b[0].length;
        if (aCol > 2 || aCol <= 0 || bCol > 2 || bCol <= 0) {
            throw new Exception("the input is not 2d matrix");
        }
        if (aRow == 1 && aCol == 2 && bRow == 1 && bCol == 2) {
            float[][] result = {{a[0][0] - b[0][0], a[0][1] - b[0][1]}};
            return result;
        } else if (aRow == 1 && aCol == 1 && bRow == 1 && bCol == 1) {
            float[][] result = {{a[0][0] - b[0][0]}};
            return result;
        } else if (aRow == 2 && aCol == 1 && bRow == 2 && bCol == 1) {
            float[][] result = {{a[0][0] - b[0][0]}, {a[1][0] - b[1][0]}};
            return result;
        } else if (aRow == 2 && aCol == 2 && bRow == 2 && bCol == 2) {
            float[][] result = {{a[0][0] - b[0][0], a[0][1] - b[0][1]},
                    {a[1][0] - b[1][0], a[1][1] - b[1][1]}};
            return result;
        } else throw new Exception(String.format("matrices do not match: a(%d, %d), b(%d, %d)",
                aRow, aCol, bRow, bCol));
    }

    public static float[][] inv(float[][]a) throws Exception {
        int aRow = a.length;
        if(aRow <= 0) throw new Exception("the input is not matrix");
        int aCol = a[0].length;
        if(aCol <= 0) throw new Exception("the input is not matrix");
        if (aRow == 2 && aCol == 2) {
            if (a[0][0]*a[1][1] - a[0][1]*a[1][0] == 0) {
                throw new Exception("Inft matrix");
            } else {
                float mul = a[0][0]*a[1][1] - a[0][1]*a[1][0];
                float[][] result = {{a[1][1] / mul, -a[0][1] / mul},
                        {-a[1][0] / mul, a[0][0] / mul}};
                return result;
            }
        } else if (aRow == 1 && aCol == 1) {
            float[][] result = {{1/a[0][0]}};
            return result;
        } else throw new Exception("matrix dimension(s) do not match");
    }

    public static float[][] identity2d() {
        float[][] result = {{1, 0}, {0, 1}};
        return result;
    }
}
