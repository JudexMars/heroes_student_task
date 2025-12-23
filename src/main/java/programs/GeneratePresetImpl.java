package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.GeneratePreset;

import java.util.*;

public class GeneratePresetImpl implements GeneratePreset {

    private static final int MAX_UNITS_PER_TYPE = 11;
    private static final int COMPUTER_ARMY_WIDTH = 3;  // Колонки x: 0, 1, 2
    private static final int FIELD_HEIGHT = 21;        // Строки y: 0-20

    @Override
    public Army generate(List<Unit> unitList, int maxPoints) {
        // 1. Сортируем типы юнитов по эффективности:
        //    - Первичный критерий: baseAttack / cost (убывание)
        //    - Вторичный критерий: health / cost (убывание)
        List<Unit> sortedUnits = new ArrayList<>(unitList);
        sortedUnits.sort((a, b) -> {
            double attackEffA = (double) a.getBaseAttack() / a.getCost();
            double attackEffB = (double) b.getBaseAttack() / b.getCost();
            if (Double.compare(attackEffA, attackEffB) != 0) {
                return Double.compare(attackEffB, attackEffA); // убывание
            }
            double healthEffA = (double) a.getHealth() / a.getCost();
            double healthEffB = (double) b.getHealth() / b.getCost();
            return Double.compare(healthEffB, healthEffA); // убывание
        });

        // 2. Жадно добавляем юнитов (до 11 каждого типа, пока есть бюджет)
        List<Unit> armyUnits = new ArrayList<>();
        int currentPoints = 0;
        Map<String, Integer> countByType = new HashMap<>();
        int unitIndex = 0; // Для назначения уникальных координат

        // Проходим по отсортированным типам и добавляем максимум юнитов
        for (Unit template : sortedUnits) {
            String unitType = template.getUnitType();
            int count = countByType.getOrDefault(unitType, 0);

            while (count < MAX_UNITS_PER_TYPE && currentPoints + template.getCost() <= maxPoints) {
                // Вычисляем координаты для размещения на поле
                int x = unitIndex / FIELD_HEIGHT; // 0, 1, 2 (колонки)
                int y = unitIndex % FIELD_HEIGHT; // 0-20 (строки)

                // Проверяем, что не вышли за пределы области компьютера
                if (x >= COMPUTER_ARMY_WIDTH) {
                    break; // Максимум 3 * 21 = 63 юнита
                }

                // Создаём копию юнита с уникальным именем и координатами
                // Формат имени: "Тип Номер" (например, "Archer 1")
                Unit newUnit = createUnitCopy(template, unitType + " " + (count + 1), x, y);
                armyUnits.add(newUnit);
                currentPoints += template.getCost();
                count++;
                unitIndex++;
            }
            countByType.put(unitType, count);
        }

        // 3. Создаём и возвращаем армию
        Army army = new Army();
        army.setUnits(armyUnits);
        army.setPoints(currentPoints);
        return army;
    }

    /**
     * Создаёт копию юнита с новым именем и координатами.
     * Сложность: O(1)
     */
    private Unit createUnitCopy(Unit template, String newName, int x, int y) {
        return new Unit(
                newName,
                template.getUnitType(),
                template.getHealth(),
                template.getBaseAttack(),
                template.getCost(),
                template.getAttackType(),
                template.getAttackBonuses(),
                template.getDefenceBonuses(),
                x,
                y
        );
    }
}
