package com.zimmerbell.photo.stamp;

import java.awt.image.BufferedImage;
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
		System.out.println("usage: run.sh SOURCE_DIRECTORY [TARGET_DIRECTORY]");
		if(args.length == 0){
			return;
		}

		File directory = new File(args[0]);
		File outDirectory = args.length > 1 ? new File(args[1]) : directory;
		
		new Main(directory, outDirectory);
	}

	private Main(File directory, File outDirectory) throws Throwable {
		stampFilesInDirectory(directory, outDirectory);
	}

	private void log(String msg) {
		System.out.println(msg);
	}

	private void stampFilesInDirectory(File directory, File outDirectory) throws Throwable {
		try {
			outDirectory.mkdirs();

			for (File file : directory.listFiles()) {
				if (file.getName().startsWith(".")) {
					continue;
				}
				if (!file.isFile()) {
					if (file.isDirectory()) {
						stampFilesInDirectory(file, new File(outDirectory, file.getName()));
					}
					continue;
				}

				try {

					Directory exif = ImageMetadataReader.readMetadata(file).getDirectory(ExifSubIFDDirectory.class);
					Date date = exif == null ? null : exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

					stampPhoto(file, new File(outDirectory, file.getName()), date);
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

	private void stampPhoto(final File file, final File outFile, final Date date) throws IOException {
		log(date + ": " + file + " -> " + outFile);

		ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);
		Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
		if (!imageReaders.hasNext()) {
			log("no image reader found: " + file);
		} else {
			ImageReader imageReader = imageReaders.next();
			imageReader.setInput(imageInputStream, true);

			try {
				IIOImage iioImage = imageReader.readAll(0, null);

				BufferedImage image = (BufferedImage) iioImage.getRenderedImage();
				stampPhoto(image, date);

				ImageWriter writer = ImageIO.getImageWriter(imageReader);
				ImageWriteParam param = writer.getDefaultWriteParam();
				param.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
				writer.setOutput(ImageIO.createImageOutputStream(outFile));

				writer.write(null, iioImage, param);
				return;
			} catch (IIOException e) {
				log(e.getMessage());
			}
		}

		// fallback for reading/reading image
		BufferedImage image = ImageIO.read(file);
		stampPhoto(image, date);
		ImageIO.write(image, "jpg", outFile);

	}

	private void stampPhoto(BufferedImage image, Date date) throws IOException {

	}
}
