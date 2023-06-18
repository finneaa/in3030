/**----------------------------------------------------------------------------
 * Brukernavn: finneaa
 * IN3030 - Spring 2019
 * Parallel Radix Sort
 * For all numbers in a[], we assume that 0 <= a[i] < 2^32

 * Abbreviations:
 * - Par      : Short for "parallel"
 * - Seq      : Short for "sequential"
 * - BITS     : A constant specifying an 'ideal' number of bits per digit
 * - digitLen : An array storing number of bits for every single digit
 * - ndigits  : Total number of digits (this equals digitLen.length)
 *---------------------------------------------------------------------------*/
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

class Oblig4 {
	public static void main(String[] args) {
		int N = 0;      //number of elements
		int ntests = 0; //number of tests

		if(args.length == 1) {
			N = Integer.parseInt(args[0]);
		} else if(args.length == 2) {
			N = Integer.parseInt(args[0]);
			ntests = Integer.parseInt(args[1]);
		} else {
			System.out.println("USAGE: java Oblig4 <N> <#TESTS>");
			System.out.println("Leave the 2nd field empty if you want it to be chosen automatically.");
			return;
		}
		new MultiRadix().runTests(N,ntests);
	}
}

class MultiRadix {
	int N;              //number of elements
	int nthreads;       //number of threads
	final int BITS = 6; //number of bits per digits

	/** Constructor */
	public MultiRadix() {
		this.nthreads = Runtime.getRuntime().availableProcessors();
	}

	/** @param ntests number of tests (for both sequential and parallel run) */
	public void runTests(int n, int ntests) {
		if(ntests <= 0) ntests = 5; //default value for ntests
		if(n > 0) this.N = n;
		else throw new RuntimeException("N cannot be less than 0.");

		/* Initialize other necessary variables */
		int[] a = new int[N];
		int[] seqResults = new int[N];
		double[] timeSeq = new double[ntests];
		double[] timePar = new double[ntests];
		final long SEED = System.nanoTime();
		Boolean same = true;

		/* Sequential tests */
		for(int i = 0; i < ntests; i++) {
			a = Oblig4Precode.generateArray(N, (int)SEED);
			timeSeq[i] = sortSeq(a);
		} testSort(a, "Seq");
		Oblig4Precode.saveResults(Oblig4Precode.Algorithm.SEQ, (int)SEED, a);
		System.arraycopy(a, 0, seqResults, 0, a.length);


		/* Parallel tests */
		for(int i = 0; i < ntests; i++) {
			a = Oblig4Precode.generateArray(N, (int)SEED);
			timePar[i] = sortPar(a);
		} testSort(a, "Par");
		Oblig4Precode.saveResults(Oblig4Precode.Algorithm.PARA, (int)SEED, a);

		/* Check if seq and par gives same result */
		for(int i = 0; i < a.length; i++){
			if(a[i] != seqResults[i]){
				same = false;
			}
		}


		/* Print results */
		Arrays.sort(timeSeq);
		Arrays.sort(timePar);
		double medianSeq = timeSeq[ntests/2];
		double medianPar = timePar[ntests/2];
		System.out.println("N = "+N+", #threads = "+nthreads+", #tests = "+ntests);
		System.out.println("Same solution from seq and par:" + same);
		System.out.println("Median time for Seq (ms): " + medianSeq);
		System.out.println("Median time for Par (ms): " + medianPar);
		System.out.printf("Speedup: %.2f\n", (medianSeq/medianPar));
	}


	/** Print an error message if a[] is not sorted in the ascending order */
	public void testSort(int[] a, String message) {
		for(int i = 0; i < a.length-1; i++) {
			if(a[i] > a[i+1]) {
				System.out.println("("+message+") Wrong at index " + i + ": " + "a["+i+"]:"+a[i]+" > a["+(i+1)+"]:"+a[i+1]);
				return;
			}
		}
	}


	/*-----------------------------------------------------------------------
	* Below are methods for sequential version of the right radix sort
	*------------------------------------------------------------------------*/


