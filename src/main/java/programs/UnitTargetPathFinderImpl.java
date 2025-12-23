package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.Edge;
import com.battle.heroes.army.programs.UnitTargetPathFinder;

import java.util.*;

public class UnitTargetPathFinderImpl implements UnitTargetPathFinder {

    private static final int WIDTH = 27;
    private static final int HEIGHT = 21;

    // 8 направлений движения с весами: {dx, dy, weight}
    // Прямые ходы: вес 1.0, диагональные: вес √2
    private static final double STRAIGHT_COST = 1.0;
    private static final double DIAGONAL_COST = Math.sqrt(2); // ≈ 1.414

    // Направления: {dx, dy, isDiagonal}
    private static final int[][] DIRECTIONS = {
            {-1, 0},  // вверх
            {1, 0},   // вниз
            {0, -1},  // влево
            {0, 1},   // вправо
            {-1, -1}, // вверх-влево (диагональ)
            {-1, 1},  // вверх-вправо (диагональ)
            {1, -1},  // вниз-влево (диагональ)
            {1, 1}    // вниз-вправо (диагональ)
    };

    /**
     * Находит кратчайший путь между атакующим и атакуемым юнитом.
     * Использует алгоритм Дейкстры с учётом разных весов для прямых и диагональных ходов.
     * <p>
     * Сложность: O(W * H * log(W * H))
     *
     * @param attackUnit       атакующий юнит
     * @param targetUnit       цель атаки
     * @param existingUnitList список всех юнитов на поле
     * @return список координат пути от attackUnit до targetUnit, или пустой список если путь не найден
     */
    @Override
    public List<Edge> getTargetPath(Unit attackUnit, Unit targetUnit, List<Unit> existingUnitList) {
        // 1. Создаём сетку заблокированных клеток (занятых другими юнитами)
        boolean[][] blocked = new boolean[WIDTH][HEIGHT];

        for (Unit unit : existingUnitList) {
            if (unit.isAlive() && unit != attackUnit && unit != targetUnit) {
                int x = unit.getxCoordinate();
                int y = unit.getyCoordinate();
                if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
                    blocked[x][y] = true;
                }
            }
        }

        int startX = attackUnit.getxCoordinate();
        int startY = attackUnit.getyCoordinate();
        int endX = targetUnit.getxCoordinate();
        int endY = targetUnit.getyCoordinate();

        // 2. Алгоритм Дейкстры
        // distance[x][y] - кратчайшее расстояние от старта до (x, y)
        double[][] distance = new double[WIDTH][HEIGHT];
        for (double[] row : distance) {
            Arrays.fill(row, Double.MAX_VALUE);
        }
        distance[startX][startY] = 0;

        // parent[x][y] - предыдущая позиция для восстановления пути
        int[][][] parent = new int[WIDTH][HEIGHT][];

        // Приоритетная очередь: {distance, x, y}
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        pq.add(new double[]{0, startX, startY});

        while (!pq.isEmpty()) {
            double[] current = pq.poll();
            double currentDist = current[0];
            int x = (int) current[1];
            int y = (int) current[2];

            // Достигли цели
            if (x == endX && y == endY) {
                return reconstructPath(parent, startX, startY, endX, endY);
            }

            // Пропускаем, если уже нашли более короткий путь
            if (currentDist > distance[x][y]) {
                continue;
            }

            // Проверяем все 8 направлений
            for (int[] direction : DIRECTIONS) {
                int dx = direction[0];
                int dy = direction[1];
                int nx = x + dx;
                int ny = y + dy;

                // Проверяем границы поля
                if (nx < 0 || nx >= WIDTH || ny < 0 || ny >= HEIGHT) {
                    continue;
                }

                // Проверяем, что клетка не занята
                if (blocked[nx][ny]) {
                    continue;
                }

                // Вычисляем вес ребра
                boolean isDiagonal = (dx != 0 && dy != 0);
                double edgeCost = isDiagonal ? DIAGONAL_COST : STRAIGHT_COST;
                double newDist = distance[x][y] + edgeCost;

                // Если нашли более короткий путь
                if (newDist < distance[nx][ny]) {
                    distance[nx][ny] = newDist;
                    parent[nx][ny] = new int[]{x, y};
                    pq.add(new double[]{newDist, nx, ny});
                }
            }
        }

        // Путь не найден
        return Collections.emptyList();
    }

    /**
     * Восстанавливает путь от стартовой точки до конечной.
     * Сложность: O(длина пути) = O(W + H) в худшем случае
     */
    private List<Edge> reconstructPath(int[][][] parent, int startX, int startY, int endX, int endY) {
        List<Edge> path = new ArrayList<>();

        int x = endX;
        int y = endY;

        // Идём от конца к началу по ссылкам parent
        while (!(x == startX && y == startY)) {
            path.add(new Edge(x, y));
            int[] prev = parent[x][y];
            if (prev == null) {
                // Путь прерван (не должно происходить)
                return Collections.emptyList();
            }
            x = prev[0];
            y = prev[1];
        }

        // Добавляем стартовую точку
        path.add(new Edge(startX, startY));

        // Переворачиваем путь (от начала к концу)
        Collections.reverse(path);

        return path;
    }
}
