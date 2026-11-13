/**
 * マチリク (Machiriku) - マップエンジン
 * Google Maps JavaScript API (カスタム配色 & 賃料バッジピン & InfoWindow連動)
 * ＋ テスト環境向けベクターマップフォールバック対応
 */
window.MachirikuMap = (function() {
  let containerEl = null;
  let onPinClickCallback = null;
  let activePinId = null;
  let currentZoom = 1;
  let panOffset = { x: 0, y: 0 };
  let isDragging = false;
  let dragStart = { x: 0, y: 0 };

  // Google Maps インスタンス & オーバーレイ管理
  let gMap = null;
  let gOverlays = new Map(); // propId -> CustomPinOverlay
  let gClusterOverlays = new Map(); // clusterId -> ClusterBadgeOverlay
  let gSpiderOverlay = null; // SpiderOverlay
  let gSpiderCluster = null; // 目前スパイダー展開中のクラスタオブジェクト
  let gInfoWindow = null;
  let gInfoWindowPropId = null;
  let gClusterTooltip = null;
  let dummyProjectionOverlay = null;
  let currentProperties = [];
  let lastPropertyIdSetKey = '';
  let lastRenderedClusterSignature = '';

  // マチリク専用 落ち着いた配色スタイル (深緑 & オフホワイト)
  const MACHIRIKU_MAP_STYLES = [
    { featureType: "all", elementType: "geometry", stylers: [{ color: "#f4f5f1" }] },
    { featureType: "all", elementType: "labels.text.fill", stylers: [{ color: "#374151" }] },
    { featureType: "all", elementType: "labels.text.stroke", stylers: [{ color: "#ffffff" }, { weight: 2.5 }] },
    { featureType: "poi.park", elementType: "geometry.fill", stylers: [{ color: "#d8ebd9" }] },
    { featureType: "poi.park", elementType: "labels.text.fill", stylers: [{ color: "#2d6a4f" }] },
    { featureType: "water", elementType: "geometry.fill", stylers: [{ color: "#cdeff5" }] },
    { featureType: "road", elementType: "geometry.fill", stylers: [{ color: "#ffffff" }] },
    { featureType: "road", elementType: "geometry.stroke", stylers: [{ color: "#e5e7eb" }] },
    { featureType: "road.highway", elementType: "geometry.fill", stylers: [{ color: "#fef3c7" }] },
    { featureType: "road.highway", elementType: "geometry.stroke", stylers: [{ color: "#fde68a" }] },
    { featureType: "transit.station", elementType: "geometry.fill", stylers: [{ color: "#e0e7ff" }] },
    { featureType: "transit.station", elementType: "labels.text.fill", stylers: [{ color: "#1f2937" }] },
    { featureType: "poi.business", stylers: [{ visibility: "off" }] }
  ];

  const AREA_LABEL_MAP = {
    orion: "オリオン通り",
    odori: "大通り",
    kamagawa: "釜川",
    bamba: "バンバ通り",
    station: "宇都宮駅周辺",
    tobu: "東武宇都宮駅周辺",
    tomatsuri: "戸祭",
    miyukigahara: "御幸ケ原"
  };

  function getClusterAreaName(props) {
    if (!props || props.length === 0) return '';
    const counts = {};
    props.forEach(p => {
      const a = p.area || '';
      counts[a] = (counts[a] || 0) + 1;
    });
    let best = '', maxC = 0;
    for (const [a, c] of Object.entries(counts)) {
      if (c > maxC) { maxC = c; best = a; }
    }
    if (maxC >= Math.ceil(props.length / 2) && AREA_LABEL_MAP[best]) {
      return AREA_LABEL_MAP[best];
    }
    return '';
  }

  function isMapped(property) {
    return !property.isOutOfMap && Number.isFinite(property.mapCoords?.x) && Number.isFinite(property.mapCoords?.y);
  }

  function rangeNote(properties) {
    const mapped = properties.filter(isMapped).length;
    const outside = properties.length - mapped;
    return `検索結果${properties.length}件のうち${mapped}件を中心街の模式図に表示。${outside ? `範囲外の${outside}件も一覧に表示しています。` : ''}ピンはおおよその位置です。`;
  }

  // ---------------------------------------------------------------------------
  // Google Maps API 実装 (クラスタリング・Spiderfier・賃料ピン & InfoWindow)
  // ---------------------------------------------------------------------------

  function createDummyProjectionOverlayClass() {
    if (typeof google === 'undefined' || !google.maps || !google.maps.OverlayView) return null;
    return class DummyProjectionOverlay extends google.maps.OverlayView {
      onAdd() {}
      draw() {}
      onRemove() {}
    };
  }

  function createCustomPinOverlayClass() {
    if (typeof google === 'undefined' || !google.maps || !google.maps.OverlayView) return null;

    return class CustomPinOverlay extends google.maps.OverlayView {
      constructor(prop, map, onClick, offset = { x: 0, y: 0 }, isSpider = false) {
        super();
        this.prop = prop;
        this.map = map;
        this.onClick = onClick;
        this.offset = offset;
        this.isSpider = isSpider;
        this.div = null;
        this.setMap(map);
      }

      onAdd() {
        const div = document.createElement('div');
        div.className = 'gmap-custom-pin' + (this.isSpider ? ' is-spider' : '');
        div.setAttribute('data-id', this.prop.id);
        div.setAttribute('role', 'button');
        div.setAttribute('tabindex', '0');
        div.setAttribute('aria-label', `${this.prop.title}、賃料${(this.prop.rent/10000).toFixed(1)}万円`);

        const topDemand = this.prop.topDemands && this.prop.topDemands[0];
        const catColor = topDemand ? (window.MACHIRIKU_DATA?.SUBCATS?.[topDemand.subcatId]?.color || "#2d6a4f") : "#2d6a4f";
        const rentMan = (this.prop.rent / 10000).toLocaleString('ja-JP', { maximumFractionDigits: 1 });

        div.innerHTML = `
          <div class="gmap-pin-badge">
            <span class="gmap-pin-rent">¥${rentMan}万</span>
            ${this.prop.isPr ? '<span class="gmap-pin-pr">PR</span>' : ''}
          </div>
          <div class="gmap-pin-anchor" style="--pin-cat-color: ${catColor};">
            <span class="gmap-pin-dot"></span>
          </div>
        `;

        div.addEventListener('click', (e) => {
          e.stopPropagation();
          if (this.onClick) this.onClick(this.prop, this);
        });

        div.addEventListener('keydown', (e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            e.stopPropagation();
            if (this.onClick) this.onClick(this.prop, this);
          }
        });

        this.div = div;
        const panes = this.getPanes();
        if (panes && panes.overlayMouseTarget) {
          panes.overlayMouseTarget.appendChild(div);
        }
      }

      draw() {
        if (!this.div) return;
        const overlayProjection = this.getProjection();
        if (!overlayProjection) return;

        const pos = new google.maps.LatLng(this.prop.location.lat, this.prop.location.lng);
        const point = overlayProjection.fromLatLngToDivPixel(pos);
        if (point) {
          this.div.style.left = (point.x + (this.offset.x || 0)) + 'px';
          this.div.style.top = (point.y + (this.offset.y || 0)) + 'px';
        }
      }

      onRemove() {
        if (this.div) {
          this.div.parentNode?.removeChild(this.div);
          this.div = null;
        }
      }

      getPosition() {
        return new google.maps.LatLng(this.prop.location.lat, this.prop.location.lng);
      }

      setActive(isActive) {
        if (!this.div) return;
        this.div.classList.toggle('is-active', Boolean(isActive));
      }
    };
  }

  function createClusterBadgeOverlayClass() {
    if (typeof google === 'undefined' || !google.maps || !google.maps.OverlayView) return null;

    return class ClusterBadgeOverlay extends google.maps.OverlayView {
      constructor(cluster, map, onClick, onHover, onLeave) {
        super();
        this.cluster = cluster;
        this.map = map;
        this.onClick = onClick;
        this.onHover = onHover;
        this.onLeave = onLeave;
        this.div = null;
        this.setMap(map);
      }

      onAdd() {
        const div = document.createElement('div');
        div.className = 'gmap-cluster-badge';
        div.setAttribute('data-cluster-id', this.cluster.id);
        div.setAttribute('role', 'button');
        div.setAttribute('tabindex', '0');
        div.setAttribute('aria-label', `この周辺の物件：${this.cluster.items.length}件`);

        const areaName = getClusterAreaName(this.cluster.items);
        const titleText = `${areaName ? areaName + '周辺・' : 'この周辺の物件：'}${this.cluster.items.length}件`;
        div.setAttribute('title', titleText);

        div.innerHTML = `<span class="cluster-badge-count">${this.cluster.items.length}</span>`;

        div.addEventListener('click', (e) => {
          e.stopPropagation();
          if (this.onClick) this.onClick(this.cluster, this);
        });

        div.addEventListener('keydown', (e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            e.stopPropagation();
            if (this.onClick) this.onClick(this.cluster, this);
          }
        });

        div.addEventListener('mouseenter', () => {
          if (this.onHover) this.onHover(this.cluster, this);
        });

        div.addEventListener('mouseleave', () => {
          if (this.onLeave) this.onLeave(this.cluster, this);
        });

        this.div = div;
        const panes = this.getPanes();
        if (panes && panes.overlayMouseTarget) {
          panes.overlayMouseTarget.appendChild(div);
        }
      }

      draw() {
        if (!this.div) return;
        const overlayProjection = this.getProjection();
        if (!overlayProjection) return;

        const point = overlayProjection.fromLatLngToDivPixel(this.cluster.centerLatLng);
        if (point) {
          this.div.style.left = point.x + 'px';
          this.div.style.top = point.y + 'px';
        }
      }

      onRemove() {
        if (this.div) {
          this.div.parentNode?.removeChild(this.div);
          this.div = null;
        }
      }

      getPosition() {
        return this.cluster.centerLatLng;
      }

      setHighlighted(isHighlighted) {
        if (!this.div) return;
        this.div.classList.toggle('is-highlighted', Boolean(isHighlighted));
      }
    };
  }

  function createSpiderOverlayClass() {
    if (typeof google === 'undefined' || !google.maps || !google.maps.OverlayView) return null;

    return class SpiderOverlay extends google.maps.OverlayView {
      constructor(cluster, map, offsets) {
        super();
        this.cluster = cluster;
        this.map = map;
        this.offsets = offsets;
        this.div = null;
        this.setMap(map);
      }

      onAdd() {
        const div = document.createElement('div');
        div.className = 'gmap-spider-overlay';

        let linesHtml = '';
        this.offsets.forEach(off => {
          linesHtml += `<line class="gmap-spider-line" x1="0" y1="0" x2="${off.x}" y2="${off.y}" />`;
        });

        div.innerHTML = `
          <svg class="gmap-spider-svg" width="1" height="1" style="overflow: visible;">
            <circle class="gmap-spider-center-dot" cx="0" cy="0" r="5" />
            ${linesHtml}
          </svg>
        `;

        this.div = div;
        const panes = this.getPanes();
        if (panes && panes.overlayLayer) {
          panes.overlayLayer.appendChild(div);
        }
      }

      draw() {
        if (!this.div) return;
        const overlayProjection = this.getProjection();
        if (!overlayProjection) return;

        const point = overlayProjection.fromLatLngToDivPixel(this.cluster.centerLatLng);
        if (point) {
          this.div.style.left = point.x + 'px';
          this.div.style.top = point.y + 'px';
        }
      }

      onRemove() {
        if (this.div) {
          this.div.parentNode?.removeChild(this.div);
          this.div = null;
        }
      }
    };
  }

  // 画面ピクセル＆ラベル表示矩形を考慮したクラスタリング計算
  function computeScreenClusters(properties, projection) {
    if (!projection) {
      return properties.map(p => ({
        id: 'p_' + p.id,
        items: [p],
        centerLatLng: (p.location?.lat && p.location?.lng && typeof google !== 'undefined' && google.maps)
          ? new google.maps.LatLng(p.location.lat, p.location.lng)
          : { lat: () => p.location?.lat, lng: () => p.location?.lng },
        isCluster: false
      }));
    }

    const items = [];
    properties.forEach(p => {
      if (!p.location?.lat || !p.location?.lng) return;
      const latLng = (typeof google !== 'undefined' && google.maps)
        ? new google.maps.LatLng(p.location.lat, p.location.lng)
        : { lat: () => p.location.lat, lng: () => p.location.lng };
      const pt = projection.fromLatLngToDivPixel(latLng);
      if (!pt) return;

      const badgeWidth = p.isPr ? 92 : 72;
      const badgeHeight = 42;
      const padX = 8;
      const padY = 6;

      items.push({
        prop: p,
        latLng,
        pt,
        box: {
          left: pt.x - (badgeWidth / 2) - padX,
          right: pt.x + (badgeWidth / 2) + padX,
          top: pt.y - badgeHeight - padY,
          bottom: pt.y + 4
        }
      });
    });

    if (items.length === 0) return [];

    const parent = items.map((_, i) => i);
    function find(i) {
      if (parent[i] === i) return i;
      parent[i] = find(parent[i]);
      return parent[i];
    }
    function union(i, j) {
      const rootI = find(i);
      const rootJ = find(j);
      if (rootI !== rootJ) parent[rootI] = rootJ;
    }

    for (let i = 0; i < items.length; i++) {
      for (let j = i + 1; j < items.length; j++) {
        const a = items[i].box;
        const b = items[j].box;
        const overlaps = !(a.right < b.left || a.left > b.right || a.bottom < b.top || a.top > b.bottom);
        if (overlaps) {
          union(i, j);
        }
      }
    }

    const groups = new Map();
    for (let i = 0; i < items.length; i++) {
      const root = find(i);
      if (!groups.has(root)) groups.set(root, []);
      groups.get(root).push(items[i]);
    }

    const clusters = [];
    groups.forEach(groupItems => {
      const props = groupItems.map(it => it.prop);
      let sumLat = 0, sumLng = 0;
      groupItems.forEach(it => {
        sumLat += it.prop.location.lat;
        sumLng += it.prop.location.lng;
      });
      const avgLat = sumLat / groupItems.length;
      const avgLng = sumLng / groupItems.length;
      const centerLatLng = (typeof google !== 'undefined' && google.maps)
        ? new google.maps.LatLng(avgLat, avgLng)
        : { lat: () => avgLat, lng: () => avgLng };
      const clusterId = 'c_' + props.map(p => p.id).sort().join('_');

      clusters.push({
        id: clusterId,
        items: props,
        centerLatLng,
        isCluster: props.length > 1
      });
    });

    return clusters;
  }

  function showClusterTooltip(cluster, overlay) {
    if (!gClusterTooltip && containerEl) {
      gClusterTooltip = document.createElement('div');
      gClusterTooltip.className = 'gmap-cluster-tooltip';
      containerEl.appendChild(gClusterTooltip);
    }
    if (!gClusterTooltip) return;

    const areaName = getClusterAreaName(cluster.items);
    gClusterTooltip.textContent = areaName ? `${areaName}周辺・${cluster.items.length}件` : `この周辺の物件：${cluster.items.length}件`;
    gClusterTooltip.style.display = 'block';

    const overlayProjection = overlay.getProjection ? overlay.getProjection() : null;
    if (overlayProjection && containerEl) {
      const point = overlayProjection.fromLatLngToContainerPixel(cluster.centerLatLng);
      if (point) {
        gClusterTooltip.style.left = point.x + 'px';
        gClusterTooltip.style.top = point.y + 'px';
      }
    }
  }

  function hideClusterTooltip() {
    if (gClusterTooltip) {
      gClusterTooltip.style.display = 'none';
    }
  }

  function spiderfyCluster(cluster) {
    collapseSpider();
    gSpiderCluster = cluster;

    const N = cluster.items.length;
    const offsets = [];
    const R = N <= 2 ? 55 : (N <= 4 ? 65 : 75);
    for (let i = 0; i < N; i++) {
      let angle;
      if (N === 2) {
        angle = (i === 0 ? -Math.PI / 4 : (3 * Math.PI) / 4);
      } else {
        angle = (2 * Math.PI * i) / N - Math.PI / 2;
      }
      offsets.push({
        x: Math.round(R * Math.cos(angle)),
        y: Math.round(R * Math.sin(angle))
      });
    }

    const badge = gClusterOverlays.get(cluster.id);
    if (badge) {
      badge.setMap(null);
      gClusterOverlays.delete(cluster.id);
    }

    const SpiderClass = createSpiderOverlayClass();
    if (SpiderClass) {
      gSpiderOverlay = new SpiderClass(cluster, gMap, offsets);
    }

    const PinOverlayClass = createCustomPinOverlayClass();
    if (PinOverlayClass) {
      cluster.items.forEach((p, idx) => {
        const pinOverlay = new PinOverlayClass(p, gMap, (clickedProp, clickedOverlay) => {
          showGoogleInfoWindow(gMap, clickedProp, clickedOverlay);
          setActivePin(clickedProp.id);
          if (onPinClickCallback) onPinClickCallback(clickedProp);
        }, offsets[idx], true);
        gOverlays.set(p.id, pinOverlay);
      });
    }
  }

  function collapseSpider() {
    if (gSpiderOverlay) {
      gSpiderOverlay.setMap(null);
      gSpiderOverlay = null;
    }
    if (gSpiderCluster) {
      gSpiderCluster.items.forEach(p => {
        const pin = gOverlays.get(p.id);
        if (pin && pin.isSpider) {
          pin.setMap(null);
          gOverlays.delete(p.id);
        }
      });
      gSpiderCluster = null;
      lastRenderedClusterSignature = '';
    }
  }

  function clearAllGoogleOverlays() {
    closeGoogleInfoWindow();
    collapseSpider();
    gOverlays.forEach(overlay => overlay.setMap(null));
    gOverlays.clear();
    gClusterOverlays.forEach(clusterOverlay => clusterOverlay.setMap(null));
    gClusterOverlays.clear();
    hideClusterTooltip();
  }

  function handleClusterClick(cluster, clusterOverlay) {
    if (!gMap) return;
    const currentZoom = gMap.getZoom() || 14;
    const MAX_ZOOM = 18;

    let allCoincident = true;
    const firstLoc = cluster.items[0].location;
    for (let i = 1; i < cluster.items.length; i++) {
      const loc = cluster.items[i].location;
      const dLat = Math.abs(loc.lat - firstLoc.lat);
      const dLng = Math.abs(loc.lng - firstLoc.lng);
      if (dLat > 0.00015 || dLng > 0.00015) {
        allCoincident = false;
        break;
      }
    }

    // 要件1: 最大ズームまたは同一座標の場合はSpiderfierへ切り替え
    if (currentZoom >= MAX_ZOOM || allCoincident) {
      spiderfyCluster(cluster);
      return;
    }

    // 確実にズームインする処理
    const bounds = new google.maps.LatLngBounds();
    cluster.items.forEach(p => {
      bounds.extend(new google.maps.LatLng(p.location.lat, p.location.lng));
    });

    const targetZoom = Math.min(MAX_ZOOM, currentZoom + 2);
    gMap.panTo(cluster.centerLatLng);
    gMap.fitBounds(bounds, { top: 70, right: 70, bottom: 70, left: 70 });

    google.maps.event.addListenerOnce(gMap, 'idle', () => {
      if (gMap.getZoom() <= currentZoom) {
        gMap.setZoom(targetZoom);
      }
    });
  }

  function recomputeAndRenderClusters() {
    if (!gMap) return;
    if (!dummyProjectionOverlay) return;
    const projection = dummyProjectionOverlay.getProjection();
    if (!projection) return;

    const validProps = currentProperties.filter(p => p.location?.lat && p.location?.lng);
    if (validProps.length === 0) {
      clearAllGoogleOverlays();
      lastRenderedClusterSignature = '';
      return;
    }

    if (gSpiderCluster) return;

    const clusters = computeScreenClusters(validProps, projection);
    const sig = clusters.map(c => c.id).sort().join('|');
    if (sig === lastRenderedClusterSignature) {
      return; // 構成が変わらない場合は不要なDOM再生成を行わない (要件4)
    }
    lastRenderedClusterSignature = sig;

    clearAllGoogleOverlays();

    const PinOverlayClass = createCustomPinOverlayClass();
    const ClusterBadgeClass = createClusterBadgeOverlayClass();
    if (!PinOverlayClass || !ClusterBadgeClass) return;

    clusters.forEach(cluster => {
      if (cluster.isCluster) {
        const clusterOverlay = new ClusterBadgeClass(
          cluster,
          gMap,
          (clickedCluster, clickedOverlay) => {
            handleClusterClick(clickedCluster, clickedOverlay);
          },
          (hoveredCluster, hoveredOverlay) => {
            showClusterTooltip(hoveredCluster, hoveredOverlay);
          },
          () => {
            hideClusterTooltip();
          }
        );
        gClusterOverlays.set(cluster.id, clusterOverlay);
      } else {
        const prop = cluster.items[0];
        const pinOverlay = new PinOverlayClass(prop, gMap, (clickedProp, clickedOverlay) => {
          showGoogleInfoWindow(gMap, clickedProp, clickedOverlay);
          setActivePin(clickedProp.id);
          if (onPinClickCallback) onPinClickCallback(clickedProp);
        });
        gOverlays.set(prop.id, pinOverlay);
      }
    });
  }

  function closeGoogleInfoWindow() {
    closeMobileMapCard();
    if (gInfoWindow) {
      gInfoWindow.close();
      gInfoWindow = null;
      gInfoWindowPropId = null;
    }
  }

  
  function isMobileViewport() {
    return (typeof window !== 'undefined' && (window.innerWidth <= 768 || ('ontouchstart' in window && window.innerWidth <= 1024)));
  }

  function closeMobileMapCard() {
    const card = document.getElementById('gmap-mobile-preview-card');
    if (card) {
      card.style.display = 'none';
    }
  }

  function showMobileMapCard(prop) {
    if (!containerEl) return;

    let card = document.getElementById('gmap-mobile-preview-card');
    if (!card) {
      card = document.createElement('div');
      card.id = 'gmap-mobile-preview-card';
      card.className = 'gmap-mobile-preview-card';
      containerEl.appendChild(card);
    }

    const rentMan = (prop.rent / 10000).toLocaleString('ja-JP', { maximumFractionDigits: 1 });
    const mgmt = prop.managementFee == null ? '管理費等なし' : `管 ${Number(prop.managementFee).toLocaleString('ja-JP')}円`;
    const topDemand = prop.topDemands && prop.topDemands[0];
    const topDemandLabel = topDemand ? `${topDemand.label} (${topDemand.votes}票 / ${topDemand.score}pt)` : "需要集計中";

    card.innerHTML = `
      <div class="map-mobile-card-inner">
        <button type="button" class="map-mobile-card-close" onclick="event.stopPropagation(); MachirikuMap.closeMobileMapCard(); return false;" aria-label="閉じる">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </button>
        <a href="#s02?id=${prop.id}" class="map-mobile-card-link">
          <div class="map-mobile-card-thumb">
            <img src="${prop.coverImage || 'assets/property-placeholder.svg'}" alt="${prop.title}">
            <span class="map-mobile-card-badge">${prop.statusLabel}</span>
            ${prop.isPr ? '<span class="map-mobile-card-pr">PR</span>' : ''}
          </div>
          <div class="map-mobile-card-info">
            <h4 class="map-mobile-card-title">${prop.title}</h4>
            <div class="map-mobile-card-rent"><strong>${rentMan}万円</strong><small>/月</small> <span class="map-mobile-card-mgmt">${mgmt}</span></div>
            <div class="map-mobile-card-specs">${prop.areaTsubo}坪 (${prop.areaSqm}㎡) ・ ${prop.floor}</div>
            <div class="map-mobile-card-demand">最多需要: <strong>${topDemandLabel}</strong></div>
          </div>
        </a>
        <a href="#s02?id=${prop.id}" class="map-mobile-card-action">物件詳細を見る</a>
      </div>
    `;

    card.style.display = 'block';

    const closeBtn = card.querySelector('.map-mobile-card-close');
    if (closeBtn) {
      const handleClose = (e) => {
        if (e) {
          e.preventDefault();
          e.stopPropagation();
        }
        closeMobileMapCard();
      };
      closeBtn.onclick = handleClose;
      closeBtn.addEventListener('touchend', handleClose, { passive: false });
    }

    if (gMap) {
      gMap.panBy(0, -25);
    }
  }

  function showGoogleInfoWindow(map, prop, anchorOverlay) {
    closeGoogleInfoWindow();

    if (isMobileViewport()) {
      showMobileMapCard(prop);
      return;
    }

    const rentMan = (prop.rent / 10000).toLocaleString('ja-JP', { maximumFractionDigits: 1 });
    const mgmt = prop.managementFee == null ? '管理費等なし' : `管理費 ${Number(prop.managementFee).toLocaleString('ja-JP')}円`;
    const topDemand = prop.topDemands && prop.topDemands[0];
    const topDemandLabel = topDemand ? `${topDemand.label} (${topDemand.votes}票 / ${topDemand.score}pt)` : "需要集計中";

    const contentString = `
      <div class="gmap-info-window">
        <div class="info-window-thumb">
          <img src="${prop.coverImage || 'assets/property-placeholder.svg'}" alt="${prop.title}">
          <span class="info-window-badge">${prop.statusLabel}</span>
          ${prop.isPr ? '<span class="info-window-pr">PR枠</span>' : ''}
        </div>
        <div class="info-window-body">
          <h4 class="info-window-title"><a href="#s02?id=${prop.id}">${prop.title}</a></h4>
          <div class="info-window-rent"><strong>${rentMan}万円</strong><small>/月</small> <span class="info-window-mgmt">${mgmt}</span></div>
          <div class="info-window-specs">${prop.areaTsubo}坪 (${prop.areaSqm}m²) ・ ${prop.floor}</div>
          <div class="info-window-demand">最多需要: <strong>${topDemandLabel}</strong></div>
          <a href="#s02?id=${prop.id}" class="info-window-btn">物件詳細を見る</a>
        </div>
      </div>
    `;

    gInfoWindowPropId = prop.id;
    gInfoWindow = new google.maps.InfoWindow({
      content: contentString,
      position: anchorOverlay.getPosition(),
      pixelOffset: new google.maps.Size(0, -38),
      maxWidth: 280
    });

    gInfoWindow.addListener('closeclick', () => {
      gInfoWindow = null;
      gInfoWindowPropId = null;
      setActivePin(null);
    });

    gInfoWindow.open(map);
  }

  function initGoogleMapsMode(container, properties) {
    containerEl = typeof container === 'string' ? document.querySelector(container) : container;
    currentProperties = properties;

    let canvas = containerEl.querySelector('#google-map-canvas');
    if (!canvas) {
      canvas = document.createElement('div');
      canvas.id = 'google-map-canvas';
      canvas.className = 'google-map-canvas';
      canvas.style.width = '100%';
      canvas.style.height = '100%';
      containerEl.innerHTML = '';
      containerEl.appendChild(canvas);
    }

    gMap = new google.maps.Map(canvas, {
      center: { lat: 36.559, lng: 139.883 },
      zoom: 14,
      styles: MACHIRIKU_MAP_STYLES,
      mapTypeControl: false,
      streetViewControl: false,
      fullscreenControl: false,
      zoomControl: true,
      gestureHandling: 'greedy',
      scrollwheel: true
    });

    const DummyProjClass = createDummyProjectionOverlayClass();
    if (DummyProjClass) {
      dummyProjectionOverlay = new DummyProjClass();
      dummyProjectionOverlay.setMap(gMap);
    }

    const newIdSetKey = properties.map(p => p.id).sort().join(',');
    lastPropertyIdSetKey = newIdSetKey;

    updateGoogleMarkers(properties, true);

    gMap.addListener('click', () => {
      if (gInfoWindow) {
        gInfoWindow.close();
        gInfoWindow = null;
      }
      collapseSpider();
      setActivePin(null);
      recomputeAndRenderClusters();
    });

    gMap.addListener('zoom_changed', () => {
      if (gSpiderCluster) {
        collapseSpider();
      }
    });

    // 要件4: 地図操作が落ち着いたタイミング（idle）でクラスタ再計算
    gMap.addListener('idle', () => {
      recomputeAndRenderClusters();
    });
  }

  function updateGoogleMarkers(properties, propertySetChanged = false) {
    if (!gMap) return;

    currentProperties = properties;

    // 検索結果が0件、または現在情報ウィンドウが開いている物件が検索対象から外れた場合はウィンドウを閉じる
    if (properties.length === 0 || (gInfoWindowPropId && !properties.some(p => p.id === gInfoWindowPropId))) {
      closeGoogleInfoWindow();
      if (activePinId === gInfoWindowPropId) {
        setActivePin(null);
      }
    }

    if (propertySetChanged) {
      collapseSpider();
      lastRenderedClusterSignature = '';

      const validProps = properties.filter(p => p.location?.lat && p.location?.lng);
      if (validProps.length > 0) {
        const bounds = new google.maps.LatLngBounds();
        validProps.forEach(p => {
          bounds.extend(new google.maps.LatLng(p.location.lat, p.location.lng));
        });
        gMap.fitBounds(bounds, { top: 50, right: 50, bottom: 50, left: 50 });
        const listener = google.maps.event.addListener(gMap, "idle", () => {
          if (gMap.getZoom() > 15) gMap.setZoom(15);
          google.maps.event.removeListener(listener);
          recomputeAndRenderClusters();
        });
      } else {
        clearAllGoogleOverlays();
      }
    } else {
      // 並び替えのみ（物件セット不変）: 中心位置・ズームレベルを維持
      recomputeAndRenderClusters();
    }
  }

  function panToLocation(lat, lng, zoom = 14) {
    if (gMap && typeof google !== 'undefined' && google.maps) {
      gMap.panTo(new google.maps.LatLng(lat, lng));
      if (zoom) gMap.setZoom(zoom);
    }
  }

  // ---------------------------------------------------------------------------
  // テスト環境 & フォールバック用: SVG ベクターマップ実装
  // ---------------------------------------------------------------------------

  function renderPins(properties) {
    return properties.filter(isMapped).map((p, idx) => {
      const topDemand = p.topDemands && p.topDemands[0];
      const catColor = topDemand ? (window.MACHIRIKU_DATA.SUBCATS[topDemand.subcatId]?.color || "#2d6a4f") : "#2d6a4f";
      const rentMan = (p.rent / 10000).toLocaleString('ja-JP', { maximumFractionDigits: 4 });
      const isPr = p.isPr ? `<span class="map-pin-pr">PR</span>` : '';
      const isActive = p.id === activePinId ? 'is-active' : '';

      return `
        <g class="map-marker ${isActive}" data-id="${p.id}" role="button" tabindex="0" aria-label="${p.title}、賃料${rentMan}万円。一覧で確認" aria-pressed="${p.id === activePinId}" transform="translate(${p.mapCoords.x}, ${p.mapCoords.y})">
          <circle cx="0" cy="-16" r="16" fill="transparent" stroke="transparent" stroke-width="48" vector-effect="non-scaling-stroke" pointer-events="all"></circle>
          <circle class="pin-halo" r="28" fill="${catColor}" opacity="0.18"></circle>
          <path class="pin-base" d="M0 0 L-14 -24 A16 16 0 1 1 14 -24 Z" fill="${catColor}" stroke="#ffffff" stroke-width="2.5" filter="drop-shadow(0 3px 5px rgba(0,0,0,0.25))"></path>
          <circle cx="0" cy="-24" r="5.5" fill="#ffffff"></circle>
          <g class="pin-badge" transform="translate(0, 14)">
            <rect x="-36" y="-8" width="72" height="18" rx="9" fill="#18201c" stroke="#ffffff" stroke-width="1.5" filter="drop-shadow(0 2px 4px rgba(0,0,0,0.2))"></rect>
            <text x="0" y="4" text-anchor="middle" font-size="10" font-weight="700" fill="#ffffff" letter-spacing="-0.3">${rentMan}万</text>
          </g>
          ${isPr ? `
          <g transform="translate(14, -36)">
            <rect x="-10" y="-6" width="20" height="13" rx="3" fill="#eab308"></rect>
            <text x="0" y="4" text-anchor="middle" font-size="8" font-weight="800" fill="#18201c">PR</text>
          </g>` : ''}
        </g>
      `;
    }).join("");
  }

  function renderSvgMap(properties) {
    const pinsHtml = renderPins(properties);
    return `
      <svg id="utsunomiya-vector-map" viewBox="0 0 1000 550" class="map-svg-root" preserveAspectRatio="xMidYMid slice">
        <defs>
          <linearGradient id="kamagawaGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#bbf2f6" />
            <stop offset="100%" stop-color="#70d6e4" />
          </linearGradient>
          <pattern id="arcadePattern" width="12" height="12" patternUnits="userSpaceOnUse">
            <line x1="0" y1="0" x2="12" y2="12" stroke="#d5bdaf" stroke-width="1.5" stroke-opacity="0.6"/>
            <line x1="12" y1="0" x2="0" y2="12" stroke="#d5bdaf" stroke-width="1.5" stroke-opacity="0.6"/>
          </pattern>
        </defs>

        <rect width="1000" height="550" fill="#f4f5f1" />

        <g id="map-viewport" transform="translate(${panOffset.x}, ${panOffset.y}) scale(${currentZoom})">
          <path d="M 420 440 Q 480 430 520 460 Q 500 510 440 510 Z" fill="#d8ebd9" stroke="#b2d6b5" stroke-width="1.5" />
          <text x="470" y="475" font-size="11" font-weight="600" fill="#4a7c59" text-anchor="middle">宇都宮城址公園</text>

          <path d="M 280 10 Q 360 30 450 15 Q 480 70 320 80 Z" fill="#d8ebd9" stroke="#b2d6b5" stroke-width="1.5" />
          <text x="370" y="50" font-size="11" font-weight="600" fill="#4a7c59" text-anchor="middle">八幡山公園 方面</text>

          <g fill="#ffffff" stroke="#e3e5de" stroke-width="1">
            <rect x="180" y="140" width="120" height="80" rx="3" />
            <rect x="330" y="140" width="100" height="80" rx="3" />
            <rect x="460" y="140" width="110" height="80" rx="3" />
            <rect x="600" y="140" width="110" height="80" rx="3" />
            <rect x="740" y="140" width="120" height="80" rx="3" />

            <rect x="180" y="270" width="110" height="90" rx="3" />
            <rect x="320" y="270" width="120" height="90" rx="3" />
            <rect x="470" y="270" width="100" height="90" rx="3" />
            <rect x="600" y="270" width="120" height="90" rx="3" />
            <rect x="750" y="270" width="110" height="90" rx="3" />

            <rect x="200" y="400" width="100" height="70" rx="3" />
            <rect x="330" y="400" width="110" height="70" rx="3" />
            <rect x="580" y="400" width="130" height="70" rx="3" />
          </g>

          <path d="M 520 80 Q 500 180 480 240 T 510 340 T 460 430 T 490 540" 
                fill="none" stroke="url(#kamagawaGrad)" stroke-width="12" stroke-linecap="round" stroke-linejoin="round" />
          <path d="M 520 80 Q 500 180 480 240 T 510 340 T 460 430 T 490 540" 
                fill="none" stroke="#ffffff" stroke-width="2" stroke-dasharray="8 6" opacity="0.6"/>
          <text x="525" y="310" font-size="10" font-weight="700" fill="#2b8a96" transform="rotate(75, 525, 310)">釜川プロムナード</text>

          <line x1="80" y1="235" x2="940" y2="235" stroke="#f1e0c6" stroke-width="26" stroke-linecap="round" />
          <line x1="80" y1="235" x2="940" y2="235" stroke="#ffffff" stroke-width="18" stroke-linecap="round" />
          <line x1="80" y1="235" x2="940" y2="235" stroke="#e0c7a5" stroke-width="1" stroke-dasharray="10 8" />
          <text x="560" y="239" font-size="12" font-weight="800" fill="#8c6d48" text-anchor="middle" letter-spacing="2">大 通 り（県道1号）</text>

          <g transform="translate(100, 200)">
            <rect x="0" y="0" width="65" height="110" rx="6" fill="#e9ecef" stroke="#ced4da" stroke-width="2" />
            <rect x="6" y="8" width="53" height="40" rx="4" fill="#1b4332" />
            <text x="32" y="26" font-size="9" font-weight="700" fill="#ffffff" text-anchor="middle">東武宇都宮駅</text>
            <text x="32" y="39" font-size="8" font-weight="600" fill="#a7f3d0" text-anchor="middle">東武百貨店</text>
            <line x1="15" y1="58" x2="15" y2="105" stroke="#6c757d" stroke-width="3" />
            <line x1="25" y1="58" x2="25" y2="105" stroke="#6c757d" stroke-width="3" />
            <line x1="10" y1="65" x2="30" y2="65" stroke="#adb5bd" stroke-width="2" />
            <line x1="10" y1="80" x2="30" y2="80" stroke="#adb5bd" stroke-width="2" />
            <line x1="10" y1="95" x2="30" y2="95" stroke="#adb5bd" stroke-width="2" />
          </g>

          <g transform="translate(870, 160)">
            <rect x="0" y="0" width="75" height="160" rx="8" fill="#e9ecef" stroke="#ced4da" stroke-width="2" />
            <rect x="8" y="12" width="60" height="50" rx="4" fill="#0f172a" />
            <text x="38" y="32" font-size="10" font-weight="800" fill="#ffffff" text-anchor="middle">JR宇都宮駅</text>
            <text x="38" y="48" font-size="8" font-weight="600" fill="#94a3b8" text-anchor="middle">新幹線・在来線</text>
            <circle cx="-15" cy="75" r="28" fill="#e2e8f0" stroke="#cbd5e1" stroke-width="1.5" />
            <text x="-15" y="78" font-size="8" font-weight="700" fill="#64748b" text-anchor="middle">西口広場</text>
          </g>

          <g>
            <path d="M 210 320 L 520 320" stroke="url(#arcadePattern)" stroke-width="18" stroke-linecap="square" />
            <path d="M 210 320 L 520 320" stroke="#c97a3e" stroke-width="2" stroke-linecap="round" opacity="0.8" />
            <rect x="230" y="312" width="110" height="16" rx="4" fill="#ffffff" stroke="#c97a3e" stroke-width="1.5" />
            <text x="285" y="324" font-size="9" font-weight="800" fill="#c97a3e" text-anchor="middle">オリオン通り（アーケード街）</text>
          </g>

          <g>
            <path d="M 170 380 L 360 380" stroke="#cbd5e1" stroke-width="14" stroke-linecap="round" />
            <text x="250" y="384" font-size="9" font-weight="700" fill="#475569" text-anchor="middle">ユニオン通り（個性的ショップ街）</text>
          </g>

          <line x1="120" y1="480" x2="880" y2="480" stroke="#ffffff" stroke-width="12" stroke-linecap="round" />
          <text x="300" y="484" font-size="9" font-weight="600" fill="#94a3b8">いちょう通り</text>

          <line x1="390" y1="100" x2="390" y2="460" stroke="#ffffff" stroke-width="10" />
          <line x1="660" y1="100" x2="660" y2="460" stroke="#ffffff" stroke-width="10" />

          <g transform="translate(420, 155)">
            <rect x="-8" y="-6" width="90" height="20" rx="4" fill="#fef2f2" stroke="#fca5a5" stroke-width="1"/>
            <text x="37" y="8" font-size="9" font-weight="700" fill="#b91c1c" text-anchor="middle">宇都宮二荒山神社</text>
          </g>

          <g id="map-pins-layer">
            ${pinsHtml}
          </g>
        </g>
      </svg>
    `;
  }

  function init(containerSelector, properties, onPinClick) {
    containerEl = document.querySelector(containerSelector);
    if (!containerEl) return;
    onPinClickCallback = onPinClick;

    // Google Maps API が利用可能な場合
    if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
      initGoogleMapsMode(containerEl, properties);
      return;
    }

    // Google Maps スクリプトが非同期ロード中の場合 (ブラウザ環境)
    if (typeof window !== 'undefined' && typeof setInterval === 'function') {
      let checks = 0;
      const interval = setInterval(() => {
        checks++;
        if (typeof google !== 'undefined' && google.maps && google.maps.Map) {
          clearInterval(interval);
          if (containerEl && (containerEl.id === 'portal-map-pane' || document.body.contains(containerEl))) {
            initGoogleMapsMode(containerEl, properties);
          }
        }
        if (checks > 50) clearInterval(interval);
      }, 100);
    }

    // テスト環境または非同期ロード完了前のフォールバック: SVGベクターマップ
    render(properties);
    bindEvents(properties);
  }

  function render(properties) {
    if (!containerEl) return;
    containerEl.innerHTML = `
      <div class="machiriku-map-wrapper">
        <div class="map-controls-bar">
          <details class="map-legend-panel"><summary>業種の色分け</summary><div class="map-legend">
            <span class="legend-item"><span class="legend-dot" style="background:var(--primary-600)"></span>未集計</span>
            ${window.MACHIRIKU_DATA.CATEGORIES.map(c => `<span class="legend-item"><span class="legend-dot" style="background:${c.color}"></span>${c.name}</span>`).join('')}
          </div></details>
          <div class="map-zoom-buttons">
            <button type="button" class="btn-map-control" id="btn-map-zoom-in" title="拡大">＋</button>
            <button type="button" class="btn-map-control" id="btn-map-zoom-out" title="縮小">−</button>
            <button type="button" class="btn-map-control" id="btn-map-reset" title="初期表示">↺</button>
          </div>
        </div>
        <div class="map-svg-container" id="map-svg-container">
          ${renderSvgMap(properties)}
        </div>
        <div class="map-tooltip-card" id="map-tooltip-card" style="display:none;"></div>
        <p class="map-note" id="map-range-note">${rangeNote(properties)}</p>
      </div>
    `;
  }

  function bindEvents(properties) {
    const svgRoot = document.getElementById("utsunomiya-vector-map");
    const container = document.getElementById("map-svg-container");
    const tooltip = document.getElementById("map-tooltip-card");
    if (!svgRoot || !container) return;

    const zoomInBtn = document.getElementById("btn-map-zoom-in");
    const zoomOutBtn = document.getElementById("btn-map-zoom-out");
    const resetBtn = document.getElementById("btn-map-reset");

    if (zoomInBtn) {
      zoomInBtn.addEventListener("click", () => {
        currentZoom = Math.min(currentZoom + 0.25, 2.2);
        updateTransform();
      });
    }
    if (zoomOutBtn) {
      zoomOutBtn.addEventListener("click", () => {
        currentZoom = Math.max(currentZoom - 0.25, 0.8);
        updateTransform();
      });
    }
    if (resetBtn) {
      resetBtn.addEventListener("click", () => {
        currentZoom = 1;
        panOffset = { x: 0, y: 0 };
        updateTransform();
      });
    }

    // マウスホイールによるズーム拡大・縮小
    container.addEventListener("wheel", (e) => {
      e.preventDefault();
      const zoomStep = 0.15;
      if (e.deltaY < 0) {
        currentZoom = Math.min(currentZoom + zoomStep, 2.5);
      } else {
        currentZoom = Math.max(currentZoom - zoomStep, 0.7);
      }
      updateTransform();
    }, { passive: false });

    bindMarkers(properties);
    container.addEventListener("click", hideTooltip);
  }

  function bindMarkers(properties) {
    const container = document.getElementById('map-svg-container');
    if (!container) return;
    const markers = container.querySelectorAll(".map-marker");
    markers.forEach(marker => {
      const propId = marker.getAttribute("data-id");
      const prop = properties.find(p => p.id === propId);
      if (!prop) return;

      marker.addEventListener("click", (e) => {
        e.stopPropagation();
        setActivePin(propId);
        if (onPinClickCallback) onPinClickCallback(prop);
      });

      marker.addEventListener('keydown', e => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          setActivePin(propId);
          if (onPinClickCallback) onPinClickCallback(prop);
        }
      });

      marker.addEventListener("mouseenter", () => showTooltip(prop, marker));
      marker.addEventListener("mouseleave", hideTooltip);
    });
  }

  function updateProperties(properties) {
    if (gMap && typeof google !== 'undefined' && google.maps) {
      const newIdSetKey = properties.map(p => p.id).sort().join(',');
      const propertySetChanged = (newIdSetKey !== lastPropertyIdSetKey);
      lastPropertyIdSetKey = newIdSetKey;
      updateGoogleMarkers(properties, propertySetChanged);
      return;
    }

    const pins = document.getElementById('map-pins-layer');
    if (!pins) return;
    if (!properties.some(p => isMapped(p) && p.id === activePinId)) activePinId = null;
    pins.innerHTML = renderPins(properties);
    const note = document.getElementById('map-range-note');
    if (note) note.textContent = rangeNote(properties);
    hideTooltip();
    bindMarkers(properties);
  }

  function updateTransform() {
    const viewport = document.getElementById("map-viewport");
    if (viewport) {
      viewport.setAttribute("transform", `translate(${panOffset.x}, ${panOffset.y}) scale(${currentZoom})`);
    }
  }

  function showTooltip(prop, markerEl) {
    const tooltip = document.getElementById("map-tooltip-card");
    if (!tooltip) return;
    const topDemand = prop.topDemands && prop.topDemands[0];
    const topDemandLabel = topDemand ? `${topDemand.label} (${topDemand.votes}票 / ${topDemand.score}pt)` : "集計中";
    const rentMan = (prop.rent / 10000).toLocaleString('ja-JP', { maximumFractionDigits: 4 });

    tooltip.innerHTML = `
      <div class="tooltip-header">
        <span class="tooltip-status status-${prop.status}">${prop.statusLabel}</span>
        ${prop.isPr ? '<span class="tooltip-pr">PR枠</span>' : ''}
      </div>
      <h4 class="tooltip-title">${prop.title}</h4>
      <div class="tooltip-meta">
        <span class="tooltip-rent">賃料 <strong>${rentMan}万円</strong>/月</span>
        <span class="tooltip-size">${prop.areaTsubo}坪 (${prop.areaSqm}m²)</span>
      </div>
      <div class="tooltip-demand">
        <span class="demand-tag">最多需要: <strong>${topDemandLabel}</strong></span>
      </div>
    `;

    tooltip.style.display = "block";
    const rect = markerEl.getBoundingClientRect();
    const containerRect = containerEl.getBoundingClientRect();

    let left = rect.left - containerRect.left - 110;
    let top = rect.top - containerRect.top - 145;

    if (left < 10) left = 10;
    if (left + 240 > containerRect.width) left = containerRect.width - 250;
    if (top < 10) top = rect.bottom - containerRect.top + 10;

    tooltip.style.left = `${left}px`;
    tooltip.style.top = `${top}px`;
  }

  function hideTooltip() {
    const tooltip = document.getElementById("map-tooltip-card");
    if (!tooltip) return;
    tooltip.style.display = "none";
  }

  function setActivePin(propId) {
    activePinId = propId;

    // Google Maps ピンの強調 & InfoWindow 表示
    if (gOverlays.size > 0) {
      gOverlays.forEach((overlay, id) => {
        const isActive = (id === propId);
        overlay.setActive(isActive);
        if (isActive && gMap) {
          gMap.panTo(overlay.getPosition());
          showGoogleInfoWindow(gMap, overlay.prop, overlay);
        }
      });
    }

    // もし対象物件がクラスタ内に含まれている場合は、そのクラスタを強調
    if (propId && !gOverlays.has(propId) && gClusterOverlays.size > 0) {
      gClusterOverlays.forEach((overlay) => {
        const hasProp = overlay.cluster && overlay.cluster.items.some(p => p.id === propId);
        overlay.setHighlighted(hasProp);
      });
    }

    // ベクターマップ要素の同期
    const markers = document.querySelectorAll(".map-marker");
    markers.forEach(m => {
      if (m.getAttribute("data-id") === propId) {
        m.classList.add("is-active");
        m.setAttribute('aria-pressed', 'true');
      } else {
        m.classList.remove("is-active");
        m.setAttribute('aria-pressed', 'false');
      }
    });
  }

  function highlightProperty(propId) {
    if (!propId) return;

    // 1. Google Maps 個別ピンが存在する場合
    const pin = gOverlays.get(propId);
    if (pin) {
      pin.setActive(true);
      return;
    }

    // 2. クラスタ内に含まれている場合
    let foundCluster = null;
    let foundOverlay = null;
    gClusterOverlays.forEach((overlay) => {
      if (overlay.cluster && overlay.cluster.items.some(p => p.id === propId)) {
        foundCluster = overlay.cluster;
        foundOverlay = overlay;
      }
    });

    if (foundCluster && foundOverlay) {
      foundOverlay.setHighlighted(true);
      showClusterTooltip(foundCluster, foundOverlay);
      return;
    }

    // 3. SVGベクターマップの場合
    const marker = document.querySelector(`.map-marker[data-id="${propId}"]`);
    if (marker) {
      marker.classList.add("is-active");
    }
  }

  function clearHighlight() {
    // 1. Google Maps ピン: activePinId 以外のハイライトを解除
    gOverlays.forEach((pin, id) => {
      if (id !== activePinId) {
        pin.setActive(false);
      }
    });

    // 2. クラスタ: すべてのハイライトを解除し、ツールチップを非表示
    gClusterOverlays.forEach(overlay => {
      overlay.setHighlighted(false);
    });
    hideClusterTooltip();

    // 3. SVGベクターマップ: activePinId 以外解除
    const markers = document.querySelectorAll(".map-marker");
    markers.forEach(m => {
      if (m.getAttribute("data-id") !== activePinId) {
        m.classList.remove("is-active");
      }
    });
  }

  return {
    init,
    render,
    updateProperties,
    setActivePin,
    highlightProperty,
    clearHighlight,
    computeScreenClusters,
    getClusterAreaName,
    panToLocation,
    closeGoogleInfoWindow,
    closeMobileMapCard
  };
})();
