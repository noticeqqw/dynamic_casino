package dynamic.casino;

public class DefaultProbabilityCalculator implements ProbabilityCalculator {

    @Override
    public double calculateWinProbability(int columns, int rows, int symbolCount) {
        if (columns <= 0 || rows <= 0 || symbolCount <= 0) {
            return 0;
        }

        // Для одной строки вероятность «все символы совпали»
        double prow = Math.pow(1.0 / symbolCount, columns - 1);

        // Для R строк вероятность «хотя бы одна строка совпала полностью»
        double pwin = 1 - Math.pow(1 - prow, rows);

        return pwin;
    }
}
