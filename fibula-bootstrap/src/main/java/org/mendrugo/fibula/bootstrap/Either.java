package org.mendrugo.fibula.bootstrap;

sealed interface Either<L, R>
{
    static <L, R> Either<L, R> left(L value)
    {
        return new Left<>(value);
    }

    static <L, R> Either<L, R> right(R value)
    {
        return new Right<>(value);
    }

    L left();

    R right();

    record Left<L, R>(L value) implements Either<L, R>
    {
        @Override
        public L left()
        {
            return value;
        }

        @Override
        public R right()
        {
            return null;
        }
    }

    record Right<L, R>(R value) implements Either<L, R>
    {
        @Override
        public L left()
        {
            return null;
        }

        @Override
        public R right()
        {
            return value;
        }
    }
}

