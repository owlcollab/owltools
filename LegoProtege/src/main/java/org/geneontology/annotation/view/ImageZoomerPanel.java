package org.geneontology.annotation.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ImageZoomerPanel extends JPanel implements ActionListener {

	// generated
	private static final long serialVersionUID = -4804940612385611394L;
	
	private ImagePanel m_imagePanel;
    private JScrollPane m_srollPane;
    private JPanel m_imageContainer;
    private final JLabel m_zoomedInfo;
    private final JButton m_zoomInButton;
    private final JButton m_zoomOutButton;
    private final JButton m_originalButton;

    /**
     * Constructor
     * @param image
     * @param zoomPercentage
     */    
    public ImageZoomerPanel(Image image, double zoomPercentage)
    {
        super();
        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

        m_zoomInButton = new JButton("Zoom In");
        m_zoomInButton.addActionListener(this);

        m_zoomOutButton = new JButton("Zoom Out");
        m_zoomOutButton.addActionListener(this);

        m_originalButton = new JButton("Original");
        m_originalButton.addActionListener(this);

        m_zoomedInfo = new JLabel("Zoomed to 100%");

        topPanel.add(new JLabel("Zoom Percentage is " +  Math.round(zoomPercentage) + "%"));
        topPanel.add(m_zoomInButton);
        topPanel.add(m_originalButton);
        topPanel.add(m_zoomOutButton);
        topPanel.add(m_zoomedInfo);

        m_imagePanel = new ImagePanel(image, zoomPercentage);

        m_imageContainer = new JPanel(new FlowLayout(FlowLayout.LEADING));
        m_imageContainer.setBackground(Color.WHITE);
        m_imageContainer.add(m_imagePanel);

        m_srollPane = new JScrollPane(m_imageContainer);
        m_srollPane.setAutoscrolls(true);

        add(BorderLayout.NORTH, topPanel);
        add(BorderLayout.CENTER, m_srollPane);
        m_imagePanel.repaint();
        setVisible(true);
    }
    
    /**
     * Action Listener method taking care of 
     * actions on the buttons
     */
    public void actionPerformed(ActionEvent ae)
    {
        if(ae.getSource().equals(m_zoomInButton))
        {
            m_imagePanel.zoomIn();
            adjustLayout();
        }
        else if(ae.getSource().equals(m_zoomOutButton))
        {
            m_imagePanel.zoomOut();
            adjustLayout();
        }
        else if(ae.getSource().equals(m_originalButton))
        {
            m_imagePanel.originalSize();
            adjustLayout();
        }
    }
    
    /**
     * This method adjusts the layout after 
     * zooming
     */
    private void adjustLayout()
    {
        m_imageContainer.doLayout();        
        m_srollPane.doLayout();
        m_zoomedInfo.setText("Zoomed to " + Math.round(m_imagePanel.getZoomedTo()) + "%");
    }
    
    /**
     * This class is the Image Panel where the image
     * is drawn and scaled.
     * 
     * @author Rahul Sapkal(rahul@javareference.com)
     */
    static class ImagePanel extends JPanel
    {
		private static final long serialVersionUID = -4073092570993352936L;
		
		private double m_zoom = 1.0;
        private double m_zoomPercentage;
        private Image m_image;
                
        /**
         * Constructor
         * 
         * @param image
         * @param zoomPercentage
         */                
        public ImagePanel(Image image, double zoomPercentage)
        {
            m_image = image;
            m_zoomPercentage = zoomPercentage / 100;
        }
        
        /**
         * This method is overriden to draw the image
         * and scale the graphics accordingly
         */
        public void paintComponent(Graphics grp) 
        { 
            Graphics2D g2D = (Graphics2D) grp;
            g2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            //set the background color to white
            g2D.setColor(Color.WHITE);
            //fill the rect
            g2D.fillRect(0, 0, getWidth(), getHeight());
            
            //scale the graphics to get the zoom effect
            g2D.scale(m_zoom, m_zoom);
            
            //draw the image
            g2D.drawImage(m_image, 0, 0, this); 
        }
         
        /**
         * This method is overriden to return the preferred size
         * which will be the width and height of the image plus
         * the zoomed width width and height. 
         * while zooming out the zoomed width and height is negative
         */
        public Dimension getPreferredSize()
        {
            return new Dimension((int)(m_image.getWidth(this) + 
                                      (m_image.getWidth(this) * (m_zoom - 1))),
                                 (int)(m_image.getHeight(this) + 
                                      (m_image.getHeight(this) * (m_zoom -1 ))));
        }
        
        /**
         * Sets the new zoomed percentage
         * @param zoomPercentage
         */
        public void setZoomPercentage(int zoomPercentage)
        {
            m_zoomPercentage = ((double)zoomPercentage) / 100;    
        }
        
        /**
         * This method set the image to the original size
         * by setting the zoom factor to 1. i.e. 100%
         */
        public void originalSize()
        {
            m_zoom = 1; 
        }
        
        /**
         * This method increments the zoom factor with
         * the zoom percentage, to create the zoom in effect 
         */
        public void zoomIn()
        {
            m_zoom += m_zoomPercentage;
        }            
        
        /**
         * This method decrements the zoom factor with the 
         * zoom percentage, to create the zoom out effect 
         */
        public void zoomOut()
        {
            m_zoom -= m_zoomPercentage;
            
            if(m_zoom < m_zoomPercentage)
            {
                if(m_zoomPercentage > 1.0)
                {
                    m_zoom = 1.0;
                }
                else
                {
                    zoomIn();
                }
            }
        }
        
        /**
         * This method returns the currently
         * zoomed percentage
         * 
         * @return precentage
         */
        public double getZoomedTo()
        {
            return m_zoom * 100; 
        }
    } 
}
