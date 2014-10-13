package com.heideblitz;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class Main {

	private final static int DIGITS = 4;
	private final static DateFormat DATE_FORMAT = new SimpleDateFormat(
									   "yyyy-MM-dd_HH:mm:ss");
									   // "yyyy-MM-dd_HH~mm~ss");
	private static Order order = null;

	public static void main(String[] args) throws Throwable {
		System.out.println("usage: run.sh [DIRECTORY] [asc|desc]");
		if (args.length > 1) {
			order = Order.valueOf(args[1]);
		}

		renameFilesInDirectory(new File(args[0]));
	}

	private static void renameFilesInDirectory(File directory) throws Throwable {
		List<FileInfo> files = new ArrayList<FileInfo>();
		try {
			for (File file : directory.listFiles()) {
				if (file.getName().startsWith(".")) {
					continue;
				}
				if (!file.isFile()) {
					if (file.isDirectory()) {
						renameFilesInDirectory(file);
					}
					continue;
				}
				files.add(new FileInfo(file));
			}

			Collections.sort(files);

			int n = 0;
			for (FileInfo file : files) {
				n++;
				if (order == null) {
					file.renameTo(null);
				} else {
					switch (order) {
					case asc:
						file.renameTo(n);
						break;
					case desc:
					    file.renameTo((int) Math.pow(10, DIGITS) - n);
						break;
					}
				}
			}
		} catch (Throwable e) {
			for (FileInfo file : files) {
				file.revertRenaming();
			}
			throw e;
		}
	}

	private static enum Order {
		asc, desc
	}

	private static class FileInfo implements Comparable<FileInfo> {

		private File originalFile;
		private File tempFile;
		private Date date;
		private String extension;

		public FileInfo(File file) throws IOException {
			this.originalFile = file;

			// extract extension
			String name = file.getName();
			int idx = name.lastIndexOf('.');
			if (idx >= 0) {
				extension = name.substring(idx + 1);
			}

			// read EXIF
			try {
				// Directory directory =
				// ImageMetadataReader.readMetadata(file).getDirectory(ExifIFD0Directory.class);
				// if(directory != null){
				// date = directory.getDate(ExifIFD0Directory.TAG_DATETIME);
				// }
				Directory directory = ImageMetadataReader.readMetadata(file)
						.getDirectory(ExifSubIFDDirectory.class);
				if (directory != null) {
					date = directory
							.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
				}
			} catch (ImageProcessingException e) {
				System.err.println(e.getMessage());
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}

			if (date == null) {
				System.err.println("date is null: " + originalFile.getName());
			}

			// rename to temp
			tempFile = File.createTempFile(file.getName() + "__", "", file.getParentFile());
			if(!file.renameTo(tempFile)){
			    throw new RuntimeException("can't rename \"" + file.getName() + "\" to \"" + tempFile.getPath() + "\"");
			}
		}

		public Date getDate() {
			return date;
		}

		public String getExtension() {
			return extension;
		}

		public void renameTo(Integer n) {
		    if(d == null && n == null){
			if(!tempFile.renameTo(originalFile)){
			    throw new RuntimeException("can't rename \"" + tempFile.getPath() + "\" to \"" + originalFile.getPath() + "\"");
			}
			return;
		    }
			String ext = getExtension();
			Date d = getDate();
			StringBuilder name = new StringBuilder();
			if (n != null) {
				name.append(String.format("%0" + DIGITS + "d", n));
			}
			if (d != null) {
				if (name.length() > 0) {
					name.append("_");
				}
				name.append(DATE_FORMAT.format(d));
			}
			File newFile;
			int copy = 0;
			do {
				newFile = new File(originalFile.getParent(), name.toString()
						+ (copy == 0 ? "" : " (" + copy + ")")
						+ (ext == null ? "" : "." + ext.toLowerCase()));
				copy++;
			} while (newFile.exists());
			System.out.println("'" + originalFile.getName() + "' -> '"
					+ newFile.getName() + "' (" + getDate() + ")");
			if(!tempFile.renameTo(newFile)){
			    throw new RuntimeException("can't rename \"" + tempFile.getPath() + "\" to \"" + newFile.getPath() + "\"");			    
			}
			tempFile = null;
		}

		public void revertRenaming() {
			if (tempFile != null) {
				tempFile.renameTo(originalFile);
			}
		}

		@Override
		public int compareTo(FileInfo fileInfo) {
			boolean descending = Order.desc.equals(order);
			Date d1 = getDate();
			Date d2 = fileInfo.getDate();
			if (d1 == d2) {
				return originalFile.getName().compareTo(
						fileInfo.originalFile.getName())
						* (descending ? -1 : 1);
			}
			if (d1 == null) {
				return (descending ? -1 : 1);
			}
			if (d2 == null) {
				return (descending ? 1 : -1);
			}
			return d1.compareTo(d2);
		}

	}
}
