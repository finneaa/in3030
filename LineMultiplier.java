import java.util.ArrayList;
import java.util.concurrent.Callable;

public class LineMultiplier implements Callable<double[][]> {
    double[][] A;
    double[][] B;
    int start;
    int end;
    String testType;
    public double[][] C;

    public LineMultiplier(double[][] a, double[][] b, int s, int e, String testType) {
        A = a;
        B = b;
        C = new double[a.length][b[0].length];
        start = s;
        end = e;
        this.testType = testType;
    }

    @Override
    public double[][] call() {
        if(testType.equals("AT")){
          for (int i = start; i < end; i++) {
              for (int k = 0; k < B.length; k++) {
                  for (int j = 0; j < B[0].length; j++) {
                      C[i][j] += A[k][i] * B[k][j];
                  }
              }
          }
        } else if(testType.equals("BT")){
          for (int i = start; i < end; i++) {
              for (int k = 0; k < B.length; k++) {
                  for (int j = 0; j < B[0].length; j++) {
                      C[i][j] += A[i][k] * B[j][k];
                  }
              }
          }
        }else{
          for (int i = start; i < end; i++) {
              for (int k = 0; k < B.length; k++) {
                  for (int j = 0; j < B[0].length; j++) {
                      C[i][j] += A[i][k] * B[k][j];
                  }
              }
          }
        }

        return C;
    }
}
