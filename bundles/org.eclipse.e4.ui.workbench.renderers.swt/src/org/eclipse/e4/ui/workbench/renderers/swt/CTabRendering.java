package org.eclipse.e4.ui.workbench.renderers.swt;

import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderRenderer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class CTabRendering extends CTabFolderRenderer {

	// Constants for circle drawing
	final static int LEFT_TOP = 0;
	final static int LEFT_BOTTOM = 1;
	final static int RIGHT_TOP = 2;
	final static int RIGHT_BOTTOM = 3;

	// drop shadow constants
	final static int SIDE_DROP_WIDTH = 3;
	final static int BOTTOM_DROP_WIDTH = 4;

	// keylines
	final static int OUTER_KEYLINE = 1;
	final static int INNER_KEYLINE = 0;
	final static int TOP_KEYLINE = 0;

	// Item Constants
	static final int ITEM_TOP_MARGIN = 2;
	static final int ITEM_BOTTOM_MARGIN = 6;
	static final int ITEM_LEFT_MARGIN = 4;
	static final int ITEM_RIGHT_MARGIN = 4;
	static final int INTERNAL_SPACING = 4;

	static final String E4_SHADOW_IMAGE = "org.eclipse.e4.renderer.shadow_image"; //$NON-NLS-1$
	static final String E4_TOOLBAR_ACTIVE_IMAGE = "org.eclipse.e4.renderer.toolbar_background_active_image"; //$NON-NLS-1$
	static final String E4_TOOLBAR_INACTIVE_IMAGE = "org.eclipse.e4.renderer.toolbar_background_inactive_image"; //$NON-NLS-1$

	static int[] shape;

	Image shadowImage, toolbarActiveImage, toolbarInactiveImage;

	int cornerSize = 14;

	boolean shadowEnabled = true;
	Color outerKeyline, innerKeyline;
	Color[] activeToolbar;
	int[] activePercents;
	Color[] inactiveToolbar;
	int[] inactivePercents;
	boolean active;

	@Inject
	public CTabRendering(CTabFolder parent) {
		super(parent);
	}

	protected Rectangle computeTrim(int part, int state, int x, int y,
			int width, int height) {
		GC gc = new GC(parent);
		gc.dispose();
		int borderTop = TOP_KEYLINE + OUTER_KEYLINE;
		int borderBottom = INNER_KEYLINE + OUTER_KEYLINE;
		int marginWidth = parent.marginWidth;
		int marginHeight = parent.marginHeight;
		int sideDropWidth = shadowEnabled ? SIDE_DROP_WIDTH : 0;
		switch (part) {
		case PART_BODY:
			x = x - marginWidth - OUTER_KEYLINE - INNER_KEYLINE - sideDropWidth
					- (cornerSize / 2);
			width = width + 2 * OUTER_KEYLINE + 2 * INNER_KEYLINE + 2
					* marginWidth + 2 * sideDropWidth + cornerSize;
			int tabHeight = parent.getTabHeight() + 1; // TODO: Figure out what
														// to do about the +1
			// TODO: Fix
			if (parent.getMinimized()) {
				y = /* parent.onBottom ? y - borderTop : */y - tabHeight
						- borderTop - 5;
				height = borderTop + borderBottom + tabHeight;
			} else {
				// y = tabFolder.onBottom ? y - marginHeight - highlight_margin
				// - borderTop: y - marginHeight - highlight_header - tabHeight
				// - borderTop;

				y = y - marginHeight - tabHeight - borderTop - (cornerSize / 4);
				height = height + borderBottom + borderTop + 2 * marginHeight
						+ tabHeight + cornerSize / 2 + cornerSize / 4
						+ (shadowEnabled ? BOTTOM_DROP_WIDTH : 0);
			}
			break;
		case PART_HEADER:
			x = x - (INNER_KEYLINE + OUTER_KEYLINE) - sideDropWidth;
			width = width + 2 * (INNER_KEYLINE + OUTER_KEYLINE + sideDropWidth);
			break;
		case PART_BORDER:
			x = x - INNER_KEYLINE - OUTER_KEYLINE - sideDropWidth
					- (cornerSize / 4);
			width = width + 2 * (INNER_KEYLINE + OUTER_KEYLINE + sideDropWidth)
					+ cornerSize / 2;
			y = y - borderTop;
			height = height + borderTop + borderBottom;
			break;
		default:
			if (0 <= part && part < parent.getItemCount()) {
				x = x - ITEM_LEFT_MARGIN;// - (CORNER_SIZE/2);
				width = width + ITEM_LEFT_MARGIN + ITEM_RIGHT_MARGIN + 1;
				y = y - ITEM_TOP_MARGIN;
				height = height + ITEM_TOP_MARGIN + ITEM_BOTTOM_MARGIN;
			}
			break;
		}
		return new Rectangle(x, y, width, height);
	}

	protected void dispose() {
		super.dispose();
	}

	protected void draw(int part, int state, Rectangle bounds, GC gc) {
		switch (part) {
		case PART_BODY:
			this.drawTabBody(gc, bounds, state);
			return;
		case PART_HEADER:
			this.drawTabHeader(gc, bounds, state);
			return;
		default:
			if (0 <= part && part < parent.getItemCount()) {
				if (bounds.width == 0 || bounds.height == 0)
					return;
				if ((state & SWT.SELECTED) != 0) {
					drawSelectedTab(part, gc, bounds, state);
					state &= ~SWT.BACKGROUND;
					super.draw(part, state, bounds, gc);
				} else {
					drawUnselectedTab(part, gc, bounds, state);
					if ((state & SWT.HOT) == 0 && !active) {
						gc.setAdvanced(true);
						gc.setAlpha(0x7f);
						state &= ~SWT.BACKGROUND;
						super.draw(part, state, bounds, gc);
						gc.setAdvanced(false);
					} else {
						state &= ~SWT.BACKGROUND;
						super.draw(part, state, bounds, gc);
					}
				}
				return;
			}
		}
		super.draw(part, state, bounds, gc);
	}

	void drawTabHeader(GC gc, Rectangle bounds, int state) {
		// gc.setClipping(bounds.x, bounds.y, bounds.width,
		// parent.getTabHeight() + 1);

		int[] points = new int[1024];
		int index = 0;
		int radius = cornerSize / 2;
		int marginWidth = parent.marginWidth;
		int marginHeight = parent.marginHeight;
		int delta = INNER_KEYLINE + OUTER_KEYLINE + 2
				* (shadowEnabled ? SIDE_DROP_WIDTH : 0) + 2 * marginWidth;
		int width = bounds.width - delta;
		int height = bounds.height - INNER_KEYLINE - OUTER_KEYLINE - 2
				* marginHeight - (shadowEnabled ? BOTTOM_DROP_WIDTH : 0);
		int circX = bounds.x + delta / 2 + radius;
		int circY = bounds.y + radius;

		// Fill in background
		Region clipping = new Region();
		gc.getClipping(clipping);
		Region region = new Region();
		region.add(shape);
		region.intersect(clipping);
		gc.setClipping(region);

		int header = 3; // TODO: this needs to be added to computeTrim for
						// HEADER
		Rectangle trim = computeTrim(PART_HEADER, state, 0, 0, 0, 0);
		trim.width = bounds.width - trim.width;
		trim.height = (parent.getTabHeight() + 1 + header) - trim.height;
		trim.x = -trim.x;
		trim.y = -trim.y;

		draw(PART_BACKGROUND, SWT.NONE, trim, gc);

		gc.setClipping(clipping);
		clipping.dispose();
		region.dispose();

		int[] ltt = drawCircle(circX + 1, circY + 1, radius, LEFT_TOP);
		System.arraycopy(ltt, 0, points, index, ltt.length);
		index += ltt.length;

		int[] lbb = drawCircle(circX + 1, circY + height - (radius * 2) - 2,
				radius, LEFT_BOTTOM);
		System.arraycopy(lbb, 0, points, index, lbb.length);
		index += lbb.length;

		int[] rb = drawCircle(circX + width - (radius * 2) - 2, circY + height
				- (radius * 2) - 2, radius, RIGHT_BOTTOM);
		System.arraycopy(rb, 0, points, index, rb.length);
		index += rb.length;

		int[] rt = drawCircle(circX + width - (radius * 2) - 2, circY + 1,
				radius, RIGHT_TOP);
		System.arraycopy(rt, 0, points, index, rt.length);
		index += rt.length;
		points[index++] = points[0];
		points[index++] = points[1];

		int[] tempPoints = new int[index];
		System.arraycopy(points, 0, tempPoints, 0, index);

		if (outerKeyline == null)
			outerKeyline = gc.getDevice().getSystemColor(SWT.COLOR_BLACK);
		gc.setForeground(outerKeyline);
		gc.drawPolyline(shape);
	}

	void drawTabBody(GC gc, Rectangle bounds, int state) {
		int[] points = new int[1024];
		int index = 0;
		int radius = cornerSize / 2;
		int marginWidth = parent.marginWidth;
		int marginHeight = parent.marginHeight;
		int delta = INNER_KEYLINE + OUTER_KEYLINE + 2
				* (shadowEnabled ? SIDE_DROP_WIDTH : 0) + 2 * marginWidth;
		int width = bounds.width - delta;
		int height = Math.max(parent.getTabHeight() + INNER_KEYLINE
				+ OUTER_KEYLINE + (shadowEnabled ? BOTTOM_DROP_WIDTH : 0),
				bounds.height - INNER_KEYLINE - OUTER_KEYLINE - 2
						* marginHeight
						- (shadowEnabled ? BOTTOM_DROP_WIDTH : 0));

		int circX = bounds.x + delta / 2 + radius;
		int circY = bounds.y + radius;

		// Body
		index = 0;
		int[] ltt = drawCircle(circX, circY, radius, LEFT_TOP);
		System.arraycopy(ltt, 0, points, index, ltt.length);
		index += ltt.length;

		int[] lbb = drawCircle(circX, circY + height - (radius * 2), radius,
				LEFT_BOTTOM);
		System.arraycopy(lbb, 0, points, index, lbb.length);
		index += lbb.length;

		int[] rb = drawCircle(circX + width - (radius * 2), circY + height
				- (radius * 2), radius, RIGHT_BOTTOM);
		System.arraycopy(rb, 0, points, index, rb.length);
		index += rb.length;

		int[] rt = drawCircle(circX + width - (radius * 2), circY, radius,
				RIGHT_TOP);
		System.arraycopy(rt, 0, points, index, rt.length);
		index += rt.length;
		points[index++] = circX;
		points[index++] = circY - radius;

		int[] tempPoints = new int[index];
		System.arraycopy(points, 0, tempPoints, 0, index);
		shape = tempPoints;
		gc.fillPolygon(shape);

		// Fill in parent background for non-rectangular shape
		Region r = new Region();
		r.add(bounds);
		r.subtract(shape);
		gc.setBackground(parent.getParent().getBackground());
		Display display = parent.getDisplay();
		Region clipping = new Region();
		gc.getClipping(clipping);
		r.intersect(clipping);
		gc.setClipping(r);
		Rectangle mappedBounds = display
				.map(parent, parent.getParent(), bounds);
		parent.getParent().drawBackground(gc, bounds.x, bounds.y, bounds.width,
				bounds.height, mappedBounds.x, mappedBounds.y);

		// Shadow
		if (shadowEnabled)
			drawShadow(display, bounds, gc);

		gc.setClipping(clipping);
		clipping.dispose();
		r.dispose();
	}

	void drawSelectedTab(int itemIndex, GC gc, Rectangle bounds, int state) {
		int width = bounds.width;

		int[] points = new int[1024];
		int index = 0;
		int radius = cornerSize / 2;
		int circX = bounds.x + radius;
		int circY = bounds.y - 1 + radius;

		if (itemIndex == 0) {
			circX -= 1;
			points[index++] = circX - radius;
			points[index++] = bounds.y + bounds.height;

			points[index++] = circX - radius;
			points[index++] = bounds.y + bounds.height;
		} else {
			points[index++] = shadowEnabled ? SIDE_DROP_WIDTH : 0
					+ INNER_KEYLINE + OUTER_KEYLINE;
			points[index++] = bounds.y + bounds.height;

			points[index++] = bounds.x;
			points[index++] = bounds.y + bounds.height;
		}
		int[] ltt = drawCircle(circX, circY, radius, LEFT_TOP);
		for (int i = 0; i < ltt.length / 2; i += 2) {
			int tmp = ltt[i];
			ltt[i] = ltt[ltt.length - i - 2];
			ltt[ltt.length - i - 2] = tmp;
			tmp = ltt[i + 1];
			ltt[i + 1] = ltt[ltt.length - i - 1];
			ltt[ltt.length - i - 1] = tmp;
		}
		System.arraycopy(ltt, 0, points, index, ltt.length);
		index += ltt.length;

		int[] rt = drawCircle(circX + width - (radius * 2), circY, radius,
				RIGHT_TOP);
		for (int i = 0; i < rt.length / 2; i += 2) {
			int tmp = rt[i];
			rt[i] = rt[rt.length - i - 2];
			rt[rt.length - i - 2] = tmp;
			tmp = rt[i + 1];
			rt[i + 1] = rt[rt.length - i - 1];
			rt[rt.length - i - 1] = tmp;
		}
		System.arraycopy(rt, 0, points, index, rt.length);
		index += rt.length;

		points[index++] = bounds.width + circX - radius;
		points[index++] = bounds.y + bounds.height;

		points[index++] = parent.getSize().x
				- (shadowEnabled ? SIDE_DROP_WIDTH : 0 + INNER_KEYLINE
						+ OUTER_KEYLINE);
		points[index++] = bounds.y + bounds.height;

		gc.setClipping(0, bounds.y, parent.getSize().x
				- (shadowEnabled ? SIDE_DROP_WIDTH : 0 + INNER_KEYLINE
						+ OUTER_KEYLINE), bounds.y + bounds.height);// bounds.height
																	// + 4);
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		int[] tmpPoints = new int[index];
		System.arraycopy(points, 0, tmpPoints, 0, index);
		gc.fillPolygon(tmpPoints);
		gc.setForeground(new Color(gc.getDevice(), 182, 188, 204));
		if (active)
			gc.drawPolyline(tmpPoints);
		Rectangle rect = null;
		gc.setClipping(rect);

		if (outerKeyline == null)
			outerKeyline = gc.getDevice().getSystemColor(SWT.COLOR_BLACK);
		gc.setForeground(outerKeyline);
		gc.drawPolyline(shape);
	}

	void drawUnselectedTab(int itemIndex, GC gc, Rectangle bounds, int state) {
		if ((state & SWT.HOT) != 0) {
			int width = bounds.width;
			int[] points = new int[1024];
			int[] inactive = new int[8];
			int index = 0, inactive_index = 0;
			int radius = cornerSize / 2;
			int circX = bounds.x + radius;
			int circY = bounds.y - 1 + radius;

			int leftIndex = circX;
			if (itemIndex == 0) {
				if (parent.getSelectionIndex() != 0)
					leftIndex -= 1;
				points[index++] = leftIndex - radius;
				points[index++] = bounds.y + bounds.height;
			} else {
				points[index++] = bounds.x;
				points[index++] = bounds.y + bounds.height;
			}

			if (!active) {
				System.arraycopy(points, 0, inactive, 0, index);
				inactive_index += 2;
			}

			int[] ltt = drawCircle(leftIndex, circY, radius, LEFT_TOP);
			for (int i = 0; i < ltt.length / 2; i += 2) {
				int tmp = ltt[i];
				ltt[i] = ltt[ltt.length - i - 2];
				ltt[ltt.length - i - 2] = tmp;
				tmp = ltt[i + 1];
				ltt[i + 1] = ltt[ltt.length - i - 1];
				ltt[ltt.length - i - 1] = tmp;
			}
			System.arraycopy(ltt, 0, points, index, ltt.length);
			index += ltt.length;

			if (!active) {
				System.arraycopy(ltt, 0, inactive, inactive_index, 2);
				inactive_index += 2;
			}

			int rightIndex = circX - 1;
			int[] rt = drawCircle(rightIndex + width - (radius * 2), circY,
					radius, RIGHT_TOP);
			for (int i = 0; i < rt.length / 2; i += 2) {
				int tmp = rt[i];
				rt[i] = rt[rt.length - i - 2];
				rt[rt.length - i - 2] = tmp;
				tmp = rt[i + 1];
				rt[i + 1] = rt[rt.length - i - 1];
				rt[rt.length - i - 1] = tmp;
			}
			System.arraycopy(rt, 0, points, index, rt.length);
			index += rt.length;
			if (!active) {
				System.arraycopy(rt, rt.length - 4, inactive, inactive_index, 2);
				inactive[inactive_index] -= 1;
				inactive_index += 2;
			}

			points[index++] = bounds.width + rightIndex - radius;
			points[index++] = bounds.y + bounds.height;

			if (!active) {
				System.arraycopy(points, index - 2, inactive, inactive_index, 2);
				inactive[inactive_index] -= 1;
				inactive_index += 2;
			}

			gc.setClipping(points[0], bounds.y, parent.getSize().x
					- (shadowEnabled ? SIDE_DROP_WIDTH : 0 + INNER_KEYLINE
							+ OUTER_KEYLINE), bounds.y + bounds.height);// bounds.height
			// + 4);
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
			int[] tmpPoints = new int[index];
			System.arraycopy(points, 0, tmpPoints, 0, index);
			gc.fillPolygon(tmpPoints);
			gc.setForeground(new Color(gc.getDevice(), 182, 188, 204));
			if (active) {
				gc.drawPolyline(tmpPoints);
			} else {
				gc.drawLine(inactive[0], inactive[1], inactive[2], inactive[3]);
				gc.drawLine(inactive[4], inactive[5], inactive[6], inactive[7]);
			}

			Rectangle rect = null;
			gc.setClipping(rect);

			if (outerKeyline == null)
				outerKeyline = gc.getDevice().getSystemColor(SWT.COLOR_BLACK);
			// gc.setForeground(outerKeyline);
			// gc.drawPolyline(shape);
		}
	}

	static int[] drawCircle(int xC, int yC, int r, int circlePart) {
		int x = 0, y = r, u = 1, v = 2 * r - 1, e = 0;
		int[] points = new int[1024];
		int[] pointsMirror = new int[1024];
		int loop = 0;
		int loopMirror = 0;
		while (x < y) {
			if (circlePart == RIGHT_BOTTOM) {
				points[loop++] = xC + x;
				points[loop++] = yC + y;
			}
			if (circlePart == RIGHT_TOP) {
				points[loop++] = xC + y;
				points[loop++] = yC - x;
			}
			if (circlePart == LEFT_TOP) {
				points[loop++] = xC - x;
				points[loop++] = yC - y;
			}
			if (circlePart == LEFT_BOTTOM) {
				points[loop++] = xC - y;
				points[loop++] = yC + x;
			}
			x++;
			e += u;
			u += 2;
			if (v < 2 * e) {
				y--;
				e -= v;
				v -= 2;
			}
			if (x > y)
				break;
			if (circlePart == RIGHT_BOTTOM) {
				pointsMirror[loopMirror++] = xC + y;
				pointsMirror[loopMirror++] = yC + x;
			}
			if (circlePart == RIGHT_TOP) {
				pointsMirror[loopMirror++] = xC + x;
				pointsMirror[loopMirror++] = yC - y;
			}
			if (circlePart == LEFT_TOP) {
				pointsMirror[loopMirror++] = xC - y;
				pointsMirror[loopMirror++] = yC - x;
			}
			if (circlePart == LEFT_BOTTOM) {
				pointsMirror[loopMirror++] = xC - x;
				pointsMirror[loopMirror++] = yC + y;
			}
			// grow?
			if ((loop + 1) > points.length) {
				int length = points.length * 2;
				int[] newPointTable = new int[length];
				int[] newPointTableMirror = new int[length];
				System.arraycopy(points, 0, newPointTable, 0, points.length);
				points = newPointTable;
				System.arraycopy(pointsMirror, 0, newPointTableMirror, 0,
						pointsMirror.length);
				pointsMirror = newPointTableMirror;
			}
		}
		int[] finalArray = new int[loop + loopMirror];
		System.arraycopy(points, 0, finalArray, 0, loop);
		for (int i = loopMirror - 1, j = loop; i > 0; i = i - 2, j = j + 2) {
			int tempY = pointsMirror[i];
			int tempX = pointsMirror[i - 1];
			finalArray[j] = tempX;
			finalArray[j + 1] = tempY;
		}
		return finalArray;
	}

	void drawShadow(final Display display, Rectangle bounds, GC gc) {
		if (shadowImage == null)
			createShadow(display);

		int x = bounds.x;
		int y = bounds.y;
		int SIZE = shadowImage.getBounds().width / 3;

		int height = Math.max(bounds.height, SIZE * 2);
		int width = Math.max(bounds.width, SIZE * 2);
		// top left
		gc.drawImage(shadowImage, 0, 0, SIZE, SIZE, 2, 10, SIZE, 20);
		int fillHeight = height - SIZE * 2;
		int fillWidth = width + 5 - SIZE * 2;

		int xFill = 0;
		for (int i = SIZE; i < fillHeight; i += SIZE) {
			xFill = i;
			gc.drawImage(shadowImage, 0, SIZE, SIZE, SIZE, 2, i, SIZE, SIZE);
		}

		// Pad the rest of the shadow
		gc.drawImage(shadowImage, 0, SIZE, SIZE, fillHeight - xFill, 2, xFill
				+ SIZE, SIZE, fillHeight - xFill);

		// bl
		gc.drawImage(shadowImage, 0, 40, 20, 20, 2, y + height - SIZE, 20, 20);

		int yFill = 0;
		for (int i = SIZE; i <= fillWidth; i += SIZE) {
			yFill = i;
			gc.drawImage(shadowImage, SIZE, SIZE * 2, SIZE, SIZE, i, y + height
					- SIZE, SIZE, SIZE);
		}
		// Pad the rest of the shadow
		gc.drawImage(shadowImage, SIZE, SIZE * 2, fillWidth - yFill, SIZE,
				yFill + SIZE, y + height - SIZE, fillWidth - yFill, SIZE);

		// br
		gc.drawImage(shadowImage, SIZE * 2, SIZE * 2, SIZE, SIZE, x + width
				- SIZE - 1, y + height - SIZE, SIZE, SIZE);

		// tr
		gc.drawImage(shadowImage, (SIZE * 2), 0, SIZE, SIZE, x + width - SIZE
				- 1, 10, SIZE, SIZE);

		xFill = 0;
		for (int i = SIZE; i < fillHeight; i += SIZE) {
			xFill = i;
			gc.drawImage(shadowImage, SIZE * 2, SIZE, SIZE, SIZE, x + width
					- SIZE - 1, i, SIZE, SIZE);
		}

		// Pad the rest of the shadow
		gc.drawImage(shadowImage, SIZE * 2, SIZE, SIZE, fillHeight - xFill, x
				+ width - SIZE - 1, xFill + SIZE, SIZE, fillHeight - xFill);
	}

	void createShadow(final Display display) {
		Object obj = display.getData(E4_SHADOW_IMAGE);
		if (obj != null) {
			shadowImage = (Image) obj;
		} else {
			ImageData data = new ImageData(60, 60, 32, new PaletteData(
					0xFF0000, 0xFF00, 0xFF));
			shadowImage = new Image(display, data);
			GC gc = new GC(shadowImage);
			gc.setBackground(new Color(display, new RGB(128, 128, 128)));
			drawTabBody(gc, new Rectangle(0, 0, 60, 60), SWT.None);
			ImageData blured = blur(shadowImage, 5, 25);
			shadowImage = new Image(display, blured);
			display.setData(E4_SHADOW_IMAGE, shadowImage);
			display.disposeExec(new Runnable() {
				public void run() {
					Object obj = display.getData(E4_SHADOW_IMAGE);
					if (obj != null) {
						Image tmp = (Image) obj;
						tmp.dispose();
						display.setData(E4_SHADOW_IMAGE, null);
					}
				}
			});
		}
	}

	void createActiveToolbarImages(final Display display) {
		Object active = display.getData(E4_TOOLBAR_ACTIVE_IMAGE);
		if (active != null) {
			toolbarActiveImage = (Image) active;
		} else {
			// No colors or percents set
			if (activeToolbar == null || activePercents == null)
				return;
			int width = 5;
			int height = parent.getTabHeight() + 4;
			int x = 0;
			int y = -1;
			toolbarActiveImage = new Image(display, width, 30);
			GC gc = new GC(toolbarActiveImage);

			//
			Color lastColor = activeToolbar[0];
			if (lastColor == null)
				lastColor = parent.getBackground();
			int pos = 0;
			for (int i = 0; i < activePercents.length; i++) {
				gc.setForeground(lastColor);
				lastColor = activeToolbar[i + 1];
				if (lastColor == null)
					lastColor = parent.getBackground();
				gc.setBackground(lastColor);
				int percentage = i > 0 ? activePercents[i]
						- activePercents[i - 1] : activePercents[i];
				int gradientHeight = percentage * height / 100;
				gc.fillGradientRectangle(x, y + pos, width, gradientHeight,
						true);
				pos += gradientHeight;
			}
			if (pos < height) {
				gc.setBackground(parent.getBackground());
				gc.fillRectangle(x, pos, width, height - pos + 1);
			}

			gc.dispose();
			display.setData(E4_TOOLBAR_ACTIVE_IMAGE, toolbarActiveImage);
			display.disposeExec(new Runnable() {
				public void run() {
					Object obj = display.getData(E4_TOOLBAR_ACTIVE_IMAGE);
					if (obj != null) {
						Image tmp = (Image) obj;
						tmp.dispose();
						display.setData(E4_TOOLBAR_ACTIVE_IMAGE, null);
					}
				}
			});
		}
	}

	void createInactiveToolbarImages(final Display display) {
		Object inactive = display.getData(E4_TOOLBAR_INACTIVE_IMAGE);
		if (inactive != null) {
			toolbarInactiveImage = (Image) inactive;
		} else {
			// No colors or percents set
			if (inactiveToolbar == null || inactivePercents == null)
				return;
			int width = 5;
			int height = parent.getTabHeight() + 4;
			int x = 0;
			int y = -4;
			toolbarInactiveImage = new Image(display, 5, 30);
			GC gc = new GC(toolbarInactiveImage);

			Color lastColor = inactiveToolbar[0];
			if (lastColor == null)
				lastColor = parent.getBackground();
			int pos = 0;
			for (int i = 0; i < inactivePercents.length; i++) {
				gc.setForeground(lastColor);
				lastColor = inactiveToolbar[i + 1];
				if (lastColor == null)
					lastColor = parent.getBackground();
				gc.setBackground(lastColor);
				int percentage = i > 0 ? inactivePercents[i]
						- inactivePercents[i - 1] : inactivePercents[i];
				int gradientHeight = percentage * height / 100;
				gc.fillGradientRectangle(x, y + pos, width, gradientHeight,
						true);
				pos += gradientHeight;
			}
			if (pos < height) {
				gc.setBackground(parent.getBackground());
				gc.fillRectangle(x, pos, width, height - pos + 1);
			}
			gc.dispose();
			display.setData(E4_TOOLBAR_INACTIVE_IMAGE, toolbarInactiveImage);
			display.disposeExec(new Runnable() {
				public void run() {
					Object obj = display.getData(E4_TOOLBAR_INACTIVE_IMAGE);
					if (obj != null) {
						Image tmp = (Image) obj;
						tmp.dispose();
						display.setData(E4_TOOLBAR_INACTIVE_IMAGE, null);
					}
				}
			});
		}
	}

	public ImageData blur(Image src, int radius, int sigma) {
		float[] kernel = create1DKernel(radius, sigma);

		ImageData imgPixels = src.getImageData();
		int width = imgPixels.width;
		int height = imgPixels.height;

		int[] inPixels = new int[width * height];
		int[] outPixels = new int[width * height];
		int offset = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				RGB rgb = imgPixels.palette.getRGB(imgPixels.getPixel(x, y));
				if (rgb.red == 255 && rgb.green == 255 && rgb.blue == 255) {
					inPixels[offset] = (rgb.red << 16) | (rgb.green << 8)
							| rgb.blue;
				} else {
					inPixels[offset] = (imgPixels.getAlpha(x, y) << 24)
							| (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
				}
				offset++;
			}
		}

		convolve(kernel, inPixels, outPixels, width, height, true);
		convolve(kernel, outPixels, inPixels, height, width, true);

		ImageData dst = new ImageData(imgPixels.width, imgPixels.height, 24,
				new PaletteData(0xff0000, 0xff00, 0xff));

		dst.setPixels(0, 0, inPixels.length, inPixels, 0);
		offset = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (inPixels[offset] == -1) {
					dst.setAlpha(x, y, 0);
				} else {
					int a = (inPixels[offset] >> 24) & 0xff;
					// if (a < 150) a = 0;
					dst.setAlpha(x, y, a);
				}
				offset++;
			}
		}
		return dst;
	}

	private void convolve(float[] kernel, int[] inPixels, int[] outPixels,
			int width, int height, boolean alpha) {
		int kernelWidth = kernel.length;
		int kernelMid = kernelWidth / 2;
		for (int y = 0; y < height; y++) {
			int index = y;
			int currentLine = y * width;
			for (int x = 0; x < width; x++) {
				// do point
				float a = 0, r = 0, g = 0, b = 0;
				for (int k = -kernelMid; k <= kernelMid; k++) {
					float val = kernel[k + kernelMid];
					int xcoord = x + k;
					if (xcoord < 0)
						xcoord = 0;
					if (xcoord >= width)
						xcoord = width - 1;
					int pixel = inPixels[currentLine + xcoord];
					// float alp = ((pixel >> 24) & 0xff);
					a += val * ((pixel >> 24) & 0xff);
					r += val * (((pixel >> 16) & 0xff));
					g += val * (((pixel >> 8) & 0xff));
					b += val * (((pixel) & 0xff));
				}
				int ia = alpha ? clamp((int) (a + 0.5)) : 0xff;
				int ir = clamp((int) (r + 0.5));
				int ig = clamp((int) (g + 0.5));
				int ib = clamp((int) (b + 0.5));
				outPixels[index] = (ia << 24) | (ir << 16) | (ig << 8) | ib;
				index += height;
			}
		}

	}

	private int clamp(int value) {
		if (value > 255)
			return 255;
		if (value < 0)
			return 0;
		return value;
	}

	private float[] create1DKernel(int radius, int sigma) {
		// guideline: 3*sigma should be the radius
		int size = radius * 2 + 1;
		float[] kernel = new float[size];
		int radiusSquare = radius * radius;
		float sigmaSquare = 2 * sigma * sigma;
		float piSigma = 2 * (float) Math.PI * sigma;
		float sqrtSigmaPi2 = (float) Math.sqrt(piSigma);
		int start = size / 2;
		int index = 0;
		float total = 0;
		for (int i = -start; i <= start; i++) {
			float d = i * i;
			if (d > radiusSquare) {
				kernel[index] = 0;
			} else {
				kernel[index] = (float) Math.exp(-(d) / sigmaSquare)
						/ sqrtSigmaPi2;
			}
			total += kernel[index];
			index++;
		}
		for (int i = 0; i < size; i++) {
			kernel[i] /= total;
		}
		return kernel;
	}

	@Inject
	@Optional
	public void setCornerRadius(@Named("radius") Integer radius) {
		cornerSize = radius.intValue();
		parent.redraw();
	}

	@Inject
	@Optional
	public void setShadowVisible(@Named("shadowVisible") Boolean visible) {
		this.shadowEnabled = visible.booleanValue();
		parent.redraw();
	}

	@Inject
	@Optional
	public void setOuterKeyline(@Named("outerKeyline") Color color) {
		this.outerKeyline = color;
		// TODO: HACK! Should be set based on pseudo-state.
		setActive(!(color.getRed() == 255 && color.getGreen() == 255 && color
				.getBlue() == 255));
		parent.redraw();
	}

	@Inject
	@Optional
	public void setInnerKeyline(@Named("innerKeyline") Color color) {
		this.innerKeyline = color;
		parent.redraw();
	}

	@Inject
	@Optional
	public void setActiveToolbarGradient(
			@Named("activeToolbarColors") Color[] color,
			@Named("activeToolbarPercents") int[] percents) {
		activeToolbar = color;
		activePercents = percents;
	}

	@Inject
	@Optional
	public void setInactiveToolbarGradient(
			@Named("inactiveToolbarColors") Color[] color,
			@Named("inactiveToolbarPercents") int[] percents) {
		inactiveToolbar = color;
		inactivePercents = percents;
	}

	public void setActive(boolean active) {
		this.active = active;
		Control topRight = parent.getTopRight();
		if (topRight != null) {
			if (active && toolbarActiveImage == null)
				createActiveToolbarImages(Display.getCurrent());
			if (!active && toolbarInactiveImage == null)
				createInactiveToolbarImages(Display.getCurrent());
			topRight.setBackgroundImage(active ? toolbarActiveImage
					: toolbarInactiveImage);
			parent.redraw();
		}
	}
}
