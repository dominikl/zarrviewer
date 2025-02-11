package zarrviewer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.zarr.zarrjava.store.FilesystemStore;
import dev.zarr.zarrjava.store.StoreHandle;
import dev.zarr.zarrjava.v3.Array;
import dev.zarr.zarrjava.v3.Group;
import ucar.ma2.DataType;

public class ZarrReader {

    String path;

    FilesystemStore fs = null;

    List<String> seriesPaths = new ArrayList<>();
    int maxColumns = 0;
    int maxFields = 0;
    ArrayList<String> resPaths = new ArrayList<>();
    int maxT = 0;
    int maxZ = 0;
    int maxC = 0;
    int maxX = 0;
    int maxY = 0;

    int[] shape = null;
    String seriesGroupKey = null;
    String columnKey = "";
    String fieldKey = "";
    String resolutionKey = "";
    String t = null;
    String c = null;
    String z = null;

    int t_index = -1;
    int z_index = -1;
    int c_index = -1;
    int x_index = -1;
    int y_index = -1;

    public ZarrReader(String path) {
        this.path = path;
        this.seriesPaths = check();
        if(seriesPaths == null)
            throw new IllegalArgumentException(
                "This is not a bioformats2raw.layout 3 ome.zarr: " + path);
    }

    public List<String> getSeriesPaths() {
        return seriesPaths;
    }
    
    public List<String> setSeries(String series) throws Exception {
        this.seriesGroupKey = series;
        Group seriesGroup = Group.open(fs.resolve(this.seriesGroupKey));
        MetadataWrapper mw = new MetadataWrapper(seriesGroup.metadata.attributes);
        Object tmp = mw.find("multiscales", "datasets");
        if(tmp == null) {
            System.out.println("No resolutions found.");
            return this.resPaths;
        }
        ArrayList tmpList = (ArrayList)tmp;
        for (Object o : tmpList) {
            Map<String, Object> tmpMap = (Map<String, Object>)o;
            resPaths.add((String)tmpMap.get("path"));
        }

        Object tmp2 = mw.find("multiscales", "axes");
        if(tmp2 == null) {
            throw new IllegalArgumentException("No axes found.");
        }
        ArrayList tmpList2 = (ArrayList)tmp2;
        for (int i=0; i<tmpList2.size(); i++) {
            Object o = tmpList2.get(i);
            Map<String, Object> tmpMap = (Map<String, Object>)o;
            if(tmpMap.get("name").equals("t")) {
                t_index = i;
            }
            else if(tmpMap.get("name").equals("z")) {
                z_index = i;
            }
            else if(tmpMap.get("name").equals("c")) {
                c_index = i;
            }
            else if(tmpMap.get("name").equals("x")) {
                x_index = i;
            }
            else if(tmpMap.get("name").equals("y")) {
                y_index = i;
            }
        }
        if (t_index == -1 || z_index == -1 || c_index == -1 || x_index == -1 || y_index == -1) {
            throw new IllegalArgumentException("No indexes found.");
        }
        return this.resPaths;
    }

    public List<String> getResPaths() {
        return resPaths;
    }

    public void setResolution(String resolution) throws Exception {
        this.resolutionKey = resolution;

        StoreHandle arrayHandle = fs.resolve(seriesGroupKey, columnKey, fieldKey, resolutionKey);
        Array array = Array.open(arrayHandle);
        long[] shape = array.metadata.shape;
        this.maxC = (int) shape[c_index];
        this.maxZ = (int) shape[z_index];
        this.maxT = (int) shape[t_index];  
        this.maxX = (int) shape[x_index];
        this.maxY = (int) shape[y_index];
    }

    public int getMaxC() {
        return maxC;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getMaxT() {
        return maxT;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public long[] loadPlane(int c, int z, int t) {
        StoreHandle arrayHandle = fs.resolve(seriesGroupKey, columnKey, fieldKey, resolutionKey);
        try {
            Array array = Array.open(arrayHandle);
            long[] offset = new long[]{0, 0, 0, 0, 0};
            offset[c_index] = c;
            offset[z_index] = z;
            offset[t_index] = t;
            offset[x_index] = 0;
            offset[y_index] = 0;
            int[] shape = new int[]{1, 1, 1, 1, 1};
            shape[c_index] = 1;
            shape[z_index] = 1;
            shape[t_index] = 1;
            shape[x_index] = maxX;
            shape[y_index] = maxY;
            System.out.println("offset "+Arrays.toString(offset)+"\nshape "+Arrays.toString(shape));
            ucar.ma2.Array data = array.read(offset, shape);
            System.out.println("DataType "+data.getDataType());
            System.out.println(data.getSize());
            long values[] = new long[(int) data.getSize()];
            long min = Long.MAX_VALUE;
            long max = 0;
            int i = 0;
            while(data.hasNext()) {
                Number value = (Number) data.next();
                value = DataType.widenNumberIfNegative(value);
                values[i] = value.longValue();
                if(value.longValue() < min)
                    min = value.longValue();
                if(value.longValue() > max)
                    max = value.longValue();
                i++;
            }
            System.out.println("Min "+min+" Max "+max);
            return values;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;   
    }

    private List<String> check() {
        if(!Files.exists(Path.of(this.path, "zarr.json")))
            return null;

        try {
            this.fs = new FilesystemStore(this.path);
            Group root = Group.open(fs.resolve(""));
            Map<String, Object> att = root.metadata.attributes;
            if(!att.get("bioformats2raw.layout").equals(3))
                return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            return Files.list(Path.of(this.path))
                .filter(Files::isDirectory)
                .filter(f -> f.getFileName().toString().matches("\\d+"))
                .map(f -> f.getFileName().toString())
                .collect(Collectors.toList()); 
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
