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
package org.codedefenders.servlets.games.melee;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.NoSuchElementException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMapProcessor;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.scoring.ScoreCalculator;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.game.GameCreatedEvent;
import org.codedefenders.notification.events.server.game.GameJoinedEvent;
import org.codedefenders.notification.events.server.game.GameLeftEvent;
import org.codedefenders.notification.events.server.game.GameStartedEvent;
import org.codedefenders.notification.events.server.game.GameStoppedEvent;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Paths;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.GAME_CREATION;
import static org.codedefenders.servlets.util.ServletUtils.ctx;
import static org.codedefenders.servlets.util.ServletUtils.formType;
import static org.codedefenders.servlets.util.ServletUtils.getFloatParameter;
import static org.codedefenders.servlets.util.ServletUtils.getIntParameter;
import static org.codedefenders.servlets.util.ServletUtils.getStringParameter;
import static org.codedefenders.servlets.util.ServletUtils.parameterThenOrOther;
import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

/**
 * This {@link HttpServlet} handles selection of {@link MeleeGame games}.
 *
 * <p>{@code GET} requests redirect to the game overview page and {@code POST}
 * requests handle creating, joining and entering {@link MultiplayerGame
 * battleground games}.
 *
 * <p>Serves under {@code /melee/games}.
 *
 * @see org.codedefenders.util.Paths#MELEE_SELECTION
 */
