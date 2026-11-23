package jp.machiriku.servlet.management;

import static jp.machiriku.servlet.management.ManagementDisplay.options;

import java.sql.SQLException;
import java.util.Map;

import jp.machiriku.bean.management.AdminCatalogView;
import jp.machiriku.dao.management.AdminCatalogDao;
import jp.machiriku.dao.management.ManagementDao;

/** 業種の分類と、業種別に案内する必要資格・許認可・書類を管理する。 */
final class AdminCatalog {
    /** 資格と書類の案内を一つのマスターで扱う種類。 */
    private static final Map<String, String> QUALIFICATION_KINDS = options("QUALIFICATION", "資格", "PERMIT", "許認可", "DOCUMENT", "書類");

    /** マスター処理を静的に利用する。 */
    private AdminCatalog() { }

    /**
     * 業種またはカテゴリの一覧、登録、編集で使うデータを取得する。
     * @param db マスターを照会するDAO接続。
     * @param input マスターの種類、編集IDまたは検索条件。
     * @return 選んだ操作に対応するJSPとデータ。
     * @throws SQLException 業種を取得できなかった場合。
     */
    static ManagementView industries(ManagementDao db, ManagementRequest input) throws SQLException {
        // 区分に応じて、業種とカテゴリを別のマスターとして照会する。
        String kind = input.value("kind").isEmpty() ? "type" : input.choice("kind", "管理する項目", "category", "type");
        boolean category = "category".equals(kind);
        Integer id = input.id("id", false);
        if (id != null || "1".equals(input.value("new"))) {
            // 新規入力と既存レコードの編集では、同じ画面に初期値だけを切り替えて渡す。
            var current = id == null ? Map.<String, Object>of() : AdminCatalogDao.industry(db, category, id);
            var categories = category ? Map.<String, String>of() : ManagementDisplay.choices(AdminCatalogDao.categoryChoices(db));
            return new ManagementView("admin/industries/edit.jsp", "業種管理",
                    new AdminCatalogView.IndustryEdit(kind, id, current, categories));
        }

        // 検索条件を表示と検索に共用し、一覧の続きがあるかを一行余分に取得して判定する。
        String query = input.search();
        String status = input.value("status");
        var rows = AdminCatalogDao.industries(db, category, query, status, ManagementRequest.PAGE_SIZE + 1, input.offset());
        return new ManagementView("admin/industries/list.jsp", "業種管理",
                new AdminCatalogView.IndustryList(kind, query, status, input.pageRows(rows)));
    }

    /**
     * 管理者が指定した業種の名称・表示順・有効状態を保存する。
     * @param db トランザクション内の接続。
     * @param input 業種管理フォーム。
     * @param actorId 実行者の会員ID。
     * @return 保存後の参照先。
     * @throws SQLException DB更新に失敗した場合。
     */
    static String saveIndustry(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        input.choice("operation", "操作", "save");
        String kind = input.choice("kind", "管理する項目", "category", "type");
        boolean category = "category".equals(kind);
        Integer id = input.id("id", false);
        String name = input.text("name", "名称", category ? 50 : 80, true);
        int order = input.number("order", "表示順", 1, Integer.MAX_VALUE);
        boolean active = Boolean.parseBoolean(input.choice("active", "有効状態", "true", "false"));
        Integer categoryId = category ? null : input.id("categoryId", true);
        if (categoryId != null) { AdminCatalogDao.requireCategory(db, categoryId); }
        String action = id == null ? "CREATE" : "UPDATE";
        String key = category ? "category_id" : "industry_type_id";
        // 追加時だけコードを入力させ、過去の履歴や既存の参照値を保持する。
        var after = new java.util.LinkedHashMap<String, Object>();
        after.put("name", name);
        after.put("display_order", order);
        after.put("is_active", active);
        if (categoryId != null) { after.put("category_id", categoryId); }
        if (id == null) {
            String code = input.text("code", "コード", category ? 3 : 5, true);
            if (!code.matches(category ? "[A-Z0-9]{3}" : "I[0-9]{4}")) { throw new ManagementProblem(400, "コードの形式を確認してください。"); }
            var inserted = category ? AdminCatalogDao.insertCategory(db, code, name, order, active) : AdminCatalogDao.insertIndustry(db, categoryId, code, name, order, active);
            id = (Integer) inserted.get(key);
            after.put("code", code);
            db.audit(actorId, action, category ? "INDUSTRY_CATEGORY" : "INDUSTRY_TYPE", Map.of(key, id), null, Map.of("after", after));
        } else {
            var before = AdminCatalogDao.lockIndustryEntry(db, category, id);
            if (category) {
                AdminCatalogDao.updateCategory(db, name, order, active, id);
            } else {
                AdminCatalogDao.updateIndustry(db, categoryId, name, order, active, id);
            }
            db.audit(actorId, action, category ? "INDUSTRY_CATEGORY" : "INDUSTRY_TYPE", Map.of(key, id), null, Map.of("before", before, "after", after));
        }
        return ManagementPaths.ADMIN_INDUSTRIES + "?kind=" + kind + "&id=" + id;
    }

