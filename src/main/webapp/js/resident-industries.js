(() => {
  'use strict';

  const DEFAULT_RESIDENT_TAXONOMY = [
    {
      id: "C01", name: "飲食",
      subcategories: [
        { id: "I0101", name: "カフェ・喫茶店" },
        { id: "I0102", name: "レストラン" },
        { id: "I0103", name: "ラーメン・麺類" },
        { id: "I0104", name: "定食・食堂" },
        { id: "I0105", name: "居酒屋・バー" },
        { id: "I0106", name: "弁当・テイクアウト" },
        { id: "I0107", name: "パン屋・ベーカリー" }
      ]
    },
    {
      id: "C02", name: "小売",
      subcategories: [
        { id: "I0201", name: "コンビニ・小型スーパー" },
        { id: "I0202", name: "食料品・生鮮店" },
        { id: "I0203", name: "雑貨・生活用品店" },
        { id: "I0204", name: "衣料品・セレクトショップ" },
        { id: "I0205", name: "書店・文具店" },
        { id: "I0206", name: "花屋・フラワーショップ" }
      ]
    },
    {
      id: "C03", name: "美容・健康",
      subcategories: [
        { id: "I0301", name: "美容室・ヘアサロン" },
        { id: "I0302", name: "理容室・バーバー" },
        { id: "I0303", name: "ネイル・エステサロン" },
        { id: "I0304", name: "ジム・フィットネススタジオ" }
      ]
    },
    {
      id: "C04", name: "教育・仕事",
      subcategories: [
        { id: "I0401", name: "学習塾・スクール" },
        { id: "I0402", name: "コワーキングスペース" }
      ]
    },
    {
      id: "C05", name: "趣味・娯楽",
      subcategories: [
        { id: "I0501", name: "ゲーム・ホビーショップ" },
        { id: "I0502", name: "娯楽施設" },
        { id: "I0503", name: "文化・体験教室" }
      ]
    },
    {
      id: "C06", name: "生活サービス",
      subcategories: [
        { id: "I0601", name: "クリーニング店" },
        { id: "I0602", name: "修理サービス" },
        { id: "I0603", name: "ペット関連店舗" }
      ]
    },
    {
      id: "C07", name: "医療・福祉",
      subcategories: [
        { id: "I0701", name: "クリニック" },
        { id: "I0702", name: "福祉・介護サービス" }
      ]
    }
  ];

  const picker = document.querySelector('.resident-industry-picker');
  if (!picker) return;

  const tabs = document.getElementById('s18-resident-cat-tabs');
  const options = document.getElementById('s18-resident-subcat-panel');
  const tags = document.getElementById('s18-resident-selected-tags');
  const count = document.getElementById('s18-resident-ind-count');
  const codesInput = document.getElementById('s18-preferred-industries');
  const labelsInput = document.getElementById('s18-industry');

  const serverTaxonomy = JSON.parse(picker.dataset.taxonomy);
  const taxonomy = serverTaxonomy.length ? serverTaxonomy : DEFAULT_RESIDENT_TAXONOMY;
  const industries = taxonomy.flatMap(category => category.subcategories);
  const labelsByCode = new Map(industries.map(industry => [industry.id, industry.name]));
  const codesByLabel = new Map(industries.map(industry => [industry.name, industry.id]));
  const selectedCodes = restoreSelection(codesInput.value);
  let activeCategory = taxonomy[0];

  function restoreSelection(rawValue) {
    const selected = new Set();
    let saved;
    try {
      saved = JSON.parse(rawValue || '[]');
    } catch (_) {
      return selected;
    }
    if (!Array.isArray(saved) || saved.length > 200) return selected;
    for (const value of saved) {
      if (typeof value !== 'string') continue;
      // 保存済みの業種コードを優先し、旧画面の名称も対応するコードに戻す。
      if (labelsByCode.has(value) || /^I[0-9]{4}$/.test(value)) {
        selected.add(value);
      } else if (codesByLabel.has(value)) {
        selected.add(codesByLabel.get(value));
      }
    }
    return selected;
  }

  function span(text, className = '') {
    const element = document.createElement('span');
    element.textContent = text;
    element.className = className;
    return element;
  }

  function button(className, onClick) {
    const element = document.createElement('button');
    element.type = 'button';
    element.className = className;
    element.addEventListener('click', onClick);
    return element;
  }

  function renderTabs() {
    tabs.replaceChildren(...taxonomy.map(category => {
      const tab = button('resident-cat-tab-btn', () => {
        activeCategory = category;
        renderTabs();
        renderOptions();
      });
      tab.classList.toggle('is-active', category === activeCategory);
      tab.append(span(category.name));
      const selectedCount = category.subcategories.filter(industry => selectedCodes.has(industry.id)).length;
      if (selectedCount > 0) tab.append(span(selectedCount, 'resident-cat-tab-badge'));
      return tab;
    }));
  }

  function pill(label, selected, onClick) {
    const element = button('resident-ind-pill', onClick);
    element.classList.toggle('is-selected', selected);
    element.append(span(selected ? '✓' : '+', 'resident-ind-pill-icon'), span(label));
    return element;
  }

  function renderOptions() {
    const choices = activeCategory.subcategories;
    const allSelected = choices.length > 0 && choices.every(industry => selectedCodes.has(industry.id));
    const selectAll = pill(allSelected ? 'すべて選択中' : 'すべて選択', allSelected, () => {
      for (const industry of choices) {
        if (allSelected) selectedCodes.delete(industry.id);
        else selectedCodes.add(industry.id);
      }
      syncSelection();
    });
    selectAll.classList.add('resident-ind-pill-all');
    options.replaceChildren(selectAll, ...choices.map(industry => {
      const selected = selectedCodes.has(industry.id);
      return pill(industry.name, selected, () => {
        if (selected) selectedCodes.delete(industry.id);
        else selectedCodes.add(industry.id);
        syncSelection();
      });
    }));
  }

  function renderTags() {
    if (selectedCodes.size === 0) {
      const hint = span('※下のタブからジャンルを選んでください（複数選択可）');
      hint.style.cssText = 'font-size:0.75rem; color:var(--text-muted); padding:0.05rem 0.2rem;';
      tags.replaceChildren(hint);
      return;
    }

    const list = document.createElement('div');
    list.style.cssText = 'display:flex; flex-wrap:wrap; gap:0.3rem 0.4rem; align-items:center; width:100%;';
    // マスターにない保存済みコードも表示し、利用者が解除できるようにする。
    const orderedCodes = [...labelsByCode.keys()].filter(code => selectedCodes.has(code));
    orderedCodes.push(...[...selectedCodes].filter(code => !labelsByCode.has(code)));
    for (const code of orderedCodes) {
      const name = labelsByCode.get(code) ?? code;
      const tag = span('', 'resident-selected-tag');
      const remove = button('resident-selected-tag-del', () => {
        selectedCodes.delete(code);
        syncSelection();
      });
      remove.textContent = '×';
      remove.title = name + 'を解除';
      tag.append(span(name), remove);
      list.append(tag);
    }
    const clear = button('resident-clear-all-btn', () => {
      selectedCodes.clear();
      syncSelection();
    });
    clear.textContent = 'すべて解除';
    list.append(clear);
    tags.replaceChildren(list);
  }

  function syncSelection() {
    count.textContent = '選択中: ' + selectedCodes.size + '件';
    codesInput.value = JSON.stringify([...selectedCodes]);
    labelsInput.value = industries.filter(industry => selectedCodes.has(industry.id))
        .map(industry => industry.name).join(', ');
    renderTabs();
    renderOptions();
    renderTags();
  }

  syncSelection();
})();
