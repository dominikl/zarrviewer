package zarrviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.util.List;

public class ZarrViewer {
    private JFrame frame;
    private ImagePanel imagePanel;
    private JComboBox<String> seriesComboBox;
    private JComboBox<String> resolutionComboBox;
    private JComboBox<Integer> channelComboBox;
    private JComboBox<Integer> zStackComboBox;
    private JComboBox<Integer> timepointComboBox;
    private ZarrReader reader;

    public ZarrViewer() {
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        frame = new JFrame("Zarr Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        setupMenuBar();
        setupImagePanel();
        setupControlPanel();
        
        frame.setSize(1000, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        
        JMenuItem openDirItem = new JMenuItem("Open Zarr", KeyEvent.VK_O);
        openDirItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openDirItem.addActionListener(e -> openZarrDirectory());
        
        fileMenu.add(openDirItem);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);
    }

    private void setupImagePanel() {
        imagePanel = new ImagePanel();
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        frame.add(scrollPane, BorderLayout.CENTER);
    }

    private void setupControlPanel() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setPreferredSize(new Dimension(800, 30));
        bottomPanel.setBackground(new Color(240, 240, 240));
        
        // Series selector
        bottomPanel.add(new JLabel("Series:"));
        seriesComboBox = new JComboBox<>();
        seriesComboBox.setPreferredSize(new Dimension(100, 25));
        seriesComboBox.addActionListener(e -> handleSeriesSelection());
        bottomPanel.add(seriesComboBox);
        
        // Resolution selector
        bottomPanel.add(new JLabel("Resolution:"));
        resolutionComboBox = new JComboBox<>();
        resolutionComboBox.setPreferredSize(new Dimension(100, 25));
        resolutionComboBox.addActionListener(e -> handleResolutionSelection());
        bottomPanel.add(resolutionComboBox);
        
        // Channel selector
        bottomPanel.add(new JLabel("Channel:"));
        channelComboBox = new JComboBox<>();
        channelComboBox.setPreferredSize(new Dimension(100, 25));
        channelComboBox.addActionListener(e -> updateImage());
        bottomPanel.add(channelComboBox);
        
        // Z-stack selector
        bottomPanel.add(new JLabel("Z-Stack:"));
        zStackComboBox = new JComboBox<>();
        zStackComboBox.setPreferredSize(new Dimension(100, 25));
        zStackComboBox.addActionListener(e -> updateImage());
        bottomPanel.add(zStackComboBox);
        
        // Timepoint selector
        bottomPanel.add(new JLabel("Timepoint:"));
        timepointComboBox = new JComboBox<>();
        timepointComboBox.setPreferredSize(new Dimension(100, 25));
        timepointComboBox.addActionListener(e -> updateImage());
        bottomPanel.add(timepointComboBox);
        
        frame.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void openZarrDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Zarr");
        //fileChooser.setCurrentDirectory(new File("/Users/dom/Testing/zarr"));
        
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            System.out.println("Selected directory: " + selectedDir.getAbsolutePath());
            reader = new ZarrReader(selectedDir.getAbsolutePath());
            updateSeriesComboBox();
        }
    }

    private void updateSeriesComboBox() {
        seriesComboBox.removeAllItems();
        for (String s : reader.getSeriesPaths()) {
            seriesComboBox.addItem(s);
        }
    }

    private void handleSeriesSelection() {
        if (seriesComboBox.getSelectedItem() != null) {
            String selectedSeries = (String) seriesComboBox.getSelectedItem();
            try {
                List<String> resolutions = reader.setSeries(selectedSeries);
                updateResolutionComboBox(resolutions);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void updateResolutionComboBox(List<String> resolutions) {
        resolutionComboBox.removeAllItems();
        for (String resolution : resolutions) {
            resolutionComboBox.addItem(resolution);
        }
    }

    private void handleResolutionSelection() {
        if (resolutionComboBox.getSelectedItem() != null) {
            String selectedResolution = (String) resolutionComboBox.getSelectedItem();
            try {
                reader.setResolution(selectedResolution);
                updateDimensionComboBoxes();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Selected resolution: " + selectedResolution);
        }
    }

    private void updateDimensionComboBoxes() {
        // Update Channel ComboBox
        channelComboBox.removeAllItems();
        for (int i = 0; i < reader.getMaxC(); i++) {
            channelComboBox.addItem(i);
        }

        // Update Z-Stack ComboBox
        zStackComboBox.removeAllItems();
        for (int i = 0; i < reader.getMaxZ(); i++) {
            zStackComboBox.addItem(i);
        }

        // Update Timepoint ComboBox
        timepointComboBox.removeAllItems();
        for (int i = 0; i < reader.getMaxT(); i++) {
            timepointComboBox.addItem(i);
        }
    }

    private void updateImage() {
        if (channelComboBox.getSelectedItem() != null && 
            zStackComboBox.getSelectedItem() != null && 
            timepointComboBox.getSelectedItem() != null) {
            
            int selectedChannel = (int) channelComboBox.getSelectedItem();
            int selectedZ = (int) zStackComboBox.getSelectedItem();
            int selectedTimepoint = (int) timepointComboBox.getSelectedItem();
            
            try {
                long[] planeData = reader.loadPlane(selectedChannel, selectedZ, selectedTimepoint);
                int x = reader.getMaxX();
                int y = reader.getMaxY();
                BufferedImage image = toBufferedImage(planeData, x, y);
                imagePanel.setImage(image);
                System.out.println("Loaded plane: C=" + selectedChannel + ", Z=" + selectedZ + ", T=" + selectedTimepoint);
                System.out.println("Image size: " + image.getWidth() + "x" + image.getHeight());
                System.out.println("planeData size: " + planeData.length);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private BufferedImage toBufferedImage(long[] data, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = image.getRaster();
        byte[] pixels = new byte[width * height];
        
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        
        for (long value : data) {
            if (value < min) min = value;
            if (value > max) max = value;
        }
        
        float scale = 255.0f / (max - min);
        for (int i = 0; i < data.length; i++) {
            pixels[i] = (byte)((data[i] - min) * scale);
        }
        
        raster.setDataElements(0, 0, width, height, pixels);
        return image;
    }
}
