package fr.kysic.imgreorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tools build for easily reorder images in a folder.<br>
 * Add xxxx_ in front of file name (where xxxx is a number)<br>
 * Image in the panel can be selected by left click (selection allowed) and move
 * at a specific place by left click.<br>
 * Selected images can also be removed by pressing "delete" key.<br>
 * Images files are really renamed or deleted only when the "Apply" button is
 * pressed.<br>
 * <br>
 * This application is not well designed or commented, it has just been built to
 * respond to a specific personal one time need with efficiency.<br>
 * Free to use, modify and distribute.
 */
public class ImgReorderApp {

    private static final int WINDOWS_HEIGHT = 600;

    private static final int WINDOWS_WIDTH = 800;

    private static final int POOL_MAX_NB_CORES = 4;

    private static final int ICON_HEIGHT = 120;

    private static final int ICON_WIDTH = 150;

    private static final Logger LOGGER = LoggerFactory.getLogger(ImgReorderApp.class);

    private static final String RESOURCES = "Resources";
    private static final String WINDOW_TITLE = "window.title";
    private static final String DIRECTORY_CHOOSER_TITLE = "button.directoryChooser";
    private static final String DIRECTORY_CHOOSER_BUTTON_LAB = "button.directoryChooser";
    private static final String APPLY_BUTTON_LAB = "button.apply";

    private JFileChooser directoryChooser;
    private JPanel imgPanel;
    
    private File currentDirectory;
    private List<ImgPreviewXPath> imageList;

    private ResourceBundle resources;

    private JFrame frame;

    private PanelListener panelListener;

    private ExecutorService buildPreviewExecutor;

    private List<String> deletedFiles;

    private void openDirectoryChooser() {
        if (currentDirectory != null) {
            directoryChooser.setSelectedFile(currentDirectory);
        }
        if (directoryChooser.showOpenDialog(imgPanel) == JFileChooser.APPROVE_OPTION) {
            setCurrentDir(directoryChooser.getSelectedFile());
            displayImgsFromCurrentDirectory();
        }
    }

    private boolean isImageFile(File file) {
        boolean isImageFile = false;
        if (file.isFile()) {
            String fileName = file.getName().toLowerCase();
            isImageFile = fileName.endsWith(".jpg") || fileName.endsWith(".png");
        }
        return isImageFile;
    }
    
    /**
     * Resizes an image using a Graphics2D object backed by a BufferedImage.
     * 
     * @param imgPath
     *            the path to the image to resize
     * @param w
     *            desired width
     * @param h
     *            desired height
     * @return the new resized image
     * @throws IOException
     * @throws ImagingOpException
     * @throws IllegalArgumentException
     */
    private Image getScaledImage(String imgPath, int w, int h) throws IllegalArgumentException, ImagingOpException,
            IOException {
        BufferedImage srcImg = ImageIO.read(new File(imgPath));
        // BufferedImage resizedImg = new BufferedImage(w, h,
        // BufferedImage.TYPE_INT_RGB);
        // Graphics2D g2 = resizedImg.createGraphics();
        // g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        // RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // double ratio = Math.min(w / (double) srcImg.getWidth(null), h /
        // (double) srcImg.getHeight(null));
        // int newW = (int) (srcImg.getWidth(null) * ratio);
        // int newH = (int) (srcImg.getHeight(null) * ratio);
        // g2.drawImage(srcImg, 0, 0, newW, newH, null);
        // g2.dispose();
        // return resizedImg;
        return Scalr.resize(srcImg, Scalr.Method.SPEED, Scalr.Mode.AUTOMATIC, w, h, Scalr.OP_ANTIALIAS);
    }

    private void buildPreview(ImgPreviewXPath rimg) {
        try {
            ImageIcon thumbnailIcon = new ImageIcon(getScaledImage(rimg.getAbsolutePath(), ICON_WIDTH, ICON_HEIGHT));
            rimg.getPreview().setText("");
            rimg.getPreview().setIcon(thumbnailIcon);
            rimg.getPreview().repaint();
        } catch (IOException e) {
            LOGGER.error("Unable to build preview of {}", rimg.getAbsolutePath(), e);
        }
    }
    
    private void buildPreviewInBg(ImgPreviewXPath rimg) {
        buildPreviewExecutor.execute( () -> { buildPreview(rimg); } );
    }
    
    private void resetPreviewExecutor() {
        if (buildPreviewExecutor != null) {
            buildPreviewExecutor.shutdownNow();
        }
        int nbCores = Runtime.getRuntime().availableProcessors();
        buildPreviewExecutor = Executors.newFixedThreadPool(Math.min(nbCores, POOL_MAX_NB_CORES));
    }

