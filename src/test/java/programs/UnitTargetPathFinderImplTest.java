package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.Edge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для UnitTargetPathFinderImpl.
 * Проверяем корректность поиска кратчайшего пути:
 * - Путь находится корректно
 * - Обход препятствий
 * - Диагональное движение
 * - Пустой путь при невозможности достичь цели
 */
class UnitTargetPathFinderImplTest {

    private UnitTargetPathFinderImpl pathFinder;

    @BeforeEach
    void setUp() {
        pathFinder = new UnitTargetPathFinderImpl();
    }

    @Test
    @DisplayName("Прямой путь без препятствий")
    void getTargetPath_directPath_shouldFindShortest() {
        Unit attacker = createUnit("Attacker", 0, 0);
        Unit target = createUnit("Target", 3, 0);

        List<Edge> path = pathFinder.getTargetPath(attacker, target, Arrays.asList(attacker, target));

        assertNotNull(path);
        assertFalse(path.isEmpty());
        // Путь должен начинаться с attacker и заканчиваться на target
        assertEquals(0, path.getFirst().getX());
        assertEquals(0, path.getFirst().getY());
        assertEquals(3, path.getLast().getX());
        assertEquals(0, path.getLast().getY());
    }

    @Test
    @DisplayName("Диагональный путь - оптимальный с учётом весов")
    void getTargetPath_diagonalPath_shouldFindShortest() {
        Unit attacker = createUnit("Attacker", 0, 0);
        Unit target = createUnit("Target", 3, 3);

        List<Edge> path = pathFinder.getTargetPath(attacker, target, Arrays.asList(attacker, target));

        assertNotNull(path);
        assertFalse(path.isEmpty());
        // Диагональный путь: (0,0) -> (1,1) -> (2,2) -> (3,3) = 4 точки
        assertEquals(4, path.size());
        // Проверяем, что путь идёт по диагонали (оптимально)
        assertEquals(0, path.get(0).getX());
        assertEquals(0, path.get(0).getY());
        assertEquals(3, path.get(3).getX());
        assertEquals(3, path.get(3).getY());
    }

    @Test
    @DisplayName("Обход препятствия")
    void getTargetPath_withObstacle_shouldFindPath() {
        Unit attacker = createUnit("Attacker", 0, 0);
        Unit target = createUnit("Target", 2, 0);
        Unit obstacle = createUnit("Obstacle", 1, 0); // Блокирует прямой путь

        List<Unit> units = Arrays.asList(attacker, target, obstacle);
        List<Edge> path = pathFinder.getTargetPath(attacker, target, units);

        assertNotNull(path);
        assertFalse(path.isEmpty());
        // Путь должен обойти препятствие
        boolean containsObstacle = path.stream()
                .anyMatch(e -> e.getX() == 1 && e.getY() == 0);
        assertFalse(containsObstacle, "Путь не должен проходить через препятствие");
    }

    @Test
    @DisplayName("Путь невозможен - окружён препятствиями")
    void getTargetPath_blockedTarget_shouldReturnEmpty() {
        Unit attacker = createUnit("Attacker", 0, 0);
        Unit target = createUnit("Target", 2, 2);

        // Окружаем цель препятствиями
        List<Unit> units = new ArrayList<>();
        units.add(attacker);
        units.add(target);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    units.add(createUnit("Block", 2 + dx, 2 + dy));
                }
            }
        }

        List<Edge> path = pathFinder.getTargetPath(attacker, target, units);

        assertNotNull(path);
        assertTrue(path.isEmpty(), "Путь должен быть пустым, если цель недостижима");
    }

    @Test
    @DisplayName("Атакующий и цель на одной позиции")
    void getTargetPath_samePosition_shouldReturnSinglePoint() {
        Unit attacker = createUnit("Attacker", 5, 5);
        Unit target = createUnit("Target", 5, 5);

        List<Edge> path = pathFinder.getTargetPath(attacker, target, Arrays.asList(attacker, target));

        assertNotNull(path);
        assertEquals(1, path.size());
        assertEquals(5, path.getFirst().getX());
        assertEquals(5, path.getFirst().getY());
    }

    @Test
    @DisplayName("Путь включает начальную и конечную точки")
    void getTargetPath_shouldIncludeStartAndEnd() {
        Unit attacker = createUnit("Attacker", 1, 1);
        Unit target = createUnit("Target", 3, 3);

        List<Edge> path = pathFinder.getTargetPath(attacker, target, Arrays.asList(attacker, target));

        assertFalse(path.isEmpty());
        // Первая точка - позиция атакующего
        assertEquals(1, path.getFirst().getX());
        assertEquals(1, path.getFirst().getY());
        // Последняя точка - позиция цели
        assertEquals(3, path.getLast().getX());
        assertEquals(3, path.getLast().getY());
    }

    @Test
    @DisplayName("Мёртвые юниты не являются препятствиями")
    void getTargetPath_deadUnits_shouldNotBlock() {
        Unit attacker = createUnit("Attacker", 0, 0);
        Unit target = createUnit("Target", 2, 0);
        Unit deadUnit = createUnit("Dead", 1, 0);
        deadUnit.setAlive(false);

        List<Edge> path = pathFinder.getTargetPath(attacker, target, Arrays.asList(attacker, target, deadUnit));

        assertNotNull(path);
        assertEquals(3, path.size()); // Прямой путь через мёртвого юнита
    }

    @Test
    @DisplayName("Путь на большом расстоянии")
    void getTargetPath_longDistance_shouldFindPath() {
        Unit attacker = createUnit("Attacker", 0, 0);
        Unit target = createUnit("Target", 20, 15);

        List<Edge> path = pathFinder.getTargetPath(attacker, target, Arrays.asList(attacker, target));

        assertNotNull(path);
        assertFalse(path.isEmpty());
        // Путь должен быть найден
        assertEquals(20, path.getLast().getX());
        assertEquals(15, path.getLast().getY());
    }

    @Test
    @DisplayName("Путь у границы поля")
    void getTargetPath_atBoundary_shouldFindPath() {
        Unit attacker = createUnit("Attacker", 0, 0);
        Unit target = createUnit("Target", 0, 10);

        List<Edge> path = pathFinder.getTargetPath(attacker, target, Arrays.asList(attacker, target));

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(11, path.size()); // Прямой путь вдоль границы
    }

    @Test
    @DisplayName("Пустой список юнитов (кроме attacker и target)")
    void getTargetPath_emptyUnits_shouldFindPath() {
        Unit attacker = createUnit("Attacker", 5, 5);
        Unit target = createUnit("Target", 7, 7);

        List<Edge> path = pathFinder.getTargetPath(attacker, target, Collections.emptyList());

        assertNotNull(path);
        assertFalse(path.isEmpty());
    }

    private Unit createUnit(String name, int x, int y) {
        Unit unit = new Unit(name, "TestType", 100, 20, 50, "melee", null, null, x, y);
        unit.setAlive(true);
        return unit;
    }
}

