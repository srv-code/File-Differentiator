package tester;

import util.text.filediff.handler.fs.FileSystemHandler;
import util.text.filediff.handler.error.ErrorHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;


public class Main1 {
	private static final float APP_VERSION = 1.0f;

	private static String[] filterPatterns = null; // default value set
	private static boolean  patternToAccept; // default value set
	private static boolean  showModDetail = true; // default value set
	private static boolean  showFilePath = true; // default value set
	private static boolean  showCommons = true; // default value set
	private static boolean  showMods = true; // default value set
	private static boolean  showNewInNewer = true; // default value set
	private static boolean  showNewInOlder = true; // default value set
	private static boolean  showErrDetails = false; // default value set
	private static boolean  checkAsTextFiles = true; // default value set
	private static String   newerRootPath = null, olderRootPath = null; // default value set
	private static boolean  checkMTime = false, checkSize = false, checkContents = true; // default values set

	private static final char regexPatternSeparatorChar = ';';

	public static void main(String[] args) {
		processCommandLineArguments(args);
		start();
	}

	private static void start() {
		ErrorHandler errHandler = new ErrorHandler(ErrorHandler.Mode.STDERR_DISPLAY, showErrDetails);
		try {
			FileSystemHandler fsHandler = new FileSystemHandler(errHandler, newerRootPath, olderRootPath, checkAsTextFiles);
			System.out.println("Newer root: " + newerRootPath);
			System.out.println("Older root: " + olderRootPath);

			if(filterPatterns != null && filterPatterns.length > 0) {
				System.out.printf("\nApplying all %d %s patterns... \n",
						filterPatterns.length, patternToAccept ? "accept" : "ignore");
				for(String patt : fsHandler.addPatterns(filterPatterns, patternToAccept))
					System.out.printf("Err: Invalid pattern: %s (ignored) \n", patt);
			}

			fsHandler.checkContents(checkContents);
			fsHandler.checkMTime(checkMTime);
			fsHandler.checkSize(checkSize);

			fsHandler.process();

			System.out.printf("\nResults: ");

			if(showMods) {
				Set<Map.Entry<Path, Object[]>> mods = fsHandler.getMods();
				System.out.println("\n    Modified file list (count: " + mods.size() + "): ");
				for (Map.Entry<Path, Object[]> entry : mods) {
					showResultLine( Files.isDirectory((Path) entry.getValue()[0]),
									entry.getKey().getFileName(),
									entry.getKey());
//									(Path) entry.getValue()[0]);

					if(showModDetail)
						System.out.printf("\tMod info:\n%s \n",
								entry.getValue()[1].toString());
				}
			}

			if(showCommons) {
				Set<Map.Entry<Path, Path>> commons = fsHandler.getCommons();
				System.out.println("\n    Common file list (count: " + commons.size() + "): ");
				for (Map.Entry<Path, Path> entry : commons) {
					showResultLine( Files.isDirectory(entry.getValue()),
									entry.getValue().getFileName(),
									entry.getKey());
				}
			}

			if(showNewInNewer) {
				Set<Map.Entry<Path, Path>> newNewer = fsHandler.getNewInNewer();
				System.out.println("\n    New files in newer dir (count: " + newNewer.size() + "): ");
				for (Map.Entry<Path, Path> entry : newNewer) {
					showResultLine( Files.isDirectory(entry.getValue()),
							entry.getValue().getFileName(),
							entry.getKey());
				}
			}

			if(showNewInOlder) {
				Set<Map.Entry<Path, Path>> newOlder = fsHandler.getNewInOlder();
				System.out.println("\n    New files in older dir (count: " + newOlder.size() + "): ");
				for (Map.Entry<Path, Path> entry : newOlder) {
					showResultLine( Files.isDirectory(entry.getValue()),
							entry.getValue().getFileName(),
							entry.getKey());
				}
			}
		} catch(Exception e) {
			errHandler.add(e);
			System.exit(1);
		}
	}

	private static void showResultLine(final boolean isDir, final Path fileName, final Path filePath) {
		System.out.printf("        %6s %s",
				isDir ? "[DIR]" : "[FILE]", fileName.toString());
		if(showFilePath)
			System.out.printf(" (%s)", filePath.toString());
		System.out.println();
	}

