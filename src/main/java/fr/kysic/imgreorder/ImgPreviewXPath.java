package fr.kysic.imgreorder;

import javax.swing.JLabel;

/**
 * Associate a JLabel containing the image preview and the absolute path of the
 * image file.
 */
public class ImgPreviewXPath {

    private String absolutePath;

    private JLabel preview;

    /**
     * Constructor
     * 
     * @param absolutePath
     *            img absolute path
     * @param preview
     *            jLabel preview
     */
    public ImgPreviewXPath(String absolutePath, JLabel preview) {
        super();
        this.absolutePath = absolutePath;
        this.preview = preview;
    }

    /**
     * @return the image file absolute path
     */
    public String getAbsolutePath() {
        return absolutePath;
    }

    /**
     * @param absolutePath
     *            the image file absolute path
     */
    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    /**
     * @return the jLabel preview
     */
    public JLabel getPreview() {
        return preview;
    }

    /**
     * @param preview
     *            the jLabel preview
     */
    public void setPreview(JLabel preview) {
        this.preview = preview;
    }

}
