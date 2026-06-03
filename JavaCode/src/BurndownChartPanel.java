import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple Swing chart that visualizes project burndown from the local task statuses.
 * Complete tasks are burned down; incomplete, in-progress, and blocked tasks remain open.
 */
public final class BurndownChartPanel extends JPanel {

    private static final int PADDING_LEFT = 70;
    private static final int PADDING_RIGHT = 35;
    private static final int PADDING_TOP = 55;
    private static final int PADDING_BOTTOM = 70;
    private static final int POINT_RADIUS = 5;

    private final String projectName;
    private final List<String> labels = new ArrayList<>();
    private final List<Integer> actualRemaining = new ArrayList<>();
    private final int totalTasks;
    private final int completeTasks;

    public BurndownChartPanel(Project project) {
        this.projectName = project.getName();
        this.totalTasks = countTasks(project);
        this.completeTasks = countCompleteTasks(project);
        buildSeries(project);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(720, 460));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawChart(g2);
        } finally {
            g2.dispose();
        }
    }

    private void drawChart(Graphics2D g2) {
        int width = getWidth();
        int height = getHeight();
        int chartX = PADDING_LEFT;
        int chartY = PADDING_TOP;
        int chartWidth = Math.max(1, width - PADDING_LEFT - PADDING_RIGHT);
        int chartHeight = Math.max(1, height - PADDING_TOP - PADDING_BOTTOM);
        int maxY = Math.max(1, totalTasks);

        drawTitle(g2, width);
        drawAxes(g2, chartX, chartY, chartWidth, chartHeight, maxY);
        drawIdealLine(g2, chartX, chartY, chartWidth, chartHeight, maxY);
        drawActualLine(g2, chartX, chartY, chartWidth, chartHeight, maxY);
        drawLegend(g2, chartX, chartY + chartHeight + 48);
    }

    private void drawTitle(Graphics2D g2, int width) {
        g2.setColor(Color.DARK_GRAY);
        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
        String title = "Burndown Chart - " + projectName;
        drawCenteredString(g2, title, width / 2, 24);

        g2.setFont(getFont().deriveFont(12f));
        String summary = "Total: " + totalTasks
                + "   Complete: " + completeTasks
                + "   Remaining: " + (totalTasks - completeTasks);
        drawCenteredString(g2, summary, width / 2, 43);
    }

    private void drawAxes(Graphics2D g2, int x, int y, int width, int height, int maxY) {
        int bottom = y + height;
        int right = x + width;

        g2.setColor(Color.GRAY);
        g2.draw(new Line2D.Double(x, y, x, bottom));
        g2.draw(new Line2D.Double(x, bottom, right, bottom));

        g2.setFont(getFont().deriveFont(11f));
        FontMetrics metrics = g2.getFontMetrics();

        int tickCount = Math.min(5, maxY);
        for (int tick = 0; tick <= tickCount; tick++) {
            int value = (int) Math.round(maxY * (tick / (double) tickCount));
            int tickY = yFor(value, y, height, maxY);
            g2.setColor(new Color(230, 230, 230));
            g2.draw(new Line2D.Double(x, tickY, right, tickY));
            g2.setColor(Color.DARK_GRAY);
            String label = String.valueOf(value);
            g2.drawString(label, x - metrics.stringWidth(label) - 8, tickY + metrics.getAscent() / 2 - 2);
        }

        int count = labels.size();
        for (int i = 0; i < count; i++) {
            int labelX = xFor(i, x, width, count);
            String label = labels.get(i);
            int labelWidth = metrics.stringWidth(label);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(label, labelX - labelWidth / 2, bottom + 20);
        }

        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        drawCenteredString(g2, "Project progress by story", x + width / 2, bottom + 45);
        drawRotatedString(g2, "Tasks remaining", x - 48, y + height / 2);
    }

    private void drawIdealLine(Graphics2D g2, int x, int y, int width, int height, int maxY) {
        int count = labels.size();
        if (count < 2) {
            return;
        }

        g2.setColor(new Color(150, 150, 150));
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 4f, new float[]{8f, 6f}, 0f));
        g2.draw(new Line2D.Double(
                xFor(0, x, width, count),
                yFor(totalTasks, y, height, maxY),
                xFor(count - 1, x, width, count),
                yFor(0, y, height, maxY)
        ));
        g2.setStroke(oldStroke);
    }

    private void drawActualLine(Graphics2D g2, int x, int y, int width, int height, int maxY) {
        int count = actualRemaining.size();
        if (count == 0) {
            return;
        }

        g2.setColor(new Color(0, 102, 204));
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(3f));

        for (int i = 1; i < count; i++) {
            g2.draw(new Line2D.Double(
                    xFor(i - 1, x, width, count),
                    yFor(actualRemaining.get(i - 1), y, height, maxY),
                    xFor(i, x, width, count),
                    yFor(actualRemaining.get(i), y, height, maxY)
            ));
        }

        for (int i = 0; i < count; i++) {
            int pointX = xFor(i, x, width, count);
            int pointY = yFor(actualRemaining.get(i), y, height, maxY);
            g2.fill(new Ellipse2D.Double(
                    pointX - POINT_RADIUS,
                    pointY - POINT_RADIUS,
                    POINT_RADIUS * 2,
                    POINT_RADIUS * 2
            ));
            g2.drawString(String.valueOf(actualRemaining.get(i)), pointX + 7, pointY - 7);
        }

        g2.setStroke(oldStroke);
    }

    private void drawLegend(Graphics2D g2, int x, int y) {
        g2.setFont(getFont().deriveFont(11f));

        g2.setColor(new Color(0, 102, 204));
        g2.setStroke(new BasicStroke(3f));
        g2.draw(new Line2D.Double(x, y, x + 28, y));
        g2.fill(new Ellipse2D.Double(x + 12, y - POINT_RADIUS, POINT_RADIUS * 2, POINT_RADIUS * 2));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Actual remaining", x + 36, y + 4);

        int idealX = x + 170;
        g2.setColor(new Color(150, 150, 150));
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 4f, new float[]{8f, 6f}, 0f));
        g2.draw(new Line2D.Double(idealX, y, idealX + 28, y));
        g2.setStroke(oldStroke);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Ideal burndown", idealX + 36, y + 4);
    }

    private void buildSeries(Project project) {
        labels.add("Start");
        actualRemaining.add(totalTasks);

        int completedSoFar = 0;
        for (Story story : project.getStories()) {
            completedSoFar += countCompleteTasks(story);
            labels.add("S" + story.getId());
            actualRemaining.add(totalTasks - completedSoFar);
        }

        if (labels.size() == 1) {
            labels.add("End");
            actualRemaining.add(totalTasks - completeTasks);
        }
    }

    private static int countTasks(Project project) {
        int count = 0;
        for (Story story : project.getStories()) {
            count += story.getTasks().size();
        }
        return count;
    }

    private static int countCompleteTasks(Project project) {
        int count = 0;
        for (Story story : project.getStories()) {
            count += countCompleteTasks(story);
        }
        return count;
    }

    private static int countCompleteTasks(Story story) {
        int count = 0;
        for (Task task : story.getTasks()) {
            if (task.isComplete()) {
                count++;
            }
        }
        return count;
    }

    private static int xFor(int index, int x, int width, int count) {
        if (count <= 1) {
            return x;
        }
        return x + (int) Math.round(index * (width / (double) (count - 1)));
    }

    private static int yFor(int value, int y, int height, int maxY) {
        return y + height - (int) Math.round(value * (height / (double) maxY));
    }

    private static void drawCenteredString(Graphics2D g2, String text, int centerX, int baselineY) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
    }

    private static void drawRotatedString(Graphics2D g2, String text, int x, int y) {
        Graphics2D rotated = (Graphics2D) g2.create();
        try {
            rotated.rotate(-Math.PI / 2.0, x, y);
            drawCenteredString(rotated, text, x, y);
        } finally {
            rotated.dispose();
        }
    }
}