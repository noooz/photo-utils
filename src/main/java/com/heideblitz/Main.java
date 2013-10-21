package com.heideblitz;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifIFD0Directory;

public class Main {
	public static void main(String[] args) throws Throwable {
		final Map<File, String> extMap = new HashMap<File, String>();
		final Map<File, File> tempFileMap = new HashMap<File, File>();

		List<File> files = new ArrayList<File>();
		try{
		for (File file : new File(args[0]).listFiles()) {
			if (file.getName().startsWith(".")) {
				continue;
			}
			File newFile = File.createTempFile(file.getName() + "__", "", file.getParentFile());
			files.add(newFile);
			file.renameTo(newFile);
			tempFileMap.put(newFile, file);

			String name = file.getName();
			int idx = name.lastIndexOf('.');
			if (idx >= 0) {
				extMap.put(newFile, name.substring(idx + 1));
			}
		}

		final SortedMap<File, Date> dateMap = new TreeMap<File, Date>();
		for (File file : files) {
			ExifIFD0Directory directory = ImageMetadataReader.readMetadata(file).getDirectory(ExifIFD0Directory.class);
			Date date = directory.getDate(ExifIFD0Directory.TAG_DATETIME);
			System.out.println(file + ": " + date);
			dateMap.put(file, date);
		}

		Collections.sort(files, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return dateMap.get(o1).compareTo(dateMap.get(o2));
			}
		});

		int n = 0;
		for (File file : files) {
			n++;
			String ext = extMap.get(file);
			File newFile = new File(file.getParent(), String.format("%04d", n) + (ext == null ? "" : "." + ext));
			System.out.println("'" + file.getName() + "' -> '" + newFile.getName() + "'");
			file.renameTo(newFile);
			tempFileMap.remove(file);
		}

		}catch(Throwable e){
		    for(Map.Entry<File,File> entry : tempFileMap.entrySet()){
			entry.getKey().renameTo(entry.getValue());
		    }
		    throw e;
		}
	}
}
