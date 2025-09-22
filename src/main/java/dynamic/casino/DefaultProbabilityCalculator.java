package dynamic.casino;

public class DefaultProbabilityCalculator implements ProbabilityCalculator {

    @Override
    public double calculateWinProbability(int columns, int rows, int symbolCount) {
        if (columns <= 0 || rows <= 0 || symbolCount <= 0) {
            return 0;
        }

        double prow = Math.pow(1.0 / symbolCount, columns - 1);

        double pwin = 1 - Math.pow(1 - prow, rows);

        return pwin;
    }
}
