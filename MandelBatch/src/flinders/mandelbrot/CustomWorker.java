/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flinders.mandelbrot;

import flinders.mandelbrot.MandelProcessor.ComputeMode;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.JList;
import javax.swing.SwingWorker;

/**
 *
 * @author Ebad Ali
 */
public class CustomWorker extends SwingWorker<int[][], Integer> {

    ICompleteCallback callback;
//    JList<MandelSetting> guiSettingList; MandelProcessor mandelProcessor; ComputeMode computeMode;
    int x, y, width, height, id;
    MandelSetting s;
    BufferedImage image;

    CustomWorker(ICompleteCallback iCompleteCallback, int X, int Y, int WIDTH, int HEIGHT, MandelSetting SETTINGS,
            int ID) {
        this.callback = iCompleteCallback;

        this.x = X;
        this.y = Y;
        this.width = WIDTH;
        this.height = HEIGHT;
        this.s = SETTINGS;

        this.id = ID;
    }


    @Override
    protected void done() {
        super.done();
        int[][] arry = null;
        try {
            arry = get();
            
        } catch (InterruptedException ex) {
            
        } catch (ExecutionException ex) {
            
        } catch (CancellationException ex) {
            // Do your task after cancellation
           
        }finally{
            this.callback.finish(arry);
        }

    }

    @Override
    protected int[][] doInBackground() throws Exception {

        int[][] data = new int[width][height];
        // generate array of C values that are to be processed

        for (int ly = y; ly < height; ly++) {
            for (int lx = x; lx < width; lx++) {

//                System.out.println("x,y:  "+ lx +" , "+ly);
                double C_real = s.getMinReal() + (lx * (s.getMaxReal() - s.getMinReal())) / (s.getWidth() - 1);
                double C_imaginary = s.getMinImaginary() + ((ly) * (s.getMaxImaginary() - s.getMinImaginary())) / (s.getHeight() - 1);
                ComplexNumber C = new ComplexNumber(C_real, C_imaginary);
                ComplexNumber Z = new ComplexNumber(0, 0);
                int iterations = 0;
                while ((Z.abs() <= 2) && (iterations < s.getMaxIterations())) {
                    Z.multiply(Z); // Z = Z*Z
                    Z.add(C); // Z = Z+C
                    iterations++;
                }
                if (iterations != s.getMaxIterations()) { // convert to colour image (change this if you don't like the colours)
                    double c = (double) (iterations - log2(log2(Z.abs()))) / s.getMaxIterations();
                    int rgb = (int) (Math.sin(c * 8 * Math.PI) * 127) + 128 << 16;
                    rgb |= (int) (Math.sin(c * 8 * Math.PI + 5) * 127) + 128 << 8;
                    rgb |= (int) (Math.sin(c * 8 * Math.PI + 10) * 127) + 128;
//                    image.setRGB(lx, ly, rgb);
                    data[lx][ly] = rgb;
//                    publish(lx,ly,rgb);
//                    System.out.println(rgb);
                }
            }
        }


        return data;
    }

    double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