    /**
     * 資格・書類マスターの管理、または業種別の必要条件を表示する。
     * @param db マスターと業種との関係を参照するDAO接続。
     * @param input タブ、編集対象ID、一覧の検索条件。
     * @return 資格・書類管理のJSPと表示データ。
     * @throws SQLException 資格・書類を取得できなかった場合。
     */
    static ManagementView qualifications(ManagementDao db, ManagementRequest input) throws SQLException {
        // 業種別の必要条件を選択した場合は、紐付けに必要な値を取得する。
        if ("requirements".equals(input.value("tab"))) {
            return requirements(db, input);
        }
        Integer id = input.id("id", false);
        if (id != null || "1".equals(input.value("new"))) {
            var current = id == null ? Map.<String, Object>of() : AdminCatalogDao.qualification(db, id);
            return new ManagementView("admin/qualifications/edit.jsp", "必要資格・書類管理",
                    new AdminCatalogView.QualificationEdit(id, current, QUALIFICATION_KINDS));
        }

        // マスター一覧では、検索で選んだ条件と表示用に変換した種別名を渡す。
        String kind = input.value("kind");
        String query = input.search();
        String status = input.value("status");
        var rows = AdminCatalogDao.qualifications(db, kind, status, query, ManagementRequest.PAGE_SIZE + 1, input.offset());
        for (var row : rows) {
            row.computeIfPresent("種類", (key, value) -> QUALIFICATION_KINDS.get(value.toString()));
        }
        return new ManagementView("admin/qualifications/list.jsp", "必要資格・書類管理",
                new AdminCatalogView.QualificationList(kind, query, status, QUALIFICATION_KINDS, input.pageRows(rows)));
    }

    /**
     * 選択された業種に必要な資格・書類と、その設定内容を取得する。
     * @param db 業種と要件を照会するDAO接続。
     * @param input 選択された業種IDと資格ID。
     * @return 業種別設定のJSPと、選択内容に対応するデータ。
     * @throws SQLException 業種または要件を取得できなかった場合。
     */
    private static ManagementView requirements(ManagementDao db, ManagementRequest input) throws SQLException {
        // 業種が選ばれる前は、選択用の業種一覧を取得する。
        Integer industry = input.id("industry", false);
        Integer id = input.id("id", false);
        var industries = ManagementDisplay.choices(AdminCatalogDao.industryChoices(db));
        if (industry == null) {
            return new ManagementView("admin/qualifications/requirements.jsp", "必要資格・書類管理",
                    new AdminCatalogView.Requirements(null, industries, null, null, Map.of(), Map.of()));
        }

        // 業種と資格の組を確認し、設定済みの必須区分や条件を読み出す。
        AdminCatalogDao.requireIndustry(db, industry);
        var rows = input.pageRows(AdminCatalogDao.requirements(db, industry, ManagementRequest.PAGE_SIZE + 1, input.offset()));
        var current = id == null ? Map.<String, Object>of() : AdminCatalogDao.requirement(db, industry, id);
        var choices = ManagementDisplay.choices(AdminCatalogDao.requirementChoices(db, industry));
        return new ManagementView("admin/qualifications/requirements.jsp", "必要資格・書類管理",
                new AdminCatalogView.Requirements(industry, industries, rows, id, current, choices));
    }