    private void displayImgsFromCurrentDirectory() {
        synchronized (imageList) {
            imgPanel.removeAll();
            imageList.clear();
            File[] dirContent = currentDirectory.listFiles();
            Arrays.sort(dirContent);
            for (File imgFile : dirContent) {
                if (isImageFile(imgFile)) {
                    JLabel label = new JLabel(imgFile.getName());
                    label.setToolTipText(imgFile.getName());
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setBorder(BorderFactory.createLineBorder(Color.black));
                    label.setPreferredSize(new Dimension(ICON_WIDTH, ICON_HEIGHT));
                    // redirect label listener to jpanel
                    // allow to capture press/release on different JLabel
                    // (otherwise adding the same listener on each label would
                    // have been simpler)
                    label.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            imgPanel.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, imgPanel));
                        }

                        public void mousePressed(MouseEvent e) {
                            imgPanel.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, imgPanel));
                        }

                        public void mouseReleased(MouseEvent e) {
                            imgPanel.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, imgPanel));
                        }
                    });
                    imgPanel.add(label);
                    ImgPreviewXPath rimg = new ImgPreviewXPath(imgFile.getAbsolutePath(), label);
                    imageList.add(rimg);
                    buildPreviewInBg(rimg);
                }
            }
            imgPanel.revalidate();
            imgPanel.repaint();
        }
    }

    private void removeImgs() {
        synchronized (imageList) {
            for (String deletedFile : deletedFiles) {
                LOGGER.info("Delete : {}", deletedFile);
                new File(deletedFile).delete();
            }
            deletedFiles.clear();
        }
    }

    private void renameImgs() {
        if (currentDirectory == null || !currentDirectory.exists()) {
            return;
        }
        int i = 0;
        boolean renameFailed = false;
        synchronized (imageList) {
            for (ImgPreviewXPath img : imageList) {
                File source = new File(img.getAbsolutePath());
                String newName = String.format("%04d_", i++) + source.getName().replaceFirst("^[0-9]+_", "");
                File dest = new File(currentDirectory, newName);
                if (!source.equals(dest)) {
                    if (dest.exists() || !source.renameTo(dest)) {
                        LOGGER.error("Can't rename {} in {}", source, dest);
                        renameFailed = true;
                    } else {
                        img.setAbsolutePath(dest.getAbsolutePath());
                        img.getPreview().setToolTipText(dest.getName());
                    }
                }
            }
        }
        if (renameFailed) {
            displayImgsFromCurrentDirectory();
        }
    }

    private void apply() {
        removeImgs();
        renameImgs();
    }

    private void setCurrentDir(File currentDir) {
        this.currentDirectory = currentDir;
        frame.setTitle(String.format(resources.getString(WINDOW_TITLE), currentDir.getAbsolutePath()));
    }

    /**
     * Open the reorder image application in the current directory
     */
    public ImgReorderApp() {
        this(System.getProperty("user.dir"));
    }

    /**
     * Open the reorder image application.
     * 
     * @param dirPath
     *            the directory to parse for image
     */
    public ImgReorderApp(String dirPath) {

        resources = ResourceBundle.getBundle(RESOURCES);
        resetPreviewExecutor();

        imageList = new ArrayList<ImgPreviewXPath>();
        deletedFiles = new ArrayList<String>();

        directoryChooser = new JFileChooser();
        directoryChooser.setDialogTitle(resources.getString(DIRECTORY_CHOOSER_TITLE));
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryChooser.setAcceptAllFileFilterUsed(false);

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        frame.setLayout(new BorderLayout());

        imgPanel = new JPanel();
        imgPanel.setLayout(new WrapLayout(WrapLayout.LEFT));
        panelListener = new PanelListener(imgPanel, imageList, deletedFiles);
        imgPanel.addMouseListener(panelListener);
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(panelListener);

        JScrollPane scrollPanel = new JScrollPane(imgPanel);
        scrollPanel.setMinimumSize(new Dimension(WINDOWS_WIDTH, WINDOWS_HEIGHT));
        frame.getContentPane().add(scrollPanel, BorderLayout.CENTER);

        JPanel toolBar = new JPanel();
        frame.getContentPane().add(toolBar, BorderLayout.SOUTH);

        JButton fileChooserButton = new JButton(resources.getString(DIRECTORY_CHOOSER_BUTTON_LAB));
        fileChooserButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openDirectoryChooser();
            }
        });
        toolBar.add(fileChooserButton);

        JButton applyButton = new JButton(resources.getString(APPLY_BUTTON_LAB));
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                apply();
            }
        });
        toolBar.add(applyButton);

        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);

        setCurrentDir(new File(dirPath));
        if (!currentDirectory.isDirectory() && currentDirectory.exists()) {
            setCurrentDir(currentDirectory.getParentFile());
        }
        if (currentDirectory != null && currentDirectory.exists()) {
            displayImgsFromCurrentDirectory();
        }

    }

    /**
     * Start the reorder image application
     * 
     * @param args
     *            the directory to open can be precise in the command line
     */
    public static void main(String args[]) {
        if (args != null && args.length > 1) {
            new ImgReorderApp(args[0]);
        } else {
            new ImgReorderApp();
        }
    }

}
