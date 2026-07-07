package com.example.gatepass.scanner;

import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;

public class BlurDetector {

    public static double calculateBlurScore(ImageProxy image) {
        ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();
        
        int w = image.getWidth();
        int h = image.getHeight();
        long sumSq = 0; 
        int count = 0;
        
        for (int y = 10; y < h - 10; y += 15) {
            for (int x = 10; x < w - 10; x += 15) {
                int i = y * rowStride + x * pixelStride;
                int i_minus_1 = y * rowStride + (x - 1) * pixelStride;
                int i_plus_1 = y * rowStride + (x + 1) * pixelStride;
                int i_minus_w = (y - 1) * rowStride + x * pixelStride;
                int i_plus_w = (y + 1) * rowStride + x * pixelStride;
                
                int val = buffer.get(i) & 0xFF;
                int lap = val * 4 
                        - (buffer.get(i_minus_1) & 0xFF) 
                        - (buffer.get(i_plus_1) & 0xFF) 
                        - (buffer.get(i_minus_w) & 0xFF) 
                        - (buffer.get(i_plus_w) & 0xFF);
                sumSq += (long) lap * lap; 
                count++;
            }
        }
        return (double) sumSq / count;
    }
}