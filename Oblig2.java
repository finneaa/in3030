import java.util.concurrent.*;
import java.util.*;


/* Oblig 2 i In3030
 * by Finn Eivind Aasen (finneaa)
 * Deadline: 4.03.2019 23:59:00
 * "Parallelization	of	Matrix	Multiplication"
 */

public class Oblig2{


  final int seed;
  int n_cores;
  int n_threads;

  double[][] a;
  double[][] b;
  double[][] aT;
  double[][] bT;

  int n;
  int[] nValues;

  ArrayList<double[][]> results_storage;
  ArrayList<double[][]> results_storageAT;
  ArrayList<double[][]> results_storageBT;
  boolean[] seq_parCompare;

  ExecutorService executor;

  double[] timePara;
	double[] timeSeq;
  int timeParaI;
  int timeSeqI;
  int seq_parCompareI;

  // Oblig2 constructor
	public Oblig2(int threads) {
		nValues = new int[]{10,100,200,500,1000};    //Change values inside this array to change testing matrix sizes
		seed = (int)System.nanoTime();
		results_storage = new ArrayList<double[][]>();
    results_storageAT = new ArrayList<double[][]>();
    results_storageBT = new ArrayList<double[][]>();
    seq_parCompare = new boolean[nValues.length * 3];
    seq_parCompareI = 0;

		n_cores = Runtime.getRuntime().availableProcessors();
		if(threads == 0) { n_threads = n_cores; }
		else { n_threads = threads; }


		timePara = new double[nValues.length * 3];
		timeSeq = new double[nValues.length * 3];
    timeParaI = 0;
    timeSeqI = 0;
	}



