/*
 * Copyright (C) 2016-2023 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.analysis.coverage.line;

import java.util.ArrayList;
import java.util.List;

import org.codedefenders.game.LineCoverage;

public class SimpleLineCoverage extends LineMapping<LineCoverageStatus> implements NewLineCoverage {
    @Override
    public LineCoverageStatus getEmpty() {
        return LineCoverageStatus.EMPTY;
    }

    @Override
    public LineCoverageStatus get(int line) {
        return super.get(line);
    }

    @Override
    public void set(int line, LineCoverageStatus elem) {
        super.set(line, elem);
    }

    @Override
    public LineCoverageStatus getStatus(int line) {
        return get(line);
    }

    // TODO: either make this an interface default method on NewLineCoverage, or (better) replace LineCoverage with
    //       NewLineCoverage and extend the interface
    public LineCoverage toLineCoverage() {
        List<Integer> coveredLines = new ArrayList<>();
        List<Integer> uncoveredLines = new ArrayList<>();
        for (int line = getFirstLine(); line <= getLastLine(); line++) {
            switch (get(line)) {
                case PARTLY_COVERED:
                case FULLY_COVERED:
                    coveredLines.add(line);
                    break;
                case NOT_COVERED:
                    uncoveredLines.add(line);
                    break;
                case EMPTY:
                    break;
            }
        }
        return new LineCoverage(coveredLines, uncoveredLines);
    }
}
