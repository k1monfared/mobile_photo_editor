package com.imgedt.editor.crop;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CropViewTest {

    private CropView cropView;
    private CropAreaView areaView;
    private Bitmap testBitmap;

    @Before
    public void setUp() throws Exception {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            cropView = new CropView(InstrumentationRegistry.getInstrumentation().getTargetContext());
            areaView = new CropAreaView(InstrumentationRegistry.getInstrumentation().getTargetContext());

            int widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY);
            int heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY);
            areaView.measure(widthSpec, heightSpec);
            areaView.layout(0, 0, 1080, 1920);
            areaView.calculateInitialRect(0);
            cropView.setAreaView(areaView);
        });

        testBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888);
        testBitmap.eraseColor(Color.BLUE);
    }

    @Test
    public void cropBitmap_nullBitmap_returnsNull() {
        assertNull(cropView.cropBitmap(1280));
    }

    @Test
    public void cropBitmap_noAreaView_returnsNull() {
        final CropView[] bare = new CropView[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            bare[0] = new CropView(InstrumentationRegistry.getInstrumentation().getTargetContext());
        });
        bare[0].setBitmap(testBitmap);
        assertNull(bare[0].cropBitmap(1280));
    }

    @Test
    public void cropBitmap_basicCrop_returnsBitmap() {
        cropView.setBitmap(testBitmap);
        cropView.resetToFit(areaView.getCropRect());
        Bitmap result = cropView.cropBitmap(1280);
        assertNotNull(result);
        assertTrue(result.getWidth() > 0);
        assertTrue(result.getHeight() > 0);
        result.recycle();
    }

    @Test
    public void cropBitmap_maxSideConstraint() {
        cropView.setBitmap(testBitmap);
        cropView.resetToFit(areaView.getCropRect());
        Bitmap result = cropView.cropBitmap(200);
        assertNotNull(result);
        assertTrue(result.getWidth() <= 200);
        assertTrue(result.getHeight() <= 200);
        result.recycle();
    }

    @Test
    public void resetToFit_setsMinimumScale() {
        cropView.setBitmap(testBitmap);
        cropView.resetToFit(areaView.getCropRect());
        CropState state = cropView.getState();
        assertEquals(state.minimumScale, state.scale, 0.001f);
    }

    @Test
    public void rotate90_updatesOrientation() {
        cropView.setBitmap(testBitmap);
        cropView.resetToFit(areaView.getCropRect());
        cropView.rotate90(90);
        assertEquals(90, cropView.getState().orientation);
    }

    @Test
    public void rotate90_cyclesOrientation() {
        cropView.setBitmap(testBitmap);
        cropView.resetToFit(areaView.getCropRect());
        cropView.rotate90(90);
        cropView.rotate90(90);
        cropView.rotate90(90);
        cropView.rotate90(90);
        assertEquals(0, cropView.getState().orientation);
    }

    @Test
    public void mirror_togglesState() {
        cropView.setBitmap(testBitmap);
        cropView.mirror();
        assertTrue(cropView.getState().mirrored);
        cropView.mirror();
        assertTrue(!cropView.getState().mirrored);
    }

    @Test
    public void allowBlackAreas_defaultFalse() {
        assertTrue(!cropView.getAllowBlackAreas());
    }

    @Test
    public void allowBlackAreas_setAndGet() {
        cropView.setAllowBlackAreas(true);
        assertTrue(cropView.getAllowBlackAreas());
    }
}
