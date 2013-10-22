package com.heideblitz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifIFD0Directory;

public class Main {
	public static void main(String[] args) throws Throwable {
		List<FileInfo> files = new ArrayList<FileInfo>();
		try {
			for (File file : new File(args[0]).listFiles()) {
				if (file.getName().startsWith(".")) {
					continue;
				}
				files.add(new FileInfo(file));
			}

			Collections.sort(files);
			if (args.length > 1 && args[1].trim().length() > 0) {
				Collections.reverse(files);
			}

			int n = 0;
			for (FileInfo file : files) {
				n++;
				file.renameTo(n);
			}
		} catch (Throwable e) {
			for (FileInfo file : files) {
				file.revertRenaming();
			}
			throw e;
		}
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
				ExifIFD0Directory directory = ImageMetadataReader.readMetadata(file).getDirectory(ExifIFD0Directory.class);
				if(directory != null){
					date = directory.getDate(ExifIFD0Directory.TAG_DATETIME);
				}
			} catch (ImageProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (date == null) {
				System.err.println("date is null: " + originalFile.getName());
			}

			// rename to temp
			tempFile = File.createTempFile(file.getName() + "__", "", file.getParentFile());
			file.renameTo(tempFile);
		}

		public Date getDate() {
			return date;
		}

		public String getExtension() {
			return extension;
		}

		public void renameTo(int n) {
			String ext = getExtension();
			File newFile = new File(originalFile.getParent(), (String.format("%04d", n) + (ext == null ? "" : "." + ext)).toLowerCase());
			System.out.println("'" + originalFile.getName() + "' -> '" + newFile.getName() + "'");
			tempFile.renameTo(newFile);
			tempFile = null;
		}

		public void revertRenaming() {
			if (tempFile != null) {
				tempFile.renameTo(originalFile);
			}
		}

		@Override
		public int compareTo(FileInfo fileInfo) {
			Date d1 = getDate();
			Date d2 = fileInfo.getDate();
			if (d1 == d2) {
				return originalFile.getName().compareTo(fileInfo.originalFile.getName());
			}
			if (d1 == null) {
				return -1;
			}
			if (d2 == null) {
				return 1;
			}
			return d1.compareTo(d2);
		}

	}
}
