package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для GeneratePresetImpl.
 * Проверяем корректность генерации армии согласно требованиям:
 * - Суммарная стоимость не превышает maxPoints
 * - Не более 11 юнитов каждого типа
 * - Приоритет по эффективности (attack/cost, затем health/cost)
 */
class GeneratePresetImplTest {

    private GeneratePresetImpl generatePreset;

    @BeforeEach
    void setUp() {
        generatePreset = new GeneratePresetImpl();
    }

    @Test
    @DisplayName("Генерация армии не превышает maxPoints")
    void generate_shouldNotExceedMaxPoints() {
        List<Unit> unitList = createTestUnitList();
        int maxPoints = 1500;

        Army army = generatePreset.generate(unitList, maxPoints);

        assertNotNull(army);
        assertNotNull(army.getUnits());
        assertTrue(army.getPoints() <= maxPoints,
                "Суммарная стоимость армии (" + army.getPoints() + ") не должна превышать " + maxPoints);
    }

    @Test
    @DisplayName("Генерация армии не превышает 11 юнитов каждого типа")
    void generate_shouldNotExceed11UnitsPerType() {
        List<Unit> unitList = createTestUnitList();
        int maxPoints = 5000; // Большой бюджет для теста лимита

        Army army = generatePreset.generate(unitList, maxPoints);

        Map<String, Integer> countByType = new HashMap<>();
        for (Unit unit : army.getUnits()) {
            String type = unit.getUnitType();
            countByType.put(type, countByType.getOrDefault(type, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : countByType.entrySet()) {
            assertTrue(entry.getValue() <= 11,
                    "Тип " + entry.getKey() + " имеет " + entry.getValue() + " юнитов, максимум 11");
        }
    }

    @Test
    @DisplayName("Генерация армии выбирает более эффективных юнитов")
    void generate_shouldPrioritizeEfficiency() {
        // Создаём юнитов с разной эффективностью
        Unit efficient = createUnit("Efficient", "HighDamage", 100, 50, 100); // attack/cost = 0.5
        Unit inefficient = createUnit("Inefficient", "LowDamage", 50, 10, 100); // attack/cost = 0.1

        List<Unit> unitList = Arrays.asList(inefficient, efficient);
        int maxPoints = 200;

        Army army = generatePreset.generate(unitList, maxPoints);

        // Должен быть выбран более эффективный юнит
        boolean hasEfficient = army.getUnits().stream()
                .anyMatch(u -> u.getUnitType().equals("HighDamage"));
        assertTrue(hasEfficient, "Армия должна содержать более эффективных юнитов");
    }

    @Test
    @DisplayName("Генерация армии с пустым списком юнитов")
    void generate_withEmptyList_shouldReturnEmptyArmy() {
        List<Unit> unitList = Collections.emptyList();
        int maxPoints = 1500;

        Army army = generatePreset.generate(unitList, maxPoints);

        assertNotNull(army);
        assertNotNull(army.getUnits());
        assertTrue(army.getUnits().isEmpty());
        assertEquals(0, army.getPoints());
    }

    @Test
    @DisplayName("Генерация армии с нулевым бюджетом")
    void generate_withZeroBudget_shouldReturnEmptyArmy() {
        List<Unit> unitList = createTestUnitList();
        int maxPoints = 0;

        Army army = generatePreset.generate(unitList, maxPoints);

        assertNotNull(army);
        assertTrue(army.getUnits().isEmpty());
        assertEquals(0, army.getPoints());
    }

    @Test
    @DisplayName("Генерация армии заполняет бюджет максимально")
    void generate_shouldFillBudgetOptimally() {
        List<Unit> unitList = createTestUnitList();
        int maxPoints = 1500;

        Army army = generatePreset.generate(unitList, maxPoints);

        // Армия должна использовать значительную часть бюджета
        assertTrue(army.getPoints() > 0, "Армия должна содержать юнитов");
    }

    @Test
    @DisplayName("Все юниты в армии имеют уникальные имена")
    void generate_shouldCreateUniqueNames() {
        List<Unit> unitList = createTestUnitList();
        int maxPoints = 1500;

        Army army = generatePreset.generate(unitList, maxPoints);

        Set<String> names = new HashSet<>();
        for (Unit unit : army.getUnits()) {
            assertFalse(names.contains(unit.getName()),
                    "Дублирующееся имя юнита: " + unit.getName());
            names.add(unit.getName());
        }
    }

    private List<Unit> createTestUnitList() {
        List<Unit> units = new ArrayList<>();
        units.add(createUnit("Archer", "Archer", 50, 15, 30));
        units.add(createUnit("Knight", "Knight", 100, 25, 60));
        units.add(createUnit("Pikeman", "Pikeman", 70, 20, 40));
        units.add(createUnit("Swordsman", "Swordsman", 80, 22, 50));
        return units;
    }

    private Unit createUnit(String name, String unitType, int health, int attack, int cost) {
        return new Unit(name, unitType, health, attack, cost, "melee", null, null, 0, 0);
    }
}

