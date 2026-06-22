package io.github.karino2.rhinocs;

/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * modified by karino2 in 2026.
 */

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DiffUtil {
    private DiffUtil() {
        // utility class, no instance.
    }
    private static final Comparator<Diagonal> DIAGONAL_COMPARATOR = new Comparator<Diagonal>() {
        @Override
        public int compare(Diagonal o1, Diagonal o2) {
            return o1.x - o2.x;
        }
    };

    public static @NonNull DiffResult calculateDiff(@NonNull Callback cb) {
        final int oldSize = cb.getOldListSize();
        final int newSize = cb.getNewListSize();
        final List<Diagonal> diagonals = new ArrayList<>();
        // instead of a recursive implementation, we keep our own stack to avoid potential stack
        // overflow exceptions
        final List<Range> stack = new ArrayList<>();
        stack.add(new Range(0, oldSize, 0, newSize));
        final int max = (oldSize + newSize + 1) / 2;
        // allocate forward and backward k-lines. K lines are diagonal lines in the matrix. (see the
        // paper for details)
        // These arrays lines keep the max reachable position for each k-line.
        final CenteredArray forward = new CenteredArray(max * 2 + 1);
        final CenteredArray backward = new CenteredArray(max * 2 + 1);
        // We pool the ranges to avoid allocations for each recursive call.
        final List<Range> rangePool = new ArrayList<>();
        while (!stack.isEmpty()) {
            final Range range = stack.remove(stack.size() - 1);
            final Snake snake = midPoint(range, cb, forward, backward);
            if (snake != null) {
                // if it has a diagonal, save it
                if (snake.diagonalSize() > 0) {
                    diagonals.add(snake.toDiagonal());
                }
                // add new ranges for left and right
                final Range left = rangePool.isEmpty() ? new Range() : rangePool.remove(
                        rangePool.size() - 1);
                left.oldListStart = range.oldListStart;
                left.newListStart = range.newListStart;
                left.oldListEnd = snake.startX;
                left.newListEnd = snake.startY;
                stack.add(left);
                // re-use range for right
                //noinspection UnnecessaryLocalVariable
                final Range right = range;
                right.oldListEnd = range.oldListEnd;
                right.newListEnd = range.newListEnd;
                right.oldListStart = snake.endX;
                right.newListStart = snake.endY;
                stack.add(right);
            } else {
                rangePool.add(range);
            }
        }
        // sort snakes
        Collections.sort(diagonals, DIAGONAL_COMPARATOR);
        return new DiffResult(diagonals, cb.getOldListSize(), cb.getNewListSize());
    }
    /**
     * Finds a middle snake in the given range.
     */
    private static @Nullable Snake midPoint(
            Range range,
            Callback cb,
            CenteredArray forward,
            CenteredArray backward) {
        if (range.oldSize() < 1 || range.newSize() < 1) {
            return null;
        }
        int max = (range.oldSize() + range.newSize() + 1) / 2;
        forward.set(1, range.oldListStart);
        backward.set(1, range.oldListEnd);
        for (int d = 0; d < max; d++) {
            Snake snake = forward(range, cb, forward, backward, d);
            if (snake != null) {
                return snake;
            }
            snake = backward(range, cb, forward, backward, d);
            if (snake != null) {
                return snake;
            }
        }
        return null;
    }
    private static @Nullable Snake forward(
            Range range,
            Callback cb,
            CenteredArray forward,
            CenteredArray backward,
            int d) {
        boolean checkForSnake = Math.abs(range.oldSize() - range.newSize()) % 2 == 1;
        int delta = range.oldSize() - range.newSize();
        for (int k = -d; k <= d; k += 2) {
            // we either come from d-1, k-1 OR d-1. k+1
            // as we move in steps of 2, array always holds both current and previous d values
            // k = x - y and each array value holds the max X, y = x - k
            final int startX;
            final int startY;
            int x, y;
            if (k == -d || (k != d && forward.get(k + 1) > forward.get(k - 1))) {
                // picking k + 1, incrementing Y (by simply not incrementing X)
                x = startX = forward.get(k + 1);
            } else {
                // picking k - 1, incrementing X
                startX = forward.get(k - 1);
                x = startX + 1;
            }
            y = range.newListStart + (x - range.oldListStart) - k;
            startY = (d == 0 || x != startX) ? y : y - 1;
            // now find snake size
            while (x < range.oldListEnd
                    && y < range.newListEnd
                    && cb.areItemsTheSame(x, y)) {
                x++;
                y++;
            }
            // now we have furthest reaching x, record it
            forward.set(k, x);
            if (checkForSnake) {
                // see if we did pass over a backwards array
                // mapping function: delta - k
                int backwardsK = delta - k;
                // if backwards K is calculated and it passed me, found match
                if (backwardsK >= -d + 1
                        && backwardsK <= d - 1
                        && backward.get(backwardsK) <= x) {
                    // match
                    Snake snake = new Snake();
                    snake.startX = startX;
                    snake.startY = startY;
                    snake.endX = x;
                    snake.endY = y;
                    snake.reverse = false;
                    return snake;
                }
            }
        }
        return null;
    }
    private static @Nullable Snake backward(
            Range range,
            Callback cb,
            CenteredArray forward,
            CenteredArray backward,
            int d) {
        boolean checkForSnake = (range.oldSize() - range.newSize()) % 2 == 0;
        int delta = range.oldSize() - range.newSize();
        // same as forward but we go backwards from end of the lists to be beginning
        // this also means we'll try to optimize for minimizing x instead of maximizing it
        for (int k = -d; k <= d; k += 2) {
            // we either come from d-1, k-1 OR d-1, k+1
            // as we move in steps of 2, array always holds both current and previous d values
            // k = x - y and each array value holds the MIN X, y = x - k
            // when x's are equal, we prioritize deletion over insertion
            final int startX;
            final int startY;
            int x, y;
            if (k == -d || (k != d && backward.get(k + 1) < backward.get(k - 1))) {
                // picking k + 1, decrementing Y (by simply not decrementing X)
                x = startX = backward.get(k + 1);
            } else {
                // picking k - 1, decrementing X
                startX = backward.get(k - 1);
                x = startX - 1;
            }
            y = range.newListEnd - ((range.oldListEnd - x) - k);
            startY = (d == 0 || x != startX) ? y : y + 1;
            // now find snake size
            while (x > range.oldListStart
                    && y > range.newListStart
                    && cb.areItemsTheSame(x - 1, y - 1)) {
                x--;
                y--;
            }
            // now we have furthest point, record it (min X)
            backward.set(k, x);
            if (checkForSnake) {
                // see if we did pass over a backwards array
                // mapping function: delta - k
                int forwardsK = delta - k;
                // if forwards K is calculated and it passed me, found match
                if (forwardsK >= -d
                        && forwardsK <= d
                        && forward.get(forwardsK) >= x) {
                    // match
                    Snake snake = new Snake();
                    // assignment are reverse since we are a reverse snake
                    snake.startX = x;
                    snake.startY = y;
                    snake.endX = startX;
                    snake.endY = startY;
                    snake.reverse = true;
                    return snake;
                }
            }
        }
        return null;
    }


    /**
     * A Callback class used by DiffUtil while calculating the diff between two lists.
     */
    public interface Callback {
        /**
         * Returns the size of the old list.
         *
         * @return The size of the old list.
         */
        int getOldListSize();
        /**
         * Returns the size of the new list.
         *
         * @return The size of the new list.
         */
        int getNewListSize();
        /**
         * Called by the DiffUtil to decide whether two object represent the same Item.
         * <p>
         * For example, if your items have unique ids, this method should check their id equality.
         *
         * @param oldItemPosition The position of the item in the old list
         * @param newItemPosition The position of the item in the new list
         * @return True if the two items represent the same object or false if they are different.
         */
        boolean areItemsTheSame(int oldItemPosition, int newItemPosition);
    }
    /**
     * A diagonal is a match in the graph.
     * Rather than snakes, we only record the diagonals in the path.
     */
    static class Diagonal {
        public final int x;
        public final int y;
        public final int size;
        Diagonal(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
        int endX() {
            return x + size;
        }
        int endY() {
            return y + size;
        }
    }
    /**
     * Snakes represent a match between two lists. It is optionally prefixed or postfixed with an
     * add or remove operation. See the Myers' paper for details.
     */
    @SuppressWarnings("WeakerAccess")
    static class Snake {
        /**
         * Position in the old list
         */
        public int startX;
        /**
         * Position in the new list
         */
        public int startY;
        /**
         * End position in the old list, exclusive
         */
        public int endX;
        /**
         * End position in the new list, exclusive
         */
        public int endY;
        /**
         * True if this snake was created in the reverse search, false otherwise.
         */
        public boolean reverse;
        boolean hasAdditionOrRemoval() {
            return endY - startY != endX - startX;
        }
        boolean isAddition() {
            return endY - startY > endX - startX;
        }
        int diagonalSize() {
            return Math.min(endX - startX, endY - startY);
        }
        /**
         * Extract the diagonal of the snake to make reasoning easier for the rest of the
         * algorithm where we try to produce a path and also find moves.
         */
        @NonNull Diagonal toDiagonal() {
            if (hasAdditionOrRemoval()) {
                if (reverse) {
                    // snake edge it at the end
                    return new Diagonal(startX, startY, diagonalSize());
                } else {
                    // snake edge it at the beginning
                    if (isAddition()) {
                        return new Diagonal(startX, startY + 1, diagonalSize());
                    } else {
                        return new Diagonal(startX + 1, startY, diagonalSize());
                    }
                }
            } else {
                // we are a pure diagonal
                return new Diagonal(startX, startY, endX - startX);
            }
        }
    }
    /**
     * Represents a range in two lists that needs to be solved.
     * <p>
     * This internal class is used when running Myers' algorithm without recursion.
     * <p>
     * Ends are exclusive
     */
    static class Range {
        int oldListStart, oldListEnd;
        int newListStart, newListEnd;
        public Range() {
        }
        public Range(int oldListStart, int oldListEnd, int newListStart, int newListEnd) {
            this.oldListStart = oldListStart;
            this.oldListEnd = oldListEnd;
            this.newListStart = newListStart;
            this.newListEnd = newListEnd;
        }
        int oldSize() {
            return oldListEnd - oldListStart;
        }
        int newSize() {
            return newListEnd - newListStart;
        }
    }

    public static class DiffResult {
        /**
         * Signifies an item not present in the list.
         */
        public static final int NO_POSITION = -1;
        /**
         * While reading the flags below, keep in mind that when multiple items move in a list,
         * Myers's may pick any of them as the anchor item and consider that one NOT_CHANGED while
         * picking others as additions and removals. This is completely fine as we later detect
         * all moves.
         * <p>
         * Below, when an item is mentioned to stay in the same "location", it means we won't
         * dispatch a move/add/remove for it, it DOES NOT mean the item is still in the same
         * position.
         */
        private final List<Diagonal> mDiagonals;
        private final int mOldListSize;
        private final int mNewListSize;

        DiffResult(List<Diagonal> diagonals, int oldListSize, int newListSize) {
            mDiagonals = diagonals;
            mOldListSize = oldListSize;
            mNewListSize = newListSize;
            addEdgeDiagonals();
        }
        /**
         * Add edge diagonals so that we can iterate as long as there are diagonals w/o lots of
         * null checks around
         */
        private void addEdgeDiagonals() {
            Diagonal first = mDiagonals.isEmpty() ? null : mDiagonals.get(0);
            // see if we should add 1 to the 0,0
            if (first == null || first.x != 0 || first.y != 0) {
                mDiagonals.add(0, new Diagonal(0, 0, 0));
            }
            // always add one last
            mDiagonals.add(new Diagonal(mOldListSize, mNewListSize, 0));
        }


        public interface ListUpdateCallback
        {
            void onRemove(int index, int count);
            void onInsert(int oldIndex, int fromNewIndex, int count);
        }

        public void dispatchUpdatesTo(@NonNull ListUpdateCallback updateCallback) {
            // posX and posY are exclusive
            int posX = mOldListSize;
            int posY = mNewListSize;
            // iterate from end of the list to the beginning.
            // this just makes offsets easier since changes in the earlier indices has an effect
            // on the later indices.
            for (int diagonalIndex = mDiagonals.size() - 1; diagonalIndex >= 0; diagonalIndex--) {
                final Diagonal diagonal = mDiagonals.get(diagonalIndex);
                int endX = diagonal.endX();
                int endY = diagonal.endY();
                // dispatch removals and additions until we reach to that diagonal
                // first remove then add so that it can go into its place and we don't need
                // to offset values
                while (posX > endX) {
                    posX--;
                    // REMOVAL
                    updateCallback.onRemove(posX, 1);
                }
                while (posY > endY) {
                    posY--;
                    // ADDITION
                    // simple addition
                    updateCallback.onInsert(posX, posY,1);
                }
                // now dispatch updates for the diagonal
                posX = diagonal.x;
                posY = diagonal.y;
                for (int i = 0; i < diagonal.size; i++) {
                    posX++;
                    posY++;
                }
                // snap back for the next diagonal
                posX = diagonal.x;
                posY = diagonal.y;
            }
        }
    }

    /**
     * Array wrapper w/ negative index support.
     * We use this array instead of a regular array so that algorithm is easier to read without
     * too many offsets when accessing the "k" array in the algorithm.
     */
    static class CenteredArray {
        private final int[] mData;
        private final int mMid;
        CenteredArray(int size) {
            mData = new int[size];
            mMid = mData.length / 2;
        }
        int get(int index) {
            return mData[index + mMid];
        }
        void set(int index, int value) {
            mData[index + mMid] = value;
        }
    }
}