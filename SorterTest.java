import java.util.*;

public class SorterTest{
  public static void main(String[]args){

    int n = 1000;
    int k = 20;

    if (args.length > 0) {
      try {
          n = Integer.parseInt(args[0]);
          k = Integer.parseInt(args[1]);
      } catch (NumberFormatException e) {
          System.err.println("Argument" + args[0] + " and " + args[1] + " must be an integer.");
          System.exit(1);
      }
    }
    Integer[] a = new Integer[n];
    Random rand = new Random();

    for (int i = 0;i < n; i++){
      a[i] = rand.nextInt(n);
    }

    Integer[] aSorted = a.clone();
    Integer[] aa = a.clone();
    //System.out.printf("Nonmodified arr[] : %s \n", Arrays.toString(aSorted));
    long startTimeSort = System.nanoTime();
    Arrays.sort(aSorted, Collections.reverseOrder());
    long endTimeSort = System.nanoTime();

    long durationSort = (endTimeSort - startTimeSort)/ 1000000.0;
    //System.out.printf("Sort Modified arr[] : %s \n", Arrays.toString(aSorted));
    long startTimeA2 = System.nanoTime();
    a2(a,k);
    long endTimeA2 = System.nanoTime();

    long durationA2 = (endTimeA2 - startTimeA2)/ 1000000.0;
    //System.out.printf("Insert Modified arr[] : %s \n", Arrays.toString(a));

    boolean equal = true;
    for (int i = 0; equal && i < k; i++) {
      int s1 = aSorted[i];
      int s2 = a[i];
      equal = s1 == s2;
      //System.out.println(aSorted[i] + " like " + a[i] + equal);
    }
    if(equal){
      System.out.printf("Same\n");
      System.out.println("time sort: " + durationSort + "\ntime A2: " + durationA2 );
    }
    else{
      System.out.printf("Not the same\n");
    }

    /*insertSortFull(aa,aa.length);
    if(Arrays.equals(aSorted, aa)){
      System.out.printf("Same\n");
    }
    else{
      System.out.printf("Not the same\n");
    }*/


  }

  private static void insertSortFull (Integer [] a, int stopp) {
        int i , toSort ;
        for ( int k = 1 ; k < stopp ; k++) {
            toSort = a [k] ;
            i = k ;
            while ( i > 0 && a[i-1] <= toSort ) {
              a [i] = a [i-1];
              i--;
            }
            a[i] = toSort;
        } // end for k
  } // end insertSort

  private static void insertSortSingle (Integer [] a, int stopp) {
    int i , toSort ;
    toSort = a[stopp] ;
    i = stopp ;
    while ( i > 0 && a[i-1] <= toSort ) {
      a [i] = a [i-1];
      i--;
    }
    a[i] = toSort;
  } // end insertSort

  private static void a2(Integer [] a,int k){
    insertSortFull(a,k);
    //System.out.println(Arrays.toString(a));
    for(int i = k; i < a.length;i++){
      if(a[i]>a[k-1]){
        //System.out.println(a[i] + " swap " + a[k-1]);
        int temp = a[k-1];
        a[k-1] = a[i];
        a[i] = temp;
        insertSortSingle(a,k-1);
        //System.out.println(Arrays.toString(a));

      }
    }
  }
}
