package fr.kysic.imgreorder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for mouse and keyboard action
 */
public class PanelListener implements MouseListener, KeyEventDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(PanelListener.class);

    private Component selectionBegin;

    private Component selectionEnd;

    private Component destination;

    private JComponent imgPanel;

    private List<ImgPreviewXPath> imageList;

    private List<String> deletedFiles;

    /**
     * @param imgPanel
     *            panel containing image preview
     * @param imageList
     *            ImgPreviewXPath list
     * @param deletedFiles
     *            list in which file to removed are added (before they are
     *            removed when the apply button is pressed).
     */
    public PanelListener(JComponent imgPanel, List<ImgPreviewXPath> imageList, List<String> deletedFiles) {
        this.imgPanel = imgPanel;
        this.imageList = imageList;
        this.deletedFiles = deletedFiles;
        this.selectionEnd = null;
    }

    private String getImgPath(Component c) {
        Optional<ImgPreviewXPath> img = imageList.stream().filter(i -> i.getPreview() == c).findFirst();
        return img.isPresent() ? img.get().getAbsolutePath() : null;
    }

    private int getIndexOf(Component c) {
        int i = 0;
        for (ImgPreviewXPath img : imageList) {
            if (img.getPreview().equals(c)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private void move(final int begin, final int end, final int dest) {
        Component componentMoved;
        ImgPreviewXPath reorderImgMoved;
        int currentSource = begin;
        int currentDest = dest;
        for (int i = begin; i <= end; i++) {
            reorderImgMoved = imageList.remove(currentSource);
            componentMoved = imgPanel.getComponent(currentSource);
            imgPanel.remove(currentSource);
            imageList.add(currentDest, reorderImgMoved);
            imgPanel.add(componentMoved, currentDest);
            if (currentSource > currentDest) {
                currentSource++;
                currentDest++;
            }
        }
    }
    
    private void moveSelection() {
        if (selectionBegin != null && selectionEnd != null && destination != null) {
            synchronized (imageList) {
                int indexInsert = getIndexOf(destination);
                if (indexInsert != -1) {
                    processSelection((b, e) -> move(b, e, indexInsert));
                }
            }
            selectionBegin = null;
            selectionEnd = null;
            destination = null;
            displaySelection();
        }
    }


    private void displaySelection() {
        synchronized (imageList) {
            for (ImgPreviewXPath img : imageList) {
                img.getPreview().setBorder(BorderFactory.createLineBorder(Color.black));
            }
            processSelection((b, e) -> imageList.subList(b, e + 1).forEach(
                    c -> c.getPreview().setBorder(BorderFactory.createLineBorder(Color.blue, 3))));
        }
        imgPanel.revalidate();
        imgPanel.repaint();
    }

    private interface SelectionProcessor {
        void processSelection(int indexBegin, int indexEnd);
    }

    private void processSelection(SelectionProcessor selectionProcessor) {
        synchronized (imageList) {
            int indexBegin = getIndexOf(selectionBegin);
            int indexEnd = getIndexOf(selectionEnd);
            if (indexBegin > indexEnd) {
                int tmp = indexBegin;
                indexBegin = indexEnd;
                indexEnd = tmp;
            }
            if (indexBegin != -1) {
                selectionProcessor.processSelection(indexBegin, indexEnd);
            }
        }
    }

    private void delete(int indexBegin, int indexEnd) {
        // Reverse browsing because of "remove(i)".
        for (int i = indexEnd; i >= indexBegin; i--) {
            ImgPreviewXPath img = imageList.remove(i);
            imgPanel.remove(img.getPreview());
            deletedFiles.add(img.getAbsolutePath());
        }
    }

    private void deleteSelection() {
        synchronized (imageList) {
            processSelection((b, e) -> delete(b, e));
            displaySelection();
        }
    }

    private Component getComponentAt(MouseEvent me) {
        Component componentClicked = SwingUtilities.getDeepestComponentAt(imgPanel, me.getX(), me.getY());
        if (componentClicked != imgPanel) {
            return componentClicked;
        } else {
            return null;
        }
    }

    private void openImg(String imgPath) {
        try {
            Desktop.getDesktop().open(new File(imgPath));
        } catch (IOException e) {
            LOGGER.error("Can't open {}", imgPath, e);
        }

    }

    @Override
    public void mouseEntered(final MouseEvent me) {
        // Nothing

    }

    @Override
    public void mouseExited(final MouseEvent me) {
        // Nothing
    }

    @Override
    public void mouseClicked(final MouseEvent me) {
        // Right button
        switch (me.getButton()) {
        case MouseEvent.BUTTON3:
            destination = getComponentAt(me);
            moveSelection();
            break;
        case MouseEvent.BUTTON2:
            Component c = getComponentAt(me);
            if (c != null) {
                String imgPath = getImgPath(c);
                if (imgPath != null) {
                    openImg(imgPath);
                }
            }
            break;
        }
        me.consume();
    }

    @Override
    public void mousePressed(final MouseEvent me) {
        // Left button
        if (me.getButton() == MouseEvent.BUTTON1) {
            synchronized (imageList) {
                selectionBegin = getComponentAt(me);
                selectionEnd = null;
                displaySelection();
            }
        }
        me.consume();
    }

    @Override
    public void mouseReleased(final MouseEvent me) {
        // Left button
        if (me.getButton() == MouseEvent.BUTTON1) {
            synchronized (imageList) {
                selectionEnd = getComponentAt(me);
                displaySelection();
            }
        }
        me.consume();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ke) {
        if (ke.getID() == KeyEvent.KEY_PRESSED && ke.getKeyCode() == KeyEvent.VK_DELETE) {
            deleteSelection();
        }
        return false;
    }

}
