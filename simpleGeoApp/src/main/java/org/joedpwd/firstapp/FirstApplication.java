package org.joedpwd.firstapp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.map.MapViewport;
import org.geotools.referencing.CRS;
import org.geotools.xsd.Parser;
import org.geotools.sld.SLDConfiguration;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.Displacement;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Font;
import org.geotools.styling.FontImpl;
import org.geotools.styling.LabelPlacement;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.swing.JMapFrame;
import org.geotools.util.factory.Hints;
import org.geotools.xsd.Configuration;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.style.ContrastMethod;

public class FirstApplication {
	
	public static final float EUROPE_LATITUDE = (float) 51.5260;
	public static final float EUROPE_LONGITUDE = (float) 15.2551;
	
	private static final Color TEXT_OUTLINE_COLOUR = Color.BLACK;
	private static final Color TEXT_FILL_COLOUR = Color.WHITE;
	
	private JMapFrame frame;
	
	private StyleFactory sf = CommonFactoryFinder.getStyleFactory();
    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
    
    private GridCoverage2DReader reader;
    
    private File rasterFile;
    private File shapeFile;
    
    private SimpleFeatureSource featureSource;
    
	public static void main(String[] args)
	{
		//System.out.print(args.length);
		if(args.length == 2) {
			FirstApplication app = new FirstApplication(args[0], args[1]);
			app.getLayerAndDisplay();
		}
		
	}

	public FirstApplication(String raster, String shape) {
		this.rasterFile = new File(raster);
		this.shapeFile = new File(shape);
	}
	
