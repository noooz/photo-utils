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
	public final static int RESIZE_DEFAULT = 1900;

	private final CommandLine cl;
	private final Set<File> newFiles = new HashSet<File>();
	private final boolean deleteSource;
	private boolean existsChangeOption = false;

	public PhotoProcessor(CommandLine cl) throws Throwable {
		this.cl = cl;

		File sourceDirectory = new File(cl.getOptionValue(Main.OPT_SOURCE, System.getProperty("user.dir")));
		File destinationDirectory;
		if (cl.hasOption(Main.OPT_DESTINATION)) {
			destinationDirectory = new File(cl.getOptionValue(Main.OPT_DESTINATION));
		} else {
			destinationDirectory = sourceDirectory;
		}

		deleteSource = sourceDirectory.equals(destinationDirectory);

		for (String changeOption : Main.CHANGE_OPTIONS) {
			if (cl.hasOption(changeOption)) {
				existsChangeOption = true;
				break;
			}
		}

		processFilesInDirectory(sourceDirectory, destinationDirectory);
	}

	private void log(String msg) {
		System.out.println(msg);
	}

	private void processFilesInDirectory(File srcDirectory, File destDirectory) throws Throwable {
		try {
			destDirectory.mkdirs();

			final Set<String> fileNames = new HashSet<>();
			for (File file : srcDirectory.listFiles()) {
				if (file.getName().startsWith(".")) {
					continue;
				}
				if (!file.isFile()) {
					if (file.isDirectory()) {
						fileNames.add(file.getName());
						processFilesInDirectory(file, new File(destDirectory, file.getName()));
					}
					continue;
				}
				
				File outFile = new File(destDirectory, file.getName());
				try{
					ExifSubIFDDirectory exif = ImageMetadataReader.readMetadata(file).getDirectory(ExifSubIFDDirectory.class);
					Date date = exif == null ? null : exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
	
					if (cl.hasOption(Main.OPT_RENAME)) {
						outFile = destinationFile(destDirectory, file.getName(), date);
					}
	
					newFiles.add(outFile);
					processFile(file, outFile, date);
				}catch(com.drew.imaging.ImageProcessingException e){
					FileUtils.copyFile(file, outFile);
				}
				fileNames.add(outFile.getName());
			}

			// delete unknown files in destination directory
			if (cl.hasOption(Main.OPT_DELETE)) {
				for (File file : destDirectory.listFiles()) {
					if (!fileNames.contains(file.getName())) {
						FileUtils.forceDelete(file);
					}
				}
			}

		} catch (Throwable e) {
			log("error while processing directory: " + srcDirectory);
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
			// } while (newFile.exists());
		} while (newFiles.contains(newFile));

		return newFile;
	}

	private void processFile(final File srcFile, final File destFile, Date date) throws ImageProcessingException, IOException {
		if (destFile.exists() && !cl.hasOption(Main.OPT_OVERWRITE)) {
			return;
		}
		log(date + ": " + srcFile + " -> " + destFile);

		if (deleteSource && !existsChangeOption) {
			// only move file
			FileUtils.moveFile(srcFile, destFile);
			return;
		}
		try {

			ImageInputStream imageInputStream = ImageIO.createImageInputStream(srcFile);
			Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
			if (!imageReaders.hasNext()) {
				log("no image reader found: " + srcFile);
			} else {
				ImageReader imageReader = imageReaders.next();
				imageReader.setInput(imageInputStream, true);

				try {
					// read image
					IIOImage iioImage = imageReader.readAll(0, null);
					final BufferedImage image = (BufferedImage) iioImage.getRenderedImage();

					// process image
					BufferedImage processedImage = processImage(image, date, destFile.getName());
					if(image != processedImage){
						iioImage.setRenderedImage(processedImage);
					}

					// write image
					ImageWriter writer = ImageIO.getImageWriter(imageReader);
					ImageWriteParam param = writer.getDefaultWriteParam();
					param.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
					writer.setOutput(ImageIO.createImageOutputStream(destFile));
					writer.write(null, iioImage, param);
					return;
				} catch (IIOException e) {
					log(e.getMessage());
				}
			}

			// fallback for reading/reading image
			BufferedImage image = ImageIO.read(srcFile);
			image = processImage(image, date, destFile.getName());
			ImageIO.write(image, "jpg", destFile);
		} catch (Exception e) {
			log(srcFile + ": " + e.getMessage());
			if (cl.hasOption(Main.OPT_OVERWRITE) || !destFile.exists()) {
				FileUtils.copyFile(srcFile, destFile);
			}
		}

		if (deleteSource && destFile.exists()) {
			srcFile.delete();
		}
	}

	private BufferedImage processImage(BufferedImage image, Date date, String fileName) throws IOException {
		if (cl.hasOption(Main.OPT_RESIZE)) {
			image = imageResize(image);
		}
		if (cl.hasOption(Main.OPT_STAMP)) {
			imageStamp(image, date, fileName);
		}
		return image;
	}

	private BufferedImage imageResize(BufferedImage image) {
		int max = Integer.parseInt(cl.getOptionValue(Main.OPT_RESIZE, Integer.toString(RESIZE_DEFAULT)));

		int h = image.getHeight();
		int w = image.getWidth();
		double scale = 1;
		if (h > w) {
			scale = max / (double) h;
		} else {
			scale = max / (double) w;
		}
		if (scale >= 1) {
			return image;
		}

		int newHeight = (int) (h * scale);
		int newWidth = (int) (w * scale);
		
		log("resize to "+  newHeight + "x" + newWidth);

		BufferedImage newImage = new BufferedImage(newWidth, newHeight, image.getType());
		newImage.createGraphics().drawImage(image, 0, 0, newWidth - 1, newHeight - 1, 0, 0, w - 1, h - 1, null);
		
		return newImage;
	}

	private void imageStamp(BufferedImage image, Date date, String fileName) throws IOException {
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
