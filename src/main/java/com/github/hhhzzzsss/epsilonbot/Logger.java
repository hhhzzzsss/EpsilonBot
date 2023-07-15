package com.github.hhhzzzsss.epsilonbot;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Logger {
	public static File logDir = new File("logs");
	public static File logFile = new File(logDir, "logFile.txt");
	public static OutputStreamWriter logWriter;
	
	public static LocalDate currentLogDate;
	public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("'['dd-MM-yy HH:mm:ss']' ");
	
	public static String prevEntry = "";
	public static int duplicateCounter = 1;
	
	public static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	public static int spamLevel = 0;
	public static long freezeTime = 0;
	
	static {
		init();
		executor.scheduleAtFixedRate(() -> {
			try {
				tick();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 0, 50, TimeUnit.MILLISECONDS);
	}
	
	public static void init() {
		if (!logDir.exists()) {
			logDir.mkdir();
		}
		
		try {
			if (!logFile.exists()) {
				makeNewLogFile();
			} else if (!logIsCurrent(logFile)) {
				compressLogFile();
				makeNewLogFile();
			} else {
				openLogFile();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* 
	 * It schedules its own tick cycle in its own executor.
	 * The tick cycle has to run independently of the bot instances because there is only a single log destination for all the bots.
	 */
	private static void tick() {
		if (freezeTime <= System.currentTimeMillis() && spamLevel > 0) {
			spamLevel--;
		}
		if (!currentLogDate.equals(LocalDate.now())) {
			try {
				compressLogFile();
				makeNewLogFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static synchronized void makeNewLogFile() throws IOException {
		currentLogDate = LocalDate.now();
		logWriter = new OutputStreamWriter(new FileOutputStream(logFile, false), StandardCharsets.UTF_8);
		logWriter.write(currentLogDate.toString() + '\n');
		logWriter.flush();
	}
	
	public static synchronized void openLogFile() throws IOException {
		currentLogDate = LocalDate.parse(getLogDate(logFile));
		logWriter = new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8);
	}
	
	public static synchronized void compressLogFile() throws IOException {
		if (Files.size(logFile.toPath()) > 100*1024*1024) { // Will not save because log file is too big
			return;
		}
		
		File gzipFile = new File(logDir, getLogDate(logFile) + ".txt.gz");
		FileInputStream in = new FileInputStream(logFile);
		GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzipFile));
		
		byte[] buffer = new byte[1024];
		int size;
		while ((size = in.read(buffer)) > 0) {
			out.write(buffer, 0, size);
		}
		in.close();
		out.finish();
		out.close();
	}
	
	public static synchronized BufferedReader getLogReader(File file) throws IOException {
		GZIPInputStream in = new GZIPInputStream(new FileInputStream(file));
		return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
	}
	
	public static synchronized String getLogDate(File file) throws IOException {
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
		BufferedReader reader = new BufferedReader(isr);
		String date = reader.readLine();
		reader.close();
		return date;
	}
	
	public static synchronized boolean logIsCurrent(File file) throws IOException {
		LocalDate date = LocalDate.now();
		return getLogDate(file).equals(date.toString());
	}
	
	public static synchronized void log(String str) {
		if (freezeTime > System.currentTimeMillis()) {
			return;
		}
		
		try {
			if (spamLevel >= 100) {
				spamLevel = 80;
				freezeTime = System.currentTimeMillis() + 20000;
				
				if (duplicateCounter > 1) {
					logWriter.write(String.format(" [x%s]\n", duplicateCounter));
				} else {
					logWriter.write("\n");
				}
				
				logWriter.write("Logs temporarily frozen due to spam");
				logWriter.flush();
				
				duplicateCounter = 1;
				prevEntry = "";
				
				return;
			}
			
			if (str.equalsIgnoreCase(prevEntry)) {
				duplicateCounter++;
			} else {
				if (duplicateCounter > 1) {
					logWriter.write(String.format(" [x%s]\n", duplicateCounter));
				} else {
					logWriter.write("\n");
				}
				logWriter.write(getTimePrefix() + str.replaceAll("\\[x(\\d+?)\\](?=$|[\r\n])", "[x/$1]")); // the replaceAll will prevent conflicts with the duplicate counter
				logWriter.flush();
				
				duplicateCounter = 1;
				prevEntry = str;
				
				spamLevel+=2;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void shutdownAndAwaitTermination() {
		Logger.executor.shutdownNow();
		try {
			Logger.executor.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	public static String getTimePrefix() {
		LocalDateTime dateTime = LocalDateTime.now();
		return dateTime.format(dateTimeFormatter);
	}
}
