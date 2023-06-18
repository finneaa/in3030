/**----------------------------------------------------------------------------
 * Brukernavn: finneaa
 * This class has been modified in order to optimize the code in Oblig5 class
 *---------------------------------------------------------------------------*/
import java.util.*;

class IntList{
	int[] data;
	int len = 0;

	public IntList() {
		data = new int[16];  // some default size
	}

	public IntList(int len) {
		data = new int[Math.max(1,len)];
	}

	/** Newly added method, to merge this list with another list */
	public void merge(IntList other) {
		int len1 = this.len;
		int len2 = other.len;
		int total = len1 + len2;

		if(total <= this.data.length-1) {
			System.arraycopy(other.data, 0, this.data, len1, len2);
		} else {
			int[] b = new int[total+1];
			System.arraycopy(this.data, 0, b, 0, len1);
			System.arraycopy(other.data, 0, b, len1, len2);
			this.data = b;
		}
		this.len = total;
	}

	/** Newly added method, to print out the contents of the list */
	public String toString() {
		String toReturn = "[";
		for(int i = 0; i < len-1; i++) {
			toReturn += data[i] + "->";
		}
		if(len > 0) toReturn += data[len-1];
		toReturn += "]";
		return toReturn;
	}

	public void add(int elem) {
		if (len == data.length-1) {
			int[] b = new int[data.length*2];
			System.arraycopy(data, 0, b, 0, data.length);
			data = b;
		}
		data[len++] = elem;
	}

	public void addAt(int elem, int pos) {
		while (pos > data.length) {
			int[] b = new int[data.length*2];
			for(int i = 0; i < data.length; i++) b[i] = data[i];
			data =b;
		}
		data[pos] = elem;
	}

	public void clear(){
		len =0;
	}

	public int get (int pos){
		if (pos > len-1 ) return -1;
		else return data [pos];
	}

	int size() {
		return len;
	}
}
