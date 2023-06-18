/*-----------------------------------------------------------------------------
 * Brukernavn: finneaa
 * UiO-IN3030: Oblig 3
 * Prime Numbers. Eratosthenes Sil and primes factorization.
 *
 * Abbreviations:
 * 	era -- Eratosthenes Sil algorithm to generate all primes <= N
 * 	fac -- Factorization algorithm to prime-factorize a number
 * 	N   -- All primes found by 'era' will be less than this number
 * 	seq -- Sequential
 * 	par -- Parallel
 *
 * Implementation of the byte array (byteArr):
 * - Prime    : 1(true)
 * - Non Prime: 0(false)
 * - byteArr[0] represents 8 numbers: 1,3,5,...,15 from RIGHT to LEFT,...
 *
 *---------------------------------------------------------------------------*/
 import java.util.*;
 import java.util.concurrent.*;
 import java.util.concurrent.locks.ReentrantLock;

class Oblig3 {
	public static void main(String[] args) {
		if(args.length != 2) {
			System.out.println("Usage: java Oblig3 <N> <#threads>");
			System.out.println("Enter 0 if you want #threads == #cores");
			return;
		}
		PFactorization pf = new PFactorization(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		pf.execute();
	}
}

class PFactorization {
	double timeEraSeq, timeEraPar;             //time(ms) to execute ERA algorithm
	double timeFacSeq, timeFacPar;             //time(ms) to factorize the last 100 numbers
	final int N;                               //all primes found by ERA will be <= N
	final int nthreads;
	int total_primes;
	int largestPrime;
	final int[] mask = {1,2,4,8,16,32,64,128};
	byte[] byteArr;                            //byteArr[0] represents 8 numbers: 1,3,5,..,15(RIGHT->LEFT)
	ArrayList<Integer> smallPrimes;            //holds all odd primes <= sqrt(N)
  HashMap<Long, ArrayList<Long>> numbStorageSeq = new HashMap<Long, ArrayList<Long>>(); //for comparison and output to precode
  HashMap<Long, ArrayList<Long>> numbStoragePar = new HashMap<Long, ArrayList<Long>>(); //for comparison and output to precode
	CyclicBarrier barrier;
  Oblig3Precode precode;                    //precode for the assignment
  Boolean same;

	public PFactorization(int N, int nthreads) {
		this.N = N;
		if(nthreads <= 0) {
			this.nthreads = Runtime.getRuntime().availableProcessors();
		} else {
			this.nthreads = nthreads;
		}
    precode = new Oblig3Precode(N);
	}

	public void execute() {
		//Sequential solution
		eraSeq();
		facSeq();
		System.out.println("------------------------------------------");

		//Parallel solution
		eraPar();
		facPar();
		System.out.println("------------------------------------------");

		//Print statistics
		findTotalAndLargestPrime();
    same = checkSame();
		printReport();
    precode.writeFactors();
	}

	private void findTotalAndLargestPrime() {
		total_primes = 1; //special case: 2 is the only even prime
		largestPrime = 0;

		//Find the coordinates of the largest odd number <= N in byteArr
		int tmp = N;
		if(tmp%2 == 0) tmp--;
		int i = tmp/16;     //vertical coordinate
		int j = (tmp%16)/2; //horitzontal coordinate

		//Find the largest prime
		while((byteArr[i] & mask[j]) == 0) {
			if(j == 0) {
				i--;
				j = 7;
			} else {
				j--;
			}
		} largestPrime = i*16 + j*2 + 1;

		//Count the total number of primes
		for(; i >= 0; i--) {
			for(; j >= 0; j--) {
				if((byteArr[i] & mask[j]) != 0) total_primes++;
			}
			j = 7; //reinitialize
		}
	}

	private void printReport() {
		System.out.println("N = " + N + "; #threads = " + nthreads);
		System.out.println("Total number of primes: " + total_primes);
		System.out.println("The largest prime is  : " + largestPrime);
    System.out.println("Same results from seq and par: " + same);
		System.out.printf("EraSeq: %8.2fms, EraPar: %8.2fms ", timeEraSeq, timeEraPar);
		System.out.printf("==> Speedup for Era: %.2f\n",timeEraSeq/timeEraPar);
		System.out.printf("FacSeq: %8.2fms, FacPar: %8.2fms ", timeFacSeq, timeFacPar);
		System.out.printf("==> Speedup for Fac: %.2f\n",timeFacSeq/timeFacPar);
		double timeSeq = timeEraSeq + timeFacSeq;
		double timePar = timeEraPar + timeFacPar;
		System.out.printf("Total time for SEQ run: %.2fms\n", timeSeq);
		System.out.printf("Total time for PAR run: %.2fms\n", timePar);
		System.out.printf("Overall speedup       : %.2f\n", timeSeq/timePar);
	}


	public class OutOfLimit extends RuntimeException {
		public OutOfLimit() {
			super();
		}
		public OutOfLimit(String message) {
			super(message);
		}
	}

