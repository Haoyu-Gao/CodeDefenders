/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.servlets.admin;

import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This {@link HttpServlet} handles admin management of puzzles.
 *
 * <p>{@code GET} requests redirect to the admin puzzle management page.
 * and {@code POST} requests handle puzzle related management.
 *
 * <p>Serves under {@code /admin/puzzles} and {@code /admin/puzzles/management}.
 *
 * @author <a href=https://github.com/werli>Phil Werli</a>
 */
@WebServlet({"/admin/puzzles", "/admin/puzzles/management"})
public class AdminPuzzleManagement extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminPuzzleManagement.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher(Constants.ADMIN_PUZZLE_MANAGEMENT_JSP).forward(request, response);
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final String formType = ServletUtils.formType(request);
        switch (formType) {
            case "updatePuzzleChapter": {

            }
            case "inactivePuzzleChapter": {

            }
            case "removePuzzleChapter": {

            }
            case "rearrangePuzzleChapter": {

            }
            case "updatePuzzle": {

            }
            case "inactivePuzzle": {

            }
            case "removePuzzle": {

            }
        }
    }
}
