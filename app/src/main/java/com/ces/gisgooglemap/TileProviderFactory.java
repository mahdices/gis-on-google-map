package com.ces.gisgooglemap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import android.util.Log;

public class TileProviderFactory {

    public static WMSTileProvider getWMSTileProviderByName(String layerName) {
        final String OSGEO_WMS = "{YOUR-URL}"
                + "LAYERS=" + layerName
                + "&FORMAT={FORMAT}"
                + "PROJECTION={PROJECTION}&"
                + "TILEORIGIN={TILEORIGIN}"
                + "TILESIZE={TILESIZE}"
                + "&MAXEXTENT={MAXEXTENT}&VERSION={VERSION}&REQUEST={REQUEST}&SRS={SRS}"
                + "&BBOX={BBOX}&HEIGHT={HEIGHT}&transparent={transparent}";


        return new WMSTileProvider(512, 512) {

            @Override
            public synchronized URL getTileUrl(int x, int y, int zoom) {
                final double[] bbox = getBoundingBox(x, y, zoom);
                String s = String.format(Locale.US, OSGEO_WMS, bbox[MINX], bbox[MINY], bbox[MAXX], bbox[MAXY]);
                try {
                    return new URL(s);
                } catch (MalformedURLException e) {
                    throw new AssertionError(e);
                }
            }
        };
    }
}