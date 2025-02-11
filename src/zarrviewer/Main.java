package zarrviewer;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.List;

public class Main {
    private static JFrame frame;
    private static ImagePanel imagePanel;
    private static JComboBox<String> seriesComboBox;
    private static JComboBox<String> resolutionComboBox;
    private static JComboBox<Integer> channelComboBox;
    private static JComboBox<Integer> zStackComboBox;
    private static JComboBox<Integer> timepointComboBox;

    private static ZarrReader reader = null;

    private static void createAndShowGUI() {
        frame = new JFrame("Image Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        
        // Create Open Directory menu item
        JMenuItem openDirItem = new JMenuItem("Open Zarr", KeyEvent.VK_O);
        openDirItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openDirItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogTitle("Select Zarr");
            fileChooser.setCurrentDirectory(new File("/Users/dom/Testing/zarr"));
            
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File selectedDir = fileChooser.getSelectedFile();

                System.out.println("Selected directory: " + selectedDir.getAbsolutePath());

                Main.reader = new ZarrReader(selectedDir.getAbsolutePath());

                // Update series dropdown when a new directory is loaded
                seriesComboBox.removeAllItems();
                for(String s : Main.reader.getSeriesPaths()) {
                    seriesComboBox.addItem(s);
                }
            }
        });
        
        fileMenu.add(openDirItem);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);
        
        imagePanel = new ImagePanel();
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        frame.add(scrollPane, BorderLayout.CENTER);
        
        // Add bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setPreferredSize(new Dimension(800, 30));
        bottomPanel.setBackground(new Color(240, 240, 240));
        
        // Add series selector
        JLabel seriesLabel = new JLabel("Series:");
        bottomPanel.add(seriesLabel);
        
        seriesComboBox = new JComboBox<>();
        seriesComboBox.setPreferredSize(new Dimension(100, 25));
        seriesComboBox.addActionListener(e -> {
            if (seriesComboBox.getSelectedItem() != null) {
                String selectedSeries = (String) seriesComboBox.getSelectedItem();
                System.out.println("Selected series: " + selectedSeries);
                resolutionComboBox.removeAllItems();
                try {
                    List<String> resPaths = Main.reader.setSeries(selectedSeries);
                    System.out.println("Available resolutions: " + resPaths);
                    // Update resolution combo box  
                    for (String resPath : resPaths) {
                        resolutionComboBox.addItem(resPath);
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        bottomPanel.add(seriesComboBox);

        // Add resolution selector
        JLabel resolutionLabel = new JLabel("Resolution:");
        bottomPanel.add(resolutionLabel);
        
        resolutionComboBox = new JComboBox<>();
        resolutionComboBox.setPreferredSize(new Dimension(100, 25));
        resolutionComboBox.addActionListener(e -> {
            if (resolutionComboBox.getSelectedItem() != null) {
                String selectedResolution = (String) resolutionComboBox.getSelectedItem();
                try {
                    Main.reader.setResolution(selectedResolution);
                    
                    channelComboBox.removeAllItems();
                    zStackComboBox.removeAllItems();
                    timepointComboBox.removeAllItems(); 
                    for(int i=0; i<Main.reader.getMaxC(); i++) {
                        channelComboBox.addItem(i);
                    }
                    for(int i=0; i<Main.reader.getMaxZ(); i++) {
                        zStackComboBox.addItem(i);  
                    }
                    for(int i=0; i<Main.reader.getMaxT(); i++) {
                        timepointComboBox.addItem(i);  
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                System.out.println("Selected resolution: " + selectedResolution);
            }
        });
        bottomPanel.add(resolutionComboBox);

        // Add channel selector
        JLabel channelLabel = new JLabel("Channel:");
        bottomPanel.add(channelLabel);
        
        channelComboBox = new JComboBox<>();
        channelComboBox.setPreferredSize(new Dimension(100, 25));
        channelComboBox.addItem(0); // Add default channel
        channelComboBox.addActionListener(e -> {
            if (channelComboBox.getSelectedItem() != null && 
                zStackComboBox.getSelectedItem() != null && 
                timepointComboBox.getSelectedItem() != null) {
                updateImage();
            }
        });
        bottomPanel.add(channelComboBox);

        // Add Z-Stack selector
        JLabel zStackLabel = new JLabel("Z-Stack:");
        bottomPanel.add(zStackLabel);
        
        zStackComboBox = new JComboBox<>();
        zStackComboBox.setPreferredSize(new Dimension(100, 25));
        zStackComboBox.addItem(0); // Add default Z position
        zStackComboBox.addActionListener(e -> {
            if (channelComboBox.getSelectedItem() != null && 
                zStackComboBox.getSelectedItem() != null && 
                timepointComboBox.getSelectedItem() != null) {
                updateImage();
            }
        });
        bottomPanel.add(zStackComboBox);

        // Add Timepoint selector
        JLabel timepointLabel = new JLabel("Timepoint:");
        bottomPanel.add(timepointLabel);
        
        timepointComboBox = new JComboBox<>();
        timepointComboBox.setPreferredSize(new Dimension(100, 25));
        timepointComboBox.addItem(0); // Add default timepoint
        timepointComboBox.addActionListener(e -> {
            if (channelComboBox.getSelectedItem() != null && 
                zStackComboBox.getSelectedItem() != null && 
                timepointComboBox.getSelectedItem() != null) {
                updateImage();
            }
        });
        bottomPanel.add(timepointComboBox);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setSize(1000, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void updateImage() {
        int selectedChannel = (int) channelComboBox.getSelectedItem();
        int selectedZ = (int) zStackComboBox.getSelectedItem();
        int selectedTimepoint = (int) timepointComboBox.getSelectedItem();
        System.out.println("selectedChannel " + selectedChannel + ", selectedZ " + selectedZ + ", selectedTimepoint " + selectedTimepoint);
        try {
            long[] planeData = reader.loadPlane(selectedChannel, selectedZ, selectedTimepoint);
            System.out.println("Loaded plane: C=" + selectedChannel + ", Z=" + selectedZ + ", T=" + selectedTimepoint);
            System.out.println("planeData size: " + planeData.length);
            int x = reader.getMaxX();
            int y = reader.getMaxY();
            BufferedImage image = toBufferedImage(planeData, x, y);
            imagePanel.setImage(image);
            System.out.println("Created Image, size: " + image.getWidth() + "x" + image.getHeight());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static BufferedImage toBufferedImage(long[] data, int width, int height) {
        // Create a grayscale image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        // Get the raster and data buffer
        WritableRaster raster = image.getRaster();
        byte[] pixels = new byte[width * height];
        
        // Convert shorts to bytes, scaling from 0-255
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        
        // Find min and max values
        for (long value : data) {
            if (value < min) min = value;
            if (value > max) max = value;
        }
        
        // Scale the values to 0-255 range
        double scale = 255.0 / (max - min);
        for (int i = 0; i < data.length; i++) {
            pixels[i] = (byte)((data[i] - min) * scale);
        }
        
        // Set the pixel data
        raster.setDataElements(0, 0, width, height, pixels);
        return image;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }
}
