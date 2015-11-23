package photoorganizer.media;

import java.io.File;
import java.io.IOException;

import mediautil.gen.MediaFormat;
import mediautil.gen.MediaInfo;
import mediautil.image.jpeg.BasicJpeg;
import mediautil.image.jpeg.Exif;

import org.aldan3.model.ServiceProvider;
import org.aldan3.util.Stream;

import photoorganizer.Controller;
import photoorganizer.formats.FileNameFormat;
import photoorganizer.formats.MediaFormatFactory;

public class Operations implements ServiceProvider {
	Controller controller;

	public static final int AUTO = -2;

	public static final String NAME = "Operations";

	public static class OperationException extends Exception {

	}

	public static interface CustomOperation {
		void customOperation(MediaFormat media);

		boolean canDelete(MediaFormat media);
	}

	public Operations(Controller controller) {
		this.controller = controller;
	}

	public String getPreferredServiceName() {
		return NAME;
	}

	public Object getServiceProvider() {
		return this;
	}

	public static int mediaInfoToTransformOp(MediaFormat format) {
		try {
			Integer i = (Integer) format.getMediaInfo().getAttribute(MediaInfo.ORIENTATION);
			if (i != null && i > 0)
				return Exif.opToCorrectOrientation[i];
		} catch (IllegalArgumentException ia) {
			System.err.printf("Orientation not supported for %s%n", format);
		}
		return -1;
	}

	/**
	 * generic method for processing media, applying transformation, rename or
	 * copy
	 * 
	 * @param medias
	 * @param target
	 * @param mask
	 *            mask defines name change, if empty, no name change
	 * @param op >
	 *            0 one of jpeg transform, -1 no transform, -2 auto based on
	 *            rotation code
	 * @param keepOrig
	 * @throws OperationException
	 */
	public void changeMedia(MediaFormat[] medias, String target, String mask, int op, boolean keepOrig,
			CustomOperation customOperation) throws OperationException {
		FileNameFormat fnf = new FileNameFormat(mask, true);
		for (int j = 0; j < medias.length; j++) {
			File file = new File(target, FileNameFormat.makeValidPathName(fnf.format(medias[j])));
			if (op == -2 && medias[j] instanceof BasicJpeg) {
				if (((BasicJpeg) medias[j]).transform(file.getPath(), mediaInfoToTransformOp(medias[j]), true) == false)
					System.err
							.println("Problem in rotation file " + medias[j].getFile() + " to album location " + file);
				else {
					if (keepOrig == false && (customOperation == null || customOperation.canDelete(medias[j])))
						if (medias[j].getFile().delete() == false)
							System.err.println("Problem in deleting file " + medias[j].getFile()
									+ " after transformation.");
					medias[j] = MediaFormatFactory.createMediaFormat(file);
				}
			} else {
				if (keepOrig == false && (customOperation == null || customOperation.canDelete(medias[j]))) {
					if (!medias[j].renameTo(file))
						System.err.println("Problem in moving file " + medias[j].getFile() + " to album location "
								+ file);
				} else {
					try {
						Stream.copyFile(medias[j].getFile(), file);
						medias[j] = MediaFormatFactory.createMediaFormat(file);
					} catch (IOException ioe) {
						System.err.println("Problem in copying file " + medias[j].getFile() + " to " + file
								+ " album. (" + ioe + ')');
					}
				}
			}
			if (customOperation != null)
				customOperation.customOperation(medias[j]);
		}
	}

}
