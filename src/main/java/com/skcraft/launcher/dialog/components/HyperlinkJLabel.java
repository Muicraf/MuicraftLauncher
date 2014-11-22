/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.dialog.components;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;

public class HyperlinkJLabel extends TransparentJLabel implements MouseListener {
	private static final long CLICK_DELAY = 250L;
	private static final long serialVersionUID = 1L;
	private String url;
	private long lastClick = System.currentTimeMillis();
	public HyperlinkJLabel(String text, String url) {
		super(text);
		this.url = url;
		super.addMouseListener(this);
	}

	public void mouseClicked(MouseEvent e) {
		if (lastClick + CLICK_DELAY > System.currentTimeMillis()) {
			return;
		}
		lastClick = System.currentTimeMillis();
		try {
			URI uri = new java.net.URI(url);
			HyperlinkJLabel.browse(uri);
		} catch (Exception ex) {
			System.err.println("Unable to open browser to " + url);
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public static void browse(URI uri) {
		try {
			Object o = Class.forName("java.awt.Desktop").getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
			o.getClass().getMethod("browse", new Class[]{URI.class}).invoke(o, new Object[]{uri});
		} catch (Exception e) {
		}
	}
}
