package com.zimmerbell.photo.stamp;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class Main {
	public static void main(String[] args) throws Throwable {
		System.out.println("usage: run.sh [DIRECTORY]");

		new Main(new File(args[0]));
	}

	private Main(File directory) throws Throwable {
		stampFilesInDirectory(directory);
	}

	private void log(String msg) {
		System.out.println(msg);
	}

	private void stampFilesInDirectory(File directory) throws Throwable {
		try {
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
					Directory exif = ImageMetadataReader.readMetadata(file).getDirectory(ExifSubIFDDirectory.class);
					if (exif == null) {
						log("can't read exif: " + file);
						continue;
					}
					Date date = exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
					stampPhoto(file, date);
				} catch (ImageProcessingException e) {
					log(file + ": " + e.getMessage());
				} catch (Throwable e) {
					log("error while processing file: " + file);
					throw e;
				}
			}
		} catch (Throwable e) {
			log("error while processing directory: " + directory);
			throw e;
		}
	}

	private void stampPhoto(File file, Date date) throws IOException {
		log(file + ": " + date);

		ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);
		Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
		if (!imageReaders.hasNext()) {
			log("no image reader found: " + file);
			return;
		}

		ImageReader imageReader = imageReaders.next();
		imageReader.setInput(imageInputStream, true);

		try {
			IIOImage image = imageReader.readAll(0, null);

			RenderedImage renderedImage = image.getRenderedImage();

			ImageWriter writer = ImageIO.getImageWriter(imageReader);

			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
			writer.setOutput(
					ImageIO.createImageOutputStream(new File(file.getParentFile(), file.getName() + "-copy.jpg")));

			writer.write(null, image, param);
		} catch (IIOException e) {
			log(e.getMessage());

			BufferedImage image = ImageIO.read(file);

			ImageIO.write(image, "jpg", new File(file.getParentFile(), file.getName() + "-copy.jpg"));
		}

	}
}
