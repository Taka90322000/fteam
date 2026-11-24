/**
 * 管理画面の生活拠点編集フォームに、会員登録画面と同じGoogle Maps選択UIを提供するスクリプト。
 * 居住地（必須）と勤務先・学校（任意・折りたたみ）の地図クリック・ピン移動・現在地取得・登録解除を扱う。
 */
(() => {
    'use strict';

    /** 生活拠点編集フォーム。 */
    const form = document.getElementById('admin-location-form');
    if (!form) {
        return;
    }

    /** 宇都宮市中心部の既定地図座標（未選択時の初期表示位置）。 */
    const DEFAULT_CENTER = { lat: 36.5610, lng: 139.8830 };

    /** 居住地（living）と勤務先・学校（work）それぞれの地図・マーカー・逆ジオコード状態。 */
    const locations = {
        living: {
            map: null,
            marker: null,
            revision: 0,
            positionRequestId: 0,
            geocodeState: 'idle',
            mapId: 'admin-living-map-picker',
            addressId: 'admin-living-address',
            latId: 'admin-living-lat',
            lngId: 'admin-living-lng',
            statusId: 'admin-living-map-status'
        },
        work: {
            map: null,
            marker: null,
            revision: 0,
            positionRequestId: 0,
            geocodeState: 'idle',
            mapId: 'admin-work-map-picker',
            addressId: 'admin-work-address',
            latId: 'admin-work-lat',
            lngId: 'admin-work-lng',
            statusId: 'admin-work-map-status'
        }
    };

    /**
     * 指定種別の隠し入力欄から有効な緯度・経度を読み取る。
     * @param {'living' | 'work'} kind 拠点種別。
     * @returns {{lat: number, lng: number} | null} 有効な座標。未設定または不正時はnull。
     */
    function readCoords(kind) {
        const loc = locations[kind];
        const latValue = document.getElementById(loc.latId).value.trim();
        const lngValue = document.getElementById(loc.lngId).value.trim();
        if (!latValue || !lngValue) {
            return null;
        }
        const lat = Number(latValue);
        const lng = Number(lngValue);
        if (Number.isFinite(lat) && Number.isFinite(lng) && Math.abs(lat) <= 90 && Math.abs(lng) <= 180) {
            return { lat, lng };
        }
        return null;
    }

    /**
     * 地図下のステータスメッセージを更新する。
     * @param {'living' | 'work'} kind 拠点種別。
     * @param {string} text 表示するメッセージ。
     */
    function updateStatus(kind, text) {
        document.getElementById(locations[kind].statusId).textContent = text;
    }

    /**
     * 指定種別の座標入力と地図上のマーカーを消去する。
     * @param {'living' | 'work'} kind 拠点種別。
     */
    function clearCoords(kind) {
        const loc = locations[kind];
        loc.revision++;
        loc.geocodeState = 'idle';
        document.getElementById(loc.latId).value = '';
        document.getElementById(loc.lngId).value = '';
        if (loc.marker) {
            loc.marker.setMap(null);
        }
        updateStatus(kind, '※地図をクリックしてピンを配置してください');
    }

    /**
     * 勤務先・学校の登録内容をクリアし、編集エリアを閉じる。
     */
    function removeWorkLocation() {
        document.getElementById(locations.work.addressId).value = '';
        clearCoords('work');
        toggleWorkMap(false);
    }

    /**
     * 地図上にドラッグ可能なピンを表示または移動する。
     * @param {'living' | 'work'} kind 拠点種別。
     * @param {{lat: number, lng: number}} point ピンの位置座標。
     */
    function showMarker(kind, point) {
        const loc = locations[kind];
        if (!loc.map) {
            return;
        }
        if (!loc.marker) {
            loc.marker = new google.maps.Marker({ map: loc.map, position: point, draggable: true });
            loc.marker.addListener('dragend', event => selectLocation(kind, {
                lat: event.latLng.lat(),
                lng: event.latLng.lng()
            }));
        } else {
            loc.marker.setMap(loc.map);
            loc.marker.setPosition(point);
        }
    }

    /**
     * 地図上で選択された座標を保持し、Google Maps Geocoderで日本語住所を取得して入力欄へ反映する。
     * @param {'living' | 'work'} kind 拠点種別。
     * @param {{lat: number, lng: number}} point 選択した座標。
     */
    function selectLocation(kind, point) {
        const loc = locations[kind];
        const revision = ++loc.revision;
        loc.geocodeState = 'loading';
        const addressInput = document.getElementById(loc.addressId);
        document.getElementById(loc.latId).value = point.lat;
        document.getElementById(loc.lngId).value = point.lng;
        addressInput.value = '';
        showMarker(kind, point);
        if (loc.map) {
            loc.map.panTo(point);
        }
        updateStatus(kind, '選択した位置の住所を確認しています…');

        if (typeof google === 'undefined' || !google.maps || !google.maps.Geocoder) {
            loc.geocodeState = 'error';
            updateStatus(kind, '住所を取得できませんでした。地図の読み込み後に選び直してください');
            return;
        }
        new google.maps.Geocoder().geocode({ location: point, language: 'ja', region: 'JP' }, (results, status) => {
            // 新しい選択や解除が先に発生した場合は古い応答を破棄する。
            if (revision !== loc.revision) {
                return;
            }
            if (status === 'OK' && results && results[0] && results[0].formatted_address) {
                loc.geocodeState = 'resolved';
                addressInput.value = results[0].formatted_address.replace(/^日本、/, '');
                updateStatus(kind, '✓ 選択中: (' + point.lat.toFixed(4) + ', ' + point.lng.toFixed(4) + ')');
            } else {
                loc.geocodeState = 'error';
                updateStatus(kind, '住所を取得できませんでした。地図上で選び直してください');
            }
        });
    }

    /**
     * 指定種別のGoogle Mapインスタンスを初期化し、既存座標がある場合はピンを復元する。
     * @param {'living' | 'work'} kind 拠点種別。
     */
    function initMap(kind) {
        const loc = locations[kind];
        if (loc.map || typeof google === 'undefined' || !google.maps || !google.maps.Map) {
            return;
        }
        const point = readCoords(kind);
        loc.map = new google.maps.Map(document.getElementById(loc.mapId), {
            center: point || DEFAULT_CENTER,
            zoom: 14,
            mapTypeControl: false,
            streetViewControl: false,
            fullscreenControl: false,
            zoomControl: false,
            gestureHandling: 'greedy',
            scrollwheel: true
        });
        loc.map.addListener('click', event => selectLocation(kind, {
            lat: event.latLng.lat(),
            lng: event.latLng.lng()
        }));
        if (point) {
            showMarker(kind, point);
            updateStatus(kind, '✓ 選択中: (' + point.lat.toFixed(4) + ', ' + point.lng.toFixed(4) + ')');
        }
    }

    /**
     * ブラウザーの現在地機能で座標を取得し、地図と住所へ反映する。
     * @param {'living' | 'work'} kind 拠点種別。
     */
    function requestCurrentLocation(kind) {
        if (!navigator.geolocation) {
            alert('現在地を取得できません。地図上で住所を選択してください');
            return;
        }
        const loc = locations[kind];
        const revision = loc.revision;
        const requestId = ++loc.positionRequestId;
        const isCurrent = () => revision === loc.revision && requestId === loc.positionRequestId;
        navigator.geolocation.getCurrentPosition(position => {
            if (!isCurrent()) {
                return;
            }
            selectLocation(kind, {
                lat: position.coords.latitude,
                lng: position.coords.longitude
            });
        }, () => {
            if (!isCurrent()) {
                return;
            }
            alert('現在地を取得できませんでした。地図上で住所を選択してください');
        }, { timeout: 7000 });
    }

    /**
     * 勤務先・学校の地図選択エリアの開閉を切り替える。
     * @param {boolean} [forceOpen] 明示的に開閉を指定する場合の真偽値。
     */
    function toggleWorkMap(forceOpen) {
        const area = document.getElementById('admin-work-map-area');
        const trigger = document.getElementById('admin-work-trigger-card');
        const shouldOpen = forceOpen === undefined ? area.style.display === 'none' : !!forceOpen;
        area.style.display = shouldOpen ? 'block' : 'none';
        trigger.style.display = shouldOpen ? 'none' : 'flex';
        if (shouldOpen) {
            initMap('work');
            const map = locations.work.map;
            if (map) {
                google.maps.event.trigger(map, 'resize');
                map.setCenter(readCoords('work') || DEFAULT_CENTER);
            }
        } else {
            const address = document.getElementById(locations.work.addressId).value.trim();
            document.getElementById('admin-work-trigger-label').textContent = address
                ? '勤務先・学校: ' + address
                : '勤務先・学校を登録する';
            document.getElementById('admin-work-trigger-icon').textContent = address ? '変更' : '＋';
        }
    }

    /**
     * フォーム送信前に居住地（必須）と勤務先・学校（任意）の選択状態・座標整合性を確認する。
     * @param {SubmitEvent} event フォーム送信イベント。
     */
    function validateForm(event) {
        const livingInput = document.getElementById(locations.living.addressId);
        if (locations.living.geocodeState === 'loading') {
            alert('選択した居住地の住所を確認しています。住所が表示されてから保存してください');
            event.preventDefault();
            return;
        }
        if (!livingInput.value.trim() || !readCoords('living')) {
            alert('居住地を地図上で選択してください（必須）');
            livingInput.focus();
            event.preventDefault();
            return;
        }

        const workInput = document.getElementById(locations.work.addressId);
        const workCoords = readCoords('work');
        if (locations.work.geocodeState === 'loading') {
            alert('選択した勤務先・学校の住所を確認しています。住所が表示されてから保存してください');
            toggleWorkMap(true);
            event.preventDefault();
            return;
        }
        if (locations.work.geocodeState === 'error') {
            alert('勤務先・学校の住所を取得できませんでした。地図上で選び直すか、「登録を解除」を押してください');
            toggleWorkMap(true);
            event.preventDefault();
            return;
        }
        if ((workInput.value.trim() && !workCoords) || (!workInput.value.trim() && workCoords)) {
            alert('勤務先・学校を登録する場合は地図上で住所を選択してください（削除する場合は「登録を解除」を押してください）');
            toggleWorkMap(true);
            workInput.focus();
            event.preventDefault();
        }
    }

    /**
     * イベントハンドラーの登録と地図の初期マウントを行う。
     */
    function init() {
        document.getElementById('admin-living-current-btn').addEventListener('click', () => requestCurrentLocation('living'));
        document.getElementById('admin-work-trigger-card').addEventListener('click', () => toggleWorkMap(true));
        document.getElementById('admin-work-current-btn').addEventListener('click', () => requestCurrentLocation('work'));
        document.getElementById('admin-work-remove-btn').addEventListener('click', removeWorkLocation);
        document.getElementById('admin-work-close-btn').addEventListener('click', () => toggleWorkMap(false));
        form.addEventListener('submit', validateForm);

        // 既に住所と座標がそろっている項目は解決済みとして初期化する。
        ['living', 'work'].forEach(kind => {
            const addr = document.getElementById(locations[kind].addressId).value.trim();
            const coords = readCoords(kind);
            if (addr && coords) {
                locations[kind].geocodeState = 'resolved';
                updateStatus(kind, '✓ 選択中: (' + coords.lat.toFixed(4) + ', ' + coords.lng.toFixed(4) + ')');
            }
        });

        // 勤務先・学校が登録済みの場合は最初から編集エリアを開いて表示する。
        const workAddress = document.getElementById(locations.work.addressId).value.trim();
        if (workAddress) {
            toggleWorkMap(true);
        }

        const mountMaps = () => {
            initMap('living');
            if (document.getElementById('admin-work-map-area').style.display !== 'none') {
                initMap('work');
            }
        };
        mountMaps();
        if (!locations.living.map) {
            let retries = 0;
            const timer = setInterval(() => {
                mountMaps();
                if (locations.living.map || ++retries > 60) {
                    clearInterval(timer);
                }
            }, 100);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
