package org.example;

import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser.WINDOWINFO;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class WindowCapture {
    // 新增鼠标移动延迟时间
    private static final int MOUSE_MOVE_DELAY = 30; // 默认延迟时间为 30 毫秒

    static {
        // 修改: 明确指定 OpenCV 动态库路径
        String opencvPath = "E:\\Program Files\\Open CV\\opencv\\build\\java\\x64";
        System.load(opencvPath + "\\opencv_java4110.dll");
    }
    static String[][] boardState = new String[8][8];
    static int Start_x = 0;
    static int Start_y = 0;

    public static void main(String[] args) throws AWTException {
        System.setProperty("sun.java2d.dpiaware", "true");
        System.setProperty("glass.winregistrydpienabled", "true");
        System.setProperty("sun.java2d.uiScale", "1.0");

        // 添加键盘监听器
        JFrame frame = new JFrame();
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F12) {
                    System.out.println("检测到 F12 键，程序退出...");
                    System.exit(0);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyTyped(KeyEvent e) {}
        });
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.requestFocus();

        String windowTitle = "Bejeweled 3";

        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, windowTitle);
        if (hwnd == null) {
            System.out.println("未找到窗口: " + windowTitle);
            return;
        }

        WINDOWINFO windowInfo = new WINDOWINFO();
        try {
            Field sizeField = WINDOWINFO.class.getDeclaredField("cbSize");
            sizeField.setAccessible(true);
            int cbSize = sizeField.getInt(windowInfo);
            windowInfo.cbSize = cbSize;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (!User32.INSTANCE.GetWindowInfo(hwnd, windowInfo)) {
            System.out.println("无法获取窗口信息");
            return;
        }

        Rectangle clientRect = new Rectangle(
                windowInfo.rcClient.left + 465,
                windowInfo.rcClient.top + 115,
                900,
                900
        );
        Start_x = windowInfo.rcClient.left + 465;
        Start_y = windowInfo.rcClient.top + 115;
        GemSwapAutomator automator = new GemSwapAutomator();
        while (true){
            Robot robot = new Robot();
            BufferedImage screenshot = robot.createScreenCapture(clientRect);

/*            try {
                ImageIO.write(screenshot, "PNG", new File("window_capture.png"));
                //System.out.println("截图已保存到 window_capture.png");
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            identifyGems(screenshot);
            automator.autoSwap(boardState, Start_x, Start_y);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }


    public static void identifyGems(BufferedImage screenshot) {
        // 记录函数开始时间
        //long startTime = System.currentTimeMillis();
        int gemSize = screenshot.getWidth() / 8;

        File outputFolder = new File("gem_images");
        if (!outputFolder.exists()) {
            outputFolder.mkdir();
        }

        // 创建固定大小的线程池
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<Future<String>> futures = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int finalX = x;
                int finalY = y;
                Future<String> future = executor.submit(() -> {
                    BufferedImage gemImage = screenshot.getSubimage(finalX * gemSize, finalY * gemSize, gemSize, gemSize);
                    String gemType = matchGemTemplate(gemImage);

/*                    try {
                        File outputFile = new File(outputFolder, "gem_" + finalY + "_" + finalX + ".png");
                        ImageIO.write(gemImage, "PNG", outputFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/

                    return gemType;
                });
                futures.add(future);
            }
        }

        // 记录函数结束时间并打印执行时间
/*        long endTime = System.currentTimeMillis();
        System.out.println("identifyGems 函数执行时间: " + (endTime - startTime) + " 毫秒");*/

        // 收集结果并填充 boardState
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                try {
                    boardState[y][x] = futures.get(y * 8 + x).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 打印结果
/*        for (String[] row : boardState) {
            System.out.println(Arrays.toString(row));
        }*/

        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        // 手动释放 Mat 对象
        Mat gemMat = bufferedImageToMat(screenshot);
        gemMat.release();

        File folder = new File("gem_templates");
        for (File file : folder.listFiles()) {
            if (!file.getName().endsWith(".png")) {
                continue;
            }
            Mat template = Imgcodecs.imread(file.getAbsolutePath());
            if (!template.empty()) {
                template.release();
            }
        }
    }

    private static String matchGemTemplate(BufferedImage gemImage) {
        Mat gemMat = bufferedImageToMat(gemImage);
        String bestMatch = null;
        double bestScore = Double.MIN_VALUE;

        File folder = new File("gem_templates");
        for (File file : folder.listFiles()) {
            if (!file.getName().endsWith(".png")) {
                continue;
            }

            String label = file.getName().replace(".png", "");
            Mat template = Imgcodecs.imread(file.getAbsolutePath());

            if (template.empty()) {
                continue;
            }

            Mat result = new Mat();
            Imgproc.matchTemplate(gemMat, template, result, Imgproc.TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            double score = mmr.maxVal;

            if (score > bestScore) {
                bestScore = score;
                bestMatch = label;
            }

            // 手动释放 template 和 result 对象
            template.release();
            result.release();
        }

        // 手动释放 gemMat 对象
        gemMat.release();

        return bestMatch != null ? bestMatch : "unknown";
    }

    private static Mat bufferedImageToMat(BufferedImage bi) {
        BufferedImage converted = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        converted.getGraphics().drawImage(bi, 0, 0, null);
        byte[] pixels = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }
}