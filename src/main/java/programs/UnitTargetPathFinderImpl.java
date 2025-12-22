package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.Edge;
import com.battle.heroes.army.programs.UnitTargetPathFinder;

import java.util.*;

public class UnitTargetPathFinderImpl implements UnitTargetPathFinder {

    private static final int WIDTH = 27;
    private static final int HEIGHT = 21;

    // 8 направлений движения: вверх, вниз, влево, вправо + 4 диагонали
    private static final int[][] DIRECTIONS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };

    /**
     * Находит кратчайший путь между атакующим и атакуемым юнитом.
     * Использует BFS (Breadth-First Search) с 8 направлениями движения.
     * <p>
     * Сложность: O(W * H), где W = 27, H = 21
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

        // 2. BFS для поиска кратчайшего пути
        Queue<int[]> queue = new LinkedList<>();
        // parent[x][y] хранит предыдущую позицию для восстановления пути
        int[][][] parent = new int[WIDTH][HEIGHT][];
        boolean[][] visited = new boolean[WIDTH][HEIGHT];

        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;
        parent[startX][startY] = null; // стартовая точка

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];

            // Достигли цели
            if (x == endX && y == endY) {
                return reconstructPath(parent, startX, startY, endX, endY);
            }

            // Проверяем все 8 направлений
            for (int[] dir : DIRECTIONS) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                // Проверяем границы поля
                if (nx < 0 || nx >= WIDTH || ny < 0 || ny >= HEIGHT) {
                    continue;
                }

                // Проверяем, что клетка не занята и не посещена
                if (!blocked[nx][ny] && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    parent[nx][ny] = new int[]{x, y};
                    queue.add(new int[]{nx, ny});
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
