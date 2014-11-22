/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.dialog.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JProgressBar;

public class LiteProgressBar extends JProgressBar implements Transparent {
	private static final long serialVersionUID = 1L;
	private final TransparentComponent transparency = new TransparentComponent(this, false);

	public LiteProgressBar() {
		setFocusable(false);
		setOpaque(true);
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) transparency.setup(g);
		
		g2d.clearRect(0, 0, getWidth(), getHeight());

		// Draw bar
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, getWidth(), getHeight());

		// Draw progress
		g2d.setColor(Color.BLUE);
		int x = (int) (getWidth() * getPercentComplete());
		g2d.fillRect(0, 0, x, getHeight());

		transparency.cleanup(g2d);
		g2d = (Graphics2D) g;

		if (this.isStringPainted() && getString().length() > 0) {
			g2d.setFont(getFont());

			final int startWidth = (getWidth() - g2d.getFontMetrics().stringWidth(getString())) / 2;
			String white = "";
			int whiteWidth = 0;
			int chars = 0;
			for (int i = 0; i < getString().length(); i++) {
				white += getString().charAt(i);
				whiteWidth = g2d.getFontMetrics().stringWidth(white);
				if (startWidth + whiteWidth > x) {
					break;
				}
				chars++;
			}
			if (chars != getString().length()) {
				white = white.substring(0, white.length() - 1);
				whiteWidth = g2d.getFontMetrics().stringWidth(white);
			}
			float height = getFont().getSize();
			g2d.setColor(Color.WHITE);
			g2d.drawString(white, startWidth, height * 1.5F);
			g2d.setColor(Color.BLACK);
			g2d.drawString(this.getString().substring(chars), whiteWidth + startWidth, height * 1.5F);
		}

		transparency.cleanup(g2d);
	}

	public float getTransparency() {
		return transparency.getTransparency();
	}

	public void setTransparency(float t) {
		transparency.setTransparency(t);
	}

	public float getHoverTransparency() {
		return transparency.getHoverTransparency();
	}

	public void setHoverTransparency(float t) {
		transparency.setHoverTransparency(t);
	}
}
