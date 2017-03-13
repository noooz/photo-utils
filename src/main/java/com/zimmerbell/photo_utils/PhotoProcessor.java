package com.zimmerbell.photo_utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.UnaryOperator;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class PhotoProcessor {
	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmm");
	private final static UnaryOperator<Integer> FONT_SIZE = (Integer imageHeight) -> Math.max((int) (imageHeight * 0.025), 10);
	private final static UnaryOperator<Integer> MARGIN = (Integer imageHeight) -> 10;

	private CommandLine cl;
	private Set<File> newFiles = new HashSet<File>();

	public PhotoProcessor(CommandLine cl) throws Throwable {
		this.cl = cl;

		File sourceDirectory = new File(cl.getOptionValue(Main.OPT_SOURCE, System.getProperty("user.dir")));
		File destinationDirectory = new File(cl.getOptionValue(Main.OPT_DESTINATION, System.getProperty("user.dir")));

		processFilesInDirectory(sourceDirectory, destinationDirectory);
	}

	private void log(String msg) {
		System.out.println(msg);
	}

	private void processFilesInDirectory(File directory, File outDirectory) throws Throwable {
		try {
			outDirectory.mkdirs();

			for (File file : directory.listFiles()) {
				if (file.getName().startsWith(".")) {
					continue;
				}
				if (!file.isFile()) {
					if (file.isDirectory()) {
						processFilesInDirectory(file, new File(outDirectory, file.getName()));
					}
					continue;
				}

				ExifSubIFDDirectory exif = ImageMetadataReader.readMetadata(file).getDirectory(ExifSubIFDDirectory.class);
				Date date = exif == null ? null : exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

				File outFile;
				if (cl.hasOption(Main.OPT_RENAME)) {
					outFile = destinationFile(outDirectory, file.getName(), date);
				}else{
					outFile = new File(outDirectory, file.getName());
				}
				
				newFiles.add(outFile);

				try {
					processFile(file, outFile, date);
				} catch (Exception e) {
					log(file + ": " + e.getMessage());
					if (cl.hasOption(Main.OPT_OVERWRITE) || !outFile.exists()) {
						FileUtils.copyFile(file, outFile);
					}
				}
			}
		} catch (Throwable e) {
			log("error while processing directory: " + directory);
			throw e;
		}
	}

	private File destinationFile(final File destinationDirectory, final String fileName, final Date date) {
		String newName;
		if (date == null) {
			newName = FilenameUtils.getBaseName(fileName);
		} else {
			newName = DATE_FORMAT.format(date);
		}

		String ext = FilenameUtils.getExtension(fileName);
		File newFile;
		int copy = 0;
		do {
			newFile = new File(destinationDirectory, newName + (copy == 0 ? "" : " (" + copy + ")") + (ext == null ? "" : "." + ext.toLowerCase()));
			copy++;
		//} while (newFile.exists());
		}while(newFiles.contains(newFile));

		return newFile;
	}

	private void processFile(final File file, final File outFile, Date date) throws ImageProcessingException, IOException {
		if (outFile.exists() && !cl.hasOption(Main.OPT_OVERWRITE)) {
			return;
		}
		log(date + ": " + file + " -> " + outFile);

		ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);
		Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
		if (!imageReaders.hasNext()) {
			log("no image reader found: " + file);
		} else {
			ImageReader imageReader = imageReaders.next();
			imageReader.setInput(imageInputStream, true);

			try {
				// read image
				IIOImage iioImage = imageReader.readAll(0, null);
				BufferedImage image = (BufferedImage) iioImage.getRenderedImage();

				// process image
				processPhoto(image, date, outFile.getName());

				// wirte image
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
		processPhoto(image, date, outFile.getName());
		ImageIO.write(image, "jpg", outFile);
	}

	private void processPhoto(BufferedImage image, Date date, String fileName) throws IOException {
		if (cl.hasOption(Main.OPT_STAMP)) {
			stampPhoto(image, date, fileName);
		}
	}

	private void stampPhoto(BufferedImage image, Date date, String fileName) throws IOException {
		log("stamp photo");
		
		String dateString = "";
		if (date != null) {
			dateString = DATE_FORMAT.format(date);
		} else {
			int idx = fileName.lastIndexOf('.');
			dateString = idx < 0 ? fileName : fileName.substring(0, idx);
		}

		int margin = MARGIN.apply(image.getHeight());
		int fontSize = FONT_SIZE.apply(image.getHeight());

		Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
		Graphics2D graphics = (Graphics2D) image.getGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		graphics.setColor(Color.WHITE);
		graphics.setFont(font);

		int x = image.getWidth() - margin - graphics.getFontMetrics(font).stringWidth(dateString);
		int y = image.getHeight() - margin;

		graphics.drawString(dateString, x, y);
	}

}
