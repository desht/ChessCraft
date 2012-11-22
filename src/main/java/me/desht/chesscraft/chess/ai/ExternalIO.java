package me.desht.chesscraft.chess.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import me.desht.dhutils.LogUtils;

public class ExternalIO {
	private Process process;
	private BufferedReader reader;
	private PrintWriter writer;
	
	private final String command;

	public ExternalIO(String command) {
		this.command = command;
	}

	public void start() {
		Runtime runtime = Runtime.getRuntime();

		try {
			process = runtime.exec(command);
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);
			runtime.addShutdownHook(new Thread() {
				@Override
				public void run() { cleanup(); }
			});
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void cleanup() {
		if (writer != null) {
			writeLine("quit");
			writer.close();
		}
		destroy();
	}

	public String readLine() throws IOException {
		String string = reader.readLine();
		LogUtils.fine("ExternalIO: " + command + ": read [" + string + "]");
		return string;
	}

	public void writeLine(String string) {
		writer.println(string);
		LogUtils.fine("ExternalIO: " + command + ": wrote [" +string + "]");
	}

	public void restart() {
		destroy();
		start();
	}

	public void destroy() {
		process.destroy();
	}
}