	private static void processCommandLineArguments(final String[] args) {
		String currOptionProcessing = null; /* For keeping a track in catch block of options which require additional argument */
		try {
			for(int i=0; i<args.length; i++) {
				currOptionProcessing = args[i];
//				System.out.println("  // processing arg: " + currOptionProcessing);
				switch (currOptionProcessing) {
					case "-b":
					case "--bin":
						if(!checkAsTextFiles)
							throw new IllegalArgumentException("Option value was already set");
						checkAsTextFiles = false; // check as binary files
						break;
					
					case "-a":
					case "--accept": /* Provide file name patterns, separated by pipe (|) */
						if(filterPatterns != null)
							throw new IllegalArgumentException("Option value was already set");
						filterPatterns = getPatternSplits(args[++i]);
						patternToAccept = true;
						break;

					case "-i":
					case "--ignore": /* Provide file name patterns, separated by pipe (|) */
						if(filterPatterns != null)
							throw new IllegalArgumentException("Option value was already set");
						filterPatterns = getPatternSplits(args[++i]);
						patternToAccept = false;
						break;

					case "-e":
					case "--fullerr": /* Enable switch in ErrorHandler object to show stack trace also */
						if(showErrDetails)
							throw new IllegalArgumentException("Option value was already set");
						showErrDetails = true;
						break;

					case "-n":
					case "--newer":
						if(newerRootPath != null)
							throw new IllegalArgumentException("Option value was already set");
						newerRootPath = args[++i];
						break;

					case "-o":
					case "--older":
						if(olderRootPath != null)
							throw new IllegalArgumentException("Option value was already set");
						olderRootPath = args[++i];
						break;

					case "-m":
					case "--mtime":
						if(checkMTime)
							throw new IllegalArgumentException("Option value was already set");
						checkMTime = true;
						break;

					case "-s":
					case "--size":
						if(checkSize)
							throw new IllegalArgumentException("Option value was already set");
						checkSize = true;
						break;

					case "-D":
					case "--nodata":
						if(!checkContents)
							throw new IllegalArgumentException("Option value was already set");
						checkContents = false;
						break;

					case "-l":
					case "--list": /* Show simplified file name list only, suppress any modification detail or file path details */
						if(!showModDetail)
							throw new IllegalArgumentException("Option value was already set");
						showModDetail = false;
						break;

					case "-P":
					case "--hidepath": /* Suppresses the relative paths beside file names */
						if(!showFilePath)
							throw new IllegalArgumentException("Option value was already set");
						showFilePath = false;
						break;

					case "-C":
					case "--nocomm": /* Suppress showing common file name list */
						showCommons = false;
						break;

					case "-M":
					case "--nomods": /* Suppress showing modified file name list */
						showMods = false;
						break;

					case "-N":
					case "--nonnewer": /* Suppress showing newer file name list in newer root dir tree */
						showNewInNewer = false;
						break;

					case "-O":
					case "--nonolder": /* Suppress showing newer file name list in older root dir tree */
						showNewInOlder = false;
						break;

					case "-h":
					case "--help":
						showHelp();
						System.exit(0);
						break;

					default:
						throw new IllegalArgumentException("Invalid option");
				}
			}

			/* Check for mandatory option values */
			if(newerRootPath == null)
				throw new IllegalArgumentException("Newer root must be specified");
			if(olderRootPath == null)
				throw new IllegalArgumentException("Older root must be specified");

		} catch(ArrayIndexOutOfBoundsException e) {
			System.err.printf("Err: Not enough option values (%s) \n", currOptionProcessing);
			System.exit(1);
		} catch(IllegalArgumentException e) {
			System.err.print("Err: " + e.getMessage());
			if(currOptionProcessing != null)
				System.out.print(" (" + currOptionProcessing + ")");
			System.out.println();
			System.exit(1);
		}
	}

	/** <p> NEEDS IMPROVEMENT!!! </p>
	 *  <p> Bug: </p>
	 *  <p> </>"  \\w+.java  |    \\w+.class   ".split("[\\s|]+");  </p>
	 *  <p> $56 ==> String[3] { "", "\\w+.java", "\\w+.class" } </p>
	 *  <p> Remove the first empty string </p>
	 */
	private static String[] getPatternSplits(final String pattStr) {
		return pattStr.split("[\\s"+ regexPatternSeparatorChar +"]+");
	}

	private static void showHelp() {
		System.out.println("Purpose:   Shows relative differences in the two specified directory trees");
		System.out.println("Usage:     <program_name> [-option1[-option1...]] -n <newer_dir_root|newer_file> -o <older_dir_root|older_file>");
		System.out.printf ("Version:   %.2f \n", APP_VERSION);

		System.out.println("Options:");
		System.out.println("  --help,     -h    Shows this help menu and exits");
		System.out.println("  --bin,      -b    Indicates that the files to be checked contains binary data");
		System.out.println("  --accept,   -a    Specify accept regex patterns, delimited by character: " + regexPatternSeparatorChar);
		System.out.println("  --ignore,   -i    Specify reject regex patterns, delimited by character: " + regexPatternSeparatorChar);
		System.out.println("  --fullerr,  -e    Enables showing error stack trace along with the error type and message");
		System.out.println("  --newer,    -n    Specify newer dir root/file");
		System.out.println("  --older,    -o    Specify older dir root/file");
		System.out.println("  --list,     -l    Show list of file names, suppressing modification details");
		System.out.println("  --mtime,    -m    Check for changes in modification timestamp");
		System.out.println("  --size,     -s    Check for changes in file sizes");
		System.out.println("  --nodata,   -D    Don't check for changes in file contents");
		System.out.println("  --hidepath, -P    Suppresses the relative paths beside file names");
		System.out.println("  --nocomm,   -C    Suppress showing common files");
		System.out.println("  --nomods,   -M    Suppress showing modified files");
		System.out.println("  --nonnewer, -N    Suppress showing new files in newer root");
		System.out.println("  --nonolder, -O    Suppress showing new files in older root");
	}
}