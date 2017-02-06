package com.zimmerbell.photo.stamp;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.FileUtils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifSubIFDDescriptor;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class Main {
	public final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	public static void main(String[] args) throws Throwable {
		System.out.println("usage: run.sh SOURCE_DIRECTORY [TARGET_DIRECTORY]");
		if (args.length == 0) {
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

				File outFile = new File(outDirectory, file.getName());

				try {
					ExifSubIFDDirectory exif = ImageMetadataReader.readMetadata(file).getDirectory(ExifSubIFDDirectory.class);
					Date date = exif == null ? null : exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

					stampPhoto(file, outFile, date);
				} catch (Exception e) {
					log(file + ": " + e.getMessage());
					FileUtils.copyFile(file, outFile);
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
				stampPhoto(image, date, outFile.getName());

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
		stampPhoto(image, date, outFile.getName());
		ImageIO.write(image, "jpg", outFile);

	}

	private void stampPhoto(BufferedImage image, Date date, String fileName) throws IOException {
		String dateString = "";
		if (date != null) {
			dateString = DATE_FORMAT.format(date);
		} else {
			int idx = fileName.lastIndexOf('.');
			dateString = idx < 0 ? fileName : fileName.substring(0, idx);
		}

		int margin = 10;

		Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 30);
		Graphics2D graphics = (Graphics2D) image.getGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		graphics.setColor(Color.WHITE);
		graphics.setFont(font);

		int x = image.getWidth() - margin - graphics.getFontMetrics(font).stringWidth(dateString);
		int y = image.getHeight() - margin;

		graphics.drawString(dateString, x, y);
	}
}