  private double[][] transposeMatrix(double [][] m){
        double[][] temp = new double[m[0].length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
                temp[j][i] = m[i][j];
        return temp;
    }

  public void execute() {
    Oblig2Precode preC = new Oblig2Precode();
		for(int i = 0; i < nValues.length; i++) {
      n = nValues[i];
      double[][] a = Oblig2Precode.generateMatrixA(seed, n);
		  double[][] b = Oblig2Precode.generateMatrixB(seed, n);
      double[][] aT = transposeMatrix(a);
      double[][] bT = transposeMatrix(b);
			solveSeq(a,b,i,preC,"N");
      solveSeq(aT,b,i,preC,"AT");
      solveSeq(a,bT,i,preC,"BT");
			solvePara(a,b,i,preC,"N");
      solvePara(aT,b,i,preC,"AT");
      solvePara(a,bT,i,preC,"BT");
		} printReport();
	}


  private void solveSeq(double[][] a, double[][] b,int run_number,Oblig2Precode preC, String testType){
    double[][] c = new double[n][n];
    double start = System.nanoTime();
    if(testType.equals("AT")){
      for(int i=0;i<n;i++){
  			for(int j=0;j<n;j++){
  				for(int k=0;k<n;k++){
  					c[i][j] += a[k][i] * b[k][j];
          }
        }
      }
    }else if(testType.equals("BT")){
      for(int i=0;i<n;i++){
  			for(int j=0;j<n;j++){
  				for(int k=0;k<n;k++){
  					c[i][j] += a[i][k] * b[j][k];
          }
        }
      }
    } else{
      for(int i=0;i<n;i++){
  			for(int j=0;j<n;j++){
  				for(int k=0;k<n;k++){
  					c[i][j] += a[i][k] * b[k][j];
          }
        }
      }
    }
    double end = System.nanoTime();
    timeSeq[timeSeqI] = (end-start)/1000000000;
    timeSeqI++;

    if(testType.equals("N")){
      results_storage.add(c);
      preC.saveResult(seed, Oblig2Precode.Mode.SEQ_NOT_TRANSPOSED, c);
    }else if(testType.equals("AT")){
      results_storageAT.add(c);
      preC.saveResult(seed, Oblig2Precode.Mode.SEQ_A_TRANSPOSED, c);
    }else if(testType.equals("BT")){
      results_storageBT.add(c);
      preC.saveResult(seed, Oblig2Precode.Mode.SEQ_B_TRANSPOSED, c);
    }


  }

  private void solvePara(double[][] a, double[][] b, int run_number, Oblig2Precode preC, String testType){
    double[][] c;

    double start = System.nanoTime();
    c = parallelMult(a,b,n_threads,testType);
		double end = System.nanoTime();
    timePara[timeParaI] = (end-start)/1000000000;
    timeParaI++;


    if(testType.equals("N")){
      seq_parCompare[seq_parCompareI] = compareMatrixS(c,results_storage.get(run_number));
      seq_parCompareI ++;
      preC.saveResult(seed, Oblig2Precode.Mode.PARA_NOT_TRANSPOSED, c);
    }else if(testType.equals("AT")){
      seq_parCompare[seq_parCompareI] = compareMatrixS(c,results_storageAT.get(run_number));
      seq_parCompareI ++;
      preC.saveResult(seed, Oblig2Precode.Mode.PARA_A_TRANSPOSED, c);
    }else if(testType.equals("BT")){
      seq_parCompare[seq_parCompareI] = compareMatrixS(c,results_storageBT.get(run_number));
      seq_parCompareI ++;
      preC.saveResult(seed, Oblig2Precode.Mode.PARA_B_TRANSPOSED, c);
    }

  }


  public double[][] parallelMult(double[][] A, double[][] B, int threadNumber, String testType) {
        double[][] C = new double[A.length][B[0].length];
        executor = Executors.newFixedThreadPool(threadNumber);
        List<Future<double[][]>> list = new ArrayList<Future<double[][]>>();

        int part = A.length / threadNumber;
        if (part < 1) {
            part = 1;
        }
        int k = 0;
        for (int j = 0; j < threadNumber; j ++) {
            System.err.println(k);
            if(j == threadNumber -1){
              Callable<double[][]> worker = new LineMultiplier(A, B, k, A.length,testType);
              Future<double[][]> submit = executor.submit(worker);
              list.add(submit);
            }else{
              Callable<double[][]> worker = new LineMultiplier(A, B, k, k+part, testType);
              Future<double[][]> submit = executor.submit(worker);
              list.add(submit);
            }
            k += part;
        }


        // now retrieve the result
        int start = 0;
        double CF[][];
        for (Future<double[][]> future : list) {
            try {
                CF = future.get();
                if(start == part * (threadNumber-1)){
                  for (int i=start; i < A.length; i += 1) {
                      C[i] = CF[i];
                  }
                }else{
                  for (int i=start; i < start+part; i += 1) {
                      C[i] = CF[i];
                  }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            start+=part;
        }
        executor.shutdown();

        return C;
    }

    public boolean compareMatrixS(double[][] arr1, double[][] arr2){
      if (arr1 == null) {
        return (arr2 == null);
      }
      if (arr2 == null) {
        return false;
      }
      if (arr1.length != arr2.length) {
        return false;
      }
      for (int i = 0; i < arr1.length; i++) {
        if (!Arrays.equals(arr1[i], arr2[i])) {
          return false;
        }
      }
      return true;
    }

    public static void main(String[] args) {
  		if(args.length != 1) {
  			System.out.println("Usage: Oblig1 <#threads>");
        System.out.print("<#threads> must be int values");
  			System.out.print("Enter 0 for <#threads> if you want it to be ");
  			System.out.print("chosen automatically.\n must use a number that all tested matrix sizes is dividable by, example 1, 5 or 10");
  			return;
  		}
  		Oblig2 experiment = new Oblig2(Integer.parseInt(args[0]));
  		System.out.print("Please wait! Report will be printed out once execution is done.\n");
  		experiment.execute();
  	}

    public void printReport(){
      System.out.println("Report: test done with " + n_threads + " threads");
      for(int i = 0; i < nValues.length;i++){
        System.out.println("Trail with matrix size: " + nValues[i] + "x" + nValues[i]);
        System.out.println("Seq time not transposed: " + timeSeq[0 + (i*3)] + " seconds");
        System.out.println("Seq time A transposed: " + timeSeq[1 + (i*3)] + " seconds");
        System.out.println("Seq time B transposed: " + timeSeq[2 + (i*3)] + " seconds");
        System.out.println("Para time not transposed: " + timePara[0 + (i*3)] + " seconds");
        System.out.println("Para time A transposed: " + timePara[1 + (i*3)] + " seconds");
        System.out.println("Para time B transposed: " + timePara[2 + (i*3)] + " seconds");
        System.out.println("\nCompare:");
        System.out.println("Not Transposed: " + seq_parCompare[0 + (i*3)]);
        System.out.println("A Transposed: " + seq_parCompare[1 + (i*3)]);
        System.out.println("B Transposed: " + seq_parCompare[2 + (i*3)]);
        System.out.println("\nSpeedup:");
        System.out.println("Not Transposed: " + (timeSeq[0 + (i*3)] / timePara[0 + (i*3)]) + " seconds");
        System.out.println("A Transposed: " + (timeSeq[1 + (i*3)] / timePara[1 + (i*3)]) + " seconds");
        System.out.println("B Transposed: " + (timeSeq[2 + (i*3)] / timePara[2 + (i*3)]) + " seconds");
      }

    }

}