	/** Return the first prime number larger 'n'
	 ** @param n this number must either be 2 or be an odd number
	 ** @throws OutOfLimit exception if the next prime is larger than N */
	private int nextPrime(int n) throws OutOfLimit {
		if(n == 2) return 3;
		int i = n/16;     //vertical coordinate of 'n' in byteArr
		int j = (n%16)/2; //horizontal coordinate of 'n' in byteArr
		j++;              //we start the search from the next number

		loop:
		while(i < byteArr.length) {
			while(j < 8) {
				if((byteArr[i] & mask[j]) != 0) break loop;
				j++;
			} i++; j = 0;
		}

		int next_prime = 16*i + 2*j + 1; //convert to actual integer
		if(next_prime > N) {
			throw new OutOfLimit("Limit N has been reached!");
		}
		return next_prime;
	}

	/** Use the odd prime 'p' to cross out non-primes X (low <= X <= high) */
	private void crossOut(int p, int low, int high) {
		// Get the first number to be crossed out
		// This number is equal to 'low' if and only if:
		// (1) 'low' is a multiple of 'p' (but low != p)
		// (2) 'low' is an odd number
		// Otherwise, it's equal to the first odd multiple of 'p' within range
		int n;
		if((low%p == 0) && (low != p) && (low%2 != 0)) {
			n = low;
		} else {
			n = (low + p) / p * p;
			if(n % 2 == 0) n += p;
		}

		//Starting crossing out non-primes (by making bit = 0)
		int i, j;
		while(n <= high) {
			i = n/16;     //vertical coordinate of 'n' in byteArr
			j = (n%16)/2; //horizontal coordinate of 'n' in byteArr
			byteArr[i] = (byte) (byteArr[i] & (~mask[j])); //bit = 0
			n += (2*p);
		}
	}


	/** Sequential version of Eratosthenes Sil algorithm */
	private void eraSeq() {
		long time = System.nanoTime();

		//Initialize the byte array and take care of the special case (1)
		byteArr = new byte[N/16+1];
		for(int i = 0; i < byteArr.length; i++) {
			byteArr[i] = (byte)0xff;
		} byteArr[0] = (byte) (byteArr[0]&(~mask[0])); //1 is not a prime!

		//Start the algorithm
		for(int p = 3; p*p <= N; p = nextPrime(p)) {
			crossOut(p, p*p, N);
		}
		time = System.nanoTime() - time;
		timeEraSeq = time / 1000000.0;
	}

	/** Parallel version of Eratosthenes Sil algorithm */
	private void eraPar() {
		long time = System.nanoTime();

		//Initialize the byte array and take care of the special case (1)
		byteArr = new byte[N/16+1];
		for (int i = 0; i < byteArr.length; i++) {
			byteArr[i] = (byte)0xff;
		} byteArr[0] = (byte) (byteArr[0]&(~mask[0])); //1 is not a prime!

		//Sequentially search for all odd primes <= sqrt(N)
		smallPrimes = new ArrayList<Integer>();
		int p = 3;
		int limit = (int) Math.sqrt(N);
		for(; p*p <= limit; p = nextPrime(p)) {
			smallPrimes.add(p);
			crossOut(p, p*p, limit);
		}
		for(; p <= limit; p = nextPrime(p)) {
			smallPrimes.add(p);
		}

		//Start the threads and let them cross out non-primes in parallel
		barrier = new CyclicBarrier(nthreads+1);
		int wload = (byteArr.length-limit/16) / nthreads; //workload per thread
		for(int i = 0; i < nthreads; i++) {
			new Thread(new ParEra(i, wload)).start();
		}
		try {
			barrier.await();
		} catch(Exception e) {return;}
		time = System.nanoTime() - time;
		timeEraPar = time / 1000000.0;
	}

	private class ParEra implements Runnable {
		private int id;
		private int wload; //total number of bytes crossed out by this thread

		public ParEra(int id, int wload) {
			this.id = id;
			this.wload = wload;
		}
		public void run() {
			/* This thread crosses out all non-primes X (LOW <= X <= HIGH)
			 * (1) A byte can only be crossed out by one single thread
			 * (2) LOW equals the lowest odd number in the first byte crossed
			 *     out by a thread (with exception of the first thread)
			 * (3) HIGH equals the highest odd number in the last byte crossed
			 *     out by a thread (with exception of the last thread)
			 */
			int low, high;
			int fbyte; //first byte to be crossed out
			int lbyte; //last byte to be crossed out

			//Compute low and high
			if(id == 0) { low = (int) Math.sqrt(N); }
			else {
				fbyte = byteArr.length - (nthreads-id)*wload;
				low = 16*fbyte + 1;
			}
			if(id == nthreads-1) { high = N; }
			else {
				lbyte = byteArr.length-1 - (nthreads-1-id)*wload;
				high = 16*lbyte + 15;
			}
			if(low > high) {
				String error = "\n\tN is too small for parallel solution!!!";
				throw new RuntimeException(error);
			}
			//Start crossing out all non-primes in the range
			for(int prime : smallPrimes) {
				crossOut(prime, low, high);
			}
			try {
				barrier.await();
			} catch(Exception e) {}
		}
	}
  /** Check if both metodes gave same results */
  private Boolean checkSame(){
    for (Long nu : numbStorageSeq.keySet()) {
      // check for same keys in storage HashMaps
      if(!numbStoragePar.containsKey(nu)){
        //System.out.println("Feil i hashmap");
        return false;
      }
      ArrayList<Long> factorsSeq = numbStorageSeq.get(nu);
      ArrayList<Long> factorsPar = numbStoragePar.get(nu);
      Collections.sort(factorsSeq);
      Collections.sort(factorsPar);

  		for(int i = 0; i < factorsSeq.size(); i++) {
        Long temp1 = factorsSeq.get(i);
        Long temp2 = factorsPar.get(i);
        if(!temp1.equals(temp2)){
          //System.out.println("Feil i compare");
          return false;
        }
        //add to precode output
        precode.addFactor(nu,temp1);
  		}
    }

    return true;
  }

