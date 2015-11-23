/* PhotoOrganizer - $RCSfile: PhotoImagePanel.java,v $
 * Copyright (C) 1999-2001 Dmitriy Rogatkin.  All rights reserved.
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
 * $Id: PhotoImagePanel.java,v 1.29 2008/04/15 23:14:42 dmitriy Exp $
 */
package photoorganizer.renderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputAdapter;

import mediautil.gen.MediaFormat;
import mediautil.image.ImageUtil;
import mediautil.image.jpeg.AbstractImageInfo;
import mediautil.image.jpeg.BasicJpeg;

import org.aldan3.util.DataConv;
import org.aldan3.util.IniPrefs;

import photoorganizer.Controller;
import photoorganizer.IrdControllable;
import photoorganizer.PhotoOrganizer;
import photoorganizer.Resources;
import photoorganizer.formats.FileNameFormat;

interface Observable {
	public void update(Dimension d);

	public void doSelection();
}

@SuppressWarnings("serial")
public class PhotoImagePanel extends JPanel implements ActionListener, Observable, IrdControllable {
	protected final static Dimension FITSIZE = new Dimension();

	private JScrollPane scroller;

	private Observable iviewer;

	private ThumbnailsPanel thumbspanel;

	private Controller controller;

	private MediaFormat jpeg;

	private Timer ssTimer;

	public PhotoImagePanel(Controller controller) {
		this.controller = controller;
		setLayout(new BorderLayout());
		add(scroller = new JScrollPane(new ImageComponent()), BorderLayout.CENTER);
		scroller.setWheelScrollingEnabled(true);
		setMinimumSize(Resources.MIN_PANEL_DIMENSION);
	}

	public void setThumbnailsPanel(ThumbnailsPanel thumbspanel) {
		this.thumbspanel = thumbspanel;
	}

	// ///////////// start Observable ///////////////////
	public void update(Dimension d) {
		Observable observable = null;
		if (iviewer == null || iviewer == this)
			observable = (Observable) this.scroller.getViewport().getView();
		else
			observable = iviewer;
		// free memory first
		observable.update(d);
		if (observable instanceof ImageFrame)
			((ImageFrame) observable).setTitle(PhotoOrganizer.PROGRAMNAME + " " + PhotoOrganizer.VERSION + " - "
					+ jpeg.getFile().getAbsolutePath());
	}

	public void doSelection() {
		Observable observable = iviewer == null || iviewer == this ? (Observable) this.scroller.getViewport().getView()
				: iviewer;
		observable.doSelection();
	}

