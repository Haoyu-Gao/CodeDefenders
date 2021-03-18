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

import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.codedefenders.util.Constants;

@ApplicationScoped
public class MeleeGameService extends AbstractGameService {

    @Override
    protected MutantDTO convertMutant(Mutant mutant, UserEntity user, Player player, AbstractGame game) {
        Role playerRole = determineRole(user, player, game);

        SimpleUser killedBy;
        int killedByTestId;
        String killMessage;
        if (mutant.getKillingTest() != null) {
            UserEntity killedByUser = userRepository.getUserById(userRepository.getUserIdForPlayerId(mutant.getKillingTest().getPlayerId()));
            killedBy = new SimpleUser(killedByUser.getId(), killedByUser.getUsername());
            killedByTestId = mutant.getKillingTest().getId();
            killMessage = StringEscapeUtils.escapeJavaScript(mutant.getKillMessage());
        } else {
            killedBy = null;
            killedByTestId = -1;
            killMessage = null;
        }
        boolean canView = playerRole != Role.NONE
                && (game.getLevel() == GameLevel.EASY
                || (game.getLevel() == GameLevel.HARD
                && (mutant.getCreatorId() == user.getId()
                || playerRole.equals(Role.OBSERVER)
                || mutant.getState().equals(Mutant.State.KILLED)
                || mutant.getState().equals(Mutant.State.EQUIVALENT))));
        boolean canMarkEquivalent = game.getState().equals(GameState.ACTIVE)
                && mutant.getState().equals(Mutant.State.ALIVE)
                && mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
                && mutant.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID
                && mutant.getCreatorId() != user.getId()
                && mutant.getLines().size() >= 1;

        return new MutantDTO(
                mutant.getId(),
                new SimpleUser(mutant.getCreatorId(), mutant.getCreatorName()),
                mutant.getState(),
                mutant.getScore(),
                StringEscapeUtils.escapeJavaScript(mutant.getHTMLReadout().stream()
                        .filter(Objects::nonNull).collect(Collectors.joining("<br>"))),
                mutant.getLines().stream().map(String::valueOf).collect(Collectors.joining(",")),
                mutant.getCoveringTests(game.getTests(false)).stream()
                        .anyMatch(t -> player != null && t.getPlayerId() == player.getId()),
                canView,
                canMarkEquivalent,
                killedBy,
                killedByTestId,
                killMessage,
                mutant.getGameId(),
                mutant.getPlayerId(),
                mutant.getLines(),
                mutant.getPatchString()
        );
    }

    @Override
    protected TestDTO convertTest(Test test, UserEntity user, Player player, AbstractGame game) {
        Role playerRole = determineRole(user, player, game);

        boolean viewable = game.getState() == GameState.FINISHED
                || playerRole == Role.OBSERVER
                || playerRole == Role.DEFENDER
                || game.getLevel() == GameLevel.EASY
                || test.getPlayerId() == player.getId();

        UserEntity creator = userRepository.getUserById(userRepository.getUserIdForPlayerId(test.getPlayerId()));
        SimpleUser simpleCreator = new SimpleUser(creator.getId(), creator.getUsername());

        return new TestDTO(test.getId(), simpleCreator, test.getScore(), viewable,
                test.getCoveredMutants(game.getMutants()).stream().map(Mutant::getId).collect(Collectors.toList()),
                test.getKilledMutants().stream().map(Mutant::getId).collect(Collectors.toList()),
                (new TestSmellsDAO()).getDetectedTestSmellsForTest(test),
                test.getGameId(),
                test.getPlayerId(),
                test.getLineCoverage().getLinesCovered(),
                test.getAsString()
        );
    }
}
