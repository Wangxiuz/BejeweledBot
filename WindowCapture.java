
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;

public class WindowCapture {


    public static void main(String[] args) {

        // 截取窗口内容区域
        BufferedImage image = robot.createScreenCapture(clientRect);

        // 保存截图到文件
        try {
            ImageIO.write(image, "PNG", new File("window_capture.png"));
            System.out.println("截图已保存到 window_capture.png");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 新增: 宝石识别
        identifyGems(image);
    }

    private static void identifyGems(BufferedImage screenshot) {
        // 宝石模板库
        Map<String, Color> gemTemplates = new HashMap<>();
        gemTemplates.put("red", new Color(255, 0, 0));
        gemTemplates.put("blue", new Color(0, 0, 255));
        gemTemplates.put("green", new Color(0, 255, 0));
        gemTemplates.put("yellow", new Color(255, 255, 0));
        gemTemplates.put("purple", new Color(128, 0, 128));
        gemTemplates.put("orange", new Color(255, 165, 0));
        gemTemplates.put("white", new Color(255, 255, 255));

        // 棋盘状态二维数组
        String[][] boardState = new String[8][8];

        // 分割截图并识别宝石
        int gemSize = screenshot.getWidth() / 8;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                // 获取单个宝石区域
                BufferedImage gemImage = screenshot.getSubimage(x * gemSize, y * gemSize, gemSize, gemSize);
                // 识别宝石颜色
                Color gemColor = getDominantColor(gemImage);
                // 匹配模板库
                String gemType = matchTemplate(gemColor, gemTemplates);
                // 更新棋盘状态
                boardState[y][x] = gemType;
            }
        }

        // 打印棋盘状态
        for (String[] row : boardState) {
            for (String gem : row) {
                System.out.print(gem + " ");
            }
            System.out.println();
        }
    }

    private static Color getDominantColor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        // 定义中心区域的大小（例如，取中心 1/4 区域）
        int centerWidth = width / 4;
        int centerHeight = height / 4;

        int redSum = 0, greenSum = 0, blueSum = 0;
        int count = 0;

        // 遍历中心区域的像素
        for (int y = centerY - centerHeight / 2; y < centerY + centerHeight / 2; y++) {
            for (int x = centerX - centerWidth / 2; x < centerX + centerWidth / 2; x++) {
                Color color = new Color(image.getRGB(x, y));
                redSum += color.getRed();
                greenSum += color.getGreen();
                blueSum += color.getBlue();
                count++;
            }
        }

        // 计算中心区域的平均颜色
        return new Color(redSum / count, greenSum / count, blueSum / count);
    }

    private static String matchTemplate(Color color, Map<String, Color> templates) {
        double minDistance = Double.MAX_VALUE;
        String bestMatch = null;
        for (Map.Entry<String, Color> entry : templates.entrySet()) {
            double distance = colorDistance(color, entry.getValue());
            if (distance < minDistance) {
                minDistance = distance;
                bestMatch = entry.getKey();
            }
        }
        // 打印调试信息，帮助分析识别结果
        System.out.println("识别颜色: " + color + ", 匹配结果: " + bestMatch + ", 距离: " + minDistance);
        return bestMatch;
    }

    private static double colorDistance(Color c1, Color c2) {
        int r1 = c1.getRed();
        int g1 = c1.getGreen();
        int b1 = c1.getBlue();
        int r2 = c2.getRed();
        int g2 = c2.getGreen();
        int b2 = c2.getBlue();
        return Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));
    }
}