	// //////////////// end Observable //////////////
	public void updateView(MediaFormat format) {
		if ((format.getType() & MediaFormat.STILL) > 0) {
			jpeg =  format;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					update(isScaling() ? FITSIZE : null);
				}
			});
		}
		Container c = getParent();
		if (c != null && c instanceof JTabbedPane) {
			((JTabbedPane) c).setSelectedComponent(this);
		}
	}

	public void actionPerformed(ActionEvent a) {
		String cmd = a.getActionCommand();
		if (a.getSource() == ssTimer)
			cmd = Resources.MENU_NEXT;
		if (cmd.equals(Resources.MENU_POPUPWIN)) {
			if (iviewer != null) {
				if (iviewer instanceof ImageFrame)
					return;
				if (iviewer instanceof ImageWindow)
					((Window) iviewer).dispose();
			}
			iviewer = new ImageFrame(PhotoOrganizer.PROGRAMNAME + " " + PhotoOrganizer.VERSION + " - "
					+ jpeg.getFile().getAbsolutePath());
		} else if (cmd.equals(Resources.MENU_INTWIN)) {
			if (iviewer != null && iviewer != this) {
				((Window) iviewer).dispose();
				iviewer = null;
			}
		} else if (cmd.equals(Resources.MENU_FULLSCREEN)) {
			if (iviewer != null) {
				if (iviewer instanceof ImageWindow)
					return;
				if (iviewer instanceof ImageFrame)
					((Window) iviewer).dispose();
			}
			iviewer = new ImageWindow();
			if (isScaling())
				iviewer.update(((ImageWindow)iviewer).getSize());
		} else if (cmd.equals(Resources.MENU_NEXT)) {
			if (thumbspanel.showNext() == false && ssTimer != null && ssTimer.isRunning())
				ssTimer.stop();
		} else if (cmd.equals(Resources.MENU_PREVIOUS)) {
			thumbspanel.showPrev();
		} else if (cmd.equals(Resources.MENU_SLIDESHOW)) {
			IniPrefs s = controller.getPrefs();
			int interval = IniPrefs.getInt(s.getProperty(MiscellaneousOptionsTab.SECNAME,
					MiscellaneousOptionsTab.SLIDESHOWPAUSE), 3) * 1000;
			if (ssTimer == null) {
				ssTimer = new Timer(interval, this);
				ssTimer.setRepeats(true);
			} else
				ssTimer.setDelay(interval);
			ssTimer.start();
		} else if (cmd.equals(Resources.MENU_CANCEL)) {
			if (ssTimer != null && ssTimer.isRunning())
				ssTimer.stop();
		} else if (cmd.equals(Resources.MENU_CROP)) {
			// Rectangle r;
			// jpeg.setCropRect(r = new Rectangle());
			// if isScaling to use scale factor
			doSelection();
		} else if (cmd.equals(Resources.MENU_FITTOSIZE)
		/*
		 * && isScaling() == false
		 */) {
			update(FITSIZE);
		} else if (cmd.equals(Resources.MENU_ORIGSIZE)) {
			update((Dimension) null);
		}
	}

	protected boolean isScaling() {
		IniPrefs s = controller.getPrefs();
		return s.getInt(s.getProperty(AppearanceOptionsTab.SECNAME, AppearanceOptionsTab.FIT_TO_SIZE), 0) == 1;
	}

	protected void trigger() {
		if (iviewer != null && iviewer != this) {
			((Window) iviewer).dispose();
			iviewer = null;
		} else
			iviewer = new ImageFrame(PhotoOrganizer.PROGRAMNAME + " " + PhotoOrganizer.VERSION + " - "
					+ jpeg.getFile().getAbsolutePath());
	}

	// remote controllable
	public String getName() {
		return Resources.COMP_IMAGE_VIEWER;
	}

	public Iterator getKeyMnemonics() {
		ArrayList list = new ArrayList(4);
		list.add(Resources.MENU_FULLSCREEN);
		list.add(Resources.MENU_NEXT);
		list.add(Resources.MENU_PREVIOUS);
		return list.iterator();
	}

	public boolean doAction(String keyCode) {
		actionPerformed(new ActionEvent(this, keyCode.hashCode(), keyCode));
		return true;
	}

	public void bringOnTop() {
	}

	@SuppressWarnings("serial")
	class ImageComponent extends JLabel implements Observable {
		boolean inCrop;

		boolean inDrag;

		Point prevCoord;

		Point cropEnd;

		double stretchFactor;

		int xOffs, yOffs;

		public ImageComponent() {
			super(new ImageIcon());

			addMouseListener(new MouseInputAdapter() {
				public void mouseClicked(MouseEvent e) {
					int m = e.getModifiers();
					if (e.getClickCount() == 2 && (m & InputEvent.BUTTON1_MASK) > 0) {
						trigger();
					} else if ((m & InputEvent.BUTTON3_MASK) > 0) { // SwingUtilities.isRightMouseButton(e)
						Point p = ((JViewport) getParent()).getViewPosition();
						new ImageRButtonMenu(PhotoImagePanel.this,
								getParent().getParent().getParent() instanceof PhotoImagePanel).show(getParent(), e
								.getX()
								- p.x, e.getY() - p.y);
					}
				}

				public void mousePressed(MouseEvent e) {
					inDrag = true;
					prevCoord = e.getPoint();
					if (inCrop) {
						cropEnd = (Point) prevCoord.clone();
					}
				}

				public void mouseReleased(MouseEvent e) {
					inDrag = false;
					if (inCrop) {
						inCrop = false;
						ImageComponent.this.setCursor(Cursor.getDefaultCursor());
						// resize rect to fit size
						Rectangle cr = getCropRect();
						scale(cr, stretchFactor, false);
						jpeg.getMediaInfo().setAttribute(AbstractImageInfo.CROP_REGION, cr);
						// debugPrint("Stored crop", getCropRect(), jpeg);
						// System.err.printf("Draw box %dx%d - %dx%d\n",
						// prevCoord.x, prevCoord.y, x - prevCoord.x,
						// y-prevCoord.y);
					}
				}
			});
			addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseDragged/* mouseMoved */(MouseEvent e) {
					if (inCrop) {
						int x = e.getX();
						int y = e.getY();

						Component c = e.getComponent();
						while (c != ImageComponent.this && c != null) {
							Point pt = c.getLocation();
							x += pt.x;
							y += pt.y;
							c = c.getParent();
						}
						drawRubberBox(x, y); // draw the connection line.
						return;
					}
					JViewport v = (JViewport) getParent();
					Dimension vs = v.getViewSize();
					Dimension es = v.getExtentSize();
					Point mp = e.getPoint();
					Point vp = v.getViewPosition();
					vp.x += prevCoord.x - mp.x;
					vp.y += prevCoord.y - mp.y;
					if (vp.x < 0)
						vp.x = 0;
					if (vp.y < 0)
						vp.y = 0;
					if (vp.y > (vs.height - es.height))
						vp.y = vs.height - es.height;
					if (vp.x > (vs.width - es.width))
						vp.x = (vs.width - es.width);
					v.setViewPosition(vp);
				}
			});
			addMouseWheelListener(new MouseWheelListener() {
				public void mouseWheelMoved(MouseWheelEvent e) {
					// add implementation of zoom in/out
				}
			});
			addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					switch (e.getKeyCode()) {
					case KeyEvent.VK_PAGE_UP:
					case KeyEvent.VK_SUBTRACT:
						thumbspanel.showPrev();
						break;
					case KeyEvent.VK_PAGE_DOWN:
					case KeyEvent.VK_ADD:
						thumbspanel.showNext();
						break;
					}
				}
			});
		}

		public void update(Dimension d) {
			// check if the image is already shown
			controller.setWaitCursor(this, true);
			try {
				// clear existing first
				setIcon(null);
				if (jpeg == null)
					return;

				Image img = BasicJpeg.getBufferedImage(jpeg.getFile());
				Dimension ed = null;
				if (img != null && d != null) {
					ed = ((JViewport) getParent()).getExtentSize();
					if (d.width == 0 || d.height == 0)
						d = ed;
					// System.err.printf("Resize to: %dx%d\n", d.width,
					// d.height);
					double[] da = new double[1];
					img =ImageUtil.getScaledBufferedImage(img, d, da);
					//img = BasicJpeg.getScaled(img, d, Image.SCALE_SMOOTH, da);
					stretchFactor = da[0];
				} else
					stretchFactor = 0.0;
				if (img == null)
					return;

				ImageIcon ii;
				setIcon(ii = new ImageIcon(img));

				if (ed != null) {
					int w = ii.getIconWidth();
					int h = ii.getIconHeight();
					if (ed.width > w)
						xOffs = (ed.width - w) / 2;
					if (ed.height > h)
						yOffs = (ed.height - h) / 2;
				} else {
					xOffs = yOffs = 0;
				}
				// applyCropCoord();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				return;
			} finally {
				controller.setWaitCursor(this, false);
			}
			revalidate();
			repaint();
			IniPrefs s = controller.getPrefs();
			String o = DataConv.arrayToString(s.getProperty(ThumbnailsOptionsTab.SECNAME, ThumbnailsOptionsTab.TOOLTIPMASK), ',');
			if (o != null)
				setToolTipText(new FileNameFormat(o).format(jpeg));
			requestFocus();
		}

		public void doSelection() {
			inCrop = true;
			applyCropCoord();

			if (cropEnd != null)
				drawRubberBox(null, cropEnd.x, cropEnd.y, false);
			setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		}

		protected void applyCropCoord() {
			if (jpeg == null) {
				prevCoord = null;
				return;
			}

			Rectangle cr = (Rectangle) jpeg.getMediaInfo().getAttribute(AbstractImageInfo.CROP_REGION);
			if (cr == null) {
				prevCoord = null;
				return;
			}
			scale(cr, stretchFactor, true);

			// debugPrint("Apply crop", cr, jpeg);

			prevCoord = new Point(cr.x, cr.y);
			cropEnd = new Point(cr.x + cr.width, cr.y + cr.height);
		}

		protected void drawRubberBox(int x, int y) {
			drawRubberBox(null, x, y, true);
		}

		protected void drawRubberBox(Graphics g, int x, int y, boolean clean) {
			boolean obtained = false;
			if (g == null) {
				g = getGraphics();
				obtained = true;
			}
			if (g == null) {
				return;
			}
			try {
				if (clean)
					drawRubberBox(g, cropEnd.x, cropEnd.y, false);
				cropEnd.x = x;
				cropEnd.y = y;
				Rectangle r = getCropRect();
				if (r != null) {
					g.setXORMode(Color.white);
					g.setColor(Color.blue);
					g.drawRect(r.x, r.y, r.width, r.height);
				}
			} finally {
				if (obtained)
					g.dispose();
			}
		}

		protected Rectangle getCropRect() {
			if (cropEnd == null || prevCoord == null)
				return null;
			Rectangle res = new Rectangle(prevCoord.x, prevCoord.y, cropEnd.x - prevCoord.x, cropEnd.y - prevCoord.y);
			// new Rectangle(prevCoord.x, prevCoord.y, cropEnd.x-prevCoord.x,
			// cropEnd.y-prevCoord.y);
			if (res.width < 0) {
				res.x = cropEnd.x;
				res.width = -res.width;
			}
			if (res.height < 0) {
				res.y = cropEnd.y;
				res.height = -res.height;
			}
			return res;
		}

		public void debugPrint(String m, Rectangle r, Object o) {
			if (r != null)
				System.err.printf("%s area: %d:%d  %dx%d, %s factor %f\n", m, r.x, r.y, r.width, r.height, o,
						stretchFactor);
			else
				System.err.printf("%s rea: null, %s\n", m, o);
		}

		public void scale(Rectangle re, double sf, boolean shrink) {
			if (sf == 0.0)
				return;
			if (shrink == false) {
				sf = 1.0 / sf;
				re.x = (int) ((re.x - xOffs) * sf);
				re.y = (int) ((re.y - yOffs) * sf);
			} else {
				re.x = (int) (re.x * sf + xOffs);
				re.y = (int) (re.y * sf + yOffs);
			}
			re.width *= sf;
			re.height *= sf;
		}

		public void paint(Graphics g) {
			try {
				super.paint(g);
				Rectangle r = (Rectangle)jpeg.getMediaInfo().getAttribute(AbstractImageInfo.CROP_REGION);
				// debugPrint("Paint crop", r, jpeg);
				// TODO: correct to scrolled position unless drawing directly to
				// an image
				if (r != null) {
					scale(r, stretchFactor, true);
					g.setXORMode(Color.white);
					g.setColor(Color.blue);
					g.drawRect(r.x, r.y, r.width, r.height);
				}
			} catch (NullPointerException e) {
				// work around Sun's VM bug
			}
			controller.setWaitCursor(this, false);
		}

		class ImageRButtonMenu extends JPopupMenu {
			ImageRButtonMenu(ActionListener listener, boolean popup) {
				JMenuItem item;
				if (popup) {
					this.add(item = new JMenuItem(Resources.MENU_POPUPWIN));
					item.addActionListener(listener);
				} else {
					this.add(item = new JMenuItem(Resources.MENU_INTWIN));
					item.addActionListener(listener);
					this.add(item = new JMenuItem(Resources.MENU_FULLSCREEN));
				}
				item.addActionListener(listener);
				this.add(item = new JMenuItem(Resources.MENU_CROP));
				item.addActionListener(listener);
				this.addSeparator();
				this.add(item = new JMenuItem(Resources.MENU_NEXT));
				item.addActionListener(listener);
				this.add(item = new JMenuItem(Resources.MENU_PREVIOUS));
				item.addActionListener(listener);
				if (ssTimer != null && ssTimer.isRunning())
					this.add(item = new JMenuItem(Resources.MENU_CANCEL));
				else
					this.add(item = new JMenuItem(Resources.MENU_SLIDESHOW));
				item.addActionListener(listener);
				this.add(item = new JMenuItem(stretchFactor == 0 ? Resources.MENU_FITTOSIZE : Resources.MENU_ORIGSIZE));
				item.addActionListener(listener);
			}
		}
	}

	@SuppressWarnings("serial")
	class ImageFrame extends JFrame implements Observable {
		public ImageFrame(String caption) {
			super(caption);
			getContentPane().add(scroller);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			pack();
			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			d.width = (d.width *2/3);
			d.height = (d.height *2/3);
			setSize(d);
			setVisible(true);
			setIconImage(controller.getMainIcon());
			scroller.getViewport().getView().requestFocus();
			addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent e) {
					// System.err.println("Resizing image to " + getSize());
					if (isScaling())
						update(getRootPane().getSize());
				}
			});
		}

		public void doSelection() {
			((Observable) scroller.getViewport().getView()).doSelection();
		}

		public void dispose() {
			iviewer = PhotoImagePanel.this;
			PhotoImagePanel.this.add(scroller);
			PhotoImagePanel.this.revalidate();
			super.dispose();
		}

		public void update(Dimension d) {
			((Observable) scroller.getViewport().getView()).update(d);
		}
	}

	class ImageWindow extends JWindow implements Observable {
		public ImageWindow() {
			super(controller.mediachest);
			getContentPane().add(scroller);
			// try Toolkit.getDesktopProperty("win.toolbar.height");
			// String [] props =
			// (String[])Toolkit.getDefaultToolkit().getDesktopProperty("win.propNames");
			// for (int i=0;i<props.length;i++)
			// System.err.println("Desktop prop["+i+"] "+props[i]);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();			
			pack();
			setVisible(true);
			setBounds(0, 0, screenSize.width, screenSize.height);
			/* scroller.getViewport().getView(). */requestFocus();
			scroller.getViewport().setExtentSize(screenSize);
		}

		public void update(Dimension d) {
			((Observable) scroller.getViewport().getView()).update(d);
		}

		public void doSelection() {
			((Observable) scroller.getViewport().getView()).doSelection();
		}

		public void dispose() {
			iviewer = PhotoImagePanel.this;
			PhotoImagePanel.this.add(scroller);
			PhotoImagePanel.this.revalidate();
			super.dispose();
		}
	}
}