    /**
     * 資格・書類マスターか業種別要件を更新する。
     * @param db トランザクション内の接続。
     * @param input 編集フォーム。
     * @param actorId 操作した管理者の会員ID。
     * @return 更新後の画面URL。
     * @throws SQLException 更新できなかった場合。
     */
    static String saveQualification(ManagementDao db, ManagementRequest input, int actorId) throws SQLException {
        String operation = input.choice("operation", "操作", "save", "requirement", "removeRequirement");
        if (!"save".equals(operation)) { return saveRequirement(db, input, actorId, "removeRequirement".equals(operation)); }
        Integer id = input.id("id", false);
        String name = input.text("name", "名称", 150, true);
        String kind = input.choice("kind", "種類", "QUALIFICATION", "PERMIT", "DOCUMENT");
        boolean active = Boolean.parseBoolean(input.choice("active", "有効状態", "true", "false"));
        String guidance = input.text("guidance", "説明", 8000, false);
        String url = input.webUrl("url", "案内・様式URL");
        if (id == null) {
            id = (Integer) AdminCatalogDao.insertQualification(db, name, kind, guidance, url, active).get("qualification_id");
            db.audit(actorId, "CREATE", "QUALIFICATION", Map.of("qualification_id", id), null, Map.of("name", name, "kind", kind, "is_active", active));
        } else {
            var before = AdminCatalogDao.lockQualification(db, id);
            if (!kind.equals(before.get("requirement_kind")) && !AdminCatalogDao.businessQualificationUsage(db, id).isEmpty()) {
                throw new ManagementProblem(409, "保有資格として登録されている項目の種類は変更できません。");
            }
            AdminCatalogDao.updateQualification(db, name, kind, guidance, url, active, id);
            db.audit(actorId, "UPDATE", "QUALIFICATION", Map.of("qualification_id", id), null,
                    Map.of("before", before, "after", Map.of("qualification_name", name, "requirement_kind", kind, "is_active", active)));
        }
        return ManagementPaths.ADMIN_QUALIFICATIONS + "?id=" + id;
    }

    /**
     * 業種と資格・書類の組を登録または解除する。
     * @param db トランザクション内の接続。
     * @param input 業種・資格・必須区分などの入力。
     * @param actorId 管理者の会員ID。
     * @param remove 関連付けを解除する場合にtrue。
     * @return 業種別要件の画面URL。
     * @throws SQLException 変更を保存できなかった場合。
     */
    private static String saveRequirement(ManagementDao db, ManagementRequest input, int actorId, boolean remove) throws SQLException {
        int industry = input.id("industry", true);
        int qualification = input.id("qualificationId", true);
        // 同じ業種の条件設定を直列化し、解除と追加が競合しても履歴と食い違わないようにする。
        AdminCatalogDao.lockIndustry(db, industry);
        var before = AdminCatalogDao.lockRequirement(db, industry, qualification);
        var key = Map.of("industry_type_id", industry, "qualification_id", qualification);
        if (remove) {
            String reason = input.text("reason", "解除理由", 2000, true);
            if (before.isEmpty()) { throw new ManagementProblem(404, "この必要条件は既に解除されています。"); }
            AdminCatalogDao.deleteRequirement(db, industry, qualification);
            db.audit(actorId, "REMOVE", "INDUSTRY_REQUIREMENT", key, reason, Map.of("before", before.getFirst()));
        } else {
            if (before.isEmpty()) { AdminCatalogDao.lockActiveQualification(db, qualification); }
            boolean required = Boolean.parseBoolean(input.choice("required", "必須区分", "true", "false"));
            String condition = input.text("condition", "適用条件", 300, false);
            AdminCatalogDao.upsertRequirement(db, industry, qualification, required, condition);
            var after = new java.util.LinkedHashMap<String, Object>();
            after.put("is_required", required);
            after.put("condition_text", condition);
            db.audit(actorId, "ASSIGN", "INDUSTRY_REQUIREMENT", key, null, Map.of("before", before.isEmpty() ? Map.of() : before.getFirst(), "after", after));
        }
        return ManagementPaths.ADMIN_QUALIFICATIONS + "?tab=requirements&industry=" + industry;
    }
}
