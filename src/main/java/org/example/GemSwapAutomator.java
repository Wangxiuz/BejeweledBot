package org.example;

import java.awt.*;
import java.awt.event.InputEvent;

import org.opencv.core.*;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;


public class GemSwapAutomator {
    private static final int BOARD_SIZE = 8;
    private final Robot robot;
    public GemSwapAutomator() {
        try {
            this.robot = new Robot();
            this.robot.setAutoWaitForIdle(false); // 更快的鼠标操作
        } catch (AWTException e) {
            throw new RuntimeException("无法创建 Robot", e);
        }
    }

    public void autoSwap(String[][] board, int startX, int startY) {
        int gemSize = 112; // 根据截图分辨率设置

        // 遍历棋盘，寻找最优交换
        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                // 如果发现宝石类型为 w，则与上方的宝石交换
                if (y > 0 && board[y][x].equals("w")) {
                    clickSwap(x, y, x, y - 1, startX, startY, gemSize);
                    return;
                }

                // 优先检查右侧交换
                if (x < BOARD_SIZE - 1 && isPotentialMatch(board, x, y, x + 1, y)) {
                    clickSwap(x, y, x + 1, y, startX, startY, gemSize);
                    return;
                }

                // 然后检查下方交换
                if (y < BOARD_SIZE - 1 && isPotentialMatch(board, x, y, x, y + 1)) {
                    clickSwap(x, y, x, y + 1, startX, startY, gemSize);
                    return;
                }
            }
        }
        System.out.println("没有可用交换！");
    }

    /**
     * 检查交换是否会形成匹配
     */
    private boolean isPotentialMatch(String[][] board, int x1, int y1, int x2, int y2) {
        // 相同宝石不需要交换
        if (board[y1][x1].equals(board[y2][x2])) {
            return false;
        }
        // 临时交换
        swap(board, x1, y1, x2, y2);
        boolean hasMatch = checkForMatches(board, x1, y1) || checkForMatches(board, x2, y2);
        // 交换回来
        swap(board, x1, y1, x2, y2);
        return hasMatch;
    }

    /**
     * 检查特定位置周围是否有匹配
     */
    private boolean checkForMatches(String[][] board, int x, int y) {
        String gem = board[y][x];

        // 检查横向匹配
        int left = x;
        while (left > 0 && board[y][left - 1].equals(gem)) {
            left--;
        }
        int right = x;
        while (right < BOARD_SIZE - 1 && board[y][right + 1].equals(gem)) {
            right++;
        }

        if (right - left >= 2) {
            return true;
        }
        // 检查纵向匹配
        int top = y;
        while (top > 0 && board[top - 1][x].equals(gem)) {
            top--;
        }

        int bottom = y;
        while (bottom < BOARD_SIZE - 1 && board[bottom + 1][x].equals(gem)) {
            bottom++;
        }

        return bottom - top >= 2;
    }

    private void swap(String[][] board, int x1, int y1, int x2, int y2) {
        String temp = board[y1][x1];
        board[y1][x1] = board[y2][x2];
        board[y2][x2] = temp;
    }

    private void clickSwap(int x1, int y1, int x2, int y2, int offsetX, int offsetY, int gemSize) {
        try {
            int clickX1 = offsetX + x1 * gemSize + gemSize / 2;
            int clickY1 = offsetY + y1 * gemSize + gemSize / 2;
            int clickX2 = offsetX + x2 * gemSize + gemSize / 2;
            int clickY2 = offsetY + y2 * gemSize + gemSize / 2;

            //System.out.println("正在交换坐标: (" + (y1+1) + "," + (x1+1) + ") 和 (" + (y2+1)+ "," + (x2+1) + ")");

            robot.mouseMove(clickX1, clickY1);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseMove(clickX2, clickY2);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ===== 新增 ORB 匹配逻辑 =====
    public static boolean matchORB(Mat roi, Mat templateImage) {
        ORB orb = ORB.create();

        MatOfKeyPoint kp1 = new MatOfKeyPoint();
        Mat desc1 = new Mat();
        orb.detectAndCompute(roi, new Mat(), kp1, desc1);

        MatOfKeyPoint kp2 = new MatOfKeyPoint();
        Mat desc2 = new Mat();
        orb.detectAndCompute(templateImage, new Mat(), kp2, desc2);

        if (desc1.empty() || desc2.empty()) {
            return false;
        }

        BFMatcher matcher = BFMatcher.create(Core.NORM_HAMMING, true);
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(desc1, desc2, matches);

        long goodMatches = matches.toList().stream()
                .filter(m -> m.distance < 60)
                .count();

        return goodMatches >= 10;
    }

    // ===== 亮度标准化处理 =====
    public static Mat preprocessBrightness(Mat input) {
        Mat gray = new Mat();
        Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);
        return gray;
    }
}
