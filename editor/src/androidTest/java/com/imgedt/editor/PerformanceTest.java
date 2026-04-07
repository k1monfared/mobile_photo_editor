package com.imgedt.editor;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.imgedt.editor.crop.CropAreaView;
import com.imgedt.editor.crop.CropView;
import com.imgedt.editor.paint.PaintView;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PerformanceTest {

    @Test
    public void paintView_undoStack_memoryBound() {
        PaintView paintView = new PaintView(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        paintView.init(500, 500);
        for (int i = 0; i < 20; i++) {
            simulateStroke(paintView, 10 + i * 5, 10 + i * 5);
        }
        int undoCount = 0;
        while (paintView.canUndo()) {
            paintView.undo();
            undoCount++;
        }
        assertTrue(undoCount == 20);
        paintView.recycle();
    }

    @Test
    public void paintView_recycle_freesAll() {
        PaintView paintView = new PaintView(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        paintView.init(500, 500);
        for (int i = 0; i < 5; i++) {
            simulateStroke(paintView, 50, 50);
        }
        assertTrue(paintView.canUndo());
        paintView.recycle();
        assertTrue(!paintView.canUndo());
    }

    @Test
    public void cropBitmap_under500ms() {
        final CropView[] cv = new CropView[1];
        final CropAreaView[] av = new CropAreaView[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            cv[0] = new CropView(InstrumentationRegistry.getInstrumentation().getTargetContext());
            av[0] = new CropAreaView(InstrumentationRegistry.getInstrumentation().getTargetContext());
            int w = android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY);
            int h = android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY);
            av[0].measure(w, h);
            av[0].layout(0, 0, 1080, 1920);
            av[0].calculateInitialRect(0);
            cv[0].setAreaView(av[0]);
        });
        Bitmap img = Bitmap.createBitmap(2000, 1500, Bitmap.Config.ARGB_8888);
        img.eraseColor(Color.BLUE);
        cv[0].setBitmap(img);
        cv[0].resetToFit(av[0].getCropRect());
        long start = System.nanoTime();
        Bitmap result = cv[0].cropBitmap(1280);
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        assertNotNull(result);
        assertTrue("cropBitmap took " + elapsed + "ms", elapsed < 500);
        result.recycle();
        img.recycle();
    }

    @Test
    public void cropView_cropBitmap_noLeak() {
        final CropView[] cv = new CropView[1];
        final CropAreaView[] av = new CropAreaView[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            cv[0] = new CropView(InstrumentationRegistry.getInstrumentation().getTargetContext());
            av[0] = new CropAreaView(InstrumentationRegistry.getInstrumentation().getTargetContext());
            int w = android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY);
            int h = android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY);
            av[0].measure(w, h);
            av[0].layout(0, 0, 1080, 1920);
            av[0].calculateInitialRect(0);
            cv[0].setAreaView(av[0]);
        });
        Bitmap img = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888);
        img.eraseColor(Color.GREEN);
        cv[0].setBitmap(img);
        cv[0].resetToFit(av[0].getCropRect());
        for (int i = 0; i < 10; i++) {
            Bitmap result = cv[0].cropBitmap(1280);
            assertNotNull(result);
            result.recycle();
        }
        img.recycle();
    }

    private void simulateStroke(PaintView view, float x, float y) {
        long now = System.currentTimeMillis();
        android.view.MotionEvent down = android.view.MotionEvent.obtain(
                now, now, android.view.MotionEvent.ACTION_DOWN, x, y, 0);
        view.onTouchEvent(down);
        down.recycle();
        android.view.MotionEvent up = android.view.MotionEvent.obtain(
                now, now + 16, android.view.MotionEvent.ACTION_UP, x, y, 0);
        view.onTouchEvent(up);
        up.recycle();
    }
}
