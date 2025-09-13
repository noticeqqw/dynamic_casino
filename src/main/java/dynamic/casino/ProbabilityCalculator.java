package dynamic.casino;

public class ProbabilityCalculator {

    /**
     * Рассчитывает вероятность выигрыша
     * @param columns количество колонок
     * @param rows количество строк
     * @param symbolCount количество различных символов
     * @return вероятность выигрыша (от 0 до 1)
     */
    public static double calculateWinProbability(int columns, int rows, int symbolCount) {
        if (columns <= 0 || rows <= 0 || symbolCount <= 0) {
            return 0;
        }

        // Общее количество возможных комбинаций
        int totalCombinations = (int) Math.pow(symbolCount, columns * rows);

        // Количество выигрышных комбинаций (все символы одинаковые)
        int winningCombinations = symbolCount;

        return (double) winningCombinations / totalCombinations;
    }

    /**
     * Рассчитывает вероятность для разных уровней совпадения
     * @param columns количество колонок
     * @param rows количество строк
     * @param symbolCount количество различных символов
     * @return массив вероятностей для разных уровней
     */
    public static double[] calculateDetailedProbabilities(int columns, int rows, int symbolCount) {
        double[] probabilities = new double[3]; // [all_same, partial_match, no_match]

        int totalPositions = columns * rows;
        double allSame = symbolCount * Math.pow(1.0 / symbolCount, totalPositions - 1);

        probabilities[0] = allSame; // Все символы совпадают
        probabilities[1] = 0.3; // Частичное совпадение (упрощенный расчет)
        probabilities[2] = 1.0 - allSame - 0.3; // Нет совпадений

        return probabilities;
    }
}
