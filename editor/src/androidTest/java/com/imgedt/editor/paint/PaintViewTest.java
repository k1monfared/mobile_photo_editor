package com.imgedt.editor.paint;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.MotionEvent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented tests for PaintView double-buffer, undo stack, and brush logic.
 */
@RunWith(AndroidJUnit4.class)
public class PaintViewTest {

    private PaintView paintView;

    @Before
    public void setUp() {
        paintView = new PaintView(InstrumentationRegistry.getInstrumentation().getTargetContext());
        paintView.init(100, 100);
    }

    @After
    public void tearDown() {
        paintView.recycle();
    }

    @Test
    public void init_createsBitmaps() {
        Bitmap result = paintView.getResult();
        assertNotNull(result);
        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
        result.recycle();
    }

    @Test
    public void getResult_returnsCanvasCopy() {
        Bitmap result1 = paintView.getResult();
        Bitmap result2 = paintView.getResult();
        assertNotSame(result1, result2);
        result1.recycle();
        result2.recycle();
    }

    @Test
    public void getResult_initiallyTransparent() {
        Bitmap result = paintView.getResult();
        assertEquals(0, result.getPixel(50, 50));
        result.recycle();
    }

    @Test
    public void canUndo_empty_returnsFalse() {
        assertFalse(paintView.canUndo());
    }

    @Test
    public void undo_restoresPreviousState() {
        // Get original state
        Bitmap before = paintView.getResult();
        int pixelBefore = before.getPixel(50, 50);
        before.recycle();

        // Draw a large visible stroke
        paintView.setBrushColor(Color.RED);
        paintView.setBrushAlpha(1.0f);
        paintView.setBrushSize(40);
        simulateStroke(50, 50, 50, 50);

        assertTrue("Should have undo after stroke", paintView.canUndo());

        // Undo
        paintView.undo();

        // Check that we returned to original state
        Bitmap afterUndo = paintView.getResult();
        int pixelAfterUndo = afterUndo.getPixel(50, 50);
        afterUndo.recycle();

        assertEquals("Pixel should return to original after undo", pixelBefore, pixelAfterUndo);
    }

    @Test
    public void undo_maxStack_evictsOldest() {
        // Perform 21 strokes (max undo is 20)
        for (int i = 0; i < 21; i++) {
            simulateStroke(10 + i, 10 + i, 10 + i, 10 + i);
        }

        // Should be able to undo 20 times
        int undoCount = 0;
        while (paintView.canUndo()) {
            paintView.undo();
            undoCount++;
        }
        assertEquals(20, undoCount);
    }

    @Test
    public void brushColor_affectsOutput() {
        paintView.setBrushColor(Color.RED);
        paintView.setBrushAlpha(1.0f);
        paintView.setBrushSize(40);
        simulateStroke(50, 50, 50, 50);

        Bitmap result = paintView.getResult();
        int pixel = result.getPixel(50, 50);
        assertTrue("Should have red component", Color.red(pixel) > 0);
        result.recycle();
    }

    @Test
    public void eraserMode_removesPixels() {
        // First draw something visible
        paintView.setBrushColor(Color.RED);
        paintView.setBrushAlpha(1.0f);
        paintView.setBrushSize(40);
        paintView.setEraser(false);
        simulateStroke(50, 50, 50, 50);

        Bitmap beforeErase = paintView.getResult();
        int alphaBefore = Color.alpha(beforeErase.getPixel(50, 50));
        beforeErase.recycle();
        assertTrue("Should have visible content before erase", alphaBefore > 0);

        // Now erase
        paintView.setEraser(true);
        paintView.setBrushSize(40);
        paintView.setBrushAlpha(1.0f);
        simulateStroke(50, 50, 50, 50);

        Bitmap afterErase = paintView.getResult();
        int alphaAfter = Color.alpha(afterErase.getPixel(50, 50));
        afterErase.recycle();

        assertTrue("Eraser should reduce alpha", alphaAfter < alphaBefore);
    }

    @Test
    public void touchBeforeInit_noCrash() {
        PaintView uninitView = new PaintView(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50, 50, 0);
        boolean handled = uninitView.onTouchEvent(event);
        assertFalse(handled);
        event.recycle();
    }

    @Test
    public void recycle_clearsState() {
        simulateStroke(50, 50, 60, 60);
        assertTrue(paintView.canUndo());
        paintView.recycle();
        assertFalse(paintView.canUndo());
    }

    private void simulateStroke(float x0, float y0, float x1, float y1) {
        long now = System.currentTimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x0, y0, 0);
        paintView.onTouchEvent(down);
        down.recycle();

        if (x0 != x1 || y0 != y1) {
            MotionEvent move = MotionEvent.obtain(now, now + 16, MotionEvent.ACTION_MOVE, x1, y1, 0);
            paintView.onTouchEvent(move);
            move.recycle();
        }

        MotionEvent up = MotionEvent.obtain(now, now + 32, MotionEvent.ACTION_UP, x1, y1, 0);
        paintView.onTouchEvent(up);
        up.recycle();
    }
}
