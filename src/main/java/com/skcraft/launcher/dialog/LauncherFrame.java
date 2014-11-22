/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.dialog;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.skcraft.concurrency.ObservableFuture;
import com.skcraft.launcher.Instance;
import com.skcraft.launcher.InstanceList;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.auth.Session;
import com.skcraft.launcher.launch.Runner;
import com.skcraft.launcher.launch.LaunchProcessHandler;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.selfupdate.UpdateChecker;
import com.skcraft.launcher.selfupdate.SelfUpdater;
import com.skcraft.launcher.dialog.components.*;
import com.skcraft.launcher.swing.*;
import com.skcraft.launcher.update.HardResetter;
import com.skcraft.launcher.update.Remover;
import com.skcraft.launcher.update.Updater;
import com.skcraft.launcher.util.SwingExecutor;

import lombok.NonNull;
import lombok.extern.java.Log;

import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.JPanel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static com.skcraft.launcher.util.SharedLocale._;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * The main launcher frame.
 */
@Log
public class LauncherFrame extends JFrame {

	private final Launcher launcher;

	private static final String logoIcon = "icon.png";
	private static final String closeIcon = "close.png";
	private static final String minimizeIcon = "minimize.png";
	private static final String optionsIcon = "options.png";
	private final HeaderPanel header = new HeaderPanel();
	private final InstanceTable instancesTable = new InstanceTable();
	private final InstanceTableModel instancesModel;
	private final JScrollPane instanceScroll = new JScrollPane(instancesTable);
	private Point initialClick;
    private JFrame parent;
	private WebpagePanel webView;
	private JSplitPane splitPane;
	private final JPanel container = new JPanel();
	private final LinedBoxPanel buttonsPanel = new LinedBoxPanel(true).fullyPadded();
	private final JButton launchButton = new JButton(_("launcher.launch"));
	private final JButton refreshButton = new JButton(_("launcher.checkForUpdates"));
	private final JButton optionsButton = new JButton(_("launcher.options"));
	private final JButton selfUpdateButton = new JButton(_("launcher.updateLauncher"));
	private final JCheckBox updateCheck = new JCheckBox(_("launcher.downloadUpdates"));
	private static final int FRAME_WIDTH = 880, FRAME_HEIGHT = 520;
	private static int mouseX = 0, mouseY = 0;
	private static final String CLOSE_ACTION = "close";
	private static final String MINIMIZE_ACTION = "minimize";
	private static final String OPTIONS_ACTION = "options";
	private static final String LOGIN_ACTION = "login";
	private URL updateUrl;
	private LiteButton login;
	private JCheckBox remember;
	private TransparentButton close, minimize, options;
	private LiteProgressBar progressBar;

	/**
	 * Create a new frame.
	 *
	 * @param launcher the launcher
	 */
	public LauncherFrame(@NonNull Launcher launcher) {
		super(_("launcher.title", launcher.getVersion()));

		this.launcher = launcher;
		instancesModel = new InstanceTableModel(launcher.getInstances());
		
		movePanel(this);
		initComponents();
		setSize(FRAME_WIDTH, FRAME_HEIGHT);;
		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);
		backgroundImage(FRAME_WIDTH, FRAME_HEIGHT);
		loadInstances();
		
