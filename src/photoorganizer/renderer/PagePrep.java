/* MediaChest - $RCSfile: PagePrep.java,v $
 * Copyright (C) 1999-2005 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  $Id: PagePrep.java,v 1.13 2008/03/02 04:42:54 dmitriy Exp $
 */

package photoorganizer.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.ImageObserver;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.File;

import mediautil.gen.MediaFormat;
import mediautil.image.jpeg.BasicJpeg;
import mediautil.image.ImageUtil;
import photoorganizer.Controller;
import photoorganizer.PageLayout;
import photoorganizer.formats.MediaFormatFactory;

public class PagePrep implements Printable { // ,Pageable{
	protected Object[] medias;

	protected Controller controller;

	protected PrintStatusUpdater statusUpdater;

	public PagePrep(Object[] medias, Controller controller) {
		this(medias, controller, null);
	}

	public PagePrep(Object[] medias, Controller controller, PrintStatusUpdater statusUpdater) {
		this.medias = medias;
		this.controller = controller;
		this.statusUpdater = statusUpdater;
	}

	// NOTE: method called twice for the same page, disregard that
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
		PageLayout.LayoutDef layout = PageLayout.getLayoutSchema(controller.getPrefs());
		int imagesOnPage = layout.getNumImages();

		if (pageIndex < (medias.length + imagesOnPage - 1) / imagesOnPage) {
			if (statusUpdater != null)
				statusUpdater.updateStatus("#" + pageIndex);
			final Graphics2D g2d = (Graphics2D) graphics;
			g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

			g2d.setColor(Color.black);
			// g2d.setBackground(Color.white);
			// g2d.setPaintMode();
			System.err.println("page:" + pageIndex + " view area " + (pageFormat.getImageableWidth() / 72) + 'x'
					+ (pageFormat.getImageableHeight() / 72) + "\" images total " + medias.length + " on page "
					+ imagesOnPage);
			double picWidth = 0, picHeight = 0;
			double currY = 0;
			int picIndex = pageIndex * imagesOnPage;
			pageLoop: for (int ri = 0; medias != null && ri < layout.rowDefs.length; ri++) {
				PageLayout.RowDef row = layout.rowDefs[ri];
				picWidth = pageFormat.getImageableWidth() / row.num;
				double sizeAvail = row.width * 72;
				if (picWidth > sizeAvail) // in inches
					picWidth = sizeAvail;
				sizeAvail = pageFormat.getImageableHeight() - currY;
				picHeight = row.height * 72;
				if (sizeAvail < row.height * 72)
					picHeight = sizeAvail;
				System.err.println("Requested size " + (picWidth / 72) + 'x' + (picHeight / 72) + '"');
				double currX = 0;
				for (int ci = 0; ci < row.num; ci++) {
					if (picIndex < medias.length && picIndex <= (pageIndex + 1) * imagesOnPage) {
						g2d.drawRect((int) (currX), (int) currY, (int) picWidth, (int) picHeight);
						Image im = null;
						MediaFormat format = null;
						if (medias[picIndex] instanceof MediaFormat)
							format = (MediaFormat) medias[picIndex];
						else if (medias[picIndex] instanceof File)
							format = MediaFormatFactory.createMediaFormat((File) medias[picIndex]);
						if (format != null) {
							if (format instanceof BasicJpeg)
								im = ((BasicJpeg) format).getImage();
							// else try to do for other formats
						}
						// im = null;
						if (im != null) {
							Dimension s = ImageUtil.getImageSize(im, true);
							AffineTransform at = new AffineTransform();

							if (row.orient) {
								at.setToTranslation(currX + picWidth, currY);
								at.rotate(Math.PI / 2);
							} else
								at.setToTranslation(currX, currY);

							g2d.drawString(format.getName(), (int) at.getTranslateX(), (int) at.getTranslateY() + 30);
							double scale = row.orient ? Math.min(picHeight / s.width, picWidth / s.height) : Math.min(
									picWidth / s.width, picHeight / s.height);
							System.err.println("Image size " + s.width + 'x' + s.height + "pxs");
							System.err.println("Scale factor " + scale + " rotated " + row.orient);
							System.err.println("Start drawing at :" + (at.getTranslateX() / 72) + '-'
									+ (at.getTranslateY() / 72));
							if (scale < 1.0) {
								at.scale(scale, scale);
							}
							final Object monitor = new Object();
							final boolean[] stateHolder = new boolean[1];
							stateHolder[0] = false;
							// if (ci==1)

							if (g2d.drawImage(im, at, new ImageObserver() {
								public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
									System.err.println("Drawing line " + y + ", state:" + infoflags);
									stateHolder[0] = true;
									if ((infoflags & (ALLBITS | ABORT | ERROR)) != 0) {
										synchronized (monitor) {
											stateHolder[0] = false;
											monitor.notify();
										}
										System.err.println("Completed");
										return false;
									}
									// if (y == height/2 && height != 0)
									// statusbar.tickProgress();
									return true;
								}
							}) == false)
								synchronized (monitor) {
									try {
										System.err.println("Waiting..");
										monitor.wait(800);
										// if (stateHolder [0])
										// monitor.wait();
									} catch (Exception ie) {
									}
								}
							System.err.println("Image " + picIndex + " drawn..");
						}
						picIndex++;
						if (statusUpdater != null)
							statusUpdater.updateStatus("#" + pageIndex + '/' + picIndex);
						currX += picWidth;
					} else
						break pageLoop;
				}
				currY += picHeight;
			}
			return PAGE_EXISTS;
		}
		if (statusUpdater != null)
			statusUpdater.finish();
		return NO_SUCH_PAGE;
	}

	public static interface PrintStatusUpdater {
		public void updateStatus(String status);

		public void setProgressPercents(int percents);

		public void finish();
	}
}
