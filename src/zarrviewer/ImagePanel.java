package zarrviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel {
    private BufferedImage image;
    private Point lastDragPoint;
    private boolean isDragging = false;
    
    public ImagePanel() {
        setBackground(Color.BLACK);
        
        // Add mouse listeners for drag scrolling
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Start dragging
                lastDragPoint = e.getPoint();
                isDragging = true;
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                // Stop dragging
                isDragging = false;
                setCursor(Cursor.getDefaultCursor());
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging && getParent() instanceof JViewport) {
                    JViewport viewport = (JViewport) getParent();
                    Point currentPoint = e.getPoint();
                    
                    // Calculate how far the mouse was moved
                    int deltaX = lastDragPoint.x - currentPoint.x;
                    int deltaY = lastDragPoint.y - currentPoint.y;
                    
                    // Get current viewport position
                    Point viewPosition = viewport.getViewPosition();
                    
                    // Calculate new position
                    viewPosition.translate(deltaX, deltaY);
                    
                    // Make sure we stay within bounds
                    int maxX = getWidth() - viewport.getWidth();
                    int maxY = getHeight() - viewport.getHeight();
                    viewPosition.x = Math.max(0, Math.min(viewPosition.x, maxX));
                    viewPosition.y = Math.max(0, Math.min(viewPosition.y, maxY));
                    
                    // Update the viewport position
                    viewport.setViewPosition(viewPosition);
                    
                    // Update last drag point
                    lastDragPoint = currentPoint;
                }
            }
        };
        
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }
    
    public void setImage(BufferedImage image) {
        this.image = image;
        if (image != null) {
            setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        }
        revalidate();
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        }
    }
}