		/*setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setSize(880, 520);
		setResizable(false);
		initComponents();
		setLocationRelativeTo(null);
		SwingHelper.setIconImage(this, Launcher.class, "icon.png");
		loadInstances();*/
	}

	public static BufferedImage readIconImage(Class<?> clazz, String path) {
		InputStream in = null;
		try {
			in = clazz.getResourceAsStream(path);
			if (in != null) {
				return ImageIO.read(in);
			}
		} catch (IOException e) {
		} finally {
			closeQuietly(in);
		}
		return null;
	}

	/*public void backgroundImage(){
		BufferedImage backgroundImage = readIconImage(Launcher.class, "background.png");
		//setLayout(new BorderLayout());
		setSize(880, 520);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		setContentPane(new JLabel(new ImageIcon(backgroundImage)));
		setLayout(new FlowLayout());
		setSize(879, 519);
		setSize(880, 520);
	}*/

	public void backgroundImage(int width, int height) {
		JLabel background = new JLabel();
		background.setVerticalAlignment(SwingConstants.CENTER);
		background.setHorizontalAlignment(SwingConstants.CENTER);
		background.setBounds(0, 0, width, height);

		BufferedImage backgroundImage = readIconImage(Launcher.class, "background.png");
		background.setIcon(new ImageIcon(backgroundImage.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
		background.setVerticalAlignment(SwingConstants.TOP);
		background.setHorizontalAlignment(SwingConstants.LEFT);
		getContentPane().add(background);
	}

	public void movePanel(final JFrame parent){
		this.parent = parent;

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				initialClick = e.getPoint();
				getComponentAt(initialClick);
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {

				// get location of Window
				int thisX = parent.getLocation().x;
				int thisY = parent.getLocation().y;

				// Determine how much the mouse moved since the initial click
				int xMoved = (thisX + e.getX()) - (thisX + initialClick.x);
				int yMoved = (thisY + e.getY()) - (thisY + initialClick.y);

				// Move window to this position
				int X = thisX + xMoved;
				int Y = thisY + yMoved;
				parent.setLocation(X, Y);
			}
		});
	}
	
	private void initComponents() {
		int xShift = 0;
		int yShift = 0;
		if (this.isUndecorated()) {
			yShift += 30;
		}

		// Setup login button
		login = new LiteButton("Launch");
		login.setBounds(FRAME_WIDTH - 90, FRAME_HEIGHT - 30, 85, 24);
		login.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				launch();
			}
		});

		// Muicraft logo
		JLabel logo = new JLabel();
		logo.setBounds(FRAME_WIDTH / 2 - 309, 0, 617, 148);
		logo.setIcon(new ImageIcon(readIconImage(Launcher.class,logoIcon)));

		// Home Link
		HyperlinkJLabel home = new HyperlinkJLabel("Home", "http://www.muicraft.net/");
		home.setToolTipText("Visit our homepage");
		home.setBounds(10, FRAME_HEIGHT - 27, 65, 20);
		home.setForeground(Color.WHITE);
		home.setOpaque(false);
		home.setTransparency(0.70F);
		home.setHoverTransparency(1F);

		// Forums link
		HyperlinkJLabel forums = new HyperlinkJLabel("Forums", "http://www.muicraft.net/forum");
		forums.setToolTipText("Visit our community forums");
		forums.setBounds(82, FRAME_HEIGHT - 27, 90, 20);
		forums.setForeground(Color.WHITE);
		forums.setOpaque(false);
		forums.setTransparency(0.70F);
		forums.setHoverTransparency(1F);

		// Donate link
		HyperlinkJLabel donate = new HyperlinkJLabel("Donate", "http://www.muicraft.net/donate");
		donate.setToolTipText("Donate to the project");
		donate.setBounds(165, FRAME_HEIGHT - 27, 85, 20);
		donate.setForeground(Color.WHITE);
		donate.setOpaque(false);
		donate.setTransparency(0.70F);
		donate.setHoverTransparency(1F);

		// Close button
		close = new TransparentButton();
		close.setIcon(new ImageIcon(readIconImage(Launcher.class,closeIcon)));
		if (OperatingSystem.getOS().isMac()) {
			close.setBounds(0, 0, 37, 20);
		} else {
			close.setBounds(FRAME_WIDTH - 37, 0, 37, 20);
		}
		close.setTransparency(0.70F);
		close.setHoverTransparency(1F);
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(EXIT_ON_CLOSE);
			}
		});
		close.setBorder(BorderFactory.createEmptyBorder());
		close.setContentAreaFilled(false);

		// Minimize button
		minimize = new TransparentButton();
		minimize.setIcon(new ImageIcon(readIconImage(Launcher.class,minimizeIcon)));
		if (OperatingSystem.getOS().isMac()) {
			minimize.setBounds(37, 0, 37, 20);
		} else {
			minimize.setBounds(FRAME_WIDTH - 74, 0, 37, 20);
		}
		minimize.setTransparency(0.70F);
		minimize.setHoverTransparency(1F);
		minimize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setState(Frame.ICONIFIED);
			}
		});
		minimize.setBorder(BorderFactory.createEmptyBorder());
		minimize.setContentAreaFilled(false);

		// Options Button
		options = new TransparentButton();
		options.setIcon(new ImageIcon(readIconImage(Launcher.class,optionsIcon)));
		if (OperatingSystem.getOS().isMac()) {
			options.setBounds(74, 0, 37, 20);
		} else {
			options.setBounds(FRAME_WIDTH - 111, 0, 37, 20);
		}
		options.setTransparency(0.70F);
		options.setHoverTransparency(1F);
		options.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showOptions();
			}
		});
		options.setBorder(BorderFactory.createEmptyBorder());
		options.setContentAreaFilled(false);

		// Rectangle
		JLabel bottomRectangle = new JLabel();
		bottomRectangle.setBounds(0, FRAME_HEIGHT - 34, FRAME_WIDTH, 34);
		bottomRectangle.setBackground(new Color(30,30,30,180));
		bottomRectangle.setOpaque(true);

		//Instances Table
		instancesTable.setModel(instancesModel);
		
		//Web View
		webView = WebpagePanel.forURL(launcher.getNewsURL(), false);
		webView.setBounds(111, 145, FRAME_WIDTH - 111 - 111, FRAME_HEIGHT-200);
		webView.setBackground(new Color(30,30,30,180));
		webView.setOpaque(true);
		
		/*//Split Pane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, instanceScroll, webView);
		splitPane.setDividerLocation(260);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerSize(4);
        SwingHelper.flattenJSplitPane(splitPane);*/
        
		Container contentPane = getContentPane();
		contentPane.setLayout(null);

		contentPane.add(home);
		contentPane.add(forums);
		contentPane.add(donate);
		contentPane.add(logo);
		contentPane.add(login);
		contentPane.add(options);
		contentPane.add(close);
		contentPane.add(minimize);
		contentPane.add(webView);
		contentPane.add(bottomRectangle);
		
		/*container.setLayout(new BorderLayout());
        container.setSize(700, 450);
        container.setBackground(new Color(0,0,0,0));
        container.setOpaque(false);
        container.add(splitPane, BorderLayout.CENTER);*/
        
		setUndecorated(true);
		
		/*JFrame frame = new JFrame();
		frame.setLayout(new BorderLayout());
		frame.add(contentPane, BorderLayout.CENTER);*/

		/**updateCheck.setSelected(true);
        instancesTable.setModel(instancesModel);
        launchButton.setFont(launchButton.getFont().deriveFont(Font.BOLD));
        splitPane.setDividerLocation(260);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerSize(4);
        SwingHelper.flattenJSplitPane(splitPane);
        buttonsPanel.addElement(refreshButton);
        buttonsPanel.addElement(updateCheck);
        buttonsPanel.addGlue();
        buttonsPanel.addElement(selfUpdateButton);
        buttonsPanel.addElement(optionsButton);
        buttonsPanel.addElement(launchButton);
        buttonsPanel.setBackground(new Color(255,255,255,0));
        buttonsPanel.setOpaque(false);
        container.setLayout(new BorderLayout());
        container.setSize(880, 520);
        container.setBackground(new Color(0,0,0,0));
        container.setOpaque(false);
        //container.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        container.add(splitPane, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
        add(container, BorderLayout.CENTER);*/

		instancesModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (instancesTable.getRowCount() > 0) {
					instancesTable.setRowSelectionInterval(0, 0);
				}
			}
		});

		instancesTable.addMouseListener(new DoubleClickToButtonAdapter(launchButton));

		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadInstances();
				checkLauncherUpdate();
			}
		});

		selfUpdateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selfUpdate();
			}
		});

		optionsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showOptions();
			}
		});

		launchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				launch();
			}
		});

		instancesTable.addMouseListener(new PopupMouseAdapter() {
			@Override
			protected void showPopup(MouseEvent e) {
				int index = instancesTable.rowAtPoint(e.getPoint());
				Instance selected = null;
				if (index >= 0) {
					instancesTable.setRowSelectionInterval(index, index);
					selected = launcher.getInstances().get(index);
				}
				popupInstanceMenu(e.getComponent(), e.getX(), e.getY(), selected);
			}
		});
	}

	private void checkLauncherUpdate() {
		if (SelfUpdater.updatedAlready) {
			return;
		}

		ListenableFuture<URL> future = launcher.getExecutor().submit(new UpdateChecker(launcher));

		Futures.addCallback(future, new FutureCallback<URL>() {
			@Override
			public void onSuccess(URL result) {
				if (result != null) {
					requestUpdate(result);
				}
			}

			@Override
			public void onFailure(Throwable t) {

			}
		}, SwingExecutor.INSTANCE);
	}

	private void selfUpdate() {
		URL url = updateUrl;
		if (url != null) {
			SelfUpdater downloader = new SelfUpdater(launcher, url);
			ObservableFuture<File> future = new ObservableFuture<File>(
					launcher.getExecutor().submit(downloader), downloader);

			Futures.addCallback(future, new FutureCallback<File>() {
				@Override
				public void onSuccess(File result) {
					selfUpdateButton.setVisible(false);
					SwingHelper.showMessageDialog(
							LauncherFrame.this,
							_("launcher.selfUpdateComplete"),
							_("launcher.selfUpdateCompleteTitle"),
							null,
							JOptionPane.INFORMATION_MESSAGE);
				}

				@Override
				public void onFailure(Throwable t) {
				}
			}, SwingExecutor.INSTANCE);

			ProgressDialog.showProgress(this, future, _("launcher.selfUpdatingTitle"), _("launcher.selfUpdatingStatus"));
			SwingHelper.addErrorDialogCallback(this, future);
		} else {
			selfUpdateButton.setVisible(false);
		}
	}

	private void requestUpdate(URL url) {
		this.updateUrl = url;
		selfUpdateButton.setVisible(true);
	}

	/**
	 * Popup the menu for the instances.
	 *
	 * @param component the component
	 * @param x mouse X
	 * @param y mouse Y
	 * @param selected the selected instance, possibly null
	 */
	private void popupInstanceMenu(Component component, int x, int y, final Instance selected) {
		JPopupMenu popup = new JPopupMenu();
		JMenuItem menuItem;

		if (selected != null) {
			menuItem = new JMenuItem(!selected.isLocal() ? "Install" : "Launch");
			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					launch();
				}
			});
			popup.add(menuItem);

			if (selected.isLocal()) {
				popup.addSeparator();

				menuItem = new JMenuItem(_("instance.openFolder"));
				menuItem.addActionListener(ActionListeners.browseDir(
						LauncherFrame.this, selected.getContentDir(), true));
				popup.add(menuItem);

				menuItem = new JMenuItem(_("instance.openSaves"));
				menuItem.addActionListener(ActionListeners.browseDir(
						LauncherFrame.this, new File(selected.getContentDir(), "saves"), true));
				popup.add(menuItem);

				menuItem = new JMenuItem(_("instance.openResourcePacks"));
				menuItem.addActionListener(ActionListeners.browseDir(
						LauncherFrame.this, new File(selected.getContentDir(), "resourcepacks"), true));
				popup.add(menuItem);

				menuItem = new JMenuItem(_("instance.openScreenshots"));
				menuItem.addActionListener(ActionListeners.browseDir(
						LauncherFrame.this, new File(selected.getContentDir(), "screenshots"), true));
				popup.add(menuItem);

				menuItem = new JMenuItem(_("instance.copyAsPath"));
				menuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						File dir = selected.getContentDir();
						dir.mkdirs();
						SwingHelper.setClipboard(dir.getAbsolutePath());
					}
				});
				popup.add(menuItem);

				popup.addSeparator();

				if (!selected.isUpdatePending()) {
					menuItem = new JMenuItem(_("instance.forceUpdate"));
					menuItem.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							selected.setUpdatePending(true);
							launch();
							instancesModel.update();
						}
					});
					popup.add(menuItem);
				}

				menuItem = new JMenuItem(_("instance.hardForceUpdate"));
				menuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						confirmHardUpdate(selected);
					}
				});
				popup.add(menuItem);

				menuItem = new JMenuItem(_("instance.deleteFiles"));
				menuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						confirmDelete(selected);
					}
				});
				popup.add(menuItem);
			}

			popup.addSeparator();
		}

		menuItem = new JMenuItem(_("launcher.refreshList"));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadInstances();
			}
		});
		popup.add(menuItem);

		popup.show(component, x, y);

	}

	private void confirmDelete(Instance instance) {
		if (!SwingHelper.confirmDialog(this,
				_("instance.confirmDelete", instance.getTitle()), _("confirmTitle"))) {
			return;
		}

		// Execute the deleter
		Remover resetter = new Remover(instance);
		ObservableFuture<Instance> future = new ObservableFuture<Instance>(
				launcher.getExecutor().submit(resetter), resetter);

		// Show progress
		ProgressDialog.showProgress(
				this, future, _("instance.deletingTitle"), _("instance.deletingStatus", instance.getTitle()));
		SwingHelper.addErrorDialogCallback(this, future);

		// Update the list of instances after updating
		future.addListener(new Runnable() {
			@Override
			public void run() {
				loadInstances();
			}
		}, SwingExecutor.INSTANCE);
	}

	private void confirmHardUpdate(Instance instance) {
		if (!SwingHelper.confirmDialog(this, _("instance.confirmHardUpdate"), _("confirmTitle"))) {
			return;
		}

		// Execute the resetter
		HardResetter resetter = new HardResetter(instance);
		ObservableFuture<Instance> future = new ObservableFuture<Instance>(
				launcher.getExecutor().submit(resetter), resetter);

		// Show progress
		ProgressDialog.showProgress( this, future, _("instance.resettingTitle"),
				_("instance.resettingStatus", instance.getTitle()));
		SwingHelper.addErrorDialogCallback(this, future);

		// Update the list of instances after updating
		future.addListener(new Runnable() {
			@Override
			public void run() {
				launch();
				instancesModel.update();
			}
		}, SwingExecutor.INSTANCE);
	}

	private void loadInstances() {
		InstanceList.Enumerator loader = launcher.getInstances().createEnumerator();
		ObservableFuture<InstanceList> future = new ObservableFuture<InstanceList>(
				launcher.getExecutor().submit(loader), loader);

		future.addListener(new Runnable() {
			@Override
			public void run() {
				instancesModel.update();
				if (instancesTable.getRowCount() > 0) {
					instancesTable.setRowSelectionInterval(0, 0);
				}
				requestFocus();
			}
		}, SwingExecutor.INSTANCE);

		ProgressDialog.showProgress(this, future, _("launcher.checkingTitle"), _("launcher.checkingStatus"));
		SwingHelper.addErrorDialogCallback(this, future);
	}

	private void showOptions() {
		ConfigurationDialog configDialog = new ConfigurationDialog(this, launcher);
		configDialog.setVisible(true);
	}

	private void launch() {
		try {
			final Instance instance = launcher.getInstances().get(instancesTable.getSelectedRow());
			boolean update = updateCheck.isSelected() && instance.isUpdatePending();

			// Store last access date
			Date now = new Date();
			instance.setLastAccessed(now);
			Persistence.commitAndForget(instance);

			// Perform login
			final Session session = LoginDialog.showLoginRequest(this, launcher);
			if (session == null) {
				return;
			}

			// If we have to update, we have to update
			if (!instance.isInstalled()) {
				update = true;
			}

			if (update) {
				// Execute the updater
				Updater updater = new Updater(launcher, instance);
				updater.setOnline(session.isOnline());
				ObservableFuture<Instance> future = new ObservableFuture<Instance>(
						launcher.getExecutor().submit(updater), updater);

				// Show progress
				ProgressDialog.showProgress(
						this, future, _("launcher.updatingTitle"), _("launcher.updatingStatus", instance.getTitle()));
				SwingHelper.addErrorDialogCallback(this, future);

				// Update the list of instances after updating
				future.addListener(new Runnable() {
					@Override
					public void run() {
						instancesModel.update();
					}
				}, SwingExecutor.INSTANCE);

				// On success, launch also
				Futures.addCallback(future, new FutureCallback<Instance>() {
					@Override
					public void onSuccess(Instance result) {
						launch(instance, session);
					}

					@Override
					public void onFailure(Throwable t) {
					}
				}, SwingExecutor.INSTANCE);
			} else {
				launch(instance, session);
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			SwingHelper.showErrorDialog(this, _("launcher.noInstanceError"), _("launcher.noInstanceTitle"));
		}
	}

	private void launch(Instance instance, Session session) {
		final File extractDir = launcher.createExtractDir();

		// Get the process
		Runner task = new Runner(launcher, instance, session, extractDir);
		ObservableFuture<Process> processFuture = new ObservableFuture<Process>(
				launcher.getExecutor().submit(task), task);

		// Show process for the process retrieval
		ProgressDialog.showProgress(
				this, processFuture, _("launcher.launchingTItle"), _("launcher.launchingStatus", instance.getTitle()));

		// If the process is started, get rid of this window
		Futures.addCallback(processFuture, new FutureCallback<Process>() {
			@Override
			public void onSuccess(Process result) {
				dispose();
			}

			@Override
			public void onFailure(Throwable t) {
			}
		});

		// Watch the created process
		ListenableFuture<?> future = Futures.transform(
				processFuture, new LaunchProcessHandler(launcher), launcher.getExecutor());
		SwingHelper.addErrorDialogCallback(null, future);

		// Clean up at the very end
		future.addListener(new Runnable() {
			@Override
			public void run() {
				try {
					log.info("Process ended; cleaning up " + extractDir.getAbsolutePath());
					FileUtils.deleteDirectory(extractDir);
				} catch (IOException e) {
					log.log(Level.WARNING, "Failed to clean up " + extractDir.getAbsolutePath(), e);
				}
				instancesModel.update();
			}
		}, sameThreadExecutor());
	}
}
