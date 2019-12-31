package tester;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class Test {
	public static void main(String[] args) throws Exception {
//		test6(args[0], args[1]);
		test7();
	}

	private static void test7() {
		String[] patternStrings = {
				// "\\w+.java",
//				 "\\w+.class"
				".*.class"
			};
		boolean patternToAccept = true;

		String[] filenames = {
				"a.java",
				"b.pptx",
				"c.class",
				"d e.class",
				"d.txt"
			};

		System.out.println("Adding patterns...");
		List<Pattern> patterns = new ArrayList<>();
		for(String patt : patternStrings) {
			patterns.add(Pattern.compile(patt));
		}

		System.out.println("patterns: " + Arrays.toString(patternStrings));
		System.out.println("filenames: " + Arrays.toString(filenames));
		System.out.println("Mode: " + (patternToAccept ? "accepting" : "rejecting"));
		System.out.println("Including files...");

		nextFname:
		for(String fname : filenames) {
			for(Pattern patt : patterns) {
				boolean matches = patt.matcher(fname).matches();
//				System.out.printf("  // fname=%s, patt='%s', patternToAccept=%b, matches=%b \n",
//						fname, patt, patternToAccept, matches);  // DEBUG

				if(matches) {
					if(patternToAccept) {
						System.out.println("  -> " + fname);
					}
					continue nextFname;
				}
			}

			if(!patternToAccept)
				System.out.println("  -> " + fname);
		}
	}

	private static void test6(final String ifname1, final String ifname2) throws IOException {
		try (FileInputStream f1 = new FileInputStream(ifname1);
		     FileInputStream f2 = new FileInputStream(ifname2)) {
			int f1Byte, f2Byte;
			long byteCount=0; // DEBUG
			while((((f1Byte=f1.read()) != -1) & ((f2Byte=f2.read()) != -1)) && f1Byte == f2Byte) {
				System.out.printf("Equal bytes at %d (f1Byte=%d, f2Byte=%d) \n", ++byteCount, f1Byte, f2Byte);
			}
			System.out.printf("loop terminated, f1Byte=%d, f2Byte=%d \n", f1Byte, f2Byte);
		}
	}

	private static void test5() throws IOException {
		LogManager.getLogManager().reset();
		Logger logger = Logger.getLogger("FileDiff.log");
		logger.setLevel(Level.ALL);
		FileHandler fileHandler = new FileHandler();
		logger.addHandler(fileHandler);
		System.out.println("logger.getName(): " + logger.getName());

		logger.warning("This is a warning log");
		fileHandler.close();
	}

	class ErrorHandlerDemo {
		void add(final String msg) {
			System.err.println(msg);
		}
	}

	/*---------------START OF PROTOTYPE 1---------------*/

	private static void test4() throws IOException {
		Test test = new Test();
		test.errHandler = test.new ErrorHandlerDemo();
		test.prototype1();
	}

	ErrorHandlerDemo errHandler;
	Path newerRootDir, olderRootDir;
	Map<Path, Path> newerMap, olderMap;
	List<Pattern> ignorePatternList;

	private void prototype1() throws IOException {
		newerRootDir = Paths.get("E:/tmp/test/DiffTest/src").toAbsolutePath();
		olderRootDir = Paths.get("E:/tmp/test/DiffTest/src2").toAbsolutePath();

		newerMap = new HashMap<>();
		olderMap = new HashMap<>();

		ignorePatternList = new ArrayList<>();

		final String[] patternStrings = { "\\w+.java", "\\w+.doc", "\\w+.docx", "\\w+.class" };

		for(String patternString : patternStrings) {
			try {
				addIgnorePattern(patternString);
			} catch(PatternSyntaxException e) {
				errHandler.add("Err: Invalid regex pattern: " + patternString);
			}
		}

		buildList(newerRootDir, newerMap);
		buildList(olderRootDir, olderMap);

		displayList("\nAfter building map of newerRootDir", newerRootDir, newerMap);
		displayList("\nAfter building map of olderRootDir", olderRootDir, olderMap);

		System.out.println();
		System.out.println("Finding diff...");
		Map<Path, Path> commons = new HashMap<>();

		// setting commons
		for(Map.Entry<Path, Path> entry : newerMap.entrySet()) {
			if(olderMap.containsKey(entry.getKey())) /* also match the files */
				commons.put(entry.getKey(), entry.getValue());
		}

		// setting uniques
		for(Map.Entry<Path, Path> entry : commons.entrySet()) {
			System.out.println("    " + entry);
			newerMap.remove(entry.getKey());
			olderMap.remove(entry.getKey());
		}


		System.out.println("\nCommon in both root dirs:");
		displayList("\nUnique in rootDir1", newerRootDir, newerMap);
		displayList("\nUnique in rootDir2", olderRootDir, olderMap);
	}

	private void addIgnorePattern(final String patternString) throws PatternSyntaxException {
		ignorePatternList.add(Pattern.compile(patternString));
	}

	private void displayList(    final String msg,
	                             final Path root,
	                             final Map<Path, Path> map) {
		System.out.println(msg + " (" + root + "):");
		for(Map.Entry<Path, Path> entry : map.entrySet()) {
			System.out.println("    " + entry);
		}
	}

	/**
	 * @param root Requires to be an absolute path
	 * */
	private void buildList(final Path root, final Map<Path, Path> map) throws IOException {
		System.out.println("root: " + root);
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				nextFile:
				for(Path file : Files.newDirectoryStream(dir)) {
					for(Pattern pattern : ignorePatternList) {
						if(pattern.matcher(file.getFileName().toString()).matches()) {
							continue nextFile; // skip incliuding this file in map (ignores)
						}
					}
					map.put(root.relativize(file), file.toAbsolutePath());
				}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException e) {
				if(e != null)
					System.err.printf("Error: Cannot list: %s (err: %s) %n", dir, e);

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException e) {
				if(e != null)
					System.err.printf("Error: Cannot list: %s (err: %s) %n", file, e);

				return FileVisitResult.CONTINUE;
			}
		});
	}

	/*---------------END OF PROTOTYPE 1---------------*/

	public static void addDirList(final Path dir, final Map<Path, Path> map) throws IOException {
		for(Path file : Files.newDirectoryStream(dir)) {
			String f = file.getFileName().toString();
			if(Files.isDirectory(file))
				f += "/";
			System.out.println("preVisitDirectory: " + f);
		}
	}


	private static void test3() throws IOException {
		String fname1="tmp/c.txt";

		try (FileInputStream file1 = new FileInputStream(fname1)) {
			long lineCount=0L, colCount=0L;
			int currByte;
			while((currByte=file1.read()) != -1) {
				colCount++;
				if(currByte=='\n') {
					lineCount++;
					colCount=0; // resets previous value
				}
			}

			System.out.printf("lineCount=%d, colCount=%d \n", lineCount, colCount);
		}
	}
	
	private static void test2() {
//		System.out.println(MyEnumTest.MyEnum.HONEY);
		// System.out.println(MyEnum2.APPLE);
	}
	
	private static void test1() {
		// int arr1[] = { 1, 2, 3, 4, 8 };
		// int arr2[] = { 1, 2, 4, 5, 6, 7, 9 };
		
		int arr2[] = { 1, 3, 2 };
		int arr1[] = { 7, 9, 6, 5, 4, 1, 2 };
		
		System.out.println("Sending these arrays to testArrDiff(int[], int[]):");
		System.out.println("  Array 1: " + Arrays.toString(arr1));
		System.out.println("  Array 2: " + Arrays.toString(arr2));
		
		testArrDiff(arr1, arr2);
	}
	
	/**
		Fills the given array to the 2D array
			initialising the parity fields also.
	*/
	private static void fillArray(	final int[] src, 
											final int[][] dst,
											final int len) {
		// Filling - src to dst array
		for(int i=0; i<len; i++) {
			dst[i][0] = src[i]; // copies the array element
			dst[i][1] = -1; // initializes the parity field
		}
				
		/* Sorting - not necessary
		// Sorting - Applying insertion sort
		for(int i=1, j, key; i<len; i++) {
			key = dst[i][0];			
			for(j=i-1; j>=0; j--) {
				if(dst[j][0] > key)
					dst[j+1][0] = dst[j][0];
				else 
					break;
			}
			dst[j+1][0] = key;
		}
		*/
	}
	
	private static void testArrDiff(final int[] arr1, final int[] arr2) {
		/*	array[i][0]=array element at i-th index &
			array[i][1]=parity indicator of i-th array element,
				with values -1=unchecked/new, 0=identical, 1=modified
		*/
		int a[][] = new int[arr1.length][2];
		int b[][] = new int[arr2.length][2];
		final int aLen = arr1.length, bLen = arr2.length;
		fillArray(arr1, a, aLen);
		fillArray(arr2, b, bLen);
		
		printArrays(a, "After filling, array 1"); // DEBUG
		printArrays(b, "After filling, array 2"); // DEBUG
		
		for(int i=0; i<aLen; i++) {
			for(int j=0; j<bLen; j++) {
				if(b[j][1] == -1) { // check if element is unchecked 
					if(a[i][0] == b[j][0]) { // match element values
						a[i][1] = b[j][1] = equalContents(a[i][0], b[j][0]) ? 0 : 1;
						break;
					}
				}
			}
		}
		
		printResult(a, "Result, array 1:"); // DEBUG
		printResult(b, "Result, array 2:"); // DEBUG
		
		printArrays(a, "After checking, array 1"); // DEBUG
		printArrays(b, "After checking, array 2"); // DEBUG
	}
	
	private static void printArrays(final int[][] arr, String msg) {
		System.out.println(msg + ": " + Arrays.deepToString(arr));
	}
	
	private static void printResult(final int[][] arr, final String msg) {
		System.out.println(msg);
		for(int i=0; i<arr.length; i++)
			System.out.printf("  [%s] : %d \n",
				arr[i][1] == -1 ? "N" : (arr[i][1] == 0 ? "I" : "M"), arr[i][0]);
	}
	
	/**
		@param a First value content to be checked
		@param b Second value content to be checked
		@return true if identical in content otherwise false
	*/
	private static boolean equalContents(final int a, final int b) {
		return a%2==0 && b%2==0; // demo return value
	}
}