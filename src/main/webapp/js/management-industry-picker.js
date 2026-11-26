/**
 * 管理画面の希望業種編集フォームに、会員登録画面と同じカテゴリタブ・ピル選択UIを提供するスクリプト。
 */
(() => {
    'use strict';

    /** ピル選択UIのルート要素。 */
    const picker = document.querySelector('.industry-picker');
    if (!picker) {
        return;
    }

    /** カテゴリタブを表示する領域。 */
    const tabsEl = document.getElementById('admin-cat-tabs');
    /** 選択中カテゴリ内の業種ピルを表示する領域。 */
    const optionsEl = document.getElementById('admin-subcat-panel');
    /** 選択済み業種のタグ一覧を表示する領域。 */
    const tagsEl = document.getElementById('admin-selected-tags');
    /** 選択件数バッジ。 */
    const countEl = document.getElementById('admin-industry-count');
    /** フォーム送信用のhidden inputを配置する領域。 */
    const inputsEl = document.getElementById('admin-industry-inputs');
    /** 送信するフォームパラメーター名。 */
    const inputName = picker.dataset.inputName || 'industries';
    /** 本登録済みの事業主・オーナーなど1件以上の選択が必須かどうか。 */
    const requireSelection = picker.dataset.required === 'true';

    /**
     * JSON文字列を安全に配列として解析する。
     * @param {string | undefined} raw JSON文字列。
     * @returns {Array} 解析結果の配列。失敗時は空配列。
     */
    function parseJsonArray(raw) {
        try {
            const parsed = JSON.parse(raw || '[]');
            return Array.isArray(parsed) ? parsed : [];
        } catch (_) {
            return [];
        }
    }

    /** サーバーから受け取ったカテゴリ・業種の階層データ。 */
    const taxonomy = parseJsonArray(picker.dataset.taxonomy);
    /** すべての業種を平坦化した一覧。 */
    const industries = taxonomy.flatMap(category => Array.isArray(category.subcategories) ? category.subcategories : []);
    /** 業種IDから表示名への対応マップ。 */
    const labelsById = new Map(industries.map(industry => [String(industry.id), industry.name]));
    /** 初期選択済みの業種ID集合。 */
    const selectedIds = new Set(
        parseJsonArray(picker.dataset.selected)
            .map(value => String(value))
            .filter(value => /^[0-9]+$/.test(value))
    );

    if (taxonomy.length === 0) {
        const empty = document.createElement('p');
        empty.className = 'industry-picker-hint';
        empty.textContent = '選択できる業種がありません。';
        optionsEl.replaceChildren(empty);
        return;
    }

    /** 現在表示中の大分類カテゴリ。 */
    let activeCategory = taxonomy[0];

    /**
     * 指定テキストとクラス名を持つspan要素を生成する。
     * @param {string | number} text 表示テキスト。
     * @param {string} [className] CSSクラス名。
     * @returns {HTMLSpanElement} 生成したspan要素。
     */
    function createSpan(text, className = '') {
        const element = document.createElement('span');
        element.textContent = String(text);
        if (className) {
            element.className = className;
        }
        return element;
    }

    /**
     * クリックハンドラー付きの非送信button要素を生成する。
     * @param {string} className CSSクラス名。
     * @param {() => void} onClick クリック時の処理。
     * @returns {HTMLButtonElement} 生成したbutton要素。
     */
    function createButton(className, onClick) {
        const element = document.createElement('button');
        element.type = 'button';
        element.className = className;
        element.addEventListener('click', onClick);
        return element;
    }

    /**
     * 業種ピル1件分のボタンを生成する。
     * @param {string} label 表示ラベル。
     * @param {boolean} selected 選択中ならtrue。
     * @param {() => void} onClick クリック時の処理。
     * @returns {HTMLButtonElement} 生成したピルボタン。
     */
    function createPill(label, selected, onClick) {
        const element = createButton('ind-pill', onClick);
        element.classList.toggle('is-selected', selected);
        element.setAttribute('aria-pressed', selected ? 'true' : 'false');
        element.append(createSpan(selected ? '✓' : '+', 'ind-pill-icon'), createSpan(label));
        return element;
    }

    /**
     * 大分類カテゴリタブとカテゴリ別の選択件数バッジを描画する。
     */
    function renderTabs() {
        tabsEl.replaceChildren(...taxonomy.map(category => {
            const tab = createButton('cat-tab-btn', () => {
                activeCategory = category;
                renderTabs();
                renderOptions();
            });
            const isActive = category === activeCategory;
            tab.classList.toggle('is-active', isActive);
            tab.setAttribute('role', 'tab');
            tab.setAttribute('aria-selected', isActive ? 'true' : 'false');
            tab.append(createSpan(category.name));
            const subcats = Array.isArray(category.subcategories) ? category.subcategories : [];
            const selectedCount = subcats.filter(industry => selectedIds.has(String(industry.id))).length;
            if (selectedCount > 0) {
                tab.append(createSpan(selectedCount, 'cat-tab-badge'));
            }
            return tab;
        }));
    }

    /**
     * 選択中カテゴリに属する業種ピルと「すべて選択」ボタンを描画する。
     */
    function renderOptions() {
        const choices = Array.isArray(activeCategory.subcategories) ? activeCategory.subcategories : [];
        const allSelected = choices.length > 0 && choices.every(industry => selectedIds.has(String(industry.id)));
        const selectAll = createPill(allSelected ? 'すべて選択中' : 'すべて選択', allSelected, () => {
            for (const industry of choices) {
                const id = String(industry.id);
                if (allSelected) {
                    selectedIds.delete(id);
                } else {
                    selectedIds.add(id);
                }
            }
            syncSelection();
        });
        selectAll.classList.add('ind-pill-all');

        optionsEl.replaceChildren(selectAll, ...choices.map(industry => {
            const id = String(industry.id);
            const selected = selectedIds.has(id);
            return createPill(industry.name, selected, () => {
                if (selected) {
                    selectedIds.delete(id);
                } else {
                    selectedIds.add(id);
                }
                syncSelection();
            });
        }));
    }

    /**
     * 選択済み業種のID一覧をマスター表示順で返す。
     * @returns {string[]} 表示順に整列した業種ID配列。
     */
    function orderedSelectedIds() {
        const ordered = [...labelsById.keys()].filter(id => selectedIds.has(id));
        // マスター一覧に含まれない既存選択IDも末尾に残し、解除操作できるようにする。
        for (const id of selectedIds) {
            if (!labelsById.has(id)) {
                ordered.push(id);
            }
        }
        return ordered;
    }

    /**
     * 選択中の業種タグと「すべて解除」ボタンを描画する。
     */
    function renderTags() {
        if (selectedIds.size === 0) {
            tagsEl.replaceChildren(createSpan('※下のタブからジャンルを選んでください（複数選択可）', 'industry-picker-hint'));
            return;
        }

        const list = document.createElement('div');
        list.className = 'selected-tags-list';
        for (const id of orderedSelectedIds()) {
            const name = labelsById.get(id) ?? ('ID: ' + id);
            const tag = createSpan('', 'selected-tag');
            const remove = createButton('selected-tag-del', () => {
                selectedIds.delete(id);
                syncSelection();
            });
            remove.textContent = '×';
            remove.title = name + 'を解除';
            remove.setAttribute('aria-label', name + 'を解除');
            tag.append(createSpan(name), remove);
            list.append(tag);
        }
        const clear = createButton('clear-all-btn', () => {
            selectedIds.clear();
            syncSelection();
        });
        clear.textContent = 'すべて解除';
        list.append(clear);
        tagsEl.replaceChildren(list);
    }

    /**
     * フォーム送信用のhidden input要素群を現在の選択状態に合わせて再構築する。
     */
    function syncInputs() {
        const fragment = document.createDocumentFragment();
        for (const id of orderedSelectedIds()) {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = inputName;
            input.value = id;
            fragment.append(input);
        }
        inputsEl.replaceChildren(fragment);
    }

    /**
     * 件数表示・隠し入力・タブ・ピル・選択タグをすべて最新状態へ同期する。
     */
    function syncSelection() {
        countEl.textContent = '選択中: ' + selectedIds.size + '件';
        syncInputs();
        renderTabs();
        renderOptions();
        renderTags();
    }

    if (requireSelection) {
        const parentForm = picker.closest('form');
        if (parentForm) {
            parentForm.addEventListener('submit', event => {
                if (selectedIds.size === 0) {
                    alert('本登録済みの事業主・物件オーナーの希望業種は1件以上選択してください。');
                    event.preventDefault();
                }
            });
        }
    }

    syncSelection();
})();