	/** @return total time it takes to execute the sequential algorithm */
	public double sortSeq(int[] a) {
		long time = System.nanoTime();
		int max;        //max number in a[]
		int shift;      //hold the shift value for each round of the sort
		int ndigits;    //total number of digits
		int[] digitLen; //stores the length of every single digit
		int[] b, t;     //extra arrays to help with the sort

		/* Step A: Find max */
		max = a[0];
		for(int i = 1; i < a.length; i++) {
			if(a[i] > max) max = a[i];
 		}
		/* Divide the total number of bits among all the digits */
		int maxBits = 1;
		while(max > (1L<<maxBits)) maxBits++;
		ndigits = Math.max(1, maxBits/BITS);
		int bit = maxBits / ndigits;
		int rem = maxBits % ndigits;
		digitLen = new int[ndigits];
		for(int i = 0; i < ndigits; i++) {
			digitLen[i] = bit;
			if(0 < rem--) digitLen[i]++;
		}

		/* Sort using a single digit at a time */
		shift = 0;
		b = new int[a.length];
		for(int i = 0; i < ndigits; i++) {
			radixSort(a, b, (1<<digitLen[i]), shift);
			shift += digitLen[i];
			t = a; a = b; b = t; //swap array pointers
		}
		/* For odd ndigits, copy all content back to original array */
		if(ndigits % 2 != 0) {
			System.arraycopy(a, 0, b, 0, a.length);
		}
		time = System.nanoTime() - time;
		return (time/1000000.0);
	}

	/**
	 * Sort the array a[] based on a single digit
	 * @param nvalues number of possible values that this digit can have
	 * @param shift indicates where this digit is located in a number */
	private void radixSort(int[] a, int[] b, int nvalues, int shift) {
		int mask = nvalues - 1;
		int[] count = new int[nvalues];

		/* Step B: Count based on the digit values */
		for(int i = 0; i < a.length; i++) {
			count[(a[i]>>shift)&mask]++;
		}
		/* Step C: Accumulate the counts */
		int temp, accum = 0;
		for(int i = 0; i < nvalues; i++) {
			temp = count[i];
			count[i] = accum;
			accum += temp;
		}
		/* Step D: Move numbers from a[] to b[] in a sorted order */
		for(int i = 0; i < a.length; i++) {
			b[count[(a[i])>>shift&mask]++] = a[i];
		}
	}


	/*-----------------------------------------------------------------------
	* Below are methods for parallel version of right radix sort
	*------------------------------------------------------------------------*/


	/* Global variables for all threads */
	int max;
	int[] offset;
	int[][] allCount;
	CyclicBarrier sync;
	ReentrantLock lock;

	/** Synchronized method to update the global max */
	private void updateMax(int num) {
		lock.lock();
		if(num > max) max = num;
		lock.unlock();
	}

	/** @return total time it takes to execute the parallel algorithm */
	public double sortPar(int[] a) {
		long time = System.nanoTime();

		/* Initialize all global variables */
		max = a[0]; //assume a[] is not empty
		offset = new int[nthreads];
		allCount = new int[nthreads][];
		sync = new CyclicBarrier(nthreads);
		lock = new ReentrantLock();

		/* Start all the threads */
		Thread[] thr = new Thread[nthreads];
		int[] b = new int[a.length];
		for(int i = 0; i < nthreads; i++) {
			thr[i] = new Thread(new WorkUnit(i,a,b));
			thr[i].start();
		}
		/* Wait for all the threads to finish */
		try {
			for(Thread t : thr) t.join();
		} catch(InterruptedException e) {}

		time = System.nanoTime() - time;
		return (time/1000000.0);
	}

	/** An instance of this class represents a working unit
	 ** that can work in parallel with other working units. */
	class WorkUnit implements Runnable {
		int id;
		int[] a, b, t;
		int[] digitLen;
		int begin1,end1; //the part of a[] that is owned by this thread
		int begin2,end2; //the part of allCount[][] that is owned by this thread

