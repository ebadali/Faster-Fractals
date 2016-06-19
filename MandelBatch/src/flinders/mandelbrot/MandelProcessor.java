package flinders.mandelbrot;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

/**
 * Mandelbrot renderer.
 */
public class MandelProcessor {

    public enum ComputeMode {

        JAVA_SINGLE, JAVA_MULTI
    };
    private BufferedImage image;
    private ILoadingCallback callback;
    int counter = 1;
    int processors;
    CustomWorker worker;

    public void compute(MandelSetting s, ComputeMode m, ILoadingCallback Callback) {

        callback = Callback;
        switch (m) {
            case JAVA_SINGLE:
                processors = 1;
                compute_java_single(s);
                break;
            case JAVA_MULTI:
                processors = Runtime.getRuntime().availableProcessors();
                compute_java_multi(s);
                break;
        }
    }

    public void Cancle() {
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
            this.callback.FinishLoading();
        }

    }

    private void compute_java_single(MandelSetting s) {

        System.out.println("Rendering Mandelbrot set with: R in [" + s.getMinReal() + "," + s.getMaxReal()
                + "], I in [" + s.getMinImaginary() + "," + s.getMaxImaginary()
                + "], maximum iterations: " + s.getMaxIterations() + " ...");
        image = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_RGB);


        final int ax = 0, ay = 0;
        final int aW = s.getWidth(),
                aH = s.getHeight();
        final JDialog dlgProgress = MandelBatchGUI.GetDialog();



        worker = new CustomWorker(new ICompleteCallback() {

            @Override
            public void finish(int[][] arry) {
                if (arry != null) {
                    for (int j = ay; j < aH; j++) {
                        for (int k = ax; k < aW; k++) {
                            image.setRGB(k, j, arry[k][j]);
                        }
                    }
                    System.out.println("Render complete ");
                    SwingUtilities.invokeLater(
                            new Runnable() {

                                @Override
                                public void run() {

                                    dlgProgress.dispose();
                                }
                            });
                }
                callback.FinishLoading();
            }
        }, ax, ay, aW, aH, s, 0);

        SwingUtilities.invokeLater(
                new Runnable() {

                    @Override
                    public void run() {

                        dlgProgress.addWindowListener(new WindowAdapter() {

                            @Override
                            public void windowClosed(WindowEvent e) {

                                super.windowClosed(e);
                                
                                Cancle();

                            }
                        });

                        dlgProgress.setVisible(true);
                    }
                });
        worker.execute();


    }

    private void compute_java_multi(MandelSetting s) {
        counter = 1;
        System.out.println("Rendering Mandelbrot set with: R in [" + s.getMinReal() + "," + s.getMaxReal()
                + "], I in [" + s.getMinImaginary() + "," + s.getMaxImaginary()
                + "], maximum iterations: " + s.getMaxIterations() + " ...");

        /*
         * 1. get No Of Processors which is = no of threads.
         * 2. If ( biggest of w and h ) / p == width of each
         *     * will do the edge case later.
         * 3. start threads.
         */
        int w_ = (s.getWidth() > s.getHeight() ? s.getWidth() / processors : s.getWidth()),
                h_ = (s.getWidth() > s.getHeight() ? s.getHeight() : s.getHeight() / processors);  // Width and height of each block.

        image = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_RGB);
//        System.out.println("width and height " + w_ + " , " + h_);
        final JDialog dlgProgress = MandelBatchGUI.GetDialog();
        SwingUtilities.invokeLater(
                new Runnable() {

                    @Override
                    public void run() {

                        dlgProgress.addWindowListener(new WindowAdapter() {

                            @Override
                            public void windowClosed(WindowEvent e) {

                                super.windowClosed(e);
                                Cancle();

                            }
                        });

                        dlgProgress.setVisible(true);
                    }
                });

        for (int i = 0; i < processors; i++) {

            final int ax = (s.getWidth() > s.getHeight()) ? w_ * i : 0, ay = (s.getWidth() > s.getHeight()) ? 0 : h_ * i;
            final int aW = ax + w_,
                    aH = ay + h_;



            // This is all ok.
            worker = new CustomWorker(new ICompleteCallback() {

                @Override
                public void finish(int[][] arry) {
                    if (arry != null) {
                        for (int j = ay; j < aH; j++) {
                            for (int k = ax; k < aW; k++) {
                                image.setRGB(k, j, arry[k][j]);
                            }
                        }
                        if (counter++ >= processors) {
                            System.out.println("Render complete ");
                            SwingUtilities.invokeLater(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            dlgProgress.dispose();
                                        }
                                    });
                            callback.FinishLoading();
                        }
                    } else {
                        SwingUtilities.invokeLater(
                                new Runnable() {

                                    @Override
                                    public void run() {

                                        dlgProgress.dispose();
                                    }
                                });

                        callback.FinishLoading();
                    }
                }
            }, ax, ay, aW, aH, s, i);

            worker.execute();
        }
    }

 
    // utlity code ---

    BufferedImage getLastImage() {
        return image;
    }

    double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
