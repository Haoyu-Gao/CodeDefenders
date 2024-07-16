<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<p:main_page title="Puzzle Management">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/puzzle_management.css")}" rel="stylesheet">
    </jsp:attribute>
    <jsp:body>
        <div class="container">
            <t:admin_navigation activePage="adminPuzzles"/>
            <div id="puzzle-management">
                <div id="puzzle-management-controls">
                    <div class="d-flex gap-2">
                        <button type="button" id="button-upload-chapters" class="btn btn-sm btn-outline-secondary">
                            Upload chapters
                            <i class="fa fa-upload ms-1"></i>
                        </button>
                        <button type="button" id="button-upload-puzzles" class="btn btn-sm btn-outline-secondary">
                            Upload puzzles
                            <i class="fa fa-upload ms-1"></i>
                        </button>
                        <button type="button" id="button-add-chapter" class="btn btn-sm btn-outline-secondary">
                            Add empty chapter
                            <i class="fa fa-plus ms-1"></i>
                        </button>
                    </div>
                    <button type="button" id="button-save" class="btn btn-primary btn-lg btn-highlight">
                        Save
                        <i class="fa fa-save ms-1"></i>
                    </button>
                </div>

                <div class="chapter" id="chapter-unassigned">
                    <div class="chapter__header">
                        <span class="chapter__title">Unassigned Puzzles</span>
                    </div>
                    <div class="puzzles"></div>
                </div>

                <div id="chapters"></div>

                <div class="chapter" id="chapter-archived">
                    <div class="chapter__header">
                        <span class="chapter__title">Archived Puzzles</span>
                    </div>
                    <div class="puzzles"></div>
                </div>
            </div>
        </div>

        <script type="module">
            import {Sortable} from '${url.forPath('/js/sortablejs.mjs')}';
            import {PuzzleAPI, Modal} from '${url.forPath("/js/codedefenders_main.mjs")}';

            const watermarkUrl = '${url.forPath("/images/achievements/")}';
            const puzzleData = await PuzzleAPI.fetchPuzzleData();
            const puzzles = puzzleData.puzzles;
            const chapters = puzzleData.puzzleChapters;

            // ==== Init Data ==========================================================================================

            const puzzlesPerChapter = new Map();
            puzzlesPerChapter.set('unassigned', []);
            puzzlesPerChapter.set('archived', []);
            for (const puzzle of puzzles) {
                if (!puzzle.active) {
                    puzzlesPerChapter.get('archived').push(puzzle);
                } else if (puzzle.chapterId === null) {
                    puzzlesPerChapter.get('unassigned').push(puzzle);
                } else {
                    if (!puzzlesPerChapter.has(puzzle.chapterId)) {
                        puzzlesPerChapter.set(puzzle.chapterId, [puzzle])
                    } else {
                        puzzlesPerChapter.get(puzzle.chapterId).push(puzzle);
                    }
                }
            }

            chapters.sort((a, b) => a.position - b.position);
            for (const puzzles of puzzlesPerChapter.values()) {
                puzzles.sort((a, b) => a.position - b.position);
            }

            // ==== Other Globals =====================================================================================

            let archivedChapter;
            let unassignedChapter;
            const chaptersContainer = document.getElementById('chapters');
            let isUnsavedChanges = false;

            // ==== Components =========================================================================================

            class ChapterComponent {
                constructor(container = null) {
                    this.container = container === null
                        ? ChapterComponent._createElement()
                        : container;
                    this.title = this.container.querySelector('.chapter__title');
                    this.description = this.container.querySelector('.chapter__description');
                    this.puzzlesContainer = this.container.querySelector('.puzzles');
                    this.puzzles = this.puzzlesContainer.children;

                    Sortable.create(this.puzzlesContainer, {
                        animation: 200,
                        group: 'puzzles',
                        onMove: function() {
                            isUnsavedChanges = true;
                        }
                    });

                    this.container.chapterComp = this;
                }

                static _createElement() {
                    const container = document.createElement('div');
                    container.classList.add('chapter');
                    container.innerHTML = `
                        <div class="chapter__header">
                            <div class="chapter__info">
                                <div class="d-flex align-items-stretch">
                                    <span class="chapter__index"></span>
                                    <span class="chapter__title"></span>
                                </div>
                                <div class="chapter__description"></div>
                            </div>
                            <div class="chapter__controls">
                                <div class="chapter__handle me-3"></div>
                                <button class="btn btn-xs btn-primary btn-fixed chapter__button__edit" title="Edit">
                                    <i class="fa fa-edit"></i>
                                </button>
                                <button class="btn btn-xs btn-primary btn-fixed chapter__button__upload" title="Upload Puzzles">
                                    <i class="fa fa-upload"></i>
                                </button>
                                <button class="btn btn-xs btn-danger btn-fixed chapter__button__delete" title="Delete">
                                    <i class="fa fa-trash"></i>
                                </button>
                            </div>
                        </div>
                        <div class="puzzles"></div>`;

                    const controls = container.querySelector('.chapter__controls');
                    controls.firstElementChild.insertAdjacentElement('afterend', createChapterSelectDropdown({
                        label: 'Move to position:',
                        tooltip: 'Move to position'
                    }));

                    return container;
                }

                static forChapter(chapter) {
                    const chapterComp = new ChapterComponent();
                    chapterComp.chapter = chapter;
                    chapterComp.container.dataset.id = chapter.id;

                    chapterComp.title.innerText = chapter.title;
                    chapterComp.title.title = chapter.title;
                    chapterComp.description.innerText = chapter.description;
                    chapterComp.description.description = chapter.description;
                    return chapterComp;
                }

                static fromChild(childElement) {
                    return childElement.closest('.chapter').chapterComp;
                }

                addPuzzle(puzzleComp) {
                    this.puzzlesContainer.appendChild(puzzleComp.container);
                }

                moveToIndex(index) {
                    index--; // 1-indexed

                    const clampedIndex = Math.max(0, Math.min(chaptersContainer.children.length - 1, index));
                    const ownIndex = [...chaptersContainer.children].indexOf(this.container);

                    if (ownIndex > clampedIndex) {
                        chaptersContainer.children.item(clampedIndex)
                                .insertAdjacentElement('beforebegin', this.container);
                    } else if (ownIndex < clampedIndex) {
                        chaptersContainer.children.item(clampedIndex)
                                .insertAdjacentElement('afterend', this.container);
                    }
                }
            }

            class PuzzleComponent {
                constructor() {
                    this.container = PuzzleComponent._createElement();
                    this.title = this.container.querySelector('.puzzle__title');
                    this.description = this.container.querySelector('.puzzle__description');
                    this.tags = {
                        id: this.container.querySelector('.puzzle__tag__id'),
                        games: this.container.querySelector('.puzzle__tag__games')
                    };
                    this.container.puzzleComp = this;
                }

                static _createElement() {
                    const watermark = document.createElement('img');
                    watermark.classList.add('puzzle__watermark');

                    const container = document.createElement('div');
                    container.classList.add('puzzle')
                    container.innerHTML = `
                        <div class="puzzle__content">
                            <div class="puzzle__info">
                                <div class="puzzle__title"></div>
                                <div class="puzzle__description"></div>
                            </div>
                            <div class="puzzle__tags">
                                <span class="badge puzzle__tag puzzle__tag__id"></span>
                                <span class="badge puzzle__tag puzzle__tag__games"></span>
                            </div>
                        </div>
                        <div class="puzzle__controls">
                            <button class="btn btn-xs btn-primary btn-fixed puzzle__button__edit" title="Edit">
                                <i class="fa fa-edit"></i>
                            </button>

                            <button class="btn btn-xs btn-secondary btn-fixed puzzle__button__unassign" title="Unassign">
                                <i class="fa fa-times"></i>
                            </button>

                            <button class="btn btn-xs btn-secondary btn-fixed puzzle__button__archive" title="Archive">
                                <i class="fa fa-archive"></i>
                            </button>

                            <button class="btn btn-xs btn-danger btn-fixed puzzle__button__delete" title="Delete">
                                <i class="fa fa-trash"></i>
                            </button>
                        </div>`;

                    container.appendChild(watermark);
                    container.querySelector('.puzzle__controls').firstElementChild
                            .insertAdjacentElement('afterend', createChapterSelectDropdown({
                                label: 'Move to chapter:',
                                tooltip: 'Move to chapter'
                            }));
                    return container;
                }

                static forPuzzle(puzzle) {
                    const puzzleComp = new PuzzleComponent();
                    puzzleComp.puzzle = puzzle;
                    puzzleComp.container.dataset.id = puzzle.id;

                    puzzleComp.title.innerText = puzzle.title;
                    puzzleComp.title.title = puzzle.title;
                    puzzleComp.description.innerText = puzzle.description;
                    puzzleComp.description.title = puzzle.title;
                    puzzleComp.tags.id.innerText = '#' + puzzle.id;
                    puzzleComp.tags.games.innerText = puzzle.gameCount + ' game' + (puzzle.gameCount === 1 ? '' : 's');

                    puzzleComp.container.classList.add(`puzzle-\${puzzle.activeRole.toLowerCase()}`);
                    puzzleComp.container.querySelector('.puzzle__watermark').src =
                            `\${watermarkUrl}codedefenders_achievements_\${puzzle.activeRole == 'ATTACKER' ? 1 : 2}_lvl_0.png`;

                    if (puzzle.gameCount > 0) {
                        const deleteButton = puzzleComp.container.querySelector('.puzzle__button__delete');
                        deleteButton.disabled = true;
                        deleteButton.title = "Puzzles with existing games can't be deleted";
                    }

                    return puzzleComp;
                }

                static fromChild(childElement) {
                    return childElement.closest('.puzzle').puzzleComp;
                }

                moveToChapterIndex(index) {
                    index--; // 1-indexed

                    const clampedIndex = Math.max(0, Math.min(chaptersContainer.children.length - 1, index));
                    const chapterComp = chaptersContainer.children.item(clampedIndex).chapterComp;
                    chapterComp.addPuzzle(this);
                }
            }

            // ==== Other Functions ====================================================================================

            function createChapterSelectDropdown(config) {
                const tooltip = config.tooltip ?? 'Move';
                const label = config.label ?? 'Move to chapter:';
                const showButtonClasses = config.showButtonClasses ?? 'btn btn-xs btn-primary btn-fixed';
                const showButtonContent = config.showButtonContent ?? '<i class="fa fa-arrow-right"></i>';
                const moveButtonContent = config.moveButtonContent ?? 'Move';

                const dropdown = document.createElement('div');
                dropdown.classList.add('dropdown', 'chapter_select', 'd-flex');
                dropdown.title = tooltip;
                dropdown.innerHTML = `
                    <button class="\${showButtonClasses} chapter_select__show"
                            data-bs-toggle="dropdown" data-bs-offset="0,8">
                            \${showButtonContent}
                    </button>
                    <div class="dropdown-menu chapter_select__menu">
                        <div class="d-flex flex-column gap-1">
                            <div class="chapter_select__label">\${label}</div>
                            <div class="d-flex flex-row gap-2">
                                <select class="form-select form-select-sm chapter_select__position"></select>
                                <button type="button" class="btn btn-primary btn-sm chapter_select__confirm">
                                    \${moveButtonContent}
                                </button>
                            </div>
                        </div
                    </div>`;
                return dropdown;
            }

            function createChapterSelectOptions(selectedChapter = null) {
                let options = document.createDocumentFragment();
                let index = 1;
                for (const chapterElem of chaptersContainer.children) {
                    const title = chapterElem.chapterComp.chapter.title;
                    const option = document.createElement('option');
                    option.value = String(index);
                    option.innerText = `\${index} | \${title}`;
                    if (chapterElem.chapterComp === selectedChapter) {
                        option.disabled = true;
                    }
                    options.appendChild(option);
                    index++;
                }
                return options;
            }

            function getPuzzlePositions() {
                const data = {};

                data.unassignedPuzzles = [...unassignedChapter.puzzles]
                        .map(puzzlesElem => PuzzleComponent.fromChild(puzzlesElem).puzzle.id);

                data.archivedPuzzles = [...archivedChapter.puzzles]
                        .map(puzzlesElem => PuzzleComponent.fromChild(puzzlesElem).puzzle.id);

                data.chapters = [];
                for (const chapterElem of chaptersContainer.children) {
                    const chapterComp = ChapterComponent.fromChild(chapterElem);
                    data.chapters.push({
                        id: chapterComp.chapter.id,
                        puzzles: [...chapterComp.puzzles]
                                .map(puzzlesElem => PuzzleComponent.fromChild(puzzlesElem).puzzle.id)
                    });
                }

                return data;
            }

            // ==== Init =============================================================================================

            function initChaptersAndPuzzles() {
                unassignedChapter = new ChapterComponent(document.getElementById('chapter-unassigned'));
                for (const puzzle of puzzlesPerChapter.get('unassigned')) {
                    unassignedChapter.addPuzzle(PuzzleComponent.forPuzzle(puzzle));
                }

                for (const chapter of chapters) {
                    const chapterComp = ChapterComponent.forChapter(chapter);
                    chaptersContainer.appendChild(chapterComp.container);
                    const puzzles = puzzlesPerChapter.get(chapter.id) ?? [];
                    for (const puzzle of puzzles) {
                        chapterComp.addPuzzle(PuzzleComponent.forPuzzle(puzzle));
                    }
                }

                archivedChapter = new ChapterComponent(document.getElementById('chapter-archived'));
                for (const puzzle of puzzlesPerChapter.get('archived')) {
                    archivedChapter.addPuzzle(PuzzleComponent.forPuzzle(puzzle));
                }
            }

            function initChapterSelects() {
                // --- Init 'Scroll to chapter' ------------------------------------------------------------------------

                const scrollDropdown = createChapterSelectDropdown({
                    tooltip: 'Scroll to chapter',
                    label: 'Scroll to chapter:',
                    moveButtonContent: 'Go',
                    showButtonClasses: 'btn btn-sm btn-outline-secondary',
                    showButtonContent: 'Scroll to chapter <i class="fa fa-arrow-down ms-1"></i>',
                });
                document.getElementById('button-add-chapter').insertAdjacentElement('afterend', scrollDropdown);

                scrollDropdown.querySelector('.chapter_select__confirm').addEventListener('click', function (event) {
                    const select = event.target.closest('.chapter_select').querySelector('.chapter_select__position');
                    const index = Number(select.value) - 1;

                    chaptersContainer.children.item(index).scrollIntoView();
                });

                scrollDropdown.querySelector('.chapter_select__show').addEventListener('click', function (event) {
                    const options = createChapterSelectOptions();
                    const select = event.target.closest('.chapter_select').querySelector('.chapter_select__position');
                    select.innerText = '';
                    select.appendChild(options);
                });

                // --- Init 'Move chapter' -----------------------------------------------------------------------------

                document.getElementById('puzzle-management').addEventListener('show.bs.dropdown', function (event) {
                    const toggleButton = event.target.closest('.chapter__controls .chapter_select__show');
                    if (toggleButton === null) {
                        return;
                    }

                    const chapterComp = ChapterComponent.fromChild(event.target);
                    const options = createChapterSelectOptions(chapterComp);
                    const select = chapterComp.container.querySelector('.chapter_select__position');
                    select.innerText = '';
                    select.appendChild(options);
                });

                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const moveButton = event.target.closest('.chapter__controls .chapter_select__confirm');
                    if (moveButton === null) {
                        return;
                    }

                    const chapterComp = ChapterComponent.fromChild(event.target);
                    const select = chapterComp.container.querySelector('.chapter_select__position');
                    const position = Number(select.value);
                    chapterComp.moveToIndex(position);
                });

                // --- Init 'Move puzzle' ------------------------------------------------------------------------------

                document.getElementById('puzzle-management').addEventListener('show.bs.dropdown', function (event) {
                    const toggleButton = event.target.closest('.puzzle__controls .chapter_select__show');
                    if (toggleButton === null) {
                        return;
                    }

                    const puzzle = PuzzleComponent.fromChild(event.target);
                    const chapter = ChapterComponent.fromChild(puzzle.container);
                    const options = createChapterSelectOptions(chapter);

                    const select = puzzle.container.querySelector('.chapter_select__position');
                    select.innerText = '';
                    select.appendChild(options);
                });

                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const moveButton = event.target.closest('.puzzle__controls .chapter_select__confirm');
                    if (moveButton === null) {
                        return;
                    }

                    const puzzle = PuzzleComponent.fromChild(event.target);
                    const select = puzzle.container.querySelector('.chapter_select__position');
                    const position = Number(select.value);
                    puzzle.moveToChapterIndex(position);
                });
            }

            function initModals() {
                chaptersContainer.addEventListener('click', function (event) {
                    const editButton = event.target.closest('.chapter__button__edit');
                    if (editButton === null) {
                        return;
                    }

                    const chapterComp = ChapterComponent.fromChild(event.target);
                    const chapter = chapterComp.chapter;

                    const modal = new Modal();
                    modal.body.innerHTML =`
                            <div class="row mb-3">
                                <div class="form-group">
                                    <label class="form-label">Title</label>
                                    <input type="text" name="title" class="form-control" value=""
                                        placeholder="Title">
                                </div>
                            </div>

                            <div class="row mb-2">
                                <div class="form-group">
                                    <label class="form-label">Description</label>
                                    <input type="text" name="description" class="form-control" value=""
                                        placeholder="Description">
                                </div>
                            </div>`;
                    modal.title.innerText = 'Edit Chapter';
                    modal.footerCloseButton.innerText = 'Cancel';
                    modal.modal.dataset.id = chapter.id;
                    modal.body.querySelector('input[name="title"]').value = chapter.title;
                    modal.body.querySelector('input[name="description"]').value = chapter.description;

                    const saveButton = document.createElement('button');
                    saveButton.classList.add('btn', 'btn-primary');
                    saveButton.role = 'button';
                    saveButton.innerText = 'Save';
                    modal.footer.insertAdjacentElement('beforeend', saveButton);

                    saveButton.addEventListener('click', function(event) {
                        const title = modal.body.querySelector('input[name="title"]').value;
                        const description = modal.body.querySelector('input[name="description"]').value;

                        PuzzleAPI.updatePuzzleChapter(chapter.id, {
                            title: title,
                            description: description,
                            id: chapter.id,
                            position: chapter.position
                        }).then(response => {
                            chapter.title = title;
                            chapter.description = description;
                            chapterComp.title.innerText = title;
                            chapterComp.description.innerText = description;
                        }).catch(error => {
                            alert('Puzzle chapter could not be updated.');
                        }).finally(() => {
                            modal.controls.hide();
                        });
                    });

                    modal.controls.show();
                });

                chaptersContainer.addEventListener('click', function (event) {
                    const deleteButton = event.target.closest('.chapter__button__delete');
                    if (deleteButton === null) {
                        return;
                    }

                    const chapterComp = ChapterComponent.fromChild(event.target);
                    const chapter = chapterComp.chapter;

                    const modal = new Modal();
                    modal.body.innerHTML = `
                        <div>
                            Are you sure you want to delete chapter '<span></span>'?
                        </div>`;

                    modal.title.innerText = 'Delete Chapter';
                    modal.footerCloseButton.innerText = 'Cancel';
                    modal.modal.dataset.id = chapter.id;
                    modal.body.querySelector('span').innerText = chapter.title;

                    const saveButton = document.createElement('button');
                    saveButton.classList.add('btn', 'btn-danger');
                    saveButton.role = 'button';
                    saveButton.innerText = 'Delete';
                    modal.footer.insertAdjacentElement('beforeend', saveButton);

                    saveButton.addEventListener('click', function(event) {
                        PuzzleAPI.deletePuzzleChapter(chapter.id)
                                .then(response => {
                                    chapterComp.container.remove();
                                    for (const puzzle of [...chapterComp.puzzles]) {
                                        unassignedChapter.addPuzzle(puzzle);
                                    }
                                }).catch(error => {
                                    alert('Puzzle chapter could not be deleted.');
                                }).finally(() => {
                                    modal.controls.hide();
                                });
                    });

                    modal.controls.show();
                });

                document.getElementById('button-add-chapter').addEventListener('click', function (event) {
                    const modal = new Modal();
                    modal.body.innerHTML =`
                            <div class="row mb-3">
                                <div class="form-group">
                                    <label class="form-label">Title</label>
                                    <input type="text" name="title" class="form-control" value=""
                                        placeholder="Title">
                                </div>
                            </div>

                            <div class="row mb-2">
                                <div class="form-group">
                                    <label class="form-label">Description</label>
                                    <input type="text" name="description" class="form-control" value=""
                                        placeholder="Description">
                                </div>
                            </div>`;
                    modal.title.innerText = 'Create New Chapter';
                    modal.footerCloseButton.innerText = 'Cancel';

                    const saveButton = document.createElement('button');
                    saveButton.classList.add('btn', 'btn-primary');
                    saveButton.role = 'button';
                    saveButton.innerText = 'Save';
                    modal.footer.insertAdjacentElement('beforeend', saveButton);

                    saveButton.addEventListener('click', function(event) {
                        const title = modal.body.querySelector('input[name="title"]').value;
                        const description = modal.body.querySelector('input[name="description"]').value;

                        PuzzleAPI.createPuzzleChapter({
                            title: title,
                            description: description,
                        }).then(response => {
                            const chapterComp = ChapterComponent.forChapter({
                                id: response.id,
                                title: response.title,
                                description: response.description
                            });
                            chaptersContainer.appendChild(chapterComp.container);
                        }).catch(error => {
                            alert('Puzzle chapter could not be created.');
                        }).finally(() => {
                            modal.controls.hide();
                        });
                    });

                    modal.controls.show();
                });
            }

            function initRemainingSortables() {
                Sortable.create(chaptersContainer, {
                    animation: 200,
                    group: 'chapters',
                    handle: '.chapter__handle',
                    onMove: function() {
                        isUnsavedChanges = true;
                    }
                });

                for (const puzzlesContainer of [unassignedChapter.puzzlesContainer, archivedChapter.puzzlesContainer]) {
                    Sortable.create(puzzlesContainer, {
                        animation: 200,
                        group: 'puzzles',
                        onMove: function() {
                            isUnsavedChanges = true;
                        }
                    });
                }
            }

            function initRemainingButtons() {
                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const archiveButton = event.target.closest('.puzzle__button__archive');
                    if (archiveButton === null) {
                        return;
                    }

                    const puzzle = PuzzleComponent.fromChild(event.target);
                    archivedChapter.addPuzzle(puzzle);
                });

                document.getElementById('puzzle-management').addEventListener('click', function (event) {
                    const unassignButton = event.target.closest('.puzzle__button__unassign');
                    if (unassignButton === null) {
                        return;
                    }

                    const puzzle = PuzzleComponent.fromChild(event.target)
                    unassignedChapter.addPuzzle(puzzle);
                });

                document.getElementById('button-save').addEventListener('click', function (event) {
                    PuzzleAPI.batchUpdatePuzzlePositions(getPuzzlePositions())
                            .then(response => {
                                isUnsavedChanges = false
                            })
                            .catch(error => {
                                alert('Could not save changes.');
                            });
                });
            }

            function init() {
                initChaptersAndPuzzles();
                initChapterSelects();
                initModals();
                initRemainingSortables();
                initRemainingButtons();

                window.addEventListener('beforeunload', function(event) {
                    if (isUnsavedChanges) {
                        event.preventDefault();
                    }
                });
            }

            init();
        </script>
    </jsp:body>
</p:main_page>
