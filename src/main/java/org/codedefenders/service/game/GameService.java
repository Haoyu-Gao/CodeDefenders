/*
 * Copyright (C) 2020 Code Defenders contributors
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

package org.codedefenders.service.game;

import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class GameService implements IGameService {

    MultiplayerGameService multiplayerGameService;

    MeleeGameService meleeGameService;

    PuzzleGameService puzzleGameService;

    @Inject
    public GameService(MultiplayerGameService multiplayerGameService, MeleeGameService meleeGameService,
                       PuzzleGameService puzzleGameService) {
        this.multiplayerGameService = multiplayerGameService;
        this.meleeGameService = meleeGameService;
        this.puzzleGameService = puzzleGameService;
    }

    @Override
    public MutantDTO getMutant(int userId, int mutantId) {
        // I can't delegate this to the other services, as I don't know the game type yet.
        Mutant mutant = MutantDAO.getMutantById(mutantId);
        return getMutant(userId, mutant);
    }

    @Override
    public MutantDTO getMutant(int userId, Mutant mutant) {
        IGameService gameService = getGameServiceForGameId(mutant.getGameId());
        if (gameService != null) {
            return gameService.getMutant(userId, mutant);
        } else {
            return null;
        }
    }

    @Override
    public MutantDTO getMutant(int userId, int mutantId, int gameId) {
        IGameService gameService = getGameServiceForGameId(gameId);
        if (gameService != null) {
            return gameService.getMutant(userId, mutantId, gameId);
        } else {
            return null;
        }
    }

    @Override
    public List<MutantDTO> getMutants(int userId, int gameId) {
        IGameService gameService = getGameServiceForGameId(gameId);
        if (gameService != null) {
            return gameService.getMutants(userId, gameId);
        } else {
            return null;
        }
    }

    @Override
    public List<MutantDTO> getMutants(User user, AbstractGame game) {
        IGameService gameService = getGameServiceForGame(game);
        if (gameService != null) {
            return gameService.getMutants(user, game);
        } else {
            return null;
        }
    }

    private IGameService getGameServiceForGameId(int gameId) {
        GameMode mode = GameDAO.getGameMode(gameId);
        switch (mode) {
            case PARTY:
                return multiplayerGameService;
            case MELEE:
                return meleeGameService;
            case PUZZLE:
                return puzzleGameService;
            default:
                return null;
        }
    }

    private IGameService getGameServiceForGame(AbstractGame game) {
        if (game instanceof MultiplayerGame) {
            return multiplayerGameService;
        } else if (game instanceof MeleeGame) {
            return meleeGameService;
        } else if (game instanceof PuzzleGame) {
            return puzzleGameService;
        } else {
            return null;
        }
    }
}
