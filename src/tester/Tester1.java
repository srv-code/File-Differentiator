package tester;

import util.text.filediff.handler.fs.FileSystemHandler;
import util.text.filediff.handler.error.ErrorHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;


@Deprecated
public class Tester1 {
	public static void main(String[] args) {
		if(args.length != 2) {
			System.err.println("Usage: %s <path 1> <path 2>");
			System.exit(1);
		}

		ErrorHandler errHandler = new ErrorHandler(ErrorHandler.Mode.STDERR_DISPLAY, true);
		try {

			FileSystemHandler fsHandler = new FileSystemHandler(errHandler, args[0], args[1], true);
			System.out.println("Newer root dir: " + args[0]);
			System.out.println("Older root dir: " + args[1]);

			System.out.println("\nApplying ignore patterns:");
			final String[] patternStrings = {
						"\\w+.java",
						"\\w+.doc",
						"\\w+.docx",
						"\\w+.class"
				};
			fsHandler.addPatterns(patternStrings, true);

			fsHandler.process();

			System.out.printf("\nResults: ");

			Set<Map.Entry<Path, Object[]>> mods = fsHandler.getMods();
			System.out.println("\n    Modified file list (count: "+ mods.size() +"): ");
			for(Map.Entry<Path, Object[]> entry : mods) {
				System.out.printf("        %6s %s (%s) \n\t\t{Mod info: %s} \n",
						Files.isDirectory((Path) entry.getValue()[0]) ? "[DIR]" : "[FILE]",
						entry.getKey().toString(),
						entry.getValue()[0].toString(),
						entry.getValue()[1].toString());
			}

			Set<Map.Entry<Path, Path>> commons = fsHandler.getCommons();
			System.out.println("\n    Common file list (count: "+ commons.size() +"): ");
			for(Map.Entry<Path, Path> entry : commons) {
				System.out.printf("        %6s %s (%s) \n",
						Files.isDirectory(entry.getValue()) ? "[DIR]" : "[FILE]",
						entry.getValue().toString(),
						entry.getKey().toString());
			}

			Set<Map.Entry<Path, Path>> newNewer = fsHandler.getNewInNewer();
			System.out.println("\n    New files in newer dir (count: "+ newNewer.size() +"): ");
			for(Map.Entry<Path, Path> entry : newNewer) {
				System.out.printf("        %6s %s (%s) \n",
						Files.isDirectory(entry.getValue()) ? "[DIR]" : "[FILE]",
						entry.getValue().toString(),
						entry.getKey().toString());
			}

			Set<Map.Entry<Path, Path>> newOlder = fsHandler.getNewInOlder();
			System.out.println("\n    New files in older dir (count: "+ newOlder.size() +"): ");
			for(Map.Entry<Path, Path> entry : newOlder) {
				System.out.printf("        %6s %s (%s) \n",
						Files.isDirectory(entry.getValue()) ? "[DIR]" : "[FILE]",
						entry.getValue().toString(),
						entry.getKey().toString());
			}
		} catch(Exception e) {
			errHandler.add(e);
			System.exit(1);
		}
	}
}