		/** Constructor */
		public WorkUnit(int id, int[] a, int[] b) {
			this.id = id;
			this.a = a;
			this.b = b;
		}

		/** Find the local max and synchronously update the global max */
		private void findMax() {
			int localMax = a[begin1];
			for(int i = begin1 + 1; i < end1; i++) {
				if(a[i] > localMax) localMax = a[i];
			}
			updateMax(localMax); //this is a synchronized method
		}

		@Override
		public void run() {
			try {
				/* Divide a[] equally among all threads */
				begin1 = id * (a.length/nthreads);
				if(id == nthreads-1) end1 = a.length;
				else                 end1 = begin1 + (a.length/nthreads);

				/* Step A: Find max */
				findMax();
				sync.await();

				/* Divide the total number of bits among all the digits */
				int maxBits = 1;
				while(max > (1L<<maxBits)) maxBits++;
				int ndigits = Math.max(1, maxBits/BITS);
				int bit = maxBits / ndigits;
				int rem = maxBits % ndigits;
				digitLen = new int[ndigits];
				for(int i = 0; i < ndigits; i++) {
					digitLen[i] = bit;
					if(0 < rem--) digitLen[i]++;
				}

				/* Sort using one digit at a time */
				int shift = 0;
				for(int i = 0; i < ndigits; i++) {
					radixSortPar(a, b, (1<<digitLen[i]), shift);
					shift += digitLen[i];
					t = a; a = b; b = t; //swap array pointers
				}
				/* For odd ndigits, copy all content back to original array */
				if(ndigits % 2 != 0) {
					System.arraycopy(a, begin1, b, begin1, (end1-begin1));
				}
			} catch (InterruptedException exc) {
				System.out.println("(Thread-" + id + ") " + exc);
			} catch (BrokenBarrierException exc) {
				System.out.println("(Thread-" + id + ") " + exc);
			}
		}

		/**
		 * Sort the array a[] in parallel based on a single digit
		 * @param nvalues number of possible values that this digit can have
		 * @param shift indicates where this digit is located in a number */
		private void radixSortPar(int[] a, int[] b, int nvalues, int shift) {
			try {
				int mask = nvalues - 1;
				int[] count = new int[nvalues];

				/* Divide allCount[][] equally among the threads */
				begin2 = id * (nvalues/nthreads);
				if(id == nthreads-1) end2 = nvalues;
				else end2 = begin2+(nvalues/nthreads);

				/* Step B: Count numbers based on the digit's values */
				for(int i = begin1; i < end1; i++) {
					count[(a[i]>>shift)&mask]++;
				}
				allCount[id] = count; //update allCount asynchronously
				sync.await();         //done with Step B

				/* Step C1: Accumulate allCount[][] and get offset[] */
				int temp, accum = 0;
				for(int col = begin2; col < end2; col++) {
					for(int row = 0; row < nthreads; row++) {
						temp = allCount[row][col];
						allCount[row][col] = accum;
						accum += temp;
					}
				}
				if(id != nthreads-1) offset[id+1] = accum;
				sync.await(); //done with Step C1

				/* Step C2: Let ONLY the first thread accumulate offset[] */
				if(id == 0) {
					for(int i = 1; i < nthreads; i++)
						offset[i] += offset[i-1];
					sync.await(); //sync with other threads
				} else {
					sync.await(); //other threads go directly to next barrier
				}

				/* Step C3: Add the offset to each value in allCount[][] */
				for(int row = 0; row < nthreads; row++) {
					for(int col = begin2; col < end2; col++) {
						allCount[row][col] += offset[id];
					}
				}
				sync.await(); //done with Step C3

				/* Step D: Move numbers from a[] to b[] in a sorted order */
				for(int i = begin1; i < end1; i++) {
					b[count[(a[i]>>shift)&mask]++] = a[i];
				}
				sync.await(); //done with Step D
			}
			catch (InterruptedException exc) {
				System.out.println("(Thread-" + id + ") " + exc);
			}
			catch (BrokenBarrierException exc) {
				System.out.println("(Thread-" + id + ") " + exc);
			}
		}
	}
}