	/** Return a string containing a number and all of its prime factors  + storing the values in hashMaps*/
	private String printFactors(long num, ArrayList<Long> factors, String type) {
    //chech for which hashMaps to put in
    if(type.equals("Seq")){
      numbStorageSeq.put(num,factors);
    } else if(type.equals("Par")){
      numbStoragePar.put(num,factors);
    }
    Collections.sort(factors);
		String str = num + " = " + factors.get(0);
		for(int i = 1; i < factors.size(); i++) {
			str = str + "*" + factors.get(i);
		} return str;
	}

	/** Sequential version of the factorization algorithm */
	private void facSeq() {
		long time = System.nanoTime();

		for(int i = 100; i > 0 ; i--) {
			long num = (long)N*N - i; //number to be factorized
			long cnum = num;        //current number
			ArrayList <Long> factors = new ArrayList<Long>();

			int p = 2;
			int stop = (int)Math.sqrt(cnum); //stop FAC when p reaches sqrt(cnum)
			while(p <= stop) {
				while(cnum%p == 0) {
					//another factor is found!
					factors.add((long) p);
					cnum = cnum / p;
					stop = (int)Math.sqrt(cnum);
				}
				try {
					p = nextPrime(p);
				} catch(OutOfLimit exception) {
          //System.out.println("feil Out of limit at number:" + p);
          break;
        }
			}
			if(cnum != 1) {
				//all primes <= N have been tried --> cnum itself must be prime
				factors.add((long) cnum);
			}
			System.out.println("Seq: " + printFactors(num, factors, "Seq"));
		}
		time = System.nanoTime() - time;
		timeFacSeq = time / 1000000.0;
	}

	/** Parallel version of the factorization algorithm */
	private void facPar() {
		long time = System.nanoTime();

		for(int i = 100; i > 0; i--) {
			NumMonitor mon = new NumMonitor((long)N*N - i);
			barrier = new CyclicBarrier(nthreads+1);
			for(int j = 0; j < nthreads;j++) {
				new Thread(new ParFac(j, mon)).start();
			}
			try {
				barrier.await();
			} catch(Exception e) {
        return;
      }

			if(mon.get_cnum() != 1) {
				//all primes <= N have been tried --> cnum itself must be prime
				mon.addNewFactor(mon.get_cnum());
			}
			System.out.println("Par: " + mon.toString());
		}
		time = System.nanoTime() - time;
		timeFacPar = time / 1000000.0;
	}

	private class NumMonitor {
		private long num;    //number to be factorized
		private long cnum;   //current number
		private int stop;    //indicates the moment when we stop FAC algorithm
		private ArrayList<Long> factors;
		private ReentrantLock lock;

		public NumMonitor(long num) {
			this.num = num;
			this.cnum = num;
			stop = (int)Math.sqrt(cnum); //stop when we reach sqrt(cvalue)
			factors = new ArrayList<Long>();
			lock = new ReentrantLock();
		}
		public long get_cnum() {
			return cnum;
		}
		public int get_stop() {
			return stop;
		}
		public void addNewFactor(long p) {
			lock.lock();
			factors.add((long) p);
			cnum = cnum / p;
			stop = (int) Math.sqrt(cnum);
			lock.unlock();
		}
		public String toString() {
			return printFactors(num, factors, "Par");
		}
	}

	private class ParFac implements Runnable {
		private int id;
		private NumMonitor mon;

		public ParFac(int id, NumMonitor mon) {
			this.id = id;
			this.mon = mon;
		}

		public void run() {
			//Get the first prime number within the range of this thread
			int p;
			if(id == 0) p = 2;
			else        p = nextPrime(id*(N/nthreads));

			//Get the max limit for the range of this thread
			int limit;
			if(id == nthreads - 1) limit = N;
			else                   limit = (id+1)*(N/nthreads);

			//Start the factorizing algorithm for this thread
			while(p <= mon.get_stop() && p <= limit) {
				while((mon.get_cnum() % p) == 0) {
					mon.addNewFactor((long) p);
				}
				try {
					p = nextPrime(p);
				} catch(OutOfLimit exception) {
          //System.out.println("feil Out of limit at number:" + p);
          break;
        }
			}
			try {
				barrier.await();
			} catch(Exception e) {}
		}
	}
}
