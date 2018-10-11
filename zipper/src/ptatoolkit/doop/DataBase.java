package ptatoolkit.doop;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * A class that represents the database of Doop.
 *
 */
public class DataBase {

	private final File dbDir;
	private final File cacheDir;
	private final String program;
	
	public DataBase(File dbDir, File cacheDir, String program) {
		this.dbDir = dbDir;
		this.cacheDir = cacheDir;
		this.program = program;
	}

	/** Return the results of the give query. */
	public Iterator<List<String>> query(Query query) {
		File resultFile = getResultFile(query);
		return new QueryResultItr(query, resultFile);
	}

	/**
	 * Get the result file according to given query. If the file does not exist,
	 * invoke bloxbatch to create the file.
	 * @param query
	 * @return
	 */
	private File getResultFile(Query query) {
		String queryName = query.name();
		File resultFile = new File(getResultFilePath(queryName));
		if (!resultFile.exists()) {
			// Invoke bloxbatch to create the result file
			String[] cmd = new String[3];
			cmd[0] = "/bin/sh";
			cmd[1] = "-c";
			// Must use single quotation marks ('') to surround the query,
			// otherwise /bin/sh may fail.
			cmd[2] = String.format("bloxbatch -db %s -query '%s' > %s",
					dbDir.getAbsolutePath(), query.toString(),
					resultFile.toString());
			try {
				Runtime rt = Runtime.getRuntime();
				Process proc = rt.exec(cmd);
				proc.waitFor();
			} catch (InterruptedException e) {
				throw new RuntimeException("Exception during query: "
						+ query.toString());
			} catch (IOException e) {
				throw new RuntimeException("Query " + query +
						" (" + queryName + ") fails, " +
						"caused by " + e.getMessage());
			}
		}
		return resultFile;
	}
	
	private String getResultFilePath(String queryName) {
		String fileName = String.format("%s.%s", program, queryName);
		String filePath = String.format("%s%s%s",
				cacheDir.getAbsolutePath(), File.separator, fileName);
		return filePath;
	}
}
