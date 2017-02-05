package com.zimmerbell;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class PhotoStamp {
	public static void main(String[] args) throws Throwable {
		System.out.println("usage: run.sh [DIRECTORY]");
		
		stampFilesInDirectory(new File(args[0]));
	}
	
	private static void log(String msg){
		System.out.println(msg);
	}
	
	private static void stampFilesInDirectory(File directory) throws Throwable {
		for (File file : directory.listFiles()) {
			if (file.getName().startsWith(".")) {
				continue;
			}
			if (!file.isFile()) {
				if (file.isDirectory()) {
					stampFilesInDirectory(file);
				}
				continue;
			}
			
			
			try {
				// Directory directory =
				// ImageMetadataReader.readMetadata(file).getDirectory(ExifIFD0Directory.class);
				// if(directory != null){
				// date = directory.getDate(ExifIFD0Directory.TAG_DATETIME);
				// }
				Directory exif = ImageMetadataReader.readMetadata(file).getDirectory(ExifSubIFDDirectory.class);
				if (exif == null) {
					log("can't read exif: " + file);
					continue;
				}
				Date date = exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
				log(file + ": " + date);
				
			} catch (ImageProcessingException e) {
				log(e.getMessage());
			} catch (IOException e) {
				log(e.getMessage());
			}
		}
	}
}
