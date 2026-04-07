package com.imgedt.editor.filter;

import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class FilterRendererTest {

    @Test
    public void renderToBitmap_notInitialized_returnsNull() {
        FilterParams params = new FilterParams();
        FilterRenderer renderer = new FilterRenderer(null, params);
        assertNull(renderer.renderToBitmap());
    }

    @Test
    public void renderToBitmap_noSourceBitmap_returnsNull() {
        FilterParams params = new FilterParams();
        FilterRenderer renderer = new FilterRenderer(null, params);
        assertNull(renderer.renderToBitmap());
    }

    @Test
    public void releaseBeforeStart_noCrash() {
        FilterParams params = new FilterParams();
        FilterRenderer renderer = new FilterRenderer(null, params);
        renderer.release();
    }

    @Test
    public void requestRender_beforeStart_noCrash() {
        FilterParams params = new FilterParams();
        FilterRenderer renderer = new FilterRenderer(null, params);
        renderer.requestRender();
    }

    @Test
    public void setSourceBitmap_beforeStart_noCrash() {
        FilterParams params = new FilterParams();
        FilterRenderer renderer = new FilterRenderer(null, params);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        renderer.setSourceBitmap(bitmap);
        bitmap.recycle();
    }
}
