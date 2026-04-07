package com.imgedt.editor.crop;

import android.graphics.RectF;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CropAreaViewTest {

    private CropAreaView areaView;

    @Before
    public void setUp() {
        areaView = new CropAreaView(InstrumentationRegistry.getInstrumentation().getTargetContext());
        int widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY);
        int heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY);
        areaView.measure(widthSpec, heightSpec);
        areaView.layout(0, 0, 1080, 1920);
        areaView.setCropRect(new RectF(100, 200, 900, 1600));
    }

    @Test
    public void hitTest_cornerTopLeft_detected() {
        CropAreaView.Control result = areaView.hitTest(105, 205);
        assertEquals(CropAreaView.Control.TOP_LEFT, result);
    }

    @Test
    public void hitTest_cornerTopRight_detected() {
        CropAreaView.Control result = areaView.hitTest(895, 205);
        assertEquals(CropAreaView.Control.TOP_RIGHT, result);
    }

    @Test
    public void hitTest_cornerBottomLeft_detected() {
        CropAreaView.Control result = areaView.hitTest(105, 1595);
        assertEquals(CropAreaView.Control.BOTTOM_LEFT, result);
    }

    @Test
    public void hitTest_cornerBottomRight_detected() {
        CropAreaView.Control result = areaView.hitTest(895, 1595);
        assertEquals(CropAreaView.Control.BOTTOM_RIGHT, result);
    }

    @Test
    public void hitTest_edgeTop_detected() {
        CropAreaView.Control result = areaView.hitTest(500, 202);
        assertEquals(CropAreaView.Control.TOP, result);
    }

    @Test
    public void hitTest_edgeBottom_detected() {
        CropAreaView.Control result = areaView.hitTest(500, 1598);
        assertEquals(CropAreaView.Control.BOTTOM, result);
    }

    @Test
    public void hitTest_edgeLeft_detected() {
        CropAreaView.Control result = areaView.hitTest(102, 900);
        assertEquals(CropAreaView.Control.LEFT, result);
    }

    @Test
    public void hitTest_edgeRight_detected() {
        CropAreaView.Control result = areaView.hitTest(898, 900);
        assertEquals(CropAreaView.Control.RIGHT, result);
    }

    @Test
    public void hitTest_outsideCrop_returnsNone() {
        CropAreaView.Control result = areaView.hitTest(500, 900);
        assertEquals(CropAreaView.Control.NONE, result);
    }

    @Test
    public void hitTest_farOutside_returnsNone() {
        CropAreaView.Control result = areaView.hitTest(10, 10);
        assertEquals(CropAreaView.Control.NONE, result);
    }

    @Test
    public void setCropRect_updateAndGet() {
        RectF newRect = new RectF(50, 50, 500, 500);
        areaView.setCropRect(newRect);
        RectF got = areaView.getCropRect();
        assertEquals(50, got.left, 0.1f);
        assertEquals(50, got.top, 0.1f);
        assertEquals(500, got.right, 0.1f);
        assertEquals(500, got.bottom, 0.1f);
    }

    @Test
    public void getCropRect_returnsCopy() {
        RectF r1 = areaView.getCropRect();
        RectF r2 = areaView.getCropRect();
        assertEquals(r1, r2);
        r1.left = 999;
        RectF r3 = areaView.getCropRect();
        assertFalse(r3.left == 999);
    }

    @Test
    public void calculateInitialRect_freeform_fillsAvailableArea() {
        areaView.calculateInitialRect(0);
        RectF rect = areaView.getCropRect();
        assertTrue(rect.width() > 900);
        assertTrue(rect.height() > 1700);
    }

    @Test
    public void calculateInitialRect_square_isSquare() {
        areaView.calculateInitialRect(1.0f);
        RectF rect = areaView.getCropRect();
        assertEquals(rect.width(), rect.height(), 1.0f);
    }

    @Test
    public void calculateInitialRect_square_isCentered() {
        areaView.calculateInitialRect(1.0f);
        RectF rect = areaView.getCropRect();
        float cx = areaView.getWidth() / 2f;
        float cy = areaView.getHeight() / 2f;
        assertEquals(cx, rect.centerX(), 2.0f);
        assertEquals(cy, rect.centerY(), 2.0f);
    }

    @Test
    public void calculateInitialRect_wideAspect_fillsWidth() {
        areaView.calculateInitialRect(16f / 9f);
        RectF rect = areaView.getCropRect();
        assertTrue(rect.width() > rect.height());
    }

    @Test
    public void calculateInitialRect_tallAspect_fillsHeight() {
        areaView.calculateInitialRect(9f / 16f);
        RectF rect = areaView.getCropRect();
        assertTrue(rect.height() > rect.width());
    }
}
