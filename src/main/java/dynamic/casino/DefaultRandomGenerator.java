package dynamic.casino;

import java.util.concurrent.ThreadLocalRandom;

public class DefaultRandomGenerator implements RandomNumberGenerator {
    @Override
    public int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    @Override
    public double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }
}
