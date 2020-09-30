
public class Recursion {

	public static int sum(int num) {
		int numSum = 0;
		for (int i = 0; i <= num; i++) {
			numSum += i;
		}
		return numSum;
	}

	public static void main(String[] args) {
		System.out.println(sum(4));
	}
}