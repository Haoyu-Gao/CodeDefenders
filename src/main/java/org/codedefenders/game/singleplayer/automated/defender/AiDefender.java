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
package org.codedefenders.game.singleplayer.automated.defender;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.execution.BackendExecutorService;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.execution.MutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.singleplayer.AiPlayer;
import org.codedefenders.game.singleplayer.NoDummyGameException;
import org.codedefenders.game.singleplayer.PrepareAI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ben Clegg
 * An AI defender. Uses tests generated by EvoSuite to kill mutants.
 */
public class AiDefender extends AiPlayer {

    @Inject
    private BackendExecutorService backend;

    @Inject
    private IMutationTester mutationTester;

    private static final Logger logger = LoggerFactory.getLogger(AiDefender.class);

    public static final int ID = 2;

    public AiDefender(DuelGame g) {
        super(g);
        role = Role.DEFENDER;
    }
    public boolean turnHard() {
        //Choose test which kills a high number of generated mutants.
        return runTurn(GenerationMethod.COVERAGE);
    }

    public boolean turnEasy() {
        //Choose random test.
        return runTurn(GenerationMethod.RANDOM);
    }

    /**
     * Attempts to submit a test, according to a strategy
     * @param strat Generation strategy to use
     * @return true if test submitted, false otherwise
     */
    protected boolean runTurn(GenerationMethod strat) {
        try {
            int tNum = selectTest(strat);
            useTestFromSuite(tNum);
        } catch (NoTestsException e) {
            //No more choices remain - do nothing
            return false;
        } catch (Exception e) {
            //Something's gone wrong
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private int selectTest(GenerationMethod strategy) throws NoTestsException, NoDummyGameException {

        List<Integer> usedTests = DatabaseAccess.getUsedAiTestsForGame(game);
        GameClass cut = game.getCUT();
        DuelGame dummyGame = cut.getDummyGame();

        //TODO: Discarding useless tests in origtests would be a sideeffect
        List<Test> candidateTests = dummyGame.getTests().stream().filter(test -> !usedTests.contains(test.getId())).collect(Collectors.toList());

        if(candidateTests.isEmpty()) {
            throw new NoTestsException("All generated tests have already been used.");
        }

        switch(strategy) {
            case COVERAGE:
                return getTestIdByCoverage(candidateTests);

            case RANDOM:
                return getTestIdByRandom(candidateTests);

            case KILLCOUNT:
                return getTestIdByKillcount(candidateTests);

            default:
                throw new UnsupportedOperationException("Invalid strategy for AI defender");
        }
    }

    private int getTestIdByCoverage(List<Test> possibleTests) {
        HashSet<Integer> linesModified = new HashSet<Integer>();
        for (Mutant m : game.getAliveMutants()) {
            linesModified.addAll(m.getLines());
        }
        logger.debug("Alive mutated lines: {}", linesModified.toString());

        Test covTest = null;
        int bestCoverage = 0;
        for (Test tst : possibleTests) {
            LineCoverage lc = tst.getLineCoverage(); // test already has line coverage information here
            List<Integer> coveredByTest = lc.getLinesCovered();
            int coverage = 0;

            StringBuilder logOutput = new StringBuilder();
            logOutput.append("String covers lines: ");
            for (int l : coveredByTest) {
                logOutput.append(l);
                if(linesModified.contains(l)) {
                    logOutput.append("[HIT]");
                    //Test covers this mutated line.
                    coverage ++;
                }
                logOutput.append(", ");
            }
            logger.info(logOutput.toString());
            if (coverage > bestCoverage) {
                //Test is the best unused test found.
                covTest = tst;
                bestCoverage = coverage;
            }
        }
        if (covTest != null) {
            //Just use the found test if using line coverage method.
            return covTest.getId();
        } else {
            logger.debug("No test covers an alive mutated line, using killcount instead.");
            return getTestIdByKillcount(possibleTests);
        }
    }

    private int getTestIdByKillcount(List<Test> possibleTests) {
        //Sort tests in order of killcount.
        Collections.sort(possibleTests, new TestComparator());

        //Get an index, using a random number biased towards later index.
        //More extreme than attacker due to smaller sample size.
        int n = PrepareAI.biasedSelection(possibleTests.size(), 0.6);
        return possibleTests.get(n).getId();
    }

    private int getTestIdByRandom(List<Test> possibleTests) {
        Random r = new Random();
        Test selected = possibleTests.get(r.nextInt(possibleTests.size()));
        return selected.getId();
    }

    private void useTestFromSuite(int origTestNum) throws NoDummyGameException{
        GameClass cut = game.getCUT();
        DuelGame dummyGame = cut.getDummyGame();
        List<Test> origTests = dummyGame.getTests();

        Test origT = null;

        for (Test t : origTests) {
            if(t.getId() == origTestNum) {
                origT = t;
                break;
            }
        }

        if(origT != null) {
            String jFile = origT.getJavaFile();
            String cFile = origT.getClassFile();
            int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ID, game.getId());
            Test t = new Test(cut.getId(), game.getId(), jFile, cFile, playerId);
            t.insert();
            t.update();
            TargetExecution newExec = new TargetExecution(t.getId(), 0, TargetExecution.Target.COMPILE_TEST, TargetExecution.Status.SUCCESS, null);
            newExec.insert();
            mutationTester.runTestOnAllMutants(game, t, messages);
            DatabaseAccess.setAiTestAsUsed(origTestNum, game);
            File dir = new File(origT.getDirectory());
            backend.testOriginal(dir, t);
            game.update();
        }
    }

    @Override
    public ArrayList<String> getMessagesLastTurn() {
        boolean killed = false;
        for (String s : messages) {
            if (s.contains("test killed")) {
                killed = true;
                break;
            }
        }
        messages.clear();
        if (killed)
            messages.add("The AI submitted a new test, which killed at least one mutant.");
        else
            messages.add("The AI submitted a new test, which did not kill any mutant.");
        return messages;
    }
}

