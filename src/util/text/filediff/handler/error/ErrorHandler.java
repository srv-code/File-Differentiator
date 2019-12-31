package util.text.filediff.handler.error;

import java.io.IOException;
import java.util.Objects;


public class ErrorHandler implements AutoCloseable { // package-private class
	// Will include logging system later
	
	public enum Mode {
		STDERR_DISPLAY,
		FORM_DISPLAY, 
		LOG_WRITE, 
		SEND_NOTIF;
	}
	
	private final Mode mode;
	private final boolean showStackTrace;
	
	public ErrorHandler(final Mode mode, final boolean showStackTrace) {
		this.mode = Objects.requireNonNull(mode, "Null error handler mode");
		this.showStackTrace = showStackTrace;
	}

	public void add(final Exception e) {
		add(null, e);
	}
	

	public void add(final String msg, final Exception e) {
		StringBuilder strExc = new StringBuilder();
		if(msg != null)
			strExc.append(msg + ": ");
		strExc.append(e.toString());

		if(showStackTrace) { // replicate same view as default JRE exc handler
			for(StackTraceElement trace : e.getStackTrace())
				strExc.append("\n\t" + trace);
		}
		add(strExc.toString());
	}
	
	public void add(final String errMsg) {
		switch(mode) {
			case STDERR_DISPLAY: 
				System.err.println("Err: " + errMsg);
				break;
			
			default: 
				throw new AssertionError("Didn't build handlers for other modes yet");
		}
	}

	@Override
	public void close() throws IOException {
		// will need later for closing logger file handlers 
	}
}