@WebServlet(Paths.MELEE_SELECTION)
public class MeleeGameSelectionManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MeleeGameSelectionManager.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private LoginBean login;

    @Inject
    private INotificationService notificationService;

    @Inject
    private EventDAO eventDAO;

    @Inject
    private ScoreCalculator scoreCalculator;

    @Inject
    private GameManagingUtils gameManagingUtils;

    @Inject
    @Named("MeleeGame")
    private MeleeGame game;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final String action = formType(request);
        switch (action) {
            case "createGame":
                createGame(request, response);
                return;
            case "joinGame":
                joinGame(request, response);
                return;
            case "leaveGame":
                leaveGame(request, response);
                return;
            case "startGame":
                startGame(request, response);
                return;
            case "endGame":
                endGame(request, response);
                return;
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
                break;
        }
    }

    private void createGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final boolean canCreateGames = AdminDAO.getSystemSetting(GAME_CREATION).getBoolValue();
        if (!canCreateGames) {
            logger.warn("User {} tried to create a battleground game, but creating games is not permitted.",
                    login.getUserId());
            Redirect.redirectBack(request, response);
            return;
        }

        String contextPath = request.getContextPath();

        int classId;
        int maxAssertionsPerTest;
        int automaticEquivalenceTrigger;
        CodeValidatorLevel mutantValidatorLevel;
        Role selectedRole;

        try {
            classId = getIntParameter(request, "class").get();
            maxAssertionsPerTest = getIntParameter(request, "maxAssertionsPerTest").get();
            automaticEquivalenceTrigger = getIntParameter(request, "automaticEquivalenceTrigger").get();
            mutantValidatorLevel = getStringParameter(request, "mutantValidatorLevel")
                    .map(CodeValidatorLevel::valueOrNull).get();
            // If we select "player in the UI this should not result in a null value
            selectedRole = getStringParameter(request, "roleSelection").map(Role::valueOrNull).orElse(Role.NONE);
        } catch (NoSuchElementException e) {
            logger.error("At least one request parameter was missing or was no valid integer value.", e);
            Redirect.redirectBack(request, response);
            return;
        }

        GameLevel level = parameterThenOrOther(request, "level", GameLevel.EASY, GameLevel.HARD);
        float lineCoverage = getFloatParameter(request, "line_cov").orElse(1.1f);
        float mutantCoverage = getFloatParameter(request, "mutant_cov").orElse(1.1f);
        boolean chatEnabled = parameterThenOrOther(request, "chatEnabled", true, false);
        boolean capturePlayersIntention = parameterThenOrOther(request, "capturePlayersIntention", true, false);

        MeleeGame nGame = new MeleeGame.Builder(classId, login.getUserId(), maxAssertionsPerTest)
                .level(level).chatEnabled(chatEnabled).capturePlayersIntention(capturePlayersIntention)
                .lineCoverage(lineCoverage).mutantCoverage(mutantCoverage).mutantValidatorLevel(mutantValidatorLevel)
                .automaticMutantEquivalenceThreshold(automaticEquivalenceTrigger).build();

        // TODO This should be handled by MeleeGameDAO. See #687
        if (nGame.insert()) {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            Event event = new Event(-1, nGame.getId(), login.getUserId(), "Game Created", EventType.GAME_CREATED,
                    EventStatus.GAME, timestamp);
            // eventDAO.insert(event);
        }

        // Always add system player to send mutants and tests at runtime!
        nGame.addPlayer(DUMMY_ATTACKER_USER_ID, Role.PLAYER);
        // TODO: Add two players or only one?
        // TODO: If only one, then GameManagingUtils.addPredefinedMutantsAndTests needs to be changed.
        nGame.addPlayer(DUMMY_DEFENDER_USER_ID, Role.PLAYER);

        // Add selected role to game if the creator participates as non-observer (i.e., player)
        if (selectedRole == Role.PLAYER) {
            nGame.addPlayer(login.getUserId(), selectedRole);
        }

        boolean withTests = parameterThenOrOther(request, "withTests", true, false);
        boolean withMutants = parameterThenOrOther(request, "withMutants", true, false);
        gameManagingUtils.addPredefinedMutantsAndTests(nGame, withMutants, withTests);

        /* Publish the event that a new game started */
        GameCreatedEvent gce = new GameCreatedEvent();
        gce.setGameId(nGame.getId());
        notificationService.post(gce);

        // Redirect to admin interface
        if (request.getParameter("fromAdmin").equals("true")) {
            response.sendRedirect(contextPath + "/admin");
            return;
        }

        // Redirect to the game selection menu.
        response.sendRedirect(contextPath + Paths.GAMES_OVERVIEW);
    }

    private void joinGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final boolean canJoinGames = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_JOINING)
                .getBoolValue();
        if (!canJoinGames) {
            logger.warn("User {} tried to join a melee game, but joining games is not permitted.", login.getUserId());
            Redirect.redirectBack(request, response);
            return;
        }

        if (game == null) {
            logger.error("No game found. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        
        int gameId = game.getId();

        if (game.hasUserJoined(login.getUserId())) {
            logger.info("User {} already in the requested game.", login.getUserId());
            return;
        }
        if (game.addPlayer(login.getUserId())) {
            logger.info("User {} joined game {}.", login.getUserId(), gameId);

            /*
             * Publish the event about the user
             */
            GameJoinedEvent gje = new GameJoinedEvent();
            gje.setGameId(game.getId());
            gje.setUserId(login.getUserId());
            gje.setUserName(login.getUser().getUsername());
            notificationService.post(gje);

            // TODO The following notification is duplicated as MeleeGame.addPlayer also
            // trigger that.
            // I leave it here because I believe the problem is having notifications inside
            // DataObjects like MeleeGame.
            // Note that MeleeGame has more than one notification.
//            final EventType notifType = EventType.PLAYER_JOINED;
//            final String message = "You successfully joined the game.";
//            final EventStatus eventStatus = EventStatus.NEW;
//            final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
//            Event notif = new Event(-1, gameId, login.getUserId(), message, notifType, eventStatus, timestamp);
//            eventDAO.insert(notif);

            response.sendRedirect(ctx(request) + Paths.MELEE_GAME + "?gameId=" + gameId);
        } else {
            logger.info("User {} failed to join game {}.", login.getUserId(), gameId);
            response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
        }
    }

    private void leaveGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = request.getContextPath();

        if (game == null) {
            logger.error("No game found. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        } else if (!(this.game instanceof MeleeGame)) {
            logger.error("Game found is no MeleeGame. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }

        int gameId = game.getId();

        final boolean removalSuccess = game.removePlayer(login.getUserId());
        if (!removalSuccess) {
            messages.add("An error occurred while leaving game " + gameId);
            response.sendRedirect(contextPath + Paths.GAMES_OVERVIEW);
            return;
        }

        messages.add("Game " + gameId + " left");
        DatabaseAccess.removePlayerEventsForGame(gameId, login.getUserId());

        final EventType notifType = EventType.GAME_PLAYER_LEFT;
        final String message = "You successfully left the game.";
        final EventStatus eventStatus = EventStatus.NEW;
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Event notif = new Event(-1, gameId, login.getUserId(), message, notifType, eventStatus, timestamp);
        eventDAO.insert(notif);

        logger.info("User {} successfully left game {}", login.getUserId(), gameId);

        /*
         * Publish the event about the user
         */
        GameLeftEvent gle = new GameLeftEvent();
        gle.setGameId(game.getId());
        gle.setUserId(login.getUserId());
        gle.setUserName(login.getUser().getUsername());

        notificationService.post(gle);

        response.sendRedirect(contextPath + Paths.GAMES_OVERVIEW);
    }

    private void startGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (game == null) {
            logger.error("No game found. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        } else if (!(this.game instanceof MeleeGame)) {
            logger.error("Game found is no MeleeGame. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }

        int gameId = game.getId();

        if (game.getState() == GameState.CREATED) {
            logger.info("Starting melee game {} (Setting state to ACTIVE)", gameId);
            game.setState(GameState.ACTIVE);
            game.update();
        }

        /*
         * Publish the event about the user
         */
        GameStartedEvent gse = new GameStartedEvent();
        gse.setGameId(game.getId());
        notificationService.post(gse);

        response.sendRedirect(ctx(request) + Paths.MELEE_GAME + "?gameId=" + gameId);
    }

    private void endGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (game == null) {
            logger.error("No game found. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        } else if (!(this.game instanceof MeleeGame)) {
            logger.error("Game found is no MeleeGame. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }

        int gameId = game.getId();

        if (game.getState() == GameState.ACTIVE) {
            logger.info("Ending multiplayer game {} (Setting state to FINISHED)", gameId);
            game.setState(GameState.FINISHED);
            boolean updated = game.update();
            if (updated) {
                KillmapDAO.enqueueJob(new KillMapProcessor.KillMapJob(KillMap.KillMapType.GAME, gameId));
            }

            scoreCalculator.storeScoresToDB(game.getId());

            /*
             * Publish the event about the user
             */
            GameStoppedEvent gse = new GameStoppedEvent();
            gse.setGameId(game.getId());
            notificationService.post(gse);

            response.sendRedirect(ctx(request) + Paths.MELEE_SELECTION);
        } else {
            // TODO Update this later !
            response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_HISTORY + "?gameId=" + gameId);
        }
    }
}
