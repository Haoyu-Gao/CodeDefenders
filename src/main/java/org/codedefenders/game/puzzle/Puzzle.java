package org.codedefenders.game.puzzle;

import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Role;
import org.codedefenders.validation.code.CodeValidatorLevel;

/**
 * Represents the blueprint for a puzzle, which can be instantiated into a {@link PuzzleGame}.
 * @see PuzzleChapter
 * @see PuzzleGame
 */
public class Puzzle {

    /**
     * ID of the chapter.
     */
    private int puzzleId;

    /**
     * Class ID of the class the puzzle uses.
     * The mutants and test for the puzzle come together with the class.
     */
    private int classId;

    /**
     * The {@link Role} the player takes in the puzzle.
     */
    private Role activeRole;

    /**
     * The {@link GameLevel} the puzzle is played on.
     */
    private GameLevel level;

    /**
     * Maximum number of allowed assertions per submitted test.
     */
    private int maxAssertionsPerTest;

    /**
     * Validation level used to check submitted mutants.
     */
    private CodeValidatorLevel mutantValidatorLevel;

    /**
     * First editable line of the class or test. Can be null.
     */
    private Integer editableLinesStart;

    /**
     * Last editable line of the class or test. Can be null.
     */
    private Integer editableLinesEnd;

    /**
     * ID of the {@link PuzzleChapter chapter} the puzzle belongs to. Can be null.
     */
    private Integer chapterId;

    /**
     * Position of the puzzle inside the {@link PuzzleChapter chapter}. Can be null.
     */
    private Integer position;

    /**
     * Title of the puzzle. Can be null.
     */
    private String title;

    /**
     * Description of the puzzle. Can be null.
     */
    private String description;

    /**
     * The {@link PuzzleChapter} this puzzle belongs to.
     */
    private PuzzleChapter chapter;

    /**
     * @param puzzleId ID of the chapter.
     * @param classId Class ID of the class the puzzle uses.
     *                The mutants and test for the puzzle come together with the class.
     * @param activeRole The {@link Role} the player takes in the puzzle.
     * @param level The {@link GameLevel} the puzzle is played on.
     * @param mutantValidatorLevel Validation level used to check submitted mutants.
     * @param maxAssertionsPerTest Maximum number of allowed assertions per submitted test.
     * @param editableLinesStart First editable line of the class or test. Can be null.
     * @param editableLinesEnd Last editable line of the class or test. Can be null.
     * @param chapterId ID of the chapter the puzzle belongs to. Can be null.
     * @param position Position of the puzzle inside the chapter. Can be null.
     * @param title Title of the puzzle. Can be null.
     * @param description Description of the puzzle. Can be null.
     */
    public Puzzle(int puzzleId,
                  int classId,
                  Role activeRole,
                  GameLevel level,
                  int maxAssertionsPerTest,
                  CodeValidatorLevel mutantValidatorLevel,
                  Integer editableLinesStart,
                  Integer editableLinesEnd,
                  Integer chapterId,
                  Integer position,
                  String title,
                  String description) {
        this.puzzleId = puzzleId;
        this.classId = classId;
        this.activeRole = activeRole;
        this.level = level;
        this.maxAssertionsPerTest = maxAssertionsPerTest;
        this.mutantValidatorLevel = mutantValidatorLevel;
        this.editableLinesStart = editableLinesStart;
        this.editableLinesEnd = editableLinesEnd;
        this.chapterId = chapterId;
        this.position = position;
        this.title = title;
        this.description = description;
        this.chapter = null;
    }

    public int getPuzzleId() {
        return puzzleId;
    }

    public void setPuzzleId(int puzzleId) {
        this.puzzleId = puzzleId;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public Role getActiveRole() {
        return activeRole;
    }

    public void setActiveRole(Role activeRole) {
        this.activeRole = activeRole;
    }

    public GameLevel getLevel() {
        return level;
    }

    public void setLevel(GameLevel level) {
        this.level = level;
    }

    public int getMaxAssertionsPerTest() {
        return maxAssertionsPerTest;
    }

    public void setMaxAssertionsPerTest(int maxAssertionsPerTest) {
        this.maxAssertionsPerTest = maxAssertionsPerTest;
    }

    public CodeValidatorLevel getMutantValidatorLevel() {
        return mutantValidatorLevel;
    }

    public void setMutantValidatorLevel(CodeValidatorLevel mutantValidatorLevel) {
        this.mutantValidatorLevel = mutantValidatorLevel;
    }

    public Integer getEditableLinesStart() {
        return editableLinesStart;
    }

    public void setEditableLinesStart(Integer editableLinesStart) {
        this.editableLinesStart = editableLinesStart;
    }

    public Integer getEditableLinesEnd() {
        return editableLinesEnd;
    }

    public void setEditableLinesEnd(Integer editableLinesEnd) {
        this.editableLinesEnd = editableLinesEnd;
    }

    public Integer getChapterId() {
        return chapterId;
    }

    public void setChapterId(Integer chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PuzzleChapter getChapter() {
        if (chapterId != null && chapter == null) {
           chapter = PuzzleDAO.getPuzzleChapterForId(chapterId);
        }
        return chapter;
    }

    public void setChapter(PuzzleChapter chapter) {
        this.chapter = chapter;
    }
}
