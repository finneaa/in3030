/**----------------------------------------------------------------------------
 * Brukernavn: finneaa
 * Oblig5 - IN3030
 * Konvekse Innhyllinga - Et rekursivt geometrisk problem
 * Note that a new method merge() has been added to the IntList class to make
 * things easier to write the code.

 * Algorithm Description:
 * ++ Step 1: Find minx and maxx.
 * ++ Step 2: Divide all the points into 2 lists (those on the left, and those
 *    on the right of line maxx->minx). At the same time, find 2 points that
 *    are furthest away from both sides of the line.
 * ++ Step 3: Recursion (which consists of 3 sub-steps):
 *    -- Step 3A: Check for the base case
 *    -- Step 3B: With p3 being the furthest point to the right of line p1->p2,
 *       this step will put all the points to the right of the line p1->p3 and
 *       the line p3->p2 into 2 separate lists. At the same time, find 2 points
 *       (on the right side) that are furthest away from the 2 lines above.
 *    -- Step 3C: Next recursive calls
 *
 * Abbreviations:
 * ++ furthest[] is an array of 4 elements, where:
 *    [0] Furthest point to the left of the line maxx->minx.
 *    [1] Furthest point to the right of the line maxx->minx.
 *    [2] Distance from the line to the furthest left point.
 *    [3] Distance from the line to the furthest right point.
 * ++ fRight[] is an array of 2 elements, where:
 *    [0] Furthest point to the right of the line p1->p3.
 *    [1] Furthest point to the right of the line p3->p2.
 *---------------------------------------------------------------------------*/
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Oblig5 {
	int MAX_X, MAX_Y; //for TegnUt class
	int n;
	int[] x, y;
	static int nthreads;

	public static void main(String[] args) {
		int ntests = 0; //number of tests
		if(args.length == 2) {
			nthreads = Integer.parseInt(args[0]);
			ntests = Integer.parseInt(args[1]);
		} else if(args.length != 0) {
			System.out.println("USAGE: java Oblig5 <#THREADS> <#TESTS>");
			System.out.println("Leave BOTH of the fields empty if you "+
			                   "want them to be chosen automatically. ");
			return;
		}
		new Oblig5().runTests(ntests);
	}

	/** @param t number of threads
	 ** @param ntests number of tests (for both sequential and parallel run) */
	public void runTests(int ntests) {
		if(ntests <= 0) ntests = 3;
		if(nthreads <= 0) nthreads = Runtime.getRuntime().availableProcessors();
		for(n = 100000000; n >= 100; n /= 10) test(ntests);
	}

	/** Draw the koHyll on the screen */
	public void draw(IntList koHyll) {
		if(koHyll == null) return;
		MAX_X = x[0];
		MAX_Y = y[0];
		for(int i = 1; i < n; i++) {
			if(x[i] > MAX_X) MAX_X = x[i];
			if(y[i] > MAX_Y) MAX_Y = y[i];
		}
		TegnUt tu = new TegnUt(this, koHyll);
		System.out.println(koHyll);
	}

	/** Run test (ntests time) for a particular value of 'n' */
	private void test(int ntests) {
		System.out.println("================================================");
		System.out.println("Generating test for n = " + n + ".....");

		double[] timeSeq = new double[ntests];
		double[] timePar = new double[ntests];
		x = new int[n];
		y = new int[n];
		NPunkter17 p = new NPunkter17(n);
		p.fyllArrayer(x,y);
		IntList koHyll = null;

		/* Sequential tests */
		for(int i = 0; i < ntests; i++) {
			long time = System.nanoTime();
			koHyll = seqMethod();
			time = System.nanoTime() - time;
			timeSeq[i] = time/1000000.0;
		}
		/* Parallel tests */
		for(int i = 0; i < ntests; i++) {
			long time = System.nanoTime();
			koHyll = parMethod();
			time = System.nanoTime() - time;
			timePar[i] = time/1000000.0;
		}
		/* Draw the koHyll only for small 'n' */
		if(n <= 200 || n == 1000) draw(koHyll);

		/* Print all results */
		Arrays.sort(timeSeq);
		Arrays.sort(timePar);
		double medianSeq = timeSeq[ntests/2];
		double medianPar = timePar[ntests/2];
		System.out.println("n = "+n+", #threads = "+nthreads+", #tests = "+ntests);
		System.out.println("Total number of points: " + koHyll.size());
		System.out.println("Median time for Seq (ms): " + medianSeq);
		System.out.println("Median time for Par (ms): " + medianPar);
		System.out.printf("Speedup: %.2f\n\n", (medianSeq/medianPar));
	}



	/*=========================================================================
	* General help methods (useful for both sequential and parallel version)
	*========================================================================*/

	/** Returns the "distance" between point p3 and line p1->p2.
	 ** Distance = 0 if p3 is on the line.
	 ** Distance > 0 if p3 is to the left of the line.
	 ** Distance < 0 if p3 is to the right of the line. */
	private int distance(int p3, int p1, int p2) {
		int a = y[p1]-y[p2];
		int b = x[p2]-x[p1];
		int c = y[p2]*x[p1] - y[p1]*x[p2];
		return a*x[p3] + b*y[p3] + c;
	}

	/** Return true if point p3 is located between p1 and p2.
	 ** OBS: We assume here that the distance between p3 and
	 **      line p1->p2 is zero (i.e. p3 must be on the line). */
	private boolean isBetween(int p3, int p1, int p2) {
		int dx1 = Math.abs(x[p3]-x[p1]);
		int dx2 = Math.abs(x[p2]-x[p1]);
		int dy1 = Math.abs(y[p3]-y[p1]);
		int dy2 = Math.abs(y[p2]-y[p1]);
		if((0<=dx1 && dx1<=dx2) && (0<=dy1 && dy1<=dy2)) return true;
		return false;
	}

	/** Algorithm Step 2, but only go through all points P (begin <= P < end).
	 ** Returns an array of 4 elements (furthest[]), where:
	 ** [0] Furthest point to the left of the line maxx->minx.
	 ** [1] Furthest point to the right of the line maxx->minx.
	 ** [2] Distance from the line to the furthest left point.
	 ** [3] Distance from the line to the furthest right point. */
	private int[] step2(int begin, int end, int maxx, int minx,
	                    IntList left, IntList right) {
		int[] furthest = { begin, begin, -1, 1 };
		for(int i = begin; i < end; i++) {
			if(i == minx || i == maxx) {
				continue;
			}
			int distance = distance(i,maxx,minx);
			if(distance >= 0) {
				if(distance > furthest[2]) {
					furthest[2] = distance;
					furthest[0] = i;
				}
				left.add(i);
			}
			if(distance <= 0) {
				if(distance < furthest[3]) {
					furthest[3] = distance;
					furthest[1] = i;
				}
				right.add(i);
			}
		}
		return furthest;
	}

	/** Algorithm Step 3B:
	 ** -- Go through all the point sets within ptSets[].
	 ** -- Put point P into list0 if P is to the right of p1->p3 line.
	 ** -- Put point P into list1 if P is to the right of p3->p2 line.
	 ** Return an array of 2 elements (fRight[]), where:
	 ** [0] Furthest point to the right of the line p1->p3.
	 ** [1] Furthest point to the right of the line p3->p2. */
	private int[] step3B(int p1, int p2, int p3, IntList[] ptSets,
	                     IntList list0, IntList list1) {
		int fRight[] = new int[2];
		int maxNegDis0 = 1; //max negative distance from p1->p3 line
		int maxNegDis1 = 1; //max negative distance from p3->p2 line

		for(IntList set : ptSets) {
			for(int i = 0; i < set.size(); i++) {
				int point = set.get(i);
				if(point == p3) {
					continue;
				}
				int distance = distance(point,p1,p3);
				if(distance < 0 || (distance == 0 && isBetween(point,p1,p3))) {
					if(distance < maxNegDis0) {
						maxNegDis0 = distance;
						fRight[0] = point;
					}
					list0.add(point);
					continue;
				}
				distance = distance(point,p3,p2);
				if(distance <= 0) {
					if(distance < maxNegDis1) {
						maxNegDis1 = distance;
						fRight[1] = point;
					}
					list1.add(point);
				}
			}
		}
		return fRight;
	}



	/*=========================================================================
	* Below are methods for the sequential version
	*========================================================================*/

	/** @return the koHyll list (with all points sorted in the right order) */
	private IntList seqMethod() {
		IntList koHyll = new IntList();
		IntList left  = new IntList();
		IntList right = new IntList();
		int[] furthest = null;
		int maxx = 0; //index of the point with maximum 'x'
		int minx = 0; //index of the point with minimum 'x'

		/* Step 1: Find maxx and minx */
		for(int i = 1; i < n; i++) {
			if(x[i] > x[maxx]) maxx = i;
			else if(x[i] < x[minx]) minx = i;
		}
		/* Step 2: Find furthest points, and divide all points into 2 lists */
		furthest = step2(0,n,maxx,minx,left,right);

		/* Step 3: Recursive method calls */
		IntList[] leftLists  = { left  };
		IntList[] rightLists = { right };
		seqRec(maxx, minx, furthest[1], rightLists, koHyll);
		seqRec(minx, maxx, furthest[0], leftLists, koHyll);
		return koHyll;
	}


	/** Recursive method with following parameters:
	 ** -- p1->p2 specifies the line as well as the direction
	 ** -- p3 specifies the furthest point to the right of the line
	 ** -- ptSets must consist of all sets with the points located to the right
	 **    of the line (including p3, but excluding p1 and p2).
	 ** -- koHyll is where the points on the boundary will be added */
	private void seqRec(int p1, int p2, int p3,
	                    IntList[] ptSets, IntList koHyll) {
		/* Algorithm Step 3A: Check for base case */
		for(int i = 0; true; i++) {
			if(i == ptSets.length) {
				//this is the base case
				koHyll.add(p1); return;
			}
			else if(ptSets[i].size() > 0) break;
		}
		/* Algorithm Step 3B */
		IntList[] list0 = {new IntList()}; //for points to the right of p1->p3
		IntList[] list1 = {new IntList()}; //for points to the right of p3->p2
		int[] fRight = step3B(p1, p2, p3, ptSets, list0[0], list1[0]);

		/* Algorithm Step 3C: Next recursive calls */
		seqRec(p1, p3, fRight[0], list0, koHyll);
		seqRec(p3, p2, fRight[1], list1, koHyll);
	}



	/*=========================================================================
	* Below are methods for the parallel version
	*========================================================================*/

	/** @return the koHyll list (with all points sorted in the right order) */
	private IntList parMethod() {
		Monitor mon = new Monitor();
		Thread[] thr = new Thread[nthreads];

		/* Start all threads to perform Algorithm Step 1 and 2 */
		for(int i = 0; i < nthreads; i++) {
			thr[i] = new Thread(new WorkerStep1_2(i,mon));
			thr[i].start();
		}
		try {
			for(Thread t : thr) t.join();
		} catch(InterruptedException e) {}

		/* Get all necessary variables from the monitor */
		int maxx = mon.maxx;
		int minx = mon.minx;
		int[] furthest = mon.furthest;
		IntList[] left = mon.leftLists;
		IntList[] right = mon.rightLists;
		IntList koHyll = new IntList();
		IntList sublist = new IntList();

		/* Get the level (limit) for the recursion */
		int level = -1;
		for(int i = 1; i < nthreads; i *= 2) level++;

		/* Start all threads to perform Algorithm Step 3 */
		Runnable r1 = new WorkerStep3(level,maxx,minx,furthest[1],right,koHyll);
		Runnable r2 = new WorkerStep3(level,minx,maxx,furthest[0],left,sublist);
		Thread t1 = new Thread(r1);
		Thread t2 = new Thread(r2);
		t1.start();
		t2.start();
		try {
			t1.join();
			t2.join();
		} catch(InterruptedException e) {}

		/* Merge koHyll and sublist together */
		koHyll.merge(sublist);
		return koHyll;
	}


	/** Monitor for all threads of class WorkerStep1_2 */
	private class Monitor {
		int maxx, minx;
		int[] furthest;
		IntList[] leftLists, rightLists;
		CyclicBarrier sync;
		ReentrantLock lock;

		public Monitor() {
			leftLists = new IntList[nthreads];
			rightLists = new IntList[nthreads];
			sync = new CyclicBarrier(nthreads);
			lock = new ReentrantLock();
			furthest = new int[4];
			furthest[2] = -1;
			furthest[3] = 1;
		}

		/** Synchronized method to update the global maxx and minx */
		public void updateX(int max_x, int min_x) {
			lock.lock();
			if(x[max_x] > x[maxx]) maxx = max_x;
			if(x[min_x] < x[minx]) minx = min_x;
			lock.unlock();
		}

		/** Synchronized method to update the global furthest points
		 ** Array f[] must contain 4 elements:
		 ** [0] Furthest point to the left of the line maxx->minx.
		 ** [1] Furthest point to the right of the line maxx->minx.
		 ** [2] Distance from the line to the furthest left point.
		 ** [3] Distance from the line to the furthest right point. */
		public void updateFurthest(int[] f) {
			lock.lock();
			if(f[2] > furthest[2]) {
				furthest[0] = f[0];
				furthest[2] = f[2];
			}
			if(f[3] < furthest[3]) {
				furthest[1] = f[1];
				furthest[3] = f[3];
			}
			lock.unlock();
		}
	}


	/** Parallelization of Algorithm Step 1 and 2 */
	private class WorkerStep1_2 implements Runnable {
		int id;
		int begin, end;
		int maxx, minx;
		int[] furthest;
		Monitor mon;
		IntList left, right;

		public WorkerStep1_2(int id, Monitor mon) {
			this.id = id;
			this.mon = mon;
		}

		@Override
		public void run() {
			try {
				/* Divide all the points equally among all threads */
				begin = id * (n/nthreads);
				if(id == nthreads-1) end = n;
				else end = begin + (n/nthreads);

				/* Algorithm Step 1: Find maxx and minx */
				maxx = begin;
				minx = begin;
				for(int i = begin+1; i < end; i++) {
					if(x[i] > x[maxx]) maxx = i;
					else if(x[i] < x[minx]) minx = i;
				}
				mon.updateX(maxx, minx);
				mon.sync.await();

				/* Algorithm Step 2 */
				left = new IntList();
				right = new IntList();
				furthest = step2(begin,end,(mon.maxx),(mon.minx),left,right);
				mon.leftLists[id]  = left;
				mon.rightLists[id] = right;
				mon.updateFurthest(furthest);
			}
			catch (InterruptedException exc) {
				System.out.println("(Thread-" + id + ") " + exc);
			}
			catch (BrokenBarrierException exc) {
				System.out.println("(Thread-" + id + ") " + exc);
			}
		}
	}


	/** Parallelization of Algorithm Step 3 */
	private class WorkerStep3 implements Runnable {
		int level, p1, p2, p3;
		IntList koHyll;
		IntList[] ptSets;

		public WorkerStep3(int level, int p1, int p2, int p3,
		                   IntList[] ptSets, IntList koHyll) {
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = p3;
			this.level = level;
			this.ptSets = ptSets;
			this.koHyll = koHyll;
		}

		@Override
		public void run() {
			parRec(level,p1,p2,p3,ptSets,koHyll);
		}

		/** Recursive parallel method */
		public void parRec(int level, int p1, int p2, int p3,
		                   IntList[] ptSets, IntList koHyll) {
			/* Algorithm Step 3A: Check for base case and the limit */
			if(level <= 0) {
				seqRec(p1, p2, p3, ptSets, koHyll); return;
			}
			for(int i = 0; true; i++) {
				if(i == ptSets.length) {
					//this is the base case
					koHyll.add(p1); return;
				}
				else if(ptSets[i].size() > 0) break;
			}
			try {
				/* Algorithm Step 3B */
				IntList[] list0 = {new IntList()}; //for right points of p1->p3
				IntList[] list1 = {new IntList()}; //for right points of p3->p2
				int[] fRight = step3B(p1, p2, p3, ptSets, list0[0], list1[0]);

				/* Algorithm Step 3C: Next recursive calls */
				IntList sublist = new IntList();
				Thread th = new Thread(new WorkerStep3((level-1), p1, p3,
				                                fRight[0],list0,koHyll));
				th.start();
				parRec((level-1),p3,p2,fRight[1],list1,sublist);

				/* Merge koHyll and sublist together */
				th.join();
				koHyll.merge(sublist);
			}
			catch (InterruptedException exc) { System.out.println(exc); }
		}
	}
}
