package tester;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class LineTerminatorDiffTest {
	public static void main(String[] args) throws Exception {
		// System.out.println("Result: " + );
		testForLineTerminatorDiff(args[0], args[1]);
	}
	
	private static void testForLineTerminatorDiff(	final String filename1, 
													final String filename2) throws IOException {
		try (BufferedReader reader1 = new BufferedReader(new FileReader(filename1));
		     BufferedReader reader2 = new BufferedReader(new FileReader(filename2))) {
			
			int char1, char2;
			/* continue till EOF reaches or a character mismatch happens in any of the file */
			while( ((char1=reader1.read()) != -1 & (char2=reader2.read()) != -1) && char1 == char2) {
				/* do nothing */
				System.out.printf("char1=%d, char2=%d \n", char1, char2);
			}
			
			if(char1 == -1 || char2 == -1) {
				System.out.println("Reached EOF");
				return;
			}
			
			/* Checks for EOL character diff */
			if((char1 == '\r' || char1 == '\n') && (char2 == '\r' || char2 == '\n')) {
				System.out.println("Mismatch in EOL terminators");
			} else { /* Difference in normal characters */
				System.out.println("Normal character differences");
			}
		}
	}
}