	private void getLayerAndDisplay() {

		try
		{
			
			FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
			
			featureSource = store.getFeatureSource();
			
			AbstractGridFormat format = GridFormatFinder.findFormat( rasterFile );
			this.reader = format.getReader( rasterFile, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE) );
			CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
			
			
			Style rasterStyle = createRGBStyle();
	        
			Style style = this.createDefaultFeatureStyle();
	        Layer layer = new FeatureLayer(featureSource, style);
	
	        //System.out.println(featureSource.getName());
	    
			Layer rasterLayer = new GridReaderLayer(reader, rasterStyle);
			final MapContent map = new MapContent();
			
			ReferencedEnvelope europe_envelope = new ReferencedEnvelope(crs);
			
			europe_envelope.init(EUROPE_LONGITUDE - 40, EUROPE_LONGITUDE + 40, EUROPE_LATITUDE + 20, EUROPE_LATITUDE - 20);
			
			MapViewport viewport = new MapViewport(europe_envelope);
			
			Rectangle r = new Rectangle(0,0,1350,675);
			
			map.addLayer(rasterLayer);
			map.addLayer(layer);
			map.setViewport(viewport);
			
			frame = new JMapFrame(map);
			frame.enableToolBar(true);
			frame.enableLayerTable(true);
			
			Dimension d = new Dimension(1350, 675);
			Dimension d1 = new Dimension(1350,675);
			
			frame.setSize(d);
			viewport.setScreenArea(r);
			
			
			frame.getMapPane().setPreferredSize(d);
			
			frame.setPreferredSize(d1);
			
			frame.pack();
			frame.setVisible(true);
			
			/*System.out.println(viewport.getScreenArea());
			System.out.println(frame.getMapPane().getSize().toString());
			System.out.println(frame.getSize());*/
					
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	/**
     * Create a Style to display a selected band of the GeoTIFF image as a greyscale layer
     *
     * @return a new Style instance to render the image in greyscale
     */
    private Style createGreyscaleStyle() {
        GridCoverage2D cov = null;
        try {
            cov = reader.read(null);
        } catch (IOException giveUp) {
            throw new RuntimeException(giveUp);
        }
        int numBands = cov.getNumSampleDimensions();
        Integer[] bandNumbers = new Integer[numBands];
        for (int i = 0; i < numBands; i++) {
            bandNumbers[i] = i + 1;
        }
        Object selection =
                JOptionPane.showInputDialog(
                        frame,
                        "Band to use for greyscale display",
                        "Select an image band",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        bandNumbers,
                        1);
        if (selection != null) {
            int band = ((Number) selection).intValue();
            return createGreyscaleStyle(band);
        }
        return null;
    }

    /**
     * Create a Style to display the specified band of the GeoTIFF image as a greyscale layer.
     *
     * <p>This method is a helper for createGreyScale() and is also called directly by the
     * displayLayers() method when the application first starts.
     *
     * @param band the image band to use for the greyscale display
     * @return a new Style instance to render the image in greyscale
     */
    private Style createGreyscaleStyle(int band) {
        ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
        SelectedChannelType sct = sf.createSelectedChannelType(String.valueOf(band), ce);

        RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
        ChannelSelection sel = sf.channelSelection(sct);
        sym.setChannelSelection(sel);

        return SLD.wrapSymbolizers(sym);
    }
    
    
    private Style createRGBStyle() {
        GridCoverage2D cov = null;
        try {
            cov = reader.read(null);
        } catch (IOException giveUp) {
            throw new RuntimeException(giveUp);
        }
        // We need at least three bands to create an RGB style
        int numBands = cov.getNumSampleDimensions();
        if (numBands < 3) {
            return null;
        }
        // Get the names of the bands
        String[] sampleDimensionNames = new String[numBands];
        for (int i = 0; i < numBands; i++) {
            GridSampleDimension dim = cov.getSampleDimension(i);
            sampleDimensionNames[i] = dim.getDescription().toString();
        }
        final int RED = 0, GREEN = 1, BLUE = 2;
        int[] channelNum = {-1, -1, -1};
        // We examine the band names looking for "red...", "green...", "blue...".
        // Note that the channel numbers we record are indexed from 1, not 0.
        for (int i = 0; i < numBands; i++) {
            String name = sampleDimensionNames[i].toLowerCase();
            if (name != null) {
                if (name.matches("red.*")) {
                    channelNum[RED] = i + 1;
                } else if (name.matches("green.*")) {
                    channelNum[GREEN] = i + 1;
                } else if (name.matches("blue.*")) {
                    channelNum[BLUE] = i + 1;
                }
            }
        }
        // If we didn't find named bands "red...", "green...", "blue..."
        // we fall back to using the first three bands in order
        if (channelNum[RED] < 0 || channelNum[GREEN] < 0 || channelNum[BLUE] < 0) {
            channelNum[RED] = 1;
            channelNum[GREEN] = 2;
            channelNum[BLUE] = 3;
        }
        // Now we create a RasterSymbolizer using the selected channels
        SelectedChannelType[] sct = new SelectedChannelType[cov.getNumSampleDimensions()];
        ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
        for (int i = 0; i < 3; i++) {
            sct[i] = sf.createSelectedChannelType(String.valueOf(channelNum[i]), ce);
        }
        RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
        ChannelSelection sel = sf.channelSelection(sct[RED], sct[GREEN], sct[BLUE]);
        sym.setChannelSelection(sel);

        return SLD.wrapSymbolizers(sym);
    }
    
    private Style createDefaultFeatureStyle() {
    	Style style = SLD.createSimpleStyle(featureSource.getSchema());
    	StyleBuilder sb = new StyleBuilder();
    	
    	Rule rule = createRule(TEXT_OUTLINE_COLOUR, TEXT_FILL_COLOUR);
    	
    	FeatureTypeStyle fts = sb.createFeatureTypeStyle("Feature", rule);
    	
    	//System.out.println(fts.featureTypeNames());
    	
    	style.featureTypeStyles().add(fts);
    	return style;
    }

	private Rule createRule(Color textOutlineColour, Color textFillColour) {
		Symbolizer symb = null;
		
		StyleBuilder sb = new StyleBuilder();
		Fill fill = sb.createFill(ff.literal(textFillColour));
		AnchorPoint anchorPoint = sb.createAnchorPoint(0.5, 0.5);
		Displacement d = sb.createDisplacement(0, 0);
		PointPlacement pointPlacement = sb.createPointPlacement(anchorPoint, d, sb.literalExpression(0));
		Font f = sb.createFont("Arial", 14);
		
		symb = sb.createTextSymbolizer(fill, new Font[] {f}, sb.createHalo(sb.createFill(ff.literal(Color.BLACK)), 2), sb.attributeExpression("CNTRY_NAME"), pointPlacement, null);
		
		Rule rule = sf.createRule();
		rule.symbolizers().add(symb);
		return rule;
	}
}
