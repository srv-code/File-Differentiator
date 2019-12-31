package util.text.filediff.diff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;


public class DiffResult {
	public enum Type {
		BYTE	("Byte"),                   /* difference in first byte in either of the file */
		NEWLINE ("Line terminator"),        /* difference in new line terminator character */
		MODTIME	("Mod time"),               /* difference in modifiication time of files */
		SIZE	("Size"),                   /* difference in file sizes */
		FTYPE   ("File type"),              /* different type of files (either dir & file OR file & dir) */
		NONE	("None");                   /* no difference, files are identical */
		
		private final String description;
		Type(String desc) {
			description = desc;
		}
		
		@Override
		public String toString() {
			return description;
		}
	}
	
	private Path newerFile, olderFile;
	private long[] diffValue;
	private Type diffType;
	private boolean areTextFiles;
	
	DiffResult( final Path file1,
				final Path file2,
				final boolean areTextFiles,
				final Type diffType,
				final long[] diffValue) { // package-private constructor
		this.newerFile = Objects.requireNonNull(file1, "Newer file is null for result");
		this.olderFile = Objects.requireNonNull(file2, "Older file is null for result");
		this.diffType = Objects.requireNonNull(diffType, "Null diff type for result");
		this.areTextFiles = areTextFiles;
		
		if(diffType != Type.NONE && diffType != Type.FTYPE && diffValue[0] == 0)
			throw new IllegalArgumentException("Diff value is 0 (zero) for " + diffType + " type in result");
		if(diffValue.length != 2)
			throw new IllegalArgumentException("Invalid array element count of diff value");
		
		this.diffValue = diffValue;
	}
	
	boolean areTextFiles() {
		return areTextFiles;
	}

	public Type getType() {
		return diffType;
	}

	public Path getNewerFile() {
		return newerFile;
	}

	public Path getOlderFile() {
		return olderFile;
	}

	public long[] getValue() {
		return diffValue;
	}

	@Override
	public String toString() {
		String strComment, strParity;

		switch(diffType) {
			case BYTE:
			case NEWLINE: /* Also displays in the same format */
				strParity = "new != old";

				if(areTextFiles) {
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
//      						"\t    Timestamp:  %ta %<td-%<tb-%<tY %<tI:%<tM:%<tS.%<tL %<Tp\n" +
								"\t    Timestamp:  %tc \n" +
								"\t    Size:       %,d B \n" +
								"\t  Older:        %s \n" +
//		        				"\t    Timestamp:  %ta %<td-%<tb-%<tY %<tI:%<tM:%<tS.%<tL %<Tp\n" +
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

	/** If any exception occurs then sends -1 */
	private long getFileTimeInMillis(final Path file) {
		try {
			return Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toMillis();
		} catch(IOException e) {
			return -1L;
		}
	}

	/** If any exception occurs then sends -1 */
	private long getFileSize(final Path file) {
		try {
			return Files.size(file);
		} catch(IOException e) {
			return -1L;
		}
	}
}