package util.text.filediff.diff;

import java.io.IOException;
import java.nio.file.*;

/**
 * Converts DiffResult object to proper string format
 *  for reporting.
 * */
@Deprecated
public class DiffResultFormatter {

	/**
	 * Formats DiffResult object to string format
	 * */
	public static String format(final DiffResult result) {
		String strComment, strParity;
		long[] diffValue = result.getValue();
		DiffResult.Type diffType = result.getType();
		Path newerFile = result.getNewerFile();
		Path olderFile = result.getOlderFile();

		switch(diffType) {
			case BYTE:
			case NEWLINE: /* Also displays in the same manner */
				strParity = "new != old";

				if(result.areTextFiles()) {
					strComment = "First mismatch at: line " + diffValue[0] + ", byte " + diffValue[1];
				} else {
					strComment = "First mismatch at: byte " + diffValue[0];
				}
				break;

			case SIZE:
				strParity = diffValue[0] == 0 ? "new = old" : (diffValue[0] > 0 ? "new > old" : "new < old");
				strComment = "Larger size is " + Math.abs(diffValue[0]);
				break;

			case MODTIME:
				strParity = diffValue[0] == 0 ? "new = old" : (diffValue[0] > 0 ? "new > old" : "new < old");
				strComment = String.format("Newer timestamp is %tI:%<tM:%<tS.%<tL %<Tp", Math.abs(diffValue[0]));
				break;

			case FTYPE:
				strParity = "new != old";
				/* Both elements of diffValue must be of value 0 or 1 with alteration
					else ArrayIndexOutOfBoundsException will be thrown */
				String[] ftype = { "dir", "file" };
				strComment = "Different file types: " + ftype[(int)diffValue[0]] + " & " + ftype[(int)diffValue[1]];
				break;

			case NONE:
				strParity = "new = old";
				strComment = "Identical files";
				break;

			default:
				throw new AssertionError("Should not get here: Invalid diffType: " + diffType);
		}

		return String.format(   "\t  Newer:        %s \n" +
//								"\t    Timestamp:  %ta %<td-%<tb-%<tY %<tI:%<tM:%<tS.%<tL %<Tp\n" +
								"\t    Timestamp:  %tc \n" +
								"\t    Size:       %,d B \n" +
								"\t  Older:        %s \n" +
//								"\t    Timestamp:  %ta %<td-%<tb-%<tY %<tI:%<tM:%<tS.%<tL %<Tp\n" +
								"\t    Timestamp:  %tc \n" +
								"\t    Size:       %,d B \n" +
								"\t  Parity:       %s \n" +
								"\t  Type:         %s \n" +
								"\t  Comment:      %s \n",

								newerFile + (Files.isDirectory(newerFile) ? "/" : ""),
								getFileTimeInMillis(newerFile),
								getFileSize(newerFile),
								olderFile + (Files.isDirectory(olderFile) ? "/" : ""),
								getFileTimeInMillis(olderFile),
								getFileSize(olderFile),
								strParity,
								diffType,
								strComment);
	}

	/**
	 * @return File modification time in milliseconds. If any exception occurs then -1.
	 * */
	private static long getFileTimeInMillis(final Path file) {
		try {
			return Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toMillis();
		} catch(IOException e) {
			return -1L;
		}
	}

	/**
	 * @return File size in bytes. If any exception occurs then -1.
	 * */
	private static long getFileSize(final Path file) {
		try {
			return Files.size(file);
		} catch(IOException e) {
			return -1L;
		}
	